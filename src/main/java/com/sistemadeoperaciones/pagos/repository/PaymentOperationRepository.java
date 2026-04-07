package com.sistemadeoperaciones.pagos.repository;

import com.sistemadeoperaciones.pagos.model.PaymentOperation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentOperationRepository extends JpaRepository<PaymentOperation, Long> {
}