package com.sistemadeoperaciones.pagos.repository;

import com.sistemadeoperaciones.pagos.model.PaymentOperation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PaymentOperationRepository extends JpaRepository<PaymentOperation, Long> {

    List<PaymentOperation> findAllByOrderByCreatedAtDesc();

    List<PaymentOperation> findBySocioComercialIdOrderByCreatedAtDesc(Long socioComercialId);

    @Query("""
    SELECT p.clienteNombre
    FROM PaymentOperation p
    WHERE p.clienteNombre IS NOT NULL
      AND TRIM(p.clienteNombre) <> ''
    GROUP BY p.clienteNombre
    ORDER BY COUNT(p.clienteNombre) DESC, p.clienteNombre ASC
""")
    List<String> findMostFrequentClientNames(Pageable pageable);
}