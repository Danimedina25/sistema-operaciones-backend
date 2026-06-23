package com.sistemadeoperaciones.pagos.model;

import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.enums.ReturnPaymentStatus;
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
     * Monto solicitado para retornar al cliente
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
     * Cuenta de la empresa desde donde sale el dinero.
     * La llena la jefa de cuentas cuando realiza el pago.
     * Nullable cuando es EFECTIVO.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_origen_id")
    private BankAccount cuentaOrigen;

    /**
     * Cuenta del cliente que recibe el dinero.
     * La indica el socio comercial cuando solicita el retorno.
     * Nullable cuando es EFECTIVO.
     */
    @Column(name = "cuenta_destino_cliente", length = 30)
    private String cuentaDestinoCliente;

    @Column(name = "cuenta_clabe_cliente", length = 30)
    private String cuentaClabeCliente;

    /**
     * Cuenta del cliente que recibe el dinero.
     * La indica el socio comercial cuando solicita el retorno.
     * Nullable cuando es EFECTIVO.
     */
    @Column(name = "cuenta_destino_titular", length = 30)
    private String cuentaDestinoTitular;

    /**
     * Cuenta del cliente que recibe el dinero.
     * La indica el socio comercial cuando solicita el retorno.
     * Nullable cuando es EFECTIVO.
     */
    @Column(name = "cuenta_destino_banco", length = 30)
    private String cuentaDestinoBanco;

    /**
     * Comprobante del retorno.
     * Lo carga la jefa de cuentas cuando realiza el pago.
     */
    @Column(name = "comprobante_url", length = 500)
    private String comprobanteUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReturnPaymentStatus estatus;

    @Column(length = 500)
    private String observaciones;

    /**
     * Usuario que solicitó el retorno.
     * Normalmente SOCIO_COMERCIAL.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitado_por", nullable = false)
    private User solicitadoPor;

    /**
     * Usuario que pagó/registró el retorno.
     * Normalmente JEFA_CAJAS o ADMIN.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pagado_por")
    private User pagadoPor;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.fechaSolicitud == null) {
            this.fechaSolicitud = LocalDateTime.now();
        }

        if (this.estatus == null) {
            this.estatus = ReturnPaymentStatus.SOLICITADO;
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

    public ReturnPaymentStatus getEstatus() {
        return estatus;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public User getSolicitadoPor() {
        return solicitadoPor;
    }

    public User getPagadoPor() {
        return pagadoPor;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
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

    public String getCuentaClabeCliente() {
        return cuentaClabeCliente;
    }

    public void setCuentaClabeCliente(String cuentaClabeCliente) {
        this.cuentaClabeCliente = cuentaClabeCliente;
    }

    public String getCuentaDestinoTitular() {
        return cuentaDestinoTitular;
    }

    public void setCuentaDestinoTitular(String cuentaDestinoTitular) {
        this.cuentaDestinoTitular = cuentaDestinoTitular;
    }

    public String getCuentaDestinoBanco() {
        return cuentaDestinoBanco;
    }

    public void setCuentaDestinoBanco(String cuentaDestinoBanco) {
        this.cuentaDestinoBanco = cuentaDestinoBanco;
    }

    public void setComprobanteUrl(String comprobanteUrl) {
        this.comprobanteUrl = comprobanteUrl;
    }

    public void setEstatus(ReturnPaymentStatus estatus) {
        this.estatus = estatus;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public void setSolicitadoPor(User solicitadoPor) {
        this.solicitadoPor = solicitadoPor;
    }

    public void setPagadoPor(User pagadoPor) {
        this.pagadoPor = pagadoPor;
    }

    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}