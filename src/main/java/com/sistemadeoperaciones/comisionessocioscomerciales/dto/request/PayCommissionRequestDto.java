package com.sistemadeoperaciones.comisionessocioscomerciales.dto.request;

public class PayCommissionRequestDto {

    private String paymentProofUrl;

    public PayCommissionRequestDto() {
    }

    public String getPaymentProofUrl() {
        return paymentProofUrl;
    }

    public void setPaymentProofUrl(String paymentProofUrl) {
        this.paymentProofUrl = paymentProofUrl;
    }
}