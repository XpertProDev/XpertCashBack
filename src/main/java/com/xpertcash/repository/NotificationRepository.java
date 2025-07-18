package com.xpertcash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 1) Récupère toutes les notifications non lues pour un user
    List<Notification> findByUserIdAndReadFalse(Long userId);

    // 2) Marque une notification comme lue (update flag)
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.read = true WHERE n.id = :id")
    void markAsRead(Long id);
}
