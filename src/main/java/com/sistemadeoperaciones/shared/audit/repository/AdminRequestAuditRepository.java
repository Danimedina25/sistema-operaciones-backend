package com.sistemadeoperaciones.shared.audit.repository;

import com.sistemadeoperaciones.shared.audit.model.AdminRequestAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRequestAuditRepository extends JpaRepository<AdminRequestAudit, Long> {}
