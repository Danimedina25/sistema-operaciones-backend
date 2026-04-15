package com.sistemadeoperaciones.pagos.model;

import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.pagos.enums.PaymentStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.usuarios.model.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "operation_payments")
public class OperationPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operacion_id", nullable = false)
    private PaymentOperation operacion;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pago", nullable = false, length = 30)
    private PaymentType tipoPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_destino_id", nullable = false)
    private BankAccount cuentaDestino;

    @Column(name = "comprobante_url", nullable = false, length = 500)
    private String comprobanteUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus estatus;

    @Column(length = 500)
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por", nullable = false)
    private User registradoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validado_por")
    private User validadoPor;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDateTime fechaPago;

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.fechaPago == null) {
            this.fechaPago = LocalDateTime.now();
        }

        if (this.estatus == null) {
            this.estatus = PaymentStatus.PENDIENTE_VALIDACION;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public OperationPayment() {
    }

    public Long getId() {
        return id;
    }

    public PaymentOperation getOperacion() {
        return operacion;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public PaymentType getTipoPago() {
        return tipoPago;
    }

    public BankAccount getCuentaDestino() {
        return cuentaDestino;
    }

    public String getComprobanteUrl() {
        return comprobanteUrl;
    }

    public PaymentStatus getEstatus() {
        return estatus;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public User getRegistradoPor() {
        return registradoPor;
    }

    public User getValidadoPor() {
        return validadoPor;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public LocalDateTime getFechaValidacion() {
        return fechaValidacion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOperacion(PaymentOperation operacion) {
        this.operacion = operacion;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public void setTipoPago(PaymentType tipoPago) {
        this.tipoPago = tipoPago;
    }

    public void setCuentaDestino(BankAccount cuentaDestino) {
        this.cuentaDestino = cuentaDestino;
    }

    public void setComprobanteUrl(String comprobanteUrl) {
        this.comprobanteUrl = comprobanteUrl;
    }

    public void setEstatus(PaymentStatus estatus) {
        this.estatus = estatus;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public void setRegistradoPor(User registradoPor) {
        this.registradoPor = registradoPor;
    }

    public void setValidadoPor(User validadoPor) {
        this.validadoPor = validadoPor;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }

    public void setFechaValidacion(LocalDateTime fechaValidacion) {
        this.fechaValidacion = fechaValidacion;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}