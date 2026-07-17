package com.sistemadeoperaciones.shared.audit.repository;

import com.sistemadeoperaciones.shared.audit.model.DeletionAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeletionAuditLogRepository extends JpaRepository<DeletionAuditLog, Long> {
}
