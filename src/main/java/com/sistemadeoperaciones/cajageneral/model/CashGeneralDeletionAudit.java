package com.sistemadeoperaciones.cajageneral.model;

import com.sistemadeoperaciones.usuarios.model.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_general_deletion_audit")
public class CashGeneralDeletionAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "deleted_day_id", nullable = false) private Long deletedDayId;
    @Column(nullable = false) private LocalDate fecha;
    @Column(name = "saldo_inicial", precision = 15, scale = 2, nullable = false) private BigDecimal saldoInicial;
    @Column(name = "saldo_esperado", precision = 15, scale = 2, nullable = false) private BigDecimal saldoEsperado;
    @Column(name = "saldo_contado", precision = 15, scale = 2) private BigDecimal saldoContado;
    @Column(name = "movement_count", nullable = false) private int movementCount;
    @Column(nullable = false, length = 500) private String motivo;
    // Cortes bancarios regenerados al borrar el día, por los cheques cobrados que contenía.
    @Column(name = "cortes_bancarios_recalculados", nullable = false) private int cortesBancariosRecalculados;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "deleted_by", nullable = false) private User deletedBy;
    @Column(name = "deleted_at", nullable = false, updatable = false) private LocalDateTime deletedAt;
    @PrePersist void prePersist() { deletedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Long getDeletedDayId() { return deletedDayId; }
    public void setDeletedDayId(Long value) { deletedDayId = value; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate value) { fecha = value; }
    public BigDecimal getSaldoInicial() { return saldoInicial; }
    public void setSaldoInicial(BigDecimal value) { saldoInicial = value; }
    public BigDecimal getSaldoEsperado() { return saldoEsperado; }
    public void setSaldoEsperado(BigDecimal value) { saldoEsperado = value; }
    public BigDecimal getSaldoContado() { return saldoContado; }
    public void setSaldoContado(BigDecimal value) { saldoContado = value; }
    public int getMovementCount() { return movementCount; }
    public void setMovementCount(int value) { movementCount = value; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String value) { motivo = value; }
    public int getCortesBancariosRecalculados() { return cortesBancariosRecalculados; }
    public void setCortesBancariosRecalculados(int value) { cortesBancariosRecalculados = value; }
    public User getDeletedBy() { return deletedBy; }
    public void setDeletedBy(User value) { deletedBy = value; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
}
