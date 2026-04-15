package com.sistemadeoperaciones.usuarios.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class UpdateCommercialPartnerSettingsRequestDto {

    @NotNull(message = "El porcentaje de comisión es obligatorio")
    @DecimalMin(value = "0.00", inclusive = false, message = "El porcentaje de comisión debe ser mayor a 0")
    @DecimalMax(value = "100.00", inclusive = true, message = "El porcentaje de comisión no puede ser mayor a 100")
    private BigDecimal commissionPercentage;

    @NotNull(message = "Debe indicar si aplica a la red")
    private Boolean appliesToNetwork;

    public UpdateCommercialPartnerSettingsRequestDto() {
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