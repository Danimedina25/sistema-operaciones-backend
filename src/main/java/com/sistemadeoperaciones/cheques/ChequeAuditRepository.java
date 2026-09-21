package com.sistemadeoperaciones.cheques;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ChequeAuditRepository extends JpaRepository<ChequeAudit, Long> {
    Optional<ChequeAudit> findByRequestId(String requestId);
    List<ChequeAudit> findByPagoIdOrderByIdAsc(Long pagoId);
    List<ChequeAudit> findByPagoIdInOrderByIdAsc(Collection<Long> ids);
}
