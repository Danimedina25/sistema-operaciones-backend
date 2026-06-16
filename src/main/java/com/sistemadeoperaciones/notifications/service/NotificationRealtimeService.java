package com.sistemadeoperaciones.notifications.service;

import com.sistemadeoperaciones.notifications.dto.NotificationResponseDto;
import com.sistemadeoperaciones.notifications.dto.UnreadCountResponseDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationRealtimeService {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationRealtimeService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendNotificationToUser(Long userId, NotificationResponseDto notification) {
        System.out.println("📤 Enviando notificación realtime a userId=" + userId);
        System.out.println("Título: " + notification.getTitulo());

        messagingTemplate.convertAndSend(
                "/topic/users/" + userId + "/notifications",
                notification
        );
    }

    public void sendUnreadCountToUser(Long userId, long unreadCount) {
        System.out.println("📤 Enviando unread-count realtime a userId=" + userId + ", count=" + unreadCount);

        messagingTemplate.convertAndSend(
                "/topic/users/" + userId + "/notifications/unread-count",
                new UnreadCountResponseDto(unreadCount)
        );
    }
}