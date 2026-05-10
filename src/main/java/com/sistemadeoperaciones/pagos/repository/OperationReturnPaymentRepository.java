package com.sistemadeoperaciones.pagos.repository;

import com.sistemadeoperaciones.pagos.model.OperationReturnPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface OperationReturnPaymentRepository
        extends JpaRepository<OperationReturnPayment, Long> {

    List<OperationReturnPayment> findByOperacionId(Long operacionId);

    @Query("""
        SELECT COALESCE(SUM(r.monto), 0)
        FROM OperationReturnPayment r
        WHERE r.operacion.id = :operacionId
    """)
    BigDecimal sumReturnedAmountByOperationId(Long operacionId);

    boolean existsByOperacionId(Long operacionId);

    long countByOperacionId(Long operacionId);

}