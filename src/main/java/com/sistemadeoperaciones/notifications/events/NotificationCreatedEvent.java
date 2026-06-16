package com.sistemadeoperaciones.notifications.events;

import com.sistemadeoperaciones.notifications.dto.NotificationResponseDto;

public record NotificationCreatedEvent(
        Long userId,
        NotificationResponseDto notification,
        long unreadCount
) {
}