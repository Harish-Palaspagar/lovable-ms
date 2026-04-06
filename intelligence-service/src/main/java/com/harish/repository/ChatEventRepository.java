package com.harish.repository;
import com.harish.entity.ChatEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatEventRepository extends JpaRepository<ChatEvent, Long> {

    Optional<ChatEvent> findBySagaId(String s);

}
