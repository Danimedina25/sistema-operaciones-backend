package com.sistemadeoperaciones.notifications.dto;

import com.sistemadeoperaciones.notifications.enums.NotificationModule;
import com.sistemadeoperaciones.notifications.enums.NotificationPriority;
import com.sistemadeoperaciones.notifications.enums.NotificationReferenceType;
import com.sistemadeoperaciones.notifications.enums.NotificationType;

import java.time.LocalDateTime;

public class NotificationResponseDto {

    private Long id;
    private String titulo;
    private String mensaje;
    private NotificationType tipo;
    private NotificationModule modulo;
    private NotificationReferenceType referenceType;
    private Long referenceId;
    private String actionUrl;
    private NotificationPriority prioridad;
    private Boolean leida;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    public NotificationResponseDto() {
    }

    public NotificationResponseDto(
            Long id,
            String titulo,
            String mensaje,
            NotificationType tipo,
            NotificationModule modulo,
            NotificationReferenceType referenceType,
            Long referenceId,
            String actionUrl,
            NotificationPriority prioridad,
            Boolean leida,
            LocalDateTime readAt,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.tipo = tipo;
        this.modulo = modulo;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.actionUrl = actionUrl;
        this.prioridad = prioridad;
        this.leida = leida;
        this.readAt = readAt;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public NotificationType getTipo() {
        return tipo;
    }

    public NotificationModule getModulo() {
        return modulo;
    }

    public NotificationReferenceType getReferenceType() {
        return referenceType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public NotificationPriority getPrioridad() {
        return prioridad;
    }

    public Boolean getLeida() {
        return leida;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public void setTipo(NotificationType tipo) {
        this.tipo = tipo;
    }

    public void setModulo(NotificationModule modulo) {
        this.modulo = modulo;
    }

    public void setReferenceType(NotificationReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    public void setPrioridad(NotificationPriority prioridad) {
        this.prioridad = prioridad;
    }

    public void setLeida(Boolean leida) {
        this.leida = leida;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}