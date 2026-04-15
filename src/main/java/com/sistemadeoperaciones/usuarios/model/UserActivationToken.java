package com.sistemadeoperaciones.usuarios.model;

import com.sistemadeoperaciones.usuarios.enums.UserTokenType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_activation_tokens")
public class UserActivationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 255)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Boolean used = false;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserTokenType type;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.used == null) {
            this.used = false;
        }
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public Boolean getUsed() {
        return used;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void setUsed(Boolean used) {
        this.used = used;
    }

    public UserTokenType getType() {
        return type;
    }

    public void setType(UserTokenType type) {
        this.type = type;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}