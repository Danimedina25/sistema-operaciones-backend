package com.sistemadeoperaciones.notifications.events;

public record NotificationUnreadCountChangedEvent(
        Long userId,
        long unreadCount
) {
}