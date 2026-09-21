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

public interface OperationPaymentRepository extends JpaRepository<OperationPayment, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<OperationPayment> {

    List<OperationPayment> findByOperacionId(Long operacionId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from OperationPayment p where p.operacion.id=:id order by p.id")
    List<OperationPayment> findByOperacionIdForUpdate(@Param("id") Long id);

    long countByOperacionId(Long operacionId);

    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"operacion", "operacion.cliente", "cuentaDestino", "registradoPor"})
    org.springframework.data.domain.Page<OperationPayment> findAll(org.springframework.data.jpa.domain.Specification<OperationPayment> spec, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    List<OperationPayment> findByOperacionIdAndEstatus(Long operacionId, PaymentStatus estatus);

    boolean existsByOperacionIdAndEstatus(Long operacionId, PaymentStatus estatus);

    long countByOperacionIdAndEstatus(Long operacionId, PaymentStatus estatus);

    long countByCuentaDestinoId(Long cuentaDestinoId);

    long countByRegistradoPorId(Long registradoPorId);

    long countByValidadoPorId(Long validadoPorId);

    boolean existsByOperacionIdAndComprobanteUrlAndEstatusNot(
            Long operacionId,
            String comprobanteUrl,
            PaymentStatus estatus
    );

    @Query("""
    SELECT COALESCE(SUM(op.monto), 0)
    FROM OperationPayment op
    WHERE op.tipoPago = :tipoPago
      AND (op.chequeEstado IS NULL OR (op.chequeEstado = 'COBRADO' AND op.chequeDestinoCobro = 'CUENTA_BANCARIA'))
      AND op.estatus = :estatus
      AND COALESCE(op.chequeFechaCobro, op.fechaValidacion) BETWEEN :inicio AND :fin
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
      AND COALESCE(p.chequeFechaCobro, p.fechaValidacion) BETWEEN :inicio AND :fin
""")
    BigDecimal sumOfficeCommissionsBetween(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("estatus") PaymentStatus estatus
    );

    @Query("""
SELECT COALESCE(SUM(p.monto),0)
FROM OperationPayment p
WHERE p.cuentaDestino.id = :bankAccountId
AND p.estatus = :status
AND (p.chequeEstado IS NULL OR (p.chequeEstado = 'COBRADO' AND p.chequeDestinoCobro = 'CUENTA_BANCARIA'))
AND p.tipoPago IN (
    com.sistemadeoperaciones.pagos.enums.PaymentType.TRANSFERENCIA,
    com.sistemadeoperaciones.pagos.enums.PaymentType.DEPOSITO,
    com.sistemadeoperaciones.pagos.enums.PaymentType.CHEQUE
)
AND COALESCE(p.chequeFechaCobro, p.fechaValidacion) BETWEEN :inicio AND :fin
""")
    BigDecimal sumEntradasCuenta(
            Long bankAccountId,
            PaymentStatus status,
            LocalDateTime inicio,
            LocalDateTime fin
    );

    @Query("""
SELECT COALESCE(SUM(p.monto),0)
FROM OperationPayment p
WHERE p.cuentaDestino.id = :bankAccountId
AND p.estatus = :status
AND (p.chequeEstado IS NULL OR (p.chequeEstado = 'COBRADO' AND p.chequeDestinoCobro = 'CUENTA_BANCARIA'))
AND p.tipoPago = com.sistemadeoperaciones.pagos.enums.PaymentType.TRANSFERENCIA
AND COALESCE(p.chequeFechaCobro, p.fechaValidacion) BETWEEN :inicio AND :fin
""")
    BigDecimal sumEntradasTransferenciaCuenta(
            @Param("bankAccountId") Long bankAccountId,
            @Param("status") PaymentStatus status,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin
    );

    @Query("""
SELECT COALESCE(SUM(p.monto),0)
FROM OperationPayment p
WHERE p.cuentaDestino.id = :bankAccountId
AND p.estatus = :status
AND (p.chequeEstado IS NULL OR (p.chequeEstado = 'COBRADO' AND p.chequeDestinoCobro = 'CUENTA_BANCARIA'))
AND p.tipoPago = com.sistemadeoperaciones.pagos.enums.PaymentType.DEPOSITO
AND COALESCE(p.chequeFechaCobro, p.fechaValidacion) BETWEEN :inicio AND :fin
""")
    BigDecimal sumEntradasDepositoCuenta(
            @Param("bankAccountId") Long bankAccountId,
            @Param("status") PaymentStatus status,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin
    );

    @Query("""
SELECT COALESCE(SUM(p.monto),0)
FROM OperationPayment p
WHERE p.cuentaDestino.id = :bankAccountId
AND p.estatus = :status
AND (p.chequeEstado IS NULL OR (p.chequeEstado = 'COBRADO' AND p.chequeDestinoCobro = 'CUENTA_BANCARIA'))
AND p.tipoPago = com.sistemadeoperaciones.pagos.enums.PaymentType.CHEQUE
AND COALESCE(p.chequeFechaCobro, p.fechaValidacion) BETWEEN :inicio AND :fin
""")
    BigDecimal sumEntradasChequeCuenta(
            @Param("bankAccountId") Long bankAccountId,
            @Param("status") PaymentStatus status,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin
    );
}