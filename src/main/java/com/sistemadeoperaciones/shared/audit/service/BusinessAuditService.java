package com.sistemadeoperaciones.shared.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistemadeoperaciones.shared.audit.model.BusinessAuditLog;
import com.sistemadeoperaciones.shared.audit.repository.BusinessAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessAuditService {
    private final BusinessAuditLogRepository repository;
    private final ObjectMapper mapper;

    public BusinessAuditService(BusinessAuditLogRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /** Must join the business transaction so the audit and mutation commit or roll back together. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Long actorId, String action, String entityType, Long entityId, Object before, Object after) {
        if (actorId == null || entityId == null || action == null || entityType == null)
            throw new IllegalArgumentException("La acción auditada requiere actor, acción y entidad");
        BusinessAuditLog row = new BusinessAuditLog();
        row.setActorUserId(actorId);
        row.setActionName(action);
        row.setEntityType(entityType);
        row.setEntityId(entityId);
        try {
            row.setBeforeJson(before == null ? null : mapper.writeValueAsString(before));
            row.setAfterJson(after == null ? null : mapper.writeValueAsString(after));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("No se pudo serializar el cambio auditado", e);
        }
        repository.save(row);
    }
}
