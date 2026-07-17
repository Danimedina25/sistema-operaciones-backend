package com.sistemadeoperaciones.comisionessocioscomerciales.repository;

import com.sistemadeoperaciones.comisionessocioscomerciales.models.CommercialPartnerCommission;
import com.sistemadeoperaciones.pagos.enums.CommissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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

    List<CommercialPartnerCommission> findByGeneratedAtBetweenAndStatus(
            LocalDateTime start,
            LocalDateTime end,
            CommissionStatus status
    );

    // =====================================================
    // DETALLE
    // =====================================================

    Optional<CommercialPartnerCommission> findById(Long id);

    boolean existsByOperationIdAndStatus(
            Long operationId,
            CommissionStatus status
    );

    List<CommercialPartnerCommission> findByOperationIdAndStatus(
            Long operationId,
            CommissionStatus status
    );

    @Query("""
    SELECT c
    FROM CommercialPartnerCommission c
    JOIN FETCH c.operation o
    JOIN FETCH o.cliente
    WHERE c.user.id = :userId
    AND o.createdAt BETWEEN :startDate AND :endDate
    ORDER BY o.createdAt DESC
""")
    List<CommercialPartnerCommission> findDetailByUser(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
    SELECT c
    FROM CommercialPartnerCommission c
    JOIN FETCH c.operation o
    JOIN FETCH o.cliente
    WHERE c.commercialPartner.id = :partnerId
    AND o.createdAt BETWEEN :startDate AND :endDate
    ORDER BY o.createdAt DESC
""")
    List<CommercialPartnerCommission> findDetailByPartner(
            @Param("partnerId") Long partnerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    List<CommercialPartnerCommission>
    findByUserIdAndOperationCreatedAtBetween(
            Long userId,
            LocalDateTime start,
            LocalDateTime end
    );

    List<CommercialPartnerCommission> findByOperationIdIn(
            List<Long> operationIds
    );

    @Query("""
    SELECT COALESCE(SUM(c.commissionAmount), 0)
    FROM CommercialPartnerCommission c
    WHERE c.status = :status
      AND c.paidAt BETWEEN :startDate AND :endDate
""")
    BigDecimal sumPaidCommissionsBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") CommissionStatus status
    );

    long countByCommercialPartnerId(Long commercialPartnerId);

    long countByUserId(Long userId);
}