package com.sistemadeoperaciones.usuarios.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class UpdateUserRequestDto {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
    private String nombre;

    @NotNull(message = "El rol es obligatorio")
    private Long roleId;

    private Boolean activo;

    @DecimalMin(value = "0.00", inclusive = false, message = "El porcentaje de comisión debe ser mayor a 0")
    @DecimalMax(value = "100.00", inclusive = true, message = "El porcentaje de comisión no puede ser mayor a 100")
    private BigDecimal commissionPercentage;

    private Boolean appliesToNetwork = true;

    public UpdateUserRequestDto() {
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public BigDecimal getCommissionPercentage() {
        return commissionPercentage;
    }

    public void setCommissionPercentage(BigDecimal commissionPercentage) {
        this.commissionPercentage = commissionPercentage;
    }

    public Boolean getAppliesToNetwork() {
        return appliesToNetwork;
    }

    public void setAppliesToNetwork(Boolean appliesToNetwork) {
        this.appliesToNetwork = appliesToNetwork;
    }
}