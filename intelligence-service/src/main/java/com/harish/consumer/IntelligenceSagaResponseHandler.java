package com.harish.consumer;

import com.harish.enums.ChatEventStatus;
import com.harish.event.FileStoreResponseEvent;
import com.harish.repository.ChatEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntelligenceSagaResponseHandler {

    private final ChatEventRepository chatEventRepository;

    @Transactional
    @KafkaListener(topics = "file-store-responses", groupId = "intelligence-group")
    public void handleSagaResponse(FileStoreResponseEvent response) {

        chatEventRepository.findBySagaId(response.sagaId()).ifPresent(event -> {
            if (!ChatEventStatus.PENDING.equals(event.getStatus())) {
                log.info("Response for Saga {} already handled. Skipping.", response.sagaId());
                return;
            }
            if (response.success()) {
                event.setStatus(ChatEventStatus.CONFIRMED);
                log.info("Saga {} CONFIRMED", response.sagaId());
            } else {
                log.warn("Saga {} FAILED. Deleting event.", response.sagaId());
                event.setStatus(ChatEventStatus.FAILED);
            }
        });

    }

}
