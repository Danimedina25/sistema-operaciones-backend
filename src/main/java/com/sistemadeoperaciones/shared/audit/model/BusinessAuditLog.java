package com.sistemadeoperaciones.shared.audit.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "business_audit_log")
public class BusinessAuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;
    @Column(name = "action_name", nullable = false, length = 80)
    private String actionName;
    @Column(name = "entity_type", nullable = false, length = 80)
    private String entityType;
    @Column(name = "entity_id", nullable = false)
    private Long entityId;
    @Lob @Column(name = "before_json", columnDefinition = "LONGTEXT")
    private String beforeJson;
    @Lob @Column(name = "after_json", columnDefinition = "LONGTEXT")
    private String afterJson;
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @PrePersist void occurred() { if (occurredAt == null) occurredAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long value) { actorUserId = value; }
    public String getActionName() { return actionName; }
    public void setActionName(String value) { actionName = value; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String value) { entityType = value; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long value) { entityId = value; }
    public String getBeforeJson() { return beforeJson; }
    public void setBeforeJson(String value) { beforeJson = value; }
    public String getAfterJson() { return afterJson; }
    public void setAfterJson(String value) { afterJson = value; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
