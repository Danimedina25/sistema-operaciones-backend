package com.sistemadeoperaciones.pagos.model;

import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.enums.ReturnInstallmentStatus;
import com.sistemadeoperaciones.usuarios.model.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Parcialidad de una solicitud de retorno.
 *
 * Una {@link OperationReturnPayment} (la "solicitud" — lo que pidió el socio) se
 * cubre con una o varias parcialidades. Cada parcialidad es un pago/entrega
 * independiente, con su propio comprobante, cuenta origen, código de retiro y
 * fechas. Se relaciona SIEMPRE con el id de una solicitud concreta, nunca por
 * operación + método (una operación puede tener dos solicitudes del mismo método).
 */
@Entity
@Table(name = "operation_return_installments")
public class OperationReturnInstallment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Solicitud de retorno a la que pertenece esta parcialidad.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private OperationReturnPayment solicitud;

    /**
     * Control optimista. La guarda principal contra sobrepago es un lock
     * pesimista sobre la operación y la solicitud; esto es un refuerzo para las
     * transiciones confirm/deliver/cancel de la propia parcialidad.
     */
    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    /**
     * Copiado de la solicitud al crear la parcialidad y validado igual.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pago", nullable = false, length = 30)
    private PaymentType tipoPago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReturnInstallmentStatus estatus;

    /**
     * Cuenta de la empresa desde donde sale el dinero (transferencia / retiro
     * sin tarjeta). Nullable para efectivo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_origen_id")
    private BankAccount cuentaOrigen;

    /**
     * Comprobante del movimiento (transferencia / depósito / cheque).
     */
    @Column(name = "comprobante_url", length = 500)
    private String comprobanteUrl;

    /**
     * Evidencia fotográfica de la entrega del efectivo.
     */
    @Column(name = "comprobante_entrega_url", length = 500)
    private String comprobanteEntregaUrl;

    /**
     * Código generado por el banco para el retiro sin tarjeta. Propio de esta
     * parcialidad — no se reutiliza entre parcialidades.
     */
    @Column(name = "codigo_retiro_sin_tarjeta", length = 40)
    private String codigoRetiroSinTarjeta;

    @Column(name = "fecha_hora_recoleccion")
    private LocalDateTime fechaHoraRecoleccion;

    /**
     * Fecha en que la parcialidad quedó efectivamente realizada/confirmada. Es la
     * fecha que cuenta para el corte de caja y los saldos bancarios.
     */
    @Column(name = "fecha_realizacion")
    private LocalDateTime fechaRealizacion;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    /**
     * Fecha en que el socio comercial confirmó la recepción del efectivo.
     */
    @Column(name = "fecha_confirmacion")
    private LocalDateTime fechaConfirmacion;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    @Column(length = 500)
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por", nullable = false)
    private User creadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "realizado_por")
    private User realizadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entregado_por")
    private User entregadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelado_por")
    private User canceladoPor;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.estatus == null) {
            this.estatus = ReturnInstallmentStatus.PROGRAMADA;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public OperationReturnInstallment() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OperationReturnPayment getSolicitud() {
        return solicitud;
    }

    public void setSolicitud(OperationReturnPayment solicitud) {
        this.solicitud = solicitud;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public PaymentType getTipoPago() {
        return tipoPago;
    }

    public void setTipoPago(PaymentType tipoPago) {
        this.tipoPago = tipoPago;
    }

    public ReturnInstallmentStatus getEstatus() {
        return estatus;
    }

    public void setEstatus(ReturnInstallmentStatus estatus) {
        this.estatus = estatus;
    }

    public BankAccount getCuentaOrigen() {
        return cuentaOrigen;
    }

    public void setCuentaOrigen(BankAccount cuentaOrigen) {
        this.cuentaOrigen = cuentaOrigen;
    }

    public String getComprobanteUrl() {
        return comprobanteUrl;
    }

    public void setComprobanteUrl(String comprobanteUrl) {
        this.comprobanteUrl = comprobanteUrl;
    }

    public String getComprobanteEntregaUrl() {
        return comprobanteEntregaUrl;
    }

    public void setComprobanteEntregaUrl(String comprobanteEntregaUrl) {
        this.comprobanteEntregaUrl = comprobanteEntregaUrl;
    }

    public String getCodigoRetiroSinTarjeta() {
        return codigoRetiroSinTarjeta;
    }

    public void setCodigoRetiroSinTarjeta(String codigoRetiroSinTarjeta) {
        this.codigoRetiroSinTarjeta = codigoRetiroSinTarjeta;
    }

    public LocalDateTime getFechaHoraRecoleccion() {
        return fechaHoraRecoleccion;
    }

    public void setFechaHoraRecoleccion(LocalDateTime fechaHoraRecoleccion) {
        this.fechaHoraRecoleccion = fechaHoraRecoleccion;
    }

    public LocalDateTime getFechaRealizacion() {
        return fechaRealizacion;
    }

    public void setFechaRealizacion(LocalDateTime fechaRealizacion) {
        this.fechaRealizacion = fechaRealizacion;
    }

    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDateTime fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public LocalDateTime getFechaConfirmacion() {
        return fechaConfirmacion;
    }

    public void setFechaConfirmacion(LocalDateTime fechaConfirmacion) {
        this.fechaConfirmacion = fechaConfirmacion;
    }

    public LocalDateTime getFechaCancelacion() {
        return fechaCancelacion;
    }

    public void setFechaCancelacion(LocalDateTime fechaCancelacion) {
        this.fechaCancelacion = fechaCancelacion;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public User getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(User creadoPor) {
        this.creadoPor = creadoPor;
    }

    public User getRealizadoPor() {
        return realizadoPor;
    }

    public void setRealizadoPor(User realizadoPor) {
        this.realizadoPor = realizadoPor;
    }

    public User getEntregadoPor() {
        return entregadoPor;
    }

    public void setEntregadoPor(User entregadoPor) {
        this.entregadoPor = entregadoPor;
    }

    public User getCanceladoPor() {
        return canceladoPor;
    }

    public void setCanceladoPor(User canceladoPor) {
        this.canceladoPor = canceladoPor;
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
