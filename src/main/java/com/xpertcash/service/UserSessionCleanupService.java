package com.xpertcash.service;

import com.xpertcash.repository.UserSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserSessionCleanupService {

    @Autowired
    private UserSessionRepository userSessionRepository;

    /**
     * Nettoie automatiquement les sessions expirées
     * Exécuté tous les jours à 3h du matin
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        int deletedCount = userSessionRepository.deleteExpiredSessions(now);
        System.out.println("🧹 Nettoyage des sessions expirées : " + deletedCount + " sessions supprimées");
        
  
        LocalDateTime fiveMinutesAgo = now.minusMinutes(5);
        int orphanCount = userSessionRepository.deleteOrphanSessions(fiveMinutesAgo);
        if (orphanCount > 0) {
            System.out.println("🧹 Nettoyage des sessions orphelines : " + orphanCount + " sessions supprimées");
        }
    }
}

