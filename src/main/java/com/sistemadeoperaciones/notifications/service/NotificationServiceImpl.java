package com.sistemadeoperaciones.notifications.service;

import com.sistemadeoperaciones.notifications.dto.NotificationResponseDto;
import com.sistemadeoperaciones.notifications.dto.UnreadCountResponseDto;
import com.sistemadeoperaciones.notifications.enums.NotificationModule;
import com.sistemadeoperaciones.notifications.enums.NotificationPriority;
import com.sistemadeoperaciones.notifications.enums.NotificationReferenceType;
import com.sistemadeoperaciones.notifications.enums.NotificationType;
import com.sistemadeoperaciones.notifications.models.Notification;
import com.sistemadeoperaciones.notifications.models.UserNotification;
import com.sistemadeoperaciones.notifications.repository.NotificationRepository;
import com.sistemadeoperaciones.notifications.repository.UserNotificationRepository;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.enums.RoleName;
import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;
import com.sistemadeoperaciones.usuarios.model.User;
import com.sistemadeoperaciones.usuarios.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final NotificationRealtimeService notificationRealtimeService;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            UserNotificationRepository userNotificationRepository,
            UserRepository userRepository,
            AuthenticatedUserService authenticatedUserService,
            NotificationRealtimeService notificationRealtimeService
    ) {
        this.notificationRepository = notificationRepository;
        this.userNotificationRepository = userNotificationRepository;
        this.userRepository = userRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.notificationRealtimeService = notificationRealtimeService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> findMyNotifications() {
        User currentUser = authenticatedUserService.getCurrentUser();

        return userNotificationRepository.findByUsuarioIdAndArchivadaFalseOrderByNotificationCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> findMyLatestNotifications(int limit) {
        User currentUser = authenticatedUserService.getCurrentUser();

        if (limit <= 0) {
            limit = 10;
        }

        if (limit > 50) {
            limit = 50;
        }

        List<UserNotification> notifications = userNotificationRepository
                .findByUsuarioIdAndArchivadaFalseOrderByNotificationCreatedAtDesc(currentUser.getId());

        return notifications.stream()
                .limit(limit)
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponseDto countMyUnreadNotifications() {
        User currentUser = authenticatedUserService.getCurrentUser();

        long unreadCount = userNotificationRepository
                .countByUsuarioIdAndLeidaFalseAndArchivadaFalse(currentUser.getId());

        return new UnreadCountResponseDto(unreadCount);
    }

    @Override
    @Transactional
    public NotificationResponseDto markAsRead(Long userNotificationId) {
        User currentUser = authenticatedUserService.getCurrentUser();

        UserNotification userNotification = userNotificationRepository
                .findByIdAndUsuarioId(userNotificationId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notificación no encontrada con id: " + userNotificationId));

        if (!Boolean.TRUE.equals(userNotification.getLeida())) {
            userNotification.setLeida(true);
            userNotification.setReadAt(LocalDateTime.now());
            userNotificationRepository.save(userNotification);
        }

        long unreadCount = userNotificationRepository
                .countByUsuarioIdAndLeidaFalseAndArchivadaFalse(currentUser.getId());

        notificationRealtimeService.sendUnreadCountToUser(currentUser.getId(), unreadCount);

        return mapToResponse(userNotification);
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        User currentUser = authenticatedUserService.getCurrentUser();

        List<UserNotification> unreadNotifications = userNotificationRepository
                .findByUsuarioIdAndLeidaFalseAndArchivadaFalse(currentUser.getId());

        LocalDateTime now = LocalDateTime.now();

        unreadNotifications.forEach(notification -> {
            notification.setLeida(true);
            notification.setReadAt(now);
        });

        userNotificationRepository.saveAll(unreadNotifications);

        notificationRealtimeService.sendUnreadCountToUser(currentUser.getId(), 0);
    }

    @Override
    @Transactional
    public void createForUser(
            Long userId,
            String titulo,
            String mensaje,
            NotificationType tipo,
            NotificationModule modulo,
            NotificationReferenceType referenceType,
            Long referenceId,
            String actionUrl,
            NotificationPriority prioridad
    ) {
        createForUsers(
                List.of(userId),
                titulo,
                mensaje,
                tipo,
                modulo,
                referenceType,
                referenceId,
                actionUrl,
                prioridad
        );
    }

    @Override
    @Transactional
    public void createForUsers(
            List<Long> userIds,
            String titulo,
            String mensaje,
            NotificationType tipo,
            NotificationModule modulo,
            NotificationReferenceType referenceType,
            Long referenceId,
            String actionUrl,
            NotificationPriority prioridad
    ) {
        System.out.println("=== createForUsers ejecutado ===");
        System.out.println("userIds recibidos: " + userIds);

        if (userIds == null || userIds.isEmpty()) {
            System.out.println("⚠️ userIds vacío o null, no se enviará nada");
            return;
        }

        Set<Long> uniqueUserIds = new LinkedHashSet<>(userIds);

        List<User> recipients = userRepository.findAllById(uniqueUserIds);
        if (recipients.isEmpty()) {
            System.out.println("⚠️ No se encontraron recipients en BD");
            return;
        }

        System.out.println("✅ Recipients encontrados: " + recipients.size());

        User createdBy = null;
        try {
            createdBy = authenticatedUserService.getCurrentUser();
            System.out.println("Notificación creada por userId=" + createdBy.getId());
        } catch (Exception ignored) {
            System.out.println("⚠️ No hay usuario autenticado creando la notificación");
        }

        Notification notification = new Notification();
        notification.setTitulo(titulo);
        notification.setMensaje(mensaje);
        notification.setTipo(tipo);
        notification.setModulo(modulo);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notification.setActionUrl(actionUrl);
        notification.setPrioridad(prioridad);
        notification.setCreatedBy(createdBy);

        Notification savedNotification = notificationRepository.save(notification);
        System.out.println("✅ Notification guardada con id=" + savedNotification.getId());

        List<UserNotification> userNotifications = recipients.stream()
                .map(user -> {
                    UserNotification userNotification = new UserNotification();
                    userNotification.setNotification(savedNotification);
                    userNotification.setUsuario(user);
                    userNotification.setLeida(false);
                    userNotification.setArchivada(false);
                    return userNotification;
                })
                .toList();

        List<UserNotification> savedUserNotifications = userNotificationRepository.saveAll(userNotifications);
        System.out.println("✅ UserNotifications guardadas: " + savedUserNotifications.size());

        for (UserNotification userNotification : savedUserNotifications) {
            NotificationResponseDto dto = mapToResponse(userNotification);

            Long recipientUserId = userNotification.getUsuario().getId();
            System.out.println("➡️ Procesando envío realtime para recipientUserId=" + recipientUserId);

            notificationRealtimeService.sendNotificationToUser(recipientUserId, dto);

            long unreadCount = userNotificationRepository
                    .countByUsuarioIdAndLeidaFalseAndArchivadaFalse(recipientUserId);

            notificationRealtimeService.sendUnreadCountToUser(recipientUserId, unreadCount);
        }
    }

    @Override
    @Transactional
    public void createForRoles(
            List<RoleName> roles,
            String titulo,
            String mensaje,
            NotificationType tipo,
            NotificationModule modulo,
            NotificationReferenceType referenceType,
            Long referenceId,
            String actionUrl,
            NotificationPriority prioridad
    ) {
        if (roles == null || roles.isEmpty()) {
            return;
        }

        List<User> recipients = userRepository.findDistinctByRoles_NameInAndActivoTrue(roles);

        if (recipients.isEmpty()) {
            return;
        }

        List<Long> userIds = recipients.stream()
                .map(User::getId)
                .toList();

        createForUsers(
                userIds,
                titulo,
                mensaje,
                tipo,
                modulo,
                referenceType,
                referenceId,
                actionUrl,
                prioridad
        );
    }

    private NotificationResponseDto mapToResponse(UserNotification userNotification) {
        Notification notification = userNotification.getNotification();

        return new NotificationResponseDto(
                userNotification.getId(),
                notification.getTitulo(),
                notification.getMensaje(),
                notification.getTipo(),
                notification.getModulo(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.getActionUrl(),
                notification.getPrioridad(),
                userNotification.getLeida(),
                userNotification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}