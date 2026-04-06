package com.harish.repository;

import com.harish.entity.ChatSession;
import com.harish.entity.ChatSessionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, ChatSessionId> {

}
