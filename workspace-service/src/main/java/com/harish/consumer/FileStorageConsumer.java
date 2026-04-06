package com.harish.consumer;

import com.harish.entity.ProcessedEvent;
import com.harish.event.FileStoreRequestEvent;
import com.harish.event.FileStoreResponseEvent;
import com.harish.repository.ProcessedEventRepository;
import com.harish.service.ProjectFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageConsumer {

    private final ProjectFileService projectFileService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    @KafkaListener(topics = "file-storage-request-event", groupId = "workspace-group")
    public void consumeFileEvent(FileStoreRequestEvent requestEvent) {

        log.info("=== FileStorageConsumer RECEIVED event === sagaId={}, projectId={}, filePath={}, contentLength={}",
                requestEvent.sagaId(),
                requestEvent.projectId(),
                requestEvent.filePath(),
                requestEvent.content() != null ? requestEvent.content().length() : 0);

        if (processedEventRepository.existsById(requestEvent.sagaId())) {
            log.info("Duplicate Saga detected: {}. Resending previous ACK.", requestEvent.sagaId());
            sendResponse(requestEvent, true, null);
            return;
        }

        if (requestEvent.filePath() == null || requestEvent.filePath().isBlank()) {
            log.error("Received file event with NULL/empty filePath! sagaId={}", requestEvent.sagaId());
            sendResponse(requestEvent, false, "filePath is null or empty");
            return;
        }

        if (requestEvent.content() == null) {
            log.error("Received file event with NULL content! sagaId={}, filePath={}",
                    requestEvent.sagaId(), requestEvent.filePath());
            sendResponse(requestEvent, false, "content is null");
            return;
        }

        try {
            log.info("Saving file to MinIO: projectId={}, path={}, contentLength={}",
                    requestEvent.projectId(), requestEvent.filePath(), requestEvent.content().length());
            projectFileService.saveFile(requestEvent.projectId(), requestEvent.filePath(), requestEvent.content());
            processedEventRepository.save(new ProcessedEvent(
                    requestEvent.sagaId(), LocalDateTime.now()
            ));
            log.info("File saved SUCCESSFULLY to MinIO: projectId={}, path={}",
                    requestEvent.projectId(), requestEvent.filePath());
            sendResponse(requestEvent, true, null);
        } catch (Exception e) {
            log.error("Error saving file to MinIO: projectId={}, path={}, error={}",
                    requestEvent.projectId(), requestEvent.filePath(), e.getMessage(), e);
            sendResponse(requestEvent, false, e.getMessage());
        }

    }

    private void sendResponse(FileStoreRequestEvent req, boolean success, String error) {

        FileStoreResponseEvent response = FileStoreResponseEvent.builder()
                .sagaId(req.sagaId())
                .projectId(req.projectId())
                .success(success)
                .errorMessage(error)
                .build();
        kafkaTemplate.send("file-store-responses", response);
        log.info("Sent FileStoreResponse: sagaId={}, success={}", req.sagaId(), success);

    }

}
