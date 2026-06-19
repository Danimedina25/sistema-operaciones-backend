package com.sistemadeoperaciones.pagos.repository;

import com.sistemadeoperaciones.pagos.enums.PaymentStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.model.OperationPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface OperationPaymentRepository extends JpaRepository<OperationPayment, Long> {

    List<OperationPayment> findByOperacionId(Long operacionId);

    List<OperationPayment> findByOperacionIdAndEstatus(Long operacionId, PaymentStatus estatus);

    boolean existsByOperacionIdAndEstatus(Long operacionId, PaymentStatus estatus);

    long countByOperacionIdAndEstatus(Long operacionId, PaymentStatus estatus);

    boolean existsByOperacionIdAndComprobanteUrlAndEstatusNot(
            Long operacionId,
            String comprobanteUrl,
            PaymentStatus estatus
    );

    @Query("""
    SELECT COALESCE(SUM(op.monto), 0)
    FROM OperationPayment op
    WHERE op.tipoPago = :tipoPago
      AND op.estatus = :estatus
      AND op.fechaValidacion BETWEEN :inicio AND :fin
""")
    BigDecimal sumValidatedPaymentsByTypeBetween(
            @Param("tipoPago") PaymentType tipoPago,
            @Param("estatus") PaymentStatus estatus,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin
    );

    @Query("""
    SELECT COALESCE(SUM(
        p.monto * (op.porcentajeComisionOficina / 100)
    ), 0)
    FROM OperationPayment p
    JOIN p.operacion op
    WHERE p.estatus = :estatus
      AND p.fechaValidacion BETWEEN :inicio AND :fin
""")
    BigDecimal sumOfficeCommissionsBetween(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("estatus") PaymentStatus estatus
    );
}