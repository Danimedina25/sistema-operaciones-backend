package com.sistemadeoperaciones.shared.config;

import com.sistemadeoperaciones.shared.audit.repository.AdminRequestAuditRepository;
import com.sistemadeoperaciones.shared.audit.service.AdminRequestAuditService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@Import(AdminRequestAuditService.class)
class AdminRequestAuditPersistenceTest {
    @Autowired AdminRequestAuditService service;
    @Autowired AdminRequestAuditRepository repository;
    @Autowired EntityManager entityManager;

    @Test void persistsStartAndOutcomeInSeparateTransactions() {
        Long id = service.begin(15L, "POST", "/api/admin/users/7/deactivate");
        var started = repository.findById(id).orElseThrow();
        assertThat(started.getActorUserId()).isEqualTo(15L);
        assertThat(started.getStartedAt()).isNotNull();
        assertThat(started.getStatusCode()).isNull();

        service.finish(id, 403);
        entityManager.clear();
        var completed = repository.findById(id).orElseThrow();
        assertThat(completed.getStatusCode()).isEqualTo(403);
        assertThat(completed.getCompletedAt()).isNotNull();
    }
}
