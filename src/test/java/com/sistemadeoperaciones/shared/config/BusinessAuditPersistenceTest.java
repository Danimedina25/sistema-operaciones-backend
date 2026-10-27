package com.sistemadeoperaciones.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistemadeoperaciones.shared.audit.repository.BusinessAuditLogRepository;
import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@Import({BusinessAuditService.class, BusinessAuditPersistenceTest.Config.class})
class BusinessAuditPersistenceTest {
    @TestConfiguration static class Config {
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
    }

    @Autowired BusinessAuditService service;
    @Autowired BusinessAuditLogRepository repository;

    @Test void storesBeforeAndAfterInTheBusinessTransaction() {
        service.record(11L, "USER_DEACTIVATE", "USER", 42L,
                Map.of("activo", true), Map.of("activo", false));
        var row = repository.findAll().get(0);
        assertThat(row.getActorUserId()).isEqualTo(11L);
        assertThat(row.getActionName()).isEqualTo("USER_DEACTIVATE");
        assertThat(row.getEntityType()).isEqualTo("USER");
        assertThat(row.getEntityId()).isEqualTo(42L);
        assertThat(row.getBeforeJson()).contains("\"activo\":true");
        assertThat(row.getAfterJson()).contains("\"activo\":false");
        assertThat(row.getOccurredAt()).isNotNull();
    }
}
