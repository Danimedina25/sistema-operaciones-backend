package com.sistemadeoperaciones.comisionessocioscomerciales.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class PayCommissionBatchRequestDto {

    @NotEmpty
    private List<Long> commissionIds;

    @NotBlank
    private String paymentProofUrl;

    /** Cuenta desde la que se transfiere todo el lote. */
    @NotNull
    private Long cuentaOrigenId;

    public Long getCuentaOrigenId() {
        return cuentaOrigenId;
    }

    public void setCuentaOrigenId(Long cuentaOrigenId) {
        this.cuentaOrigenId = cuentaOrigenId;
    }

    public PayCommissionBatchRequestDto() {
    }

    public List<Long> getCommissionIds() {
        return commissionIds;
    }

    public void setCommissionIds(List<Long> commissionIds) {
        this.commissionIds = commissionIds;
    }

    public String getPaymentProofUrl() {
        return paymentProofUrl;
    }

    public void setPaymentProofUrl(String paymentProofUrl) {
        this.paymentProofUrl = paymentProofUrl;
    }
}