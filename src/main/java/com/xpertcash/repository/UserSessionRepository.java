package com.xpertcash.repository;

import com.xpertcash.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    
    // Trouver une session par token
    Optional<UserSession> findBySessionToken(String sessionToken);
    
    // Trouver toutes les sessions actives d'un utilisateur
    List<UserSession> findByUserUuidAndIsActiveTrue(String userUuid);
    
    // Trouver une session par deviceId et userUuid
    Optional<UserSession> findByDeviceIdAndUserUuidAndIsActiveTrue(String deviceId, String userUuid);
    
    // Trouver une session par deviceId et userUuid avec verrou pessimiste (évite les race conditions)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM UserSession s WHERE s.deviceId = :deviceId AND s.userUuid = :userUuid AND s.isActive = true")
    Optional<UserSession> findByDeviceIdAndUserUuidAndIsActiveTrueWithLock(@Param("deviceId") String deviceId, @Param("userUuid") String userUuid);
    
    // Trouver toutes les sessions d'un utilisateur (actives et inactives)
    List<UserSession> findByUserUuid(String userUuid);
    
    // Révoker toutes les sessions d'un utilisateur sauf celle spécifiée
    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.userUuid = :userUuid AND s.id != :sessionIdToKeep")
    void revokeAllSessionsExcept(@Param("userUuid") String userUuid, @Param("sessionIdToKeep") Long sessionIdToKeep);
    
    // Révoker toutes les sessions d'un utilisateur
    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.userUuid = :userUuid")
    void revokeAllSessions(@Param("userUuid") String userUuid);
    
    // Révoker une session spécifique
    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.id = :sessionId")
    void revokeSession(@Param("sessionId") Long sessionId);
    
    // Supprimer les sessions expirées
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :now")
    int deleteExpiredSessions(@Param("now") LocalDateTime now);
    
    // Compter les sessions actives d'un utilisateur
    long countByUserUuidAndIsActiveTrue(String userUuid);
    
    // Mettre à jour lastActivity sans faire de save() complet (optimisation)
    @Modifying
    @Query("UPDATE UserSession s SET s.lastActivity = :now WHERE s.id = :sessionId")
    void updateLastActivity(@Param("sessionId") Long sessionId, @Param("now") LocalDateTime now);
    
    // Trouver les sessions orphelines (sans token ou créées il y a plus de 5 minutes sans token)
    @Query("SELECT s FROM UserSession s WHERE s.sessionToken IS NULL AND s.createdAt < :threshold")
    List<UserSession> findOrphanSessions(@Param("threshold") LocalDateTime threshold);
    
    // Supprimer les sessions orphelines
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.sessionToken IS NULL AND s.createdAt < :threshold")
    int deleteOrphanSessions(@Param("threshold") LocalDateTime threshold);
    
    // Mettre à jour le token d'une session (évite un deuxième save())
    @Modifying
    @Query("UPDATE UserSession s SET s.sessionToken = :token WHERE s.id = :sessionId")
    void updateSessionToken(@Param("sessionId") Long sessionId, @Param("token") String token);
}


