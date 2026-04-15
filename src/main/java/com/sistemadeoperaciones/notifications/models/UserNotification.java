package com.sistemadeoperaciones.notifications.models;

import com.sistemadeoperaciones.usuarios.model.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_notifications",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_notification_unique",
                        columnNames = {"notification_id", "user_id"}
                )
        }
)
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User usuario;

    @Column(nullable = false)
    private Boolean leida;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(nullable = false)
    private Boolean archivada;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();

        if (this.leida == null) {
            this.leida = false;
        }

        if (this.archivada == null) {
            this.archivada = false;
        }
    }

    public UserNotification() {
    }

    public Long getId() {
        return id;
    }

    public Notification getNotification() {
        return notification;
    }

    public User getUsuario() {
        return usuario;
    }

    public Boolean getLeida() {
        return leida;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public Boolean getArchivada() {
        return archivada;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    public void setUsuario(User usuario) {
        this.usuario = usuario;
    }

    public void setLeida(Boolean leida) {
        this.leida = leida;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public void setArchivada(Boolean archivada) {
        this.archivada = archivada;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}