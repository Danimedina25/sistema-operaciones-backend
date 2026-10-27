package com.sistemadeoperaciones.shared.audit.service;

import com.sistemadeoperaciones.shared.audit.model.AdminRequestAudit;
import com.sistemadeoperaciones.shared.audit.repository.AdminRequestAuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AdminRequestAuditService {
    private final AdminRequestAuditRepository repository;

    public AdminRequestAuditService(AdminRequestAuditRepository repository) { this.repository = repository; }

    /** Written before dispatch so an audit outage cannot silently permit an Admin action. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long begin(Long actorUserId, String method, String path) {
        AdminRequestAudit row = new AdminRequestAudit();
        row.setActorUserId(actorUserId);
        row.setHttpMethod(method);
        row.setRequestPath(path);
        return repository.saveAndFlush(row).getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finish(Long id, int statusCode) {
        AdminRequestAudit row = repository.findById(id).orElseThrow();
        row.setCompletedAt(LocalDateTime.now());
        row.setStatusCode(statusCode);
    }
}
