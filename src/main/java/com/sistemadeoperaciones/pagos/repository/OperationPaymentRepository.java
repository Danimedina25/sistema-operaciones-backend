package com.sistemadeoperaciones.pagos.repository;

import com.sistemadeoperaciones.pagos.enums.PaymentStatus;
import com.sistemadeoperaciones.pagos.model.OperationPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
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
}