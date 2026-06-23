package com.sistemadeoperaciones.corte.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BankAccountBalanceDetailResponseDto {

    private Long bankAccountId;

    private String banco;

    private String titular;

    private String numeroCuenta;

    private String clabe;

    private LocalDate fecha;

    private BigDecimal saldoInicial;

    private BigDecimal entradasTransferencia;

    private BigDecimal entradasDeposito;

    private BigDecimal salidasRetornos;

    private BigDecimal salidasComisiones;

    private BigDecimal totalEntradas;

    private BigDecimal totalSalidas;

    private BigDecimal saldoFinal;

    public BankAccountBalanceDetailResponseDto() {
    }

    public Long getBankAccountId() {
        return bankAccountId;
    }

    public void setBankAccountId(Long bankAccountId) {
        this.bankAccountId = bankAccountId;
    }

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public String getTitular() {
        return titular;
    }

    public void setTitular(String titular) {
        this.titular = titular;
    }

    public String getNumeroCuenta() {
        return numeroCuenta;
    }

    public void setNumeroCuenta(String numeroCuenta) {
        this.numeroCuenta = numeroCuenta;
    }

    public String getClabe() {
        return clabe;
    }

    public void setClabe(String clabe) {
        this.clabe = clabe;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getSaldoInicial() {
        return saldoInicial;
    }

    public void setSaldoInicial(BigDecimal saldoInicial) {
        this.saldoInicial = saldoInicial;
    }

    public BigDecimal getEntradasTransferencia() {
        return entradasTransferencia;
    }

    public void setEntradasTransferencia(BigDecimal entradasTransferencia) {
        this.entradasTransferencia = entradasTransferencia;
    }

    public BigDecimal getEntradasDeposito() {
        return entradasDeposito;
    }

    public void setEntradasDeposito(BigDecimal entradasDeposito) {
        this.entradasDeposito = entradasDeposito;
    }

    public BigDecimal getSalidasRetornos() {
        return salidasRetornos;
    }

    public void setSalidasRetornos(BigDecimal salidasRetornos) {
        this.salidasRetornos = salidasRetornos;
    }

    public BigDecimal getSalidasComisiones() {
        return salidasComisiones;
    }

    public void setSalidasComisiones(BigDecimal salidasComisiones) {
        this.salidasComisiones = salidasComisiones;
    }

    public BigDecimal getTotalEntradas() {
        return totalEntradas;
    }

    public void setTotalEntradas(BigDecimal totalEntradas) {
        this.totalEntradas = totalEntradas;
    }

    public BigDecimal getTotalSalidas() {
        return totalSalidas;
    }

    public void setTotalSalidas(BigDecimal totalSalidas) {
        this.totalSalidas = totalSalidas;
    }

    public BigDecimal getSaldoFinal() {
        return saldoFinal;
    }

    public void setSaldoFinal(BigDecimal saldoFinal) {
        this.saldoFinal = saldoFinal;
    }
}