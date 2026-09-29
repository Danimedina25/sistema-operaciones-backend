package com.sistemadeoperaciones.retiroscaja;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BankCashWithdrawalRepository extends JpaRepository<BankCashWithdrawal, Long> {
    Optional<BankCashWithdrawal> findByRequestId(String requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from BankCashWithdrawal w where w.id = :id")
    Optional<BankCashWithdrawal> findByIdForUpdate(@Param("id") Long id);

    /** Efectivo en tránsito de una cuenta: retiros registrados que Cajas aún no confirma. */
    @Query("""
        select coalesce(sum(w.monto), 0) from BankCashWithdrawal w
        where w.cuentaBancaria.id = :accountId
          and w.estatus = com.sistemadeoperaciones.retiroscaja.WithdrawalStatus.PENDIENTE
        """)
    BigDecimal sumPendingByAccount(@Param("accountId") Long accountId);

    @EntityGraph(attributePaths = {"cuentaBancaria", "registradoPor", "resueltoPor"})
    List<BankCashWithdrawal> findByEstatusOrderByRegistradoEnAscIdAsc(WithdrawalStatus estatus);

    @EntityGraph(attributePaths = {"cuentaBancaria", "registradoPor", "resueltoPor"})
    List<BankCashWithdrawal> findByRegistradoEnGreaterThanEqualAndRegistradoEnLessThanOrderByRegistradoEnDescIdDesc(
            LocalDateTime inicio, LocalDateTime fin);

    List<BankCashWithdrawal> findByMovimientoCajaIdIn(Collection<Long> movementIds);
}
