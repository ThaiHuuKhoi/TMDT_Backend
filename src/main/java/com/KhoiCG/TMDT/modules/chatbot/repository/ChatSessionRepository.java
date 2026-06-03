package com.KhoiCG.TMDT.modules.chatbot.repository;

import com.KhoiCG.TMDT.modules.chatbot.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {

	Optional<ChatSession> findFirstByUser_IdOrderByCreatedAtDesc(Long userId);

	@Modifying
	@Query("DELETE FROM ChatSession s WHERE s.user IS NULL AND s.createdAt < :before")
	int deleteAnonymousSessionsOlderThan(LocalDateTime before);
}

