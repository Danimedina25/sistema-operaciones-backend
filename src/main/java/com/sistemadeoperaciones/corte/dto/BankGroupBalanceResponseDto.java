package com.sistemadeoperaciones.corte.dto;

import java.math.BigDecimal;
import java.util.List;

public class BankGroupBalanceResponseDto {

    private String banco;

    private BigDecimal saldoTotalBanco;

    private Integer totalCuentas;

    private List<BankAccountBalanceResponseDto> cuentas;

    public BankGroupBalanceResponseDto() {
    }

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public BigDecimal getSaldoTotalBanco() {
        return saldoTotalBanco;
    }

    public void setSaldoTotalBanco(BigDecimal saldoTotalBanco) {
        this.saldoTotalBanco = saldoTotalBanco;
    }

    public Integer getTotalCuentas() {
        return totalCuentas;
    }

    public void setTotalCuentas(Integer totalCuentas) {
        this.totalCuentas = totalCuentas;
    }

    public List<BankAccountBalanceResponseDto> getCuentas() {
        return cuentas;
    }

    public void setCuentas(List<BankAccountBalanceResponseDto> cuentas) {
        this.cuentas = cuentas;
    }
}