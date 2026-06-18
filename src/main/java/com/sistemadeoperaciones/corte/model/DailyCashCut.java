package com.sistemadeoperaciones.corte.model;

import com.sistemadeoperaciones.corte.enums.DailyCashCutStatus;
import com.sistemadeoperaciones.usuarios.model.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "daily_cash_cuts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_daily_cash_cut_fecha",
                        columnNames = "fecha"
                )
        }
)
public class DailyCashCut {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    // ==========================
    // SALDOS
    // ==========================

    @Column(name = "saldo_inicial", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoInicial = BigDecimal.ZERO;

    @Column(name = "saldo_final", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoFinal = BigDecimal.ZERO;

    // ==========================
    // ENTRADAS
    // ==========================

    @Column(name = "entradas_transferencia", nullable = false, precision = 15, scale = 2)
    private BigDecimal entradasTransferencia = BigDecimal.ZERO;

    @Column(name = "entradas_deposito", nullable = false, precision = 15, scale = 2)
    private BigDecimal entradasDeposito = BigDecimal.ZERO;

    @Column(name = "entradas_efectivo", nullable = false, precision = 15, scale = 2)
    private BigDecimal entradasEfectivo = BigDecimal.ZERO;

    @Column(name = "total_entradas", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalEntradas = BigDecimal.ZERO;

    // ==========================
    // RETORNOS A CLIENTES
    // ==========================

    @Column(name = "retornos_transferencia", nullable = false, precision = 15, scale = 2)
    private BigDecimal retornosTransferencia = BigDecimal.ZERO;

    @Column(name = "retornos_deposito", nullable = false, precision = 15, scale = 2)
    private BigDecimal retornosDeposito = BigDecimal.ZERO;

    @Column(name = "retornos_efectivo", nullable = false, precision = 15, scale = 2)
    private BigDecimal retornosEfectivo = BigDecimal.ZERO;

    @Column(name = "total_retornos", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalRetornos = BigDecimal.ZERO;

    // ==========================
    // COMISIONES SOCIOS
    // ==========================
    @Column(name = "total_comisiones_socios", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalComisionesSocios = BigDecimal.ZERO;

    // ==========================
    // COMISIONES OFICINA
    // ==========================

    @Column(name = "comisiones_oficina_transferencia", nullable = false, precision = 15, scale = 2)
    private BigDecimal comisionesOficinaTransferencia = BigDecimal.ZERO;

    @Column(name = "comisiones_oficina_deposito", nullable = false, precision = 15, scale = 2)
    private BigDecimal comisionesOficinaDeposito = BigDecimal.ZERO;

    @Column(name = "comisiones_oficina_efectivo", nullable = false, precision = 15, scale = 2)
    private BigDecimal comisionesOficinaEfectivo = BigDecimal.ZERO;

    @Column(name = "total_comisiones_oficina", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalComisionesOficina = BigDecimal.ZERO;

    // ==========================
    // SALIDAS
    // ==========================

    @Column(name = "total_salidas", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalSalidas = BigDecimal.ZERO;

    // ==========================
    // AUDITORIA
    // ==========================

    @Column(length = 500)
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generado_por")
    private User generadoPor;

    @Column(name = "cerrado_automaticamente", nullable = false)
    private Boolean cerradoAutomaticamente = true;

    @Column(name = "fecha_cierre", nullable = false)
    private LocalDateTime fechaCierre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DailyCashCutStatus estatus;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {

        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (fechaCierre == null) {
            fechaCierre = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public DailyCashCut() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BigDecimal getComisionesOficinaTransferencia() {
        return comisionesOficinaTransferencia;
    }

    public void setComisionesOficinaTransferencia(BigDecimal comisionesOficinaTransferencia) {
        this.comisionesOficinaTransferencia = comisionesOficinaTransferencia;
    }

    public BigDecimal getComisionesOficinaDeposito() {
        return comisionesOficinaDeposito;
    }

    public void setComisionesOficinaDeposito(BigDecimal comisionesOficinaDeposito) {
        this.comisionesOficinaDeposito = comisionesOficinaDeposito;
    }

    public BigDecimal getComisionesOficinaEfectivo() {
        return comisionesOficinaEfectivo;
    }

    public void setComisionesOficinaEfectivo(BigDecimal comisionesOficinaEfectivo) {
        this.comisionesOficinaEfectivo = comisionesOficinaEfectivo;
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

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public User getGeneradoPor() {
        return generadoPor;
    }

    public void setGeneradoPor(User generadoPor) {
        this.generadoPor = generadoPor;
    }

    public Boolean getCerradoAutomaticamente() {
        return cerradoAutomaticamente;
    }

    public void setCerradoAutomaticamente(Boolean cerradoAutomaticamente) {
        this.cerradoAutomaticamente = cerradoAutomaticamente;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public DailyCashCutStatus getEstatus() {
        return estatus;
    }

    public void setEstatus(DailyCashCutStatus estatus) {
        this.estatus = estatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}