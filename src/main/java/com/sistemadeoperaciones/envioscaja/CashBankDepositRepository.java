package com.sistemadeoperaciones.envioscaja;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CashBankDepositRepository extends JpaRepository<CashBankDeposit, Long> {
    Optional<CashBankDeposit> findByRequestId(String requestId);
    @Query("select d.origen from CashBankDeposit d where d.id = :id")
    Optional<CashBankDepositOrigin> findOriginById(@Param("id") Long id);
    @Query("select d.chequePayment.id from CashBankDeposit d where d.id = :id")
    Optional<Long> findChequePaymentIdById(@Param("id") Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from CashBankDeposit d where d.id = :id")
    Optional<CashBankDeposit> findByIdForUpdate(@Param("id") Long id);
    List<CashBankDeposit> findByEstatusOrderByRegistradoEnAscIdAsc(CashBankDepositStatus status);
    List<CashBankDeposit> findByRegistradoEnGreaterThanEqualAndRegistradoEnLessThanOrderByRegistradoEnDescIdDesc(
            LocalDateTime start, LocalDateTime end);
    @Query("select coalesce(sum(d.monto), 0) from CashBankDeposit d where d.origen = com.sistemadeoperaciones.envioscaja.CashBankDepositOrigin.EFECTIVO_CAJA and d.estatus in (com.sistemadeoperaciones.envioscaja.CashBankDepositStatus.PENDIENTE, com.sistemadeoperaciones.envioscaja.CashBankDepositStatus.RECHAZADO_PENDIENTE_DEVOLUCION)")
    BigDecimal sumReservedCash();
    boolean existsByMovimientoCajaDiaId(Long dayId);
    boolean existsByChequePaymentIdAndEstatus(Long paymentId, CashBankDepositStatus status);
    List<CashBankDeposit> findByChequePaymentIdOrderByIdDesc(Long paymentId);
}
