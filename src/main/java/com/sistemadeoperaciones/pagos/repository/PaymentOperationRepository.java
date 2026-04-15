package com.sistemadeoperaciones.pagos.repository;

import com.sistemadeoperaciones.pagos.model.PaymentOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentOperationRepository extends JpaRepository<PaymentOperation, Long> {

    List<PaymentOperation> findAllByOrderByCreatedAtDesc();

    List<PaymentOperation> findBySocioComercialIdOrderByCreatedAtDesc(Long socioComercialId);
}