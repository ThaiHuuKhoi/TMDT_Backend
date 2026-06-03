package com.KhoiCG.TMDT.modules.chatbot.service;

import com.KhoiCG.TMDT.modules.chatbot.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionCleanupService {

    private final ChatSessionRepository chatSessionRepository;

    // Runs at 03:00 every day; deletes anonymous sessions older than 7 days
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeOldAnonymousSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        int deleted = chatSessionRepository.deleteAnonymousSessionsOlderThan(cutoff);
        if (deleted > 0) {
            log.info("Purged {} anonymous chat sessions older than {}", deleted, cutoff);
        }
    }
}
