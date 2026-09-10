package com.sistemadeoperaciones.notifications.repository;

import com.sistemadeoperaciones.notifications.enums.NotificationReferenceType;
import com.sistemadeoperaciones.notifications.models.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    long countByCreatedById(Long createdById);

    /**
     * referenceType + referenceId es un puntero suelto (sin FK) hacia la entidad de origen.
     * Se usa para limpiar las notificaciones cuyo destino se elimina y evitar enlaces rotos.
     */
    List<Notification> findByReferenceTypeAndReferenceIdIn(
            NotificationReferenceType referenceType,
            Collection<Long> referenceIds
    );
}
