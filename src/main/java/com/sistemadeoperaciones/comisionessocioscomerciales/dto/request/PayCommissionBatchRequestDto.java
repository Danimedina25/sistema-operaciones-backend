package com.sistemadeoperaciones.comisionessocioscomerciales.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class PayCommissionBatchRequestDto {

    @NotEmpty
    private List<Long> commissionIds;

    @NotBlank
    private String paymentProofUrl;

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