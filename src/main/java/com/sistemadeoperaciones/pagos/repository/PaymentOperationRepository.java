package com.sistemadeoperaciones.pagos.repository;

import com.sistemadeoperaciones.pagos.enums.OperationStatus;
import com.sistemadeoperaciones.pagos.model.PaymentOperation;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentOperationRepository extends
        JpaRepository<PaymentOperation, Long>,
        JpaSpecificationExecutor<PaymentOperation> {

    /**
     * Bloqueo pesimista de la fila de la operación. Se toma como primer lock
     * (antes que el de la solicitud) al crear/transicionar parcialidades de
     * retorno, para serializar el recálculo de saldos y estatus y evitar
     * sobrepagos por solicitudes concurrentes.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT op FROM PaymentOperation op WHERE op.id = :id")
    Optional<PaymentOperation> findByIdForUpdate(@Param("id") Long id);

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

    long countByClienteId(Long clienteId);

    long countBySocioComercialId(Long socioComercialId);

    long countBySocioComercialNivel2Id(Long socioComercialNivel2Id);

    long countBySocioComercialNivel3Id(Long socioComercialNivel3Id);
}