package com.xpertcash.repository;

import com.xpertcash.entity.Notification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NotificationRepository extends CrudRepository<Notification, Long> {

    // 1) Récupère toutes les notifications non lues pour un user
    List<Notification> findByUserIdAndReadFalse(Long userId);

    // 2) Marque une notification comme lue (update flag)
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.read = true WHERE n.id = :id")
    void markAsRead(Long id);
}
