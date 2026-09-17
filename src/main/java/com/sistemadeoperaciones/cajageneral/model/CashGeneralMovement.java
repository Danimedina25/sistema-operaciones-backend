package com.sistemadeoperaciones.cajageneral.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import com.sistemadeoperaciones.cajageneral.enums.*;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.pagos.model.OperationPayment;
import com.sistemadeoperaciones.pagos.model.OperationReturnInstallment;
import com.sistemadeoperaciones.usuarios.model.User;

@Entity
@Table(name = "cash_general_movements")
public class CashGeneralMovement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "day_id", nullable = false)
    private CashGeneralDay dia;

    @Column(nullable = false, unique = true, length = 36)
    private String requestId;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10)
    private CashMovementDirection direccion;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private CashMovementConcept tipo;

    @Column(nullable = false, length = 300)
    private String concepto;

    // Snapshot histórico del nombre del banco. Sigue siendo la única referencia de los
    // movimientos anteriores a la integración.
    @Column(length = 50)
    private String banco;

    // Cuenta bancaria real de la que salió el dinero. La llenan los retiros de banco hacia
    // la caja —cheque cobrado y retiro sin tarjeta—: esa fila es a la vez la entrada de
    // efectivo y la salida bancaria, así que es imposible que exista una sin la otra.
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "bank_account_id")
    private BankAccount cuentaBancaria;

    @Column(precision = 15, scale = 2)
    private BigDecimal montoManual;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "installment_id", unique = true)
    private OperationReturnInstallment parcialidad;

    // Pago de ingreso en efectivo que originó la entrada. Único: un pago validado produce
    // una sola entrada de caja, y esa unicidad es la que impide duplicarla.
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "operation_payment_id", unique = true)
    private OperationPayment pago;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal saldoAcumulado;

    @ElementCollection
    @CollectionTable(name = "cash_general_movement_counts", joinColumns = @JoinColumn(name = "movement_id"))
    @MapKeyEnumerated(EnumType.STRING) @MapKeyColumn(name = "denomination", length = 10)
    @Column(name = "quantity", nullable = false)
    private Map<CashDenomination, Integer> denominaciones;

    @Column(length = 500)
    private String comprobanteUrl;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "creado_por", nullable = false)
    private User creadoPor;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Una salida vinculada guarda únicamente la FK. El monto financiero sigue en la parcialidad.
    public BigDecimal importe() { return parcialidad == null ? montoManual : parcialidad.getMonto(); }
    @PrePersist public void prePersist() { createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public CashGeneralDay getDia() { return dia; }
    public void setDia(CashGeneralDay value) { this.dia = value; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String value) { this.requestId = value; }
    public CashMovementDirection getDireccion() { return direccion; }
    public void setDireccion(CashMovementDirection value) { this.direccion = value; }
    public CashMovementConcept getTipo() { return tipo; }
    public void setTipo(CashMovementConcept value) { this.tipo = value; }
    public String getConcepto() { return concepto; }
    public void setConcepto(String value) { this.concepto = value; }
    public String getBanco() { return banco; }
    public void setBanco(String value) { this.banco = value; }
    public BankAccount getCuentaBancaria() { return cuentaBancaria; }
    public void setCuentaBancaria(BankAccount value) { this.cuentaBancaria = value; }
    public BigDecimal getMontoManual() { return montoManual; }
    public void setMontoManual(BigDecimal value) { this.montoManual = value; }
    public OperationPayment getPago() { return pago; }
    public void setPago(OperationPayment value) { this.pago = value; }
    public OperationReturnInstallment getParcialidad() { return parcialidad; }
    public void setParcialidad(OperationReturnInstallment value) { this.parcialidad = value; }
    public BigDecimal getSaldoAcumulado() { return saldoAcumulado; }
    public void setSaldoAcumulado(BigDecimal value) { this.saldoAcumulado = value; }
    public Map<CashDenomination, Integer> getDenominaciones() { return denominaciones; }
    public void setDenominaciones(Map<CashDenomination, Integer> value) { this.denominaciones = value; }
    public String getComprobanteUrl() { return comprobanteUrl; }
    public void setComprobanteUrl(String value) { this.comprobanteUrl = value; }
    public User getCreadoPor() { return creadoPor; }
    public void setCreadoPor(User value) { this.creadoPor = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
}
