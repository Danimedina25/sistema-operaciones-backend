package com.sistemadeoperaciones.pagos.repository;

import com.sistemadeoperaciones.pagos.enums.OperationStatus;
import com.sistemadeoperaciones.pagos.model.PaymentOperation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface PaymentOperationRepository extends
        JpaRepository<PaymentOperation, Long>,
        JpaSpecificationExecutor<PaymentOperation> {

    Page<PaymentOperation> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<PaymentOperation> findBySocioComercialIdOrderByCreatedAtDesc(
            Long socioComercialId,
            Pageable pageable
    );

    Page<PaymentOperation> findByEstatusInOrderByCreatedAtDesc(
            Collection<OperationStatus> estatus,
            Pageable pageable
    );

    @Query("""
        SELECT op.cliente.nombre
        FROM PaymentOperation op
        WHERE op.activo = true
        GROUP BY op.cliente.nombre
        ORDER BY COUNT(op.id) DESC
    """)
    List<String> findMostFrequentClientNames(Pageable pageable);

    List<PaymentOperation> findByEstatusIn(
            Collection<OperationStatus> statuses
    );

    List<PaymentOperation> findByEstatusInAndActivoTrue(
            Collection<OperationStatus> statuses
    );

    List<PaymentOperation> findByEstatusInAndCreatedAtBetween(
            Collection<OperationStatus> statuses,
            LocalDateTime start,
            LocalDateTime end
    );
}