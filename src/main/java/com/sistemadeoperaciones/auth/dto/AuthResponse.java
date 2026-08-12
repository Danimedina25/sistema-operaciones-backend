package com.sistemadeoperaciones.auth.dto;

import java.math.BigDecimal;
import java.util.List;

public class AuthResponse {

    private String token;
    private Long userId;
    private String correo;
    private String nombre;
    private List<String> roles;
    private BigDecimal porcentajeComision;

    public AuthResponse(String token, Long userId, String correo, String nombre, List<String> roles, BigDecimal porcentajeComision) {
        this.token = token;
        this.userId = userId;
        this.correo = correo;
        this.nombre = nombre;
        this.roles = roles;
        this.porcentajeComision = porcentajeComision;
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

    public BigDecimal getPorcentajeComision() {
        return porcentajeComision;
    }
}
