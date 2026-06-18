package com.sistemadeoperaciones.corte.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CashCutRangeResponse {

    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    private BigDecimal saldoInicial;
    private BigDecimal saldoFinal;

    private BigDecimal entradasTransferencia;
    private BigDecimal entradasDeposito;
    private BigDecimal entradasEfectivo;
    private BigDecimal totalEntradas;

    private BigDecimal retornosTransferencia;
    private BigDecimal retornosDeposito;
    private BigDecimal retornosEfectivo;
    private BigDecimal totalRetornos;

    private BigDecimal totalComisionesSocios;
    private BigDecimal totalComisionesOficina;

    private BigDecimal totalSalidas;

    private boolean incluyeDiaActualEnVivo;

    public CashCutRangeResponse() {
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public BigDecimal getSaldoInicial() {
        return saldoInicial;
    }

    public void setSaldoInicial(BigDecimal saldoInicial) {
        this.saldoInicial = saldoInicial;
    }

    public BigDecimal getSaldoFinal() {
        return saldoFinal;
    }

    public void setSaldoFinal(BigDecimal saldoFinal) {
        this.saldoFinal = saldoFinal;
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

    public BigDecimal getEntradasEfectivo() {
        return entradasEfectivo;
    }

    public void setEntradasEfectivo(BigDecimal entradasEfectivo) {
        this.entradasEfectivo = entradasEfectivo;
    }

    public BigDecimal getTotalEntradas() {
        return totalEntradas;
    }

    public void setTotalEntradas(BigDecimal totalEntradas) {
        this.totalEntradas = totalEntradas;
    }

    public BigDecimal getRetornosTransferencia() {
        return retornosTransferencia;
    }

    public void setRetornosTransferencia(BigDecimal retornosTransferencia) {
        this.retornosTransferencia = retornosTransferencia;
    }

    public BigDecimal getRetornosDeposito() {
        return retornosDeposito;
    }

    public void setRetornosDeposito(BigDecimal retornosDeposito) {
        this.retornosDeposito = retornosDeposito;
    }

    public BigDecimal getRetornosEfectivo() {
        return retornosEfectivo;
    }

    public void setRetornosEfectivo(BigDecimal retornosEfectivo) {
        this.retornosEfectivo = retornosEfectivo;
    }

    public BigDecimal getTotalRetornos() {
        return totalRetornos;
    }

    public void setTotalRetornos(BigDecimal totalRetornos) {
        this.totalRetornos = totalRetornos;
    }

    public BigDecimal getTotalComisionesSocios() {
        return totalComisionesSocios;
    }

    public void setTotalComisionesSocios(BigDecimal totalComisionesSocios) {
        this.totalComisionesSocios = totalComisionesSocios;
    }

    public BigDecimal getTotalComisionesOficina() {
        return totalComisionesOficina;
    }

    public void setTotalComisionesOficina(BigDecimal totalComisionesOficina) {
        this.totalComisionesOficina = totalComisionesOficina;
    }

    public BigDecimal getTotalSalidas() {
        return totalSalidas;
    }

    public void setTotalSalidas(BigDecimal totalSalidas) {
        this.totalSalidas = totalSalidas;
    }

    public boolean isIncluyeDiaActualEnVivo() {
        return incluyeDiaActualEnVivo;
    }

    public void setIncluyeDiaActualEnVivo(boolean incluyeDiaActualEnVivo) {
        this.incluyeDiaActualEnVivo = incluyeDiaActualEnVivo;
    }
}