package com.harish.impl;

import com.harish.client.WorkspaceClient;
import com.harish.dto.chat.StreamResponse;
import com.harish.entity.ChatEvent;
import com.harish.entity.ChatMessage;
import com.harish.entity.ChatSession;
import com.harish.entity.ChatSessionId;
import com.harish.enums.ChatEventStatus;
import com.harish.enums.ChatEventType;
import com.harish.enums.MessageRole;
import com.harish.event.FileStoreRequestEvent;
import com.harish.llm.CodeGenerationTools;
import com.harish.llm.FileTreeContextAdvisor;
import com.harish.llm.LlmResponseParser;
import com.harish.llm.PromptUtils;
import com.harish.repository.ChatEventRepository;
import com.harish.repository.ChatMessageRepository;
import com.harish.repository.ChatSessionRepository;
import com.harish.security.AuthUtil;
import com.harish.service.AiGenerationService;
import com.harish.service.UsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiGenerationServiceImpl implements AiGenerationService {

    private final ChatClient chatClient;
    private final AuthUtil authUtil;
    private final FileTreeContextAdvisor fileTreeContextAdvisor;
    private final ChatSessionRepository chatSessionRepository;
    private final LlmResponseParser llmResponseParser;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatEventRepository chatEventRepository;
    private final UsageService usageService;
    private final WorkspaceClient workspaceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @PreAuthorize("@security.canEditProject(#projectId)")
    public Flux<StreamResponse> streamResponse(String userMessage, Long projectId) {

        Long userId = authUtil.getCurrentUserId();
        ChatSession chatSession = createChatSessionIfNotExists(projectId, userId);
        StringBuilder fullResponseBuffer = new StringBuilder();
        CodeGenerationTools codeGenerationTools = new CodeGenerationTools(projectId, workspaceClient);
        AtomicReference<Long> startTime = new AtomicReference<>(System.currentTimeMillis());
        AtomicReference<Long> endTime = new AtomicReference<>(0L);
        AtomicReference<Usage> usageRef = new AtomicReference<>();
        AtomicBoolean finalized = new AtomicBoolean(false);
        return chatClient.prompt()
                .system(PromptUtils.CODE_GENERATION_SYSTEM_PROMPT)
                .user(userMessage)
                .tools(codeGenerationTools)
                .advisors(fileTreeContextAdvisor)
                .stream()
                .chatResponse()
                .map(response -> {
                    if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                        log.warn("Received null/incomplete ChatResponse for projectId={}", projectId);
                        return new StreamResponse("");
                    }
                    String text = response.getResult().getOutput().getText();
                    if (text == null) {
                        text = "";
                    }
                    if (!text.isEmpty() && endTime.get() == 0L) {
                        endTime.set(System.currentTimeMillis());
                    }
                    if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                        usageRef.set(response.getMetadata().getUsage());
                    }
                    fullResponseBuffer.append(text);
                    return new StreamResponse(text);
                })
                .filter(sr -> sr.text() != null)
                .doOnComplete(() -> {
                    scheduleFinalize(finalized, userMessage, chatSession, fullResponseBuffer, startTime, endTime, usageRef, userId);
                })
                .doOnCancel(() -> {
                    log.warn("Streaming cancelled for projectId={}, finalizing buffered response", projectId);
                    scheduleFinalize(finalized, userMessage, chatSession, fullResponseBuffer, startTime, endTime, usageRef, userId);
                })
                .doOnError(error -> log.error("Error during streaming for projectId: {}", projectId, error))
                .onErrorResume(error -> Flux.just(new StreamResponse("Something went wrong while generating the response.")));

    }

    private void scheduleFinalize(AtomicBoolean finalized, String userMessage, ChatSession chatSession,
                                  StringBuilder fullResponseBuffer, AtomicReference<Long> startTime,
                                  AtomicReference<Long> endTime, AtomicReference<Usage> usageRef, Long userId) {

        if (!finalized.compareAndSet(false, true)) {
            return;
        }
        Schedulers.boundedElastic().schedule(() -> {
            try {
                long finish = endTime.get() == 0L ? System.currentTimeMillis() : endTime.get();
                long duration = (finish - startTime.get()) / 1000;
                finalizeChats(userMessage, chatSession, fullResponseBuffer.toString(), duration, usageRef.get(), userId);
            } catch (Exception e) {
                log.error("CRITICAL: finalizeChats failed for session projectId={}, userId={}. " +
                          "LLM-generated code will NOT be persisted to MinIO!",
                        chatSession.getId().getProjectId(), userId, e);
            }
        });

    }

    private void finalizeChats(String userMessage, ChatSession chatSession, String fullText, Long duration, Usage usage, Long userId) {

        Long projectId = chatSession.getId().getProjectId();

        log.info("=== finalizeChats START === projectId={}, userId={}, fullTextLength={}",
                projectId, userId, fullText.length());

        if (fullText.isBlank()) {
            log.warn("fullResponseBuffer is EMPTY for projectId={}. Nothing to parse.", projectId);
            return;
        }

        log.debug("Full LLM response (first 500 chars): {}",
                fullText.substring(0, Math.min(500, fullText.length())));

        if (usage != null) {
            int totalTokens = usage.getTotalTokens();
            usageService.recordTokenUsage(chatSession.getId().getUserId(), totalTokens);
        }
        chatMessageRepository.save(
                ChatMessage.builder()
                        .chatSession(chatSession)
                        .role(MessageRole.USER)
                        .content(userMessage)
                        .tokensUsed(usage != null ? usage.getPromptTokens() : 0)
                        .build()
        );
        ChatMessage assistantChatMessage = ChatMessage.builder()
                .role(MessageRole.ASSISTANT)
                .content("Assistant Message here...")
                .chatSession(chatSession)
                .tokensUsed(usage != null ? usage.getCompletionTokens() : 0)
                .build();
        assistantChatMessage = chatMessageRepository.save(assistantChatMessage);

        List<ChatEvent> chatEventList = llmResponseParser.parseChatEvents(fullText, assistantChatMessage);

        log.info("Parsed {} chat events from LLM response for projectId={}", chatEventList.size(), projectId);

        long fileEditCount = chatEventList.stream()
                .filter(e -> e.getType() == ChatEventType.FILE_EDIT)
                .count();
        log.info("Found {} FILE_EDIT events for projectId={}", fileEditCount, projectId);

        if (fileEditCount == 0) {
            log.warn("No FILE_EDIT events parsed from LLM response for projectId={}. " +
                     "Checking if response contains <file> tags...", projectId);
            boolean hasFileTags = fullText.contains("<file") && fullText.contains("</file>");
            log.warn("Response contains <file> tags: {}", hasFileTags);
            if (hasFileTags) {
                log.error("LLM response CONTAINS <file> tags but parser did NOT extract them! " +
                         "Regex pattern may not match. Full response length={}", fullText.length());
                // Log the area around the first <file tag for debugging
                int fileTagIdx = fullText.indexOf("<file");
                if (fileTagIdx >= 0) {
                    int snippetEnd = Math.min(fileTagIdx + 200, fullText.length());
                    log.error("Content around first <file tag: {}", fullText.substring(fileTagIdx, snippetEnd));
                }
            }
        }

        chatEventList.addFirst(ChatEvent.builder()
                .type(ChatEventType.THOUGHT)
                .status(ChatEventStatus.CONFIRMED)
                .chatMessage(assistantChatMessage)
                .content("Thought for " + duration + "s")
                .sequenceOrder(0)
                .build());

        // Save events to DB FIRST, then send Kafka events
        chatEventRepository.saveAll(chatEventList);
        log.info("Saved {} chat events to DB for projectId={}", chatEventList.size(), projectId);

        chatEventList.stream()
                .filter(e -> e.getType() == ChatEventType.FILE_EDIT)
                .forEach(
                        e ->
                        {
                            if (e.getFilePath() == null || e.getFilePath().isBlank()) {
                                log.error("FILE_EDIT event has NULL/empty filePath! Skipping. Content preview: {}",
                                        e.getContent() != null ? e.getContent().substring(0, Math.min(100, e.getContent().length())) : "null");
                                return;
                            }
                            if (e.getContent() == null || e.getContent().isBlank()) {
                                log.error("FILE_EDIT event has NULL/empty content for path={}! Skipping.", e.getFilePath());
                                return;
                            }

                            String sagaId = UUID.randomUUID().toString();
                            e.setSagaId(sagaId);
                            chatEventRepository.save(e);

                            FileStoreRequestEvent fileStoreRequestEvent = new FileStoreRequestEvent(
                                    projectId,
                                    sagaId,
                                    e.getFilePath(),
                                    e.getContent(),
                                    userId
                            );
                            log.info("Sending storage request: sagaId={}, path={}, contentLength={}",
                                    sagaId, e.getFilePath(), e.getContent().length());
                            kafkaTemplate.send("file-storage-request-event", "project-"+projectId, fileStoreRequestEvent);
                            log.info("Storage request event SENT for: {}", e.getFilePath());
                        }
                );

        log.info("=== finalizeChats COMPLETE === projectId={}", projectId);

    }

    private ChatSession createChatSessionIfNotExists(Long projectId, Long userId) {

        ChatSessionId chatSessionId = new ChatSessionId(projectId, userId);
        ChatSession chatSession = chatSessionRepository.findById(chatSessionId).orElse(null);
        if (chatSession == null) {
            chatSession = ChatSession.builder()
                    .id(chatSessionId)
                    .build();
            chatSession = chatSessionRepository.save(chatSession);
        }
        return chatSession;

    }

}
