package com.sistemadeoperaciones.pagos.service;

import com.sistemadeoperaciones.cajageneral.service.CashGeneralService;
import com.sistemadeoperaciones.clientes.model.Clientes;
import com.sistemadeoperaciones.comisionessocioscomerciales.service.CommercialPartnerCommissionService;
import com.sistemadeoperaciones.configuraciones.service.ConfiguracionGeneralService;
import com.sistemadeoperaciones.notifications.service.NotificationService;
import com.sistemadeoperaciones.pagos.enums.OperationStatus;
import com.sistemadeoperaciones.pagos.model.PaymentOperation;
import com.sistemadeoperaciones.pagos.repository.PaymentOperationRepository;
import com.sistemadeoperaciones.shared.audit.service.DeletionAuditService;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.usuarios.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(PaymentOperationServiceImpl.class)
class PaymentOperationStalledSearchTest {
    @Autowired PaymentOperationService service;
    @Autowired PaymentOperationRepository repository;
    @PersistenceContext EntityManager em;

    @MockBean CashGeneralService cashGeneralService;
    @MockBean AuthenticatedUserService authenticatedUserService;
    @MockBean NotificationService notificationService;
    @MockBean CommercialPartnerCommissionService commercialPartnerCommissionService;
    @MockBean ConfiguracionGeneralService configuracionGeneralService;
    @MockBean DeletionAuditService deletionAuditService;

    @Test
    void searchesAllStalledRowsBeforePaginationAndKeepsStalledScope() {
        User owner = new User();
        owner.setNombre("Socio de prueba");
        owner.setCorreo("stalled-search@example.test");
        em.persist(owner);

        Clientes ordinary = client("Cliente común", owner);
        Clientes target = client("Cliente objetivo", owner);
        for (int index = 0; index < 12; index++) operation(ordinary, owner, OperationStatus.PENDIENTE_VALIDACION, true);
        PaymentOperation match = operation(target, owner, OperationStatus.PENDIENTE_VALIDACION, true);
        operation(target, owner, OperationStatus.RETORNADA, true);
        operation(target, owner, OperationStatus.PENDIENTE_VALIDACION, false);
        em.flush();
        em.createNativeQuery("UPDATE payment_operations SET updated_at = :oldDate")
                .setParameter("oldDate", LocalDateTime.now().minusDays(5)).executeUpdate();
        em.clear();

        Page<?> page = service.searchStalledOperations("objetivo", 48, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting("id").containsExactly(match.getId());
        assertThat(service.searchStalledOperations("objetivo", 48, PageRequest.of(0, 10))
                .getContent().get(0).getSaldoPendientePorSolicitar()).isEqualByComparingTo("10000.00");
    }

    private Clientes client(String name, User owner) {
        Clientes client = new Clientes();
        client.setNombre(name);
        client.setUser(owner);
        client.setPorcentajeComisionSocio(BigDecimal.ZERO);
        client.setPorcentajeComisionOficina(BigDecimal.ZERO);
        em.persist(client);
        return client;
    }

    private PaymentOperation operation(Clientes client, User owner, OperationStatus status, boolean active) {
        PaymentOperation operation = new PaymentOperation();
        operation.setCliente(client);
        operation.setSocioComercial(owner);
        operation.setMontoTotal(new BigDecimal("10000.00"));
        operation.setMontoValidado(BigDecimal.ZERO);
        operation.setPorcentajeComisionSocio(BigDecimal.ZERO);
        operation.setPorcentajeComisionOficina(BigDecimal.ZERO);
        operation.setEstatus(status);
        operation.setActivo(active);
        em.persist(operation);
        return operation;
    }
}
