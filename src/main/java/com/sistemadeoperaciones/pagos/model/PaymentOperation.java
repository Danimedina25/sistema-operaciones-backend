package com.sistemadeoperaciones.pagos.model;

import com.sistemadeoperaciones.clientes.model.Clientes;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.pagos.enums.OperationStatus;
import com.sistemadeoperaciones.socioscomerciales.models.CommercialPartner;
import com.sistemadeoperaciones.usuarios.model.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payment_operations")
public class PaymentOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Clientes cliente;

    @Column(name = "monto_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "monto_validado", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoValidado = BigDecimal.ZERO;

    @Column(name = "saldo_pendiente", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoPendiente = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OperationStatus estatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "socio_comercial_id", nullable = false)
    private User socioComercial;

    @Column(name = "niveles_red_comercial", nullable = false)
    private Integer nivelesRedComercial = 1;

    @Column(name = "porcentaje_comision_aplicado", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeComisionAplicado;

    @Column(name = "porcentaje_comision_oficina", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeComisionOficina;

    @Column(length = 500)
    private String observaciones;

    @OneToMany(mappedBy = "operacion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OperationPayment> pagos = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.montoValidado == null) {
            this.montoValidado = BigDecimal.ZERO;
        }

        if (this.saldoPendiente == null && this.montoTotal != null) {
            this.saldoPendiente = this.montoTotal;
        }

        if (this.estatus == null) {
            this.estatus = OperationStatus.PENDIENTE_VALIDACION;
        }

        if (this.nivelesRedComercial == null) {
            this.nivelesRedComercial = 1;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public PaymentOperation() {
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getMontoTotal() {
        return montoTotal;
    }

    public BigDecimal getMontoValidado() {
        return montoValidado;
    }

    public BigDecimal getSaldoPendiente() {
        return saldoPendiente;
    }

    public OperationStatus getEstatus() {
        return estatus;
    }

    public User getSocioComercial() {
        return socioComercial;
    }

    public Integer getNivelesRedComercial() {
        return nivelesRedComercial;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public List<OperationPayment> getPagos() {
        return pagos;
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

    public Clientes getCliente() {
        return cliente;
    }

    public void setCliente(Clientes cliente) {
        this.cliente = cliente;
    }

    public void setMontoTotal(BigDecimal montoTotal) {
        this.montoTotal = montoTotal;
    }

    public void setMontoValidado(BigDecimal montoValidado) {
        this.montoValidado = montoValidado;
    }

    public void setSaldoPendiente(BigDecimal saldoPendiente) {
        this.saldoPendiente = saldoPendiente;
    }

    public void setEstatus(OperationStatus estatus) {
        this.estatus = estatus;
    }

    public void setSocioComercial(User socioComercial) {
        this.socioComercial = socioComercial;
    }

    public void setNivelesRedComercial(Integer nivelesRedComercial) {
        this.nivelesRedComercial = nivelesRedComercial;
    }

    public BigDecimal getPorcentajeComisionAplicado() {
        return porcentajeComisionAplicado;
    }

    public void setPorcentajeComisionAplicado(BigDecimal porcentajeComisionAplicado) {
        this.porcentajeComisionAplicado = porcentajeComisionAplicado;
    }

    public BigDecimal getPorcentajeComisionOficina() {
        return porcentajeComisionOficina;
    }

    public void setPorcentajeComisionOficina(BigDecimal porcentajeComisionOficina) {
        this.porcentajeComisionOficina = porcentajeComisionOficina;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public void setPagos(List<OperationPayment> pagos) {
        this.pagos = pagos;
    }
}