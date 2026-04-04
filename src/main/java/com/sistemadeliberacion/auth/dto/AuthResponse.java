package com.sistemadeliberacion.auth.dto;

import java.util.List;

public class AuthResponse {

    private String token;
    private Long userId;
    private String correo;
    private String nombre;
    private List<String> roles;

    public AuthResponse(String token, Long userId, String correo, String nombre, List<String> roles) {
        this.token = token;
        this.userId = userId;
        this.correo = correo;
        this.nombre = nombre;
        this.roles = roles;
    }

    public String getToken() {
        return token;
    }

    public Long getUserId() {
        return userId;
    }

    public String getCorreo() {
        return correo;
    }

    public String getNombre() {
        return nombre;
    }

    public List<String> getRoles() {
        return roles;
    }
}