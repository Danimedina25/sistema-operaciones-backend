package com.sistemadeoperaciones.pagos.repository;

import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.enums.ReturnPaymentStatus;
import com.sistemadeoperaciones.pagos.model.OperationReturnPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface OperationReturnPaymentRepository
        extends JpaRepository<OperationReturnPayment, Long> {
    @Query("""
        SELECT COALESCE(SUM(r.monto), 0)
        FROM OperationReturnPayment r
        WHERE r.operacion.id = :operacionId
    """)
    BigDecimal sumReturnedAmountByOperationId(Long operacionId);

    boolean existsByOperacionId(Long operacionId);

    long countByOperacionId(Long operacionId);

    List<OperationReturnPayment> findByOperacionId(Long operationId);

    @Query("""
        SELECT COALESCE(SUM(r.monto), 0)
        FROM OperationReturnPayment r
        WHERE r.operacion.id = :operationId
          AND r.estatus IN :statuses
    """)
    BigDecimal sumAmountByOperationIdAndStatuses(
            @Param("operationId") Long operationId,
            @Param("statuses") List<ReturnPaymentStatus> statuses
    );

    default BigDecimal sumRequestedAmountByOperationId(Long operationId) {
        return sumAmountByOperationIdAndStatuses(
                operationId,
                List.of(
                        ReturnPaymentStatus.SOLICITADO,
                        ReturnPaymentStatus.RETORNADO
                )
        );
    }

    default BigDecimal sumRealizedAmountByOperationId(Long operationId) {
        return sumAmountByOperationIdAndStatuses(
                operationId,
                List.of(ReturnPaymentStatus.RETORNADO)
        );
    }

    @Query("""
    SELECT COALESCE(SUM(r.monto), 0)
    FROM OperationReturnPayment r
    WHERE r.tipoPago = :tipoPago
      AND r.estatus = com.sistemadeoperaciones.pagos.enums.ReturnPaymentStatus.RETORNADO
      AND r.fechaPago BETWEEN :inicio AND :fin
""")
    BigDecimal sumPaidReturnsByTypeBetween(
            @Param("tipoPago") PaymentType tipoPago,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin
    );

    @Query("""
SELECT COALESCE(SUM(op.monto),0)
FROM OperationReturnPayment op
WHERE op.cuentaOrigen.id = :bankAccountId
AND op.fechaPago BETWEEN :inicio AND :fin
""")
    BigDecimal sumSalidasByCuenta(
            Long bankAccountId,
            LocalDateTime inicio,
            LocalDateTime fin
    );

    @Query("""
    SELECT COALESCE(SUM(r.monto), 0)
    FROM OperationReturnPayment r
    WHERE r.cuentaOrigen.id = :bankAccountId
      AND r.estatus = com.sistemadeoperaciones.pagos.enums.ReturnPaymentStatus.RETORNADO
      AND r.fechaPago BETWEEN :inicio AND :fin
""")
    BigDecimal sumRetornosCuenta(
            @Param("bankAccountId") Long bankAccountId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin
    );
}