package com.KhoiCG.TMDT.modules.chatbot.repository;

import com.KhoiCG.TMDT.modules.chatbot.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findTop20BySessionIdOrderByCreatedAtDesc(String sessionId);
}

