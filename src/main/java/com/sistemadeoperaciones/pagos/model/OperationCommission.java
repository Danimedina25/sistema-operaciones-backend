package com.sistemadeoperaciones.pagos.model;

import com.sistemadeoperaciones.pagos.enums.CommissionStatus;
import com.sistemadeoperaciones.socioscomerciales.models.CommercialPartner;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "operation_commissions")
public class OperationCommission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Operación origen
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operacion_id", nullable = false)
    private PaymentOperation operacion;

    /**
     * Socio que recibirá comisión
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "socio_comercial_id", nullable = false)
    private CommercialPartner socioComercial;

    /**
     * Nivel informativo
     */
    @Column(nullable = false)
    private Integer nivel;

    /**
     * Porcentaje usado en esta operación
     */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;

    /**
     * Comisión generada
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    /**
     * Estatus del pago semanal
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommissionStatus estatus;

    /**
     * Semana de pago
     */
    private LocalDate semanaPago;

    /**
     * Fecha pagada
     */
    private LocalDateTime fechaPago;

    /**
     * Comprobante
     */
    private String comprobanteUrl;
}