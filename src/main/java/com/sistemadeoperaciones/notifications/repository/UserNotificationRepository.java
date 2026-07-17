package com.sistemadeoperaciones.notifications.repository;

import com.sistemadeoperaciones.notifications.models.UserNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    List<UserNotification> findByUsuarioIdAndArchivadaFalseOrderByNotificationCreatedAtDesc(Long usuarioId);

    List<UserNotification> findTop10ByUsuarioIdAndArchivadaFalseOrderByNotificationCreatedAtDesc(Long usuarioId);

    long countByUsuarioIdAndLeidaFalseAndArchivadaFalse(Long usuarioId);

    long countByUsuarioId(Long usuarioId);

    Optional<UserNotification> findByIdAndUsuarioId(Long id, Long usuarioId);

    List<UserNotification> findByUsuarioIdAndLeidaFalseAndArchivadaFalse(Long usuarioId);

    @Query("""
    SELECT un
    FROM UserNotification un
    JOIN FETCH un.notification n
    WHERE un.usuario.id = :userId
    AND un.archivada = false
    ORDER BY n.createdAt DESC
""")
    List<UserNotification> findLatestByUserId(
            @Param("userId") Long userId,
            Pageable pageable
    );
}