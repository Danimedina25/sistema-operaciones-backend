package com.sistemadeoperaciones.notifications.repository;

import com.sistemadeoperaciones.notifications.models.UserNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    /**
     * user_notifications.notification_id es NOT NULL y sin cascada: hay que borrar
     * estas filas antes que sus notificaciones.
     */
    @Modifying
    @Query("DELETE FROM UserNotification un WHERE un.notification.id IN :notificationIds")
    void deleteByNotificationIdIn(@Param("notificationIds") Collection<Long> notificationIds);

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