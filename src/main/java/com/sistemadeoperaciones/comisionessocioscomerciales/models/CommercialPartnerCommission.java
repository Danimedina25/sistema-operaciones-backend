package com.sistemadeoperaciones.comisionessocioscomerciales.models;

import com.sistemadeoperaciones.pagos.enums.CommissionStatus;
import com.sistemadeoperaciones.pagos.model.PaymentOperation;
import com.sistemadeoperaciones.socioscomerciales.models.CommercialPartner;
import com.sistemadeoperaciones.usuarios.model.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "commercial_partner_commissions",
        indexes = {
                @Index(name = "idx_cpc_status", columnList = "status"),
                @Index(name = "idx_cpc_generated_at", columnList = "generated_at"),
                @Index(name = "idx_cpc_operation", columnList = "operation_id"),
                @Index(name = "idx_cpc_user", columnList = "user_id"),
                @Index(name = "idx_cpc_partner", columnList = "commercial_partner_id")
        }
)
public class CommercialPartnerCommission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Operación origen
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operation_id", nullable = false)
    private PaymentOperation operation;

    // Nivel 1
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Nivel 2 o 3
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commercial_partner_id")
    private CommercialPartner commercialPartner;

    @Column(
            name = "commission_amount",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal commissionAmount;

    @Column(
            name = "commission_percentage",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal commissionPercentage;

    @Column(name = "base_amount",
            nullable = false,
            precision = 15,
            scale = 2)
    private BigDecimal baseAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommissionStatus status;

    @Column(name = "payment_proof_url", length = 500)
    private String paymentProofUrl;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Integer nivel;

    @PrePersist
    public void prePersist() {

        validateBeneficiary();

        generatedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (status == null) {
            status = CommissionStatus.GENERADA;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        validateBeneficiary();
    }

    private void validateBeneficiary() {

        if (user == null && commercialPartner == null) {
            throw new IllegalStateException(
                    "Debe existir un beneficiario"
            );
        }

        if (user != null && commercialPartner != null) {
            throw new IllegalStateException(
                    "Solo puede existir un beneficiario"
            );
        }
        if (nivel == null || (nivel != 1 && nivel != 2 && nivel != 3)) {
            throw new IllegalStateException(
                    "Nivel inválido"
            );
        }
        if (status == CommissionStatus.PAGADA && paidAt == null) {
            throw new IllegalStateException(
                    "La fecha de pago es obligatoria"
            );
        }
    }

    public CommercialPartnerCommission() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PaymentOperation getOperation() {
        return operation;
    }

    public void setOperation(PaymentOperation operation) {
        this.operation = operation;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public CommercialPartner getCommercialPartner() {
        return commercialPartner;
    }

    public void setCommercialPartner(CommercialPartner commercialPartner) {
        this.commercialPartner = commercialPartner;
    }

    public BigDecimal getCommissionAmount() {
        return commissionAmount;
    }

    public void setCommissionAmount(BigDecimal commissionAmount) {
        this.commissionAmount = commissionAmount;
    }

    public BigDecimal getCommissionPercentage() {
        return commissionPercentage;
    }

    public void setCommissionPercentage(BigDecimal commissionPercentage) {
        this.commissionPercentage = commissionPercentage;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public CommissionStatus getStatus() {
        return status;
    }

    public void setStatus(CommissionStatus status) {
        this.status = status;
    }

    public String getPaymentProofUrl() {
        return paymentProofUrl;
    }

    public void setPaymentProofUrl(String paymentProofUrl) {
        this.paymentProofUrl = paymentProofUrl;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Integer getNivel() {
        return nivel;
    }

    public void setNivel(Integer nivel) {
        this.nivel = nivel;
    }

    public BigDecimal getBaseAmount() {
        return baseAmount;
    }

    public void setBaseAmount(BigDecimal baseAmount) {
        this.baseAmount = baseAmount;
    }
}