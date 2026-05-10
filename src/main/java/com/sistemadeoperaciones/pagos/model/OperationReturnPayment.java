package com.sistemadeoperaciones.pagos.model;

import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.usuarios.model.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "operation_return_payments")
public class OperationReturnPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Operación a la que pertenece el retorno
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operacion_id", nullable = false)
    private PaymentOperation operacion;

    /**
     * Monto retornado al cliente
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    /**
     * TRANSFERENCIA / DEPOSITO / EFECTIVO
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pago", nullable = false, length = 30)
    private PaymentType tipoPago;

    /**
     * Cuenta de la empresa desde donde sale el dinero
     * Nullable cuando es EFECTIVO
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_origen_id")
    private BankAccount cuentaOrigen;

    /**
     * Cuenta del cliente que recibe el dinero
     * Nullable cuando es EFECTIVO
     */
    @Column(name = "cuenta_destino_cliente", length = 30)
    private String cuentaDestinoCliente;

    /**
     * Comprobante del retorno
     * Opcional para EFECTIVO
     */
    @Column(name = "comprobante_url", length = 500)
    private String comprobanteUrl;

    @Column(length = 500)
    private String observaciones;

    /**
     * Usuario que registró el retorno
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por", nullable = false)
    private User registradoPor;

    @Column(name = "fecha_retorno", nullable = false)
    private LocalDateTime fechaRetorno;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {

        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.fechaRetorno == null) {
            this.fechaRetorno = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public OperationReturnPayment() {
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

    public BankAccount getCuentaOrigen() {
        return cuentaOrigen;
    }

    public String getCuentaDestinoCliente() {
        return cuentaDestinoCliente;
    }

    public String getComprobanteUrl() {
        return comprobanteUrl;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public User getRegistradoPor() {
        return registradoPor;
    }

    public LocalDateTime getFechaRetorno() {
        return fechaRetorno;
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

    public void setCuentaOrigen(BankAccount cuentaOrigen) {
        this.cuentaOrigen = cuentaOrigen;
    }

    public void setCuentaDestinoCliente(String cuentaDestinoCliente) {
        this.cuentaDestinoCliente = cuentaDestinoCliente;
    }
    public void setComprobanteUrl(String comprobanteUrl) {
        this.comprobanteUrl = comprobanteUrl;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public void setRegistradoPor(User registradoPor) {
        this.registradoPor = registradoPor;
    }

    public void setFechaRetorno(LocalDateTime fechaRetorno) {
        this.fechaRetorno = fechaRetorno;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}