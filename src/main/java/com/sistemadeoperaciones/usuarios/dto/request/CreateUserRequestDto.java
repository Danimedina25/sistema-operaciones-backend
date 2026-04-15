package com.sistemadeoperaciones.usuarios.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreateUserRequestDto {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 150, message = "El correo no puede exceder 150 caracteres")
    private String correo;

    @NotNull(message = "El rol es obligatorio")
    private Long roleId;

    // Solo aplica si el rol es SOCIO_COMERCIAL
    @DecimalMin(value = "0.00", inclusive = false, message = "El porcentaje de comisión debe ser mayor a 0")
    @DecimalMax(value = "100.00", inclusive = true, message = "El porcentaje de comisión no puede ser mayor a 100")
    private BigDecimal commissionPercentage;

    // Solo aplica si el rol es SOCIO_COMERCIAL
    private Boolean appliesToNetwork = true;

    public CreateUserRequestDto() {
    }

    public CreateUserRequestDto(String nombre, String correo, Long roleId,
                                BigDecimal commissionPercentage, Boolean appliesToNetwork) {
        this.nombre = nombre;
        this.correo = correo;
        this.roleId = roleId;
        this.commissionPercentage = commissionPercentage;
        this.appliesToNetwork = appliesToNetwork;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
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