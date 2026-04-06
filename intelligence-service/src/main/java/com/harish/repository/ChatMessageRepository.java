package com.harish.repository;

import com.harish.entity.ChatMessage;
import com.harish.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
            SELECT DISTINCT m FROM ChatMessage m
            LEFT JOIN FETCH m.events e
            WHERE m.chatSession = :chatSession
            ORDER BY m.createdAt ASC, e.sequenceOrder ASC
            """)
    List<ChatMessage> findByChatSession(ChatSession chatSession);

}



