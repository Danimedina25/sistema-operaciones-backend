package com.sistemadeoperaciones.pagos.repository;

import com.sistemadeoperaciones.pagos.model.OperationCommission;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * La tabla operation_commissions quedó superada por commercial_partner_commissions,
 * pero OperationCommission sigue siendo una @Entity y con ddl-auto=update Hibernate
 * mantiene viva la tabla y su FK NOT NULL hacia payment_operations. Este repositorio
 * existe únicamente para poder detectar filas históricas antes de borrar una operación.
 */
public interface OperationCommissionRepository extends JpaRepository<OperationCommission, Long> {

    long countByOperacionId(Long operacionId);
}
