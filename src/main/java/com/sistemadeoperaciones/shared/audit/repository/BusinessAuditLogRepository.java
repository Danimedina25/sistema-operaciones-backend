package com.sistemadeoperaciones.shared.audit.repository;

import com.sistemadeoperaciones.shared.audit.model.BusinessAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessAuditLogRepository extends JpaRepository<BusinessAuditLog, Long> {}
