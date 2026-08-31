package com.sistemadeoperaciones.pagos.repository;

import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.enums.ReturnInstallmentStatus;
import com.sistemadeoperaciones.pagos.model.OperationReturnInstallment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface OperationReturnInstallmentRepository
        extends JpaRepository<OperationReturnInstallment, Long>,
        JpaSpecificationExecutor<OperationReturnInstallment> {

    List<OperationReturnInstallment> findBySolicitudIdOrderByCreatedAtAsc(Long solicitudId);

    long countBySolicitudIdAndEstatusNot(Long solicitudId, ReturnInstallmentStatus estatus);

    boolean existsBySolicitudId(Long solicitudId);

    boolean existsByCuentaOrigenId(Long cuentaOrigenId);

    // ------------------------------------------------------------------
    // Sumas por solicitud
    // ------------------------------------------------------------------

    @Query("""
        SELECT COALESCE(SUM(i.monto), 0)
        FROM OperationReturnInstallment i
        WHERE i.solicitud.id = :solicitudId
          AND i.estatus IN :statuses
    """)
    BigDecimal sumAmountBySolicitudAndStatuses(
            @Param("solicitudId") Long solicitudId,
            @Param("statuses") List<ReturnInstallmentStatus> statuses
    );

    default BigDecimal sumCompletedBySolicitud(Long solicitudId) {
        return sumAmountBySolicitudAndStatuses(
                solicitudId,
                List.of(ReturnInstallmentStatus.COMPLETADA)
        );
    }

    default BigDecimal sumInFlightBySolicitud(Long solicitudId) {
        return sumAmountBySolicitudAndStatuses(
                solicitudId,
                List.of(
                        ReturnInstallmentStatus.PROGRAMADA,
                        ReturnInstallmentStatus.ENTREGADA
                )
        );
    }

    long countBySolicitudIdAndEstatusIn(Long solicitudId, List<ReturnInstallmentStatus> statuses);

    // ------------------------------------------------------------------
    // Sumas por operación
    // ------------------------------------------------------------------

    @Query("""
        SELECT COALESCE(SUM(i.monto), 0)
        FROM OperationReturnInstallment i
        WHERE i.solicitud.operacion.id = :operationId
          AND i.estatus = com.sistemadeoperaciones.pagos.enums.ReturnInstallmentStatus.COMPLETADA
    """)
    BigDecimal sumCompletedByOperation(@Param("operationId") Long operationId);

    // ------------------------------------------------------------------
    // Corte de caja / saldos bancarios — reemplazan las queries basadas en
    // OperationReturnPayment (estatus = RETORNADO, fechaPago). Con el backfill
    // los totales históricos son idénticos.
    // ------------------------------------------------------------------

    @Query("""
        SELECT COALESCE(SUM(i.monto), 0)
        FROM OperationReturnInstallment i
        WHERE i.tipoPago = :tipoPago
          AND i.estatus = com.sistemadeoperaciones.pagos.enums.ReturnInstallmentStatus.COMPLETADA
          AND i.fechaRealizacion BETWEEN :inicio AND :fin
    """)
    BigDecimal sumCompletedByTypeBetween(
            @Param("tipoPago") PaymentType tipoPago,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin
    );

    @Query("""
        SELECT COALESCE(SUM(i.monto), 0)
        FROM OperationReturnInstallment i
        WHERE i.cuentaOrigen.id = :bankAccountId
          AND i.estatus = com.sistemadeoperaciones.pagos.enums.ReturnInstallmentStatus.COMPLETADA
          AND i.fechaRealizacion BETWEEN :inicio AND :fin
    """)
    BigDecimal sumCompletedByCuentaOrigenBetween(
            @Param("bankAccountId") Long bankAccountId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin
    );

    // ------------------------------------------------------------------
    // Colas operativas (entregas de hoy / atrasadas) — sobre parcialidades
    // ------------------------------------------------------------------

    @Query("""
        SELECT i
        FROM OperationReturnInstallment i
        WHERE i.tipoPago IN :tipos
          AND i.estatus IN :statuses
          AND (:inicio IS NULL OR i.fechaHoraRecoleccion >= :inicio)
          AND (:fin IS NULL OR i.fechaHoraRecoleccion <= :fin)
    """)
    List<OperationReturnInstallment> findPickupInstallments(
            @Param("tipos") List<PaymentType> tipos,
            @Param("statuses") List<ReturnInstallmentStatus> statuses,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            Pageable pageable
    );
}
