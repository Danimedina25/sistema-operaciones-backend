package com.sistemadeoperaciones.notifications.events;

import com.sistemadeoperaciones.notifications.service.NotificationRealtimeService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
public class NotificationEventListener {

    private final NotificationRealtimeService notificationRealtimeService;

    public NotificationEventListener(NotificationRealtimeService notificationRealtimeService) {
        this.notificationRealtimeService = notificationRealtimeService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreated(NotificationCreatedEvent event) {
        System.out.println("🔥 AFTER_COMMIT enviando WS a userId=" + event.userId());
        notificationRealtimeService.sendNotificationToUser(
                event.userId(),
                event.notification()
        );

        notificationRealtimeService.sendUnreadCountToUser(
                event.userId(),
                event.unreadCount()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUnreadCountChanged(NotificationUnreadCountChangedEvent event) {
        notificationRealtimeService.sendUnreadCountToUser(
                event.userId(),
                event.unreadCount()
        );
    }
}