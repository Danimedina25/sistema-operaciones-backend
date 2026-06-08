package com.sistemadeoperaciones.comisionessocioscomerciales.repository;

import com.sistemadeoperaciones.comisionessocioscomerciales.models.CommercialPartnerCommission;
import com.sistemadeoperaciones.pagos.enums.CommissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CommercialPartnerCommissionRepository
        extends JpaRepository<CommercialPartnerCommission, Long> {

    // =====================================================
    // OPERACIÓN
    // =====================================================

    List<CommercialPartnerCommission> findByOperationId(Long operationId);

    List<CommercialPartnerCommission> findByOperationIdOrderByNivelAsc(
            Long operationId
    );

    boolean existsByOperationId(Long operationId);

    void deleteByOperationId(Long operationId);

    List<CommercialPartnerCommission>
    findByPaidAtBetweenAndStatus(
            LocalDateTime start,
            LocalDateTime end,
            CommissionStatus status
    );

    // =====================================================
    // USUARIO NIVEL 1
    // =====================================================

    List<CommercialPartnerCommission> findByUserId(Long userId);

    List<CommercialPartnerCommission> findByUserIdAndStatus(
            Long userId,
            CommissionStatus status
    );

    // =====================================================
    // SOCIOS NIVEL 2 Y 3
    // =====================================================

    List<CommercialPartnerCommission> findByCommercialPartnerId(
            Long commercialPartnerId
    );

    List<CommercialPartnerCommission> findByCommercialPartnerIdAndStatus(
            Long commercialPartnerId,
            CommissionStatus status
    );

    // =====================================================
    // ESTATUS
    // =====================================================

    List<CommercialPartnerCommission> findByStatus(
            CommissionStatus status
    );

    // =====================================================
    // SEMANAS
    // =====================================================

    List<CommercialPartnerCommission> findByGeneratedAtBetween(
            LocalDateTime start,
            LocalDateTime end
    );

    List<CommercialPartnerCommission>
    findByOperationCreatedAtBetween(
            LocalDateTime start,
            LocalDateTime end
    );

    List<CommercialPartnerCommission>
    findByOperationCreatedAtBetweenAndStatus(
            LocalDateTime start,
            LocalDateTime end,
            CommissionStatus status
    );

    List<CommercialPartnerCommission> findByGeneratedAtBetweenAndStatus(
            LocalDateTime start,
            LocalDateTime end,
            CommissionStatus status
    );

    // =====================================================
    // PAGADAS
    // =====================================================

    List<CommercialPartnerCommission> findByPaidAtBetween(
            LocalDateTime start,
            LocalDateTime end
    );

    // =====================================================
    // DETALLE
    // =====================================================

    Optional<CommercialPartnerCommission> findById(Long id);

    boolean existsByOperationIdAndStatus(
            Long operationId,
            CommissionStatus status
    );

    List<CommercialPartnerCommission>
    findByStatusOrderByGeneratedAtDesc(
            CommissionStatus status
    );

    List<CommercialPartnerCommission> findByOperationIdAndStatus(
            Long operationId,
            CommissionStatus status
    );

}