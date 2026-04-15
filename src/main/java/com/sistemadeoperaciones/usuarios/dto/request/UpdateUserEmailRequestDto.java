package com.sistemadeoperaciones.usuarios.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateUserEmailRequestDto {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 150, message = "El correo no puede exceder 150 caracteres")
    private String correo;

    public UpdateUserEmailRequestDto() {
    }

    public UpdateUserEmailRequestDto(String correo) {
        this.correo = correo;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }
}