package com.KhoiCG.TMDT.modules.chatbot.repository;

import com.KhoiCG.TMDT.modules.chatbot.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {
}

