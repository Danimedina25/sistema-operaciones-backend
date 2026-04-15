package com.sistemadeoperaciones.usuarios.dto.response;

public class ActivateAccountResponseDto {

    private boolean activated;

    public ActivateAccountResponseDto(boolean activated) {
        this.activated = activated;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }
}