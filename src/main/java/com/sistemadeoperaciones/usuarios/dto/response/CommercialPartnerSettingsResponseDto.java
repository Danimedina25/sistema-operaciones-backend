package com.sistemadeoperaciones.usuarios.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CommercialPartnerSettingsResponseDto {

    private Long id;
    private Long userId;
    private BigDecimal commissionPercentage;
    private Boolean appliesToNetwork;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CommercialPartnerSettingsResponseDto() {
    }

    public CommercialPartnerSettingsResponseDto(Long id, Long userId, BigDecimal commissionPercentage,
                                                Boolean appliesToNetwork, LocalDateTime createdAt,
                                                LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.commissionPercentage = commissionPercentage;
        this.appliesToNetwork = appliesToNetwork;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getCommissionPercentage() {
        return commissionPercentage;
    }

    public Boolean getAppliesToNetwork() {
        return appliesToNetwork;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setCommissionPercentage(BigDecimal commissionPercentage) {
        this.commissionPercentage = commissionPercentage;
    }

    public void setAppliesToNetwork(Boolean appliesToNetwork) {
        this.appliesToNetwork = appliesToNetwork;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}