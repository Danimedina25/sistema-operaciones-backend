package com.sistemadeoperaciones.notifications.models;

import com.sistemadeoperaciones.notifications.enums.NotificationModule;
import com.sistemadeoperaciones.notifications.enums.NotificationPriority;
import com.sistemadeoperaciones.notifications.enums.NotificationReferenceType;
import com.sistemadeoperaciones.notifications.enums.NotificationType;
import com.sistemadeoperaciones.usuarios.model.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 500)
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationModule modulo;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 50)
    private NotificationReferenceType referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "action_url", length = 255)
    private String actionUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationPriority prioridad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();

        if (this.prioridad == null) {
            this.prioridad = NotificationPriority.MEDIUM;
        }

        if (this.referenceType == null) {
            this.referenceType = NotificationReferenceType.NONE;
        }
    }

    public Notification() {
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

    public User getCreatedBy() {
        return createdBy;
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

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}