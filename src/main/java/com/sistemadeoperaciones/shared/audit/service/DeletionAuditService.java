package com.sistemadeoperaciones.shared.audit.service;

import com.sistemadeoperaciones.shared.audit.model.DeletionAuditLog;
import com.sistemadeoperaciones.shared.audit.repository.DeletionAuditLogRepository;
import com.sistemadeoperaciones.usuarios.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeletionAuditService {

    private final DeletionAuditLogRepository deletionAuditLogRepository;

    public DeletionAuditService(DeletionAuditLogRepository deletionAuditLogRepository) {
        this.deletionAuditLogRepository = deletionAuditLogRepository;
    }

    @Transactional
    public void record(String entityType, Long entityId, String entityLabel, User performedBy) {
        DeletionAuditLog log = new DeletionAuditLog();
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setEntityLabel(entityLabel);
        log.setDeletedByUserId(performedBy.getId());
        log.setDeletedByLabel(performedBy.getNombre() + " (" + performedBy.getCorreo() + ")");

        deletionAuditLogRepository.save(log);
    }
}
