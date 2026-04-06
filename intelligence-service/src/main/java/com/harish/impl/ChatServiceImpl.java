package com.harish.impl;

import com.harish.dto.chat.ChatResponse;
import com.harish.entity.ChatMessage;
import com.harish.entity.ChatSession;
import com.harish.entity.ChatSessionId;
import com.harish.mapper.ChatMapper;
import com.harish.repository.ChatMessageRepository;
import com.harish.repository.ChatSessionRepository;
import com.harish.security.AuthUtil;
import com.harish.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final AuthUtil authUtil;
    private final ChatMapper chatMapper;

    @Override
    public List<ChatResponse> getProjectChatHistory(Long projectId) {

        Long userId = authUtil.getCurrentUserId();
        ChatSession chatSession = chatSessionRepository.getReferenceById(
                new ChatSessionId(projectId, userId)
        );
        List<ChatMessage> chatMessageList = chatMessageRepository.findByChatSession(chatSession);
        return chatMapper.fromListOfChatMessage(chatMessageList);

    }

}
