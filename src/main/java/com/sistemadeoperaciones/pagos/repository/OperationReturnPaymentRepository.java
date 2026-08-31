package com.sistemadeoperaciones.pagos.repository;

import com.sistemadeoperaciones.pagos.dto.retornos.ReturnDestinationAccountSuggestionDto;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.enums.ReturnPaymentStatus;
import com.sistemadeoperaciones.pagos.model.OperationReturnPayment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OperationReturnPaymentRepository
        extends JpaRepository<OperationReturnPayment, Long>,
        JpaSpecificationExecutor<OperationReturnPayment> {
    @Query("""
        SELECT COALESCE(SUM(r.monto), 0)
        FROM OperationReturnPayment r
        WHERE r.operacion.id = :operacionId
    """)
    BigDecimal sumReturnedAmountByOperationId(Long operacionId);

    boolean existsByOperacionId(Long operacionId);

    /**
     * Bloqueo pesimista de la fila de la solicitud. Se toma como segundo lock
     * (después del de la operación) al crear/transicionar parcialidades.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM OperationReturnPayment r WHERE r.id = :id")
    Optional<OperationReturnPayment> findByIdForUpdate(@Param("id") Long id);

    long countByOperacionId(Long operacionId);

    long countByCuentaOrigenId(Long cuentaOrigenId);

    long countBySolicitadoPorId(Long solicitadoPorId);

    long countByPagadoPorId(Long pagadoPorId);

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

    /**
     * Monto total ya "comprometido" en solicitudes de retorno de la operación
     * (para el tope al solicitar). Cuenta toda solicitud que no esté totalmente
     * fuera de juego: incluye {@code ENTREGADO} y {@code PARCIALMENTE_RETORNADO},
     * que antes se omitían (permitía pedir de más mientras una solicitud estaba
     * en esos estados).
     */
    default BigDecimal sumRequestedAmountByOperationId(Long operationId) {
        return sumAmountByOperationIdAndStatuses(
                operationId,
                List.of(
                        ReturnPaymentStatus.SOLICITADO,
                        ReturnPaymentStatus.EN_RECOLECCION,
                        ReturnPaymentStatus.ENTREGADO,
                        ReturnPaymentStatus.PARCIALMENTE_RETORNADO,
                        ReturnPaymentStatus.RETORNADO
                )
        );
    }

    /**
     * @deprecated el monto efectivamente retornado de una operación ahora se
     * calcula sumando parcialidades COMPLETADA
     * ({@code OperationReturnInstallmentRepository.sumCompletedByOperation}).
     * Se conserva solo por compatibilidad con llamadas históricas.
     */
    @Deprecated
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

    @Query("""
    SELECT new com.sistemadeoperaciones.pagos.dto.retornos.ReturnDestinationAccountSuggestionDto(
        r.cuentaDestinoBanco,
        r.cuentaDestinoTitular,
        r.cuentaDestinoCliente,
        r.cuentaClabeCliente,
        COUNT(r.id)
    )
    FROM OperationReturnPayment r
    WHERE r.operacion.cliente.id = :clienteId
      AND r.tipoPago <> com.sistemadeoperaciones.pagos.enums.PaymentType.EFECTIVO
      AND r.cuentaDestinoBanco IS NOT NULL
      AND r.cuentaDestinoTitular IS NOT NULL
      AND (
        r.cuentaDestinoCliente IS NOT NULL
        OR r.cuentaClabeCliente IS NOT NULL
      )
    GROUP BY
        r.cuentaDestinoBanco,
        r.cuentaDestinoTitular,
        r.cuentaDestinoCliente,
        r.cuentaClabeCliente
    ORDER BY COUNT(r.id) DESC, MAX(r.fechaSolicitud) DESC
""")
    List<ReturnDestinationAccountSuggestionDto> findTopDestinationAccountsByClienteId(
            @Param("clienteId") Long clienteId,
            Pageable pageable
    );

    default List<ReturnDestinationAccountSuggestionDto> findTop10DestinationAccountsByClienteId(
            Long clienteId
    ) {
        return findTopDestinationAccountsByClienteId(
                clienteId,
                org.springframework.data.domain.PageRequest.of(0, 10)
        );
    }

    boolean existsByOperacionIdAndTipoPago(Long operacionId, PaymentType tipoPago);
}