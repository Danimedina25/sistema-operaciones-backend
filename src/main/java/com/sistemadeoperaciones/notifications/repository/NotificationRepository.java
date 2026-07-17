package com.sistemadeoperaciones.notifications.repository;

import com.sistemadeoperaciones.notifications.models.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    long countByCreatedById(Long createdById);
}