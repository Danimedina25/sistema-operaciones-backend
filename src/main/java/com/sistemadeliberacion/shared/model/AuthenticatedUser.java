package com.sistemadeliberacion.shared.model;
import java.util.List;

public class AuthenticatedUser {

    private final Long userId;
    private final String email;
    private final List<String> roles;

    public AuthenticatedUser(Long userId, String email, List<String> roles) {
        this.userId = userId;
        this.email = email;
        this.roles = roles;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public List<String> getRoles() {
        return roles;
    }
}