package com.sistemadeoperaciones.notifications.controller;

import com.sistemadeoperaciones.notifications.dto.NotificationResponseDto;
import com.sistemadeoperaciones.notifications.dto.UnreadCountResponseDto;
import com.sistemadeoperaciones.notifications.service.NotificationService;
import com.sistemadeoperaciones.shared.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponseDto>>> findMyNotifications() {
        List<NotificationResponseDto> response = notificationService.findMyNotifications();

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Notificaciones obtenidas exitosamente", response, null)
        );
    }

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<List<NotificationResponseDto>>> findMyLatestNotifications(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<NotificationResponseDto> response = notificationService.findMyLatestNotifications(limit);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Últimas notificaciones obtenidas exitosamente", response, null)
        );
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponseDto>> countMyUnreadNotifications() {
        UnreadCountResponseDto response = notificationService.countMyUnreadNotifications();

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Conteo de notificaciones no leídas obtenido exitosamente", response, null)
        );
    }

    @PatchMapping("/{userNotificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponseDto>> markAsRead(
            @PathVariable Long userNotificationId
    ) {
        NotificationResponseDto response = notificationService.markAsRead(userNotificationId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Notificación marcada como leída", response, null)
        );
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        notificationService.markAllAsRead();

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Todas las notificaciones fueron marcadas como leídas", null, null)
        );
    }
}