package com.sistemadeoperaciones.comisionessocioscomerciales.dto.request;

public class PayCommissionRequestDto {

    private String paymentProofUrl;

    /** Cuenta desde la que se transfiere. Obligatoria: la comisión sale de un banco. */
    private Long cuentaOrigenId;

    public Long getCuentaOrigenId() {
        return cuentaOrigenId;
    }

    public void setCuentaOrigenId(Long cuentaOrigenId) {
        this.cuentaOrigenId = cuentaOrigenId;
    }

    public PayCommissionRequestDto() {
    }

    public String getPaymentProofUrl() {
        return paymentProofUrl;
    }

    public void setPaymentProofUrl(String paymentProofUrl) {
        this.paymentProofUrl = paymentProofUrl;
    }
}