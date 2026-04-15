package com.sistemadeoperaciones.usuarios.dto.request;

import jakarta.validation.constraints.NotBlank;

public class CompleteEmailVerificationRequestDto {

    @NotBlank(message = "El token es obligatorio")
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}