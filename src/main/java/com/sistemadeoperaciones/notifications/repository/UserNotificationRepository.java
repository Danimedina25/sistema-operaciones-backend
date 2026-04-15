package com.sistemadeoperaciones.notifications.repository;

import com.sistemadeoperaciones.notifications.models.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    List<UserNotification> findByUsuarioIdAndArchivadaFalseOrderByNotificationCreatedAtDesc(Long usuarioId);

    List<UserNotification> findTop10ByUsuarioIdAndArchivadaFalseOrderByNotificationCreatedAtDesc(Long usuarioId);

    long countByUsuarioIdAndLeidaFalseAndArchivadaFalse(Long usuarioId);

    Optional<UserNotification> findByIdAndUsuarioId(Long id, Long usuarioId);

    List<UserNotification> findByUsuarioIdAndLeidaFalseAndArchivadaFalse(Long usuarioId);
}