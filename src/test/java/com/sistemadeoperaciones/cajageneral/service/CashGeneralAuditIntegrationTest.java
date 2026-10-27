package com.sistemadeoperaciones.cajageneral.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistemadeoperaciones.cajageneral.dto.CloseCashDayRequest;
import com.sistemadeoperaciones.cajageneral.dto.OpenCashDayRequest;
import com.sistemadeoperaciones.cajageneral.enums.CashDenomination;
import com.sistemadeoperaciones.shared.audit.repository.BusinessAuditLogRepository;
import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.corte.service.BankAccountDailyCutService;
import com.sistemadeoperaciones.usuarios.model.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:cash_general_audit;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({CashGeneralService.class, BusinessAuditService.class, CashGeneralAuditIntegrationTest.Config.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CashGeneralAuditIntegrationTest {
    @TestConfiguration static class Config {
        @Bean ObjectMapper objectMapper() { return new ObjectMapper().findAndRegisterModules(); }
    }

    @Autowired CashGeneralService cash;
    @Autowired BusinessAuditLogRepository audits;
    @Autowired PlatformTransactionManager manager;
    @Autowired EntityManager em;
    @MockBean AuthenticatedUserService auth;
    @MockBean BankAccountDailyCutService bankCuts;

    @AfterEach void cleanup() {
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            for (String table : new String[]{"business_audit_log", "cash_general_opening_counts",
                    "cash_general_closing_counts", "cash_general_days", "cash_general_register", "users"})
                em.createNativeQuery("delete from " + table).executeUpdate();
        });
    }

    @Test void closeSerializesTheOldAndNewDenominationCounts() {
        User user = new TransactionTemplate(manager).execute(status -> {
            User row = new User(); row.setNombre("Jefa local");
            row.setCorreo(UUID.randomUUID() + "@example.test"); em.persist(row); return row;
        });
        when(auth.getCurrentUser()).thenReturn(user);
        var counts = new EnumMap<CashDenomination, Integer>(CashDenomination.class);
        for (var denomination : CashDenomination.values()) counts.put(denomination, 0);
        counts.put(CashDenomination.D100, 1);

        var opened = cash.open(new OpenCashDayRequest(LocalDate.now(), new BigDecimal("100"), counts));
        var closed = cash.close(opened.id(), new CloseCashDayRequest(new BigDecimal("100"),
                opened.version(), counts, null));

        assertThat(closed.closedAt()).isNotNull();
        var closeAudit = audits.findAll().stream()
                .filter(row -> row.getActionName().equals("CASH_DAY_CLOSE")).findFirst().orElseThrow();
        assertThat(closeAudit.getBeforeJson()).contains("\"closingCounts\":{}");
        assertThat(closeAudit.getAfterJson()).contains("\"D100\":1");
    }
}
