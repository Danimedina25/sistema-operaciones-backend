package com.sistemadeoperaciones.notifications.service;

import com.sistemadeoperaciones.notifications.dto.NotificationResponseDto;
import com.sistemadeoperaciones.notifications.dto.UnreadCountResponseDto;
import com.sistemadeoperaciones.notifications.enums.NotificationModule;
import com.sistemadeoperaciones.notifications.enums.NotificationPriority;
import com.sistemadeoperaciones.notifications.enums.NotificationReferenceType;
import com.sistemadeoperaciones.notifications.enums.NotificationType;
import com.sistemadeoperaciones.shared.enums.RoleName;

import java.util.List;

public interface NotificationService {

    List<NotificationResponseDto> findMyNotifications();

    List<NotificationResponseDto> findMyLatestNotifications(int limit);

    UnreadCountResponseDto countMyUnreadNotifications();

    NotificationResponseDto markAsRead(Long userNotificationId);

    void markAllAsRead();

    void createForUser(
            Long userId,
            String titulo,
            String mensaje,
            NotificationType tipo,
            NotificationModule modulo,
            NotificationReferenceType referenceType,
            Long referenceId,
            String actionUrl,
            NotificationPriority prioridad
    );

    void createForUsers(
            List<Long> userIds,
            String titulo,
            String mensaje,
            NotificationType tipo,
            NotificationModule modulo,
            NotificationReferenceType referenceType,
            Long referenceId,
            String actionUrl,
            NotificationPriority prioridad
    );

    void createForRoles(
            List<RoleName> roles,
            String titulo,
            String mensaje,
            NotificationType tipo,
            NotificationModule modulo,
            NotificationReferenceType referenceType,
            Long referenceId,
            String actionUrl,
            NotificationPriority prioridad
    );
}