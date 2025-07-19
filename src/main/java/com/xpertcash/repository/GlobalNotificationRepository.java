package com.xpertcash.repository;

import com.xpertcash.entity.GlobalNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GlobalNotificationRepository extends JpaRepository<GlobalNotification, Long> {
    List<GlobalNotification> findByRecipientIdOrderByCreatedAtDesc(Long userId);
}
