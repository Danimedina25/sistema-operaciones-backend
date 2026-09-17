package com.sistemadeoperaciones.pagos.service;

import com.sistemadeoperaciones.clientes.model.Clientes;
import com.sistemadeoperaciones.comisionessocioscomerciales.service.CommercialPartnerCommissionService;
import com.sistemadeoperaciones.configuraciones.service.ConfiguracionGeneralService;
import com.sistemadeoperaciones.notifications.service.NotificationService;
import com.sistemadeoperaciones.pagos.enums.OperationStatus;
import com.sistemadeoperaciones.pagos.model.PaymentOperation;
import com.sistemadeoperaciones.pagos.repository.PaymentOperationRepository;
import com.sistemadeoperaciones.shared.audit.repository.DeletionAuditLogRepository;
import com.sistemadeoperaciones.shared.audit.service.DeletionAuditService;
import com.sistemadeoperaciones.cajageneral.service.CashGeneralService;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.exception.ConflictException;
import com.sistemadeoperaciones.usuarios.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * El estatus se revisa después de tomar el lock, no antes: si otra transacción
 * valida un comprobante mientras el usuario confirma el borrado, la eliminación
 * debe rechazarse en lugar de arrastrar una operación que ya avanzó.
 */
@DataJpaTest
@Import({ PaymentOperationServiceImpl.class, DeletionAuditService.class })
class PaymentOperationDeleteConcurrencyTest {

    @Autowired PaymentOperationService service;
    @Autowired PaymentOperationRepository operationRepository;
    @Autowired DeletionAuditLogRepository auditRepository;
    @Autowired PlatformTransactionManager txManager;
    @Autowired JpaTransactionManager jpaTxManager;

    // La validación de un pago en efectivo entra a Caja General; estas pruebas son de
    // eliminación y no la ejercitan.
    @MockBean CashGeneralService cashGeneralService;
    @MockBean AuthenticatedUserService authenticatedUserService;
    @MockBean NotificationService notificationService;
    @MockBean CommercialPartnerCommissionService commercialPartnerCommissionService;
    @MockBean ConfiguracionGeneralService configuracionGeneralService;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // el servicio usa su propia transacción
    void rejectsDeletionWhenStatusChangedInAnotherTransaction() {
        TransactionTemplate tx = new TransactionTemplate(txManager);

        Long[] ids = tx.execute(status -> {
            var em = jpaTxManager.getEntityManagerFactory().createEntityManager();
            em.getTransaction().begin();

            User u = new User();
            u.setNombre("Ana Admin");
            u.setCorreo("admin+" + System.nanoTime() + "@example.com");
            em.persist(u);

            Clientes c = new Clientes();
            c.setNombre("Cliente Demo");
            c.setUser(u);
            c.setPorcentajeComisionSocio(BigDecimal.ZERO);
            c.setPorcentajeComisionOficina(BigDecimal.ZERO);
            em.persist(c);

            PaymentOperation op = new PaymentOperation();
            op.setCliente(c);
            op.setSocioComercial(u);
            op.setMontoTotal(new BigDecimal("10000.00"));
            op.setMontoValidado(BigDecimal.ZERO);
            op.setPorcentajeComisionSocio(BigDecimal.ZERO);
            op.setPorcentajeComisionOficina(BigDecimal.ZERO);
            op.setEstatus(OperationStatus.PENDIENTE_VALIDACION);
            em.persist(op);

            em.getTransaction().commit();
            Long[] result = { op.getId(), u.getId() };
            em.close();
            return result;
        });

        Long operationId = ids[0];
        User actor = new User();
        actor.setId(ids[1]);
        actor.setNombre("Ana Admin");
        actor.setCorreo("admin@example.com");
        when(authenticatedUserService.getCurrentUser()).thenReturn(actor);

        // Otra transacción avanza la operación antes de que se confirme el borrado.
        tx.executeWithoutResult(status -> {
            var em = jpaTxManager.getEntityManagerFactory().createEntityManager();
            em.getTransaction().begin();
            PaymentOperation op = em.find(PaymentOperation.class, operationId);
            op.setEstatus(OperationStatus.INGRESO_PARCIAL);
            em.merge(op);
            em.getTransaction().commit();
            em.close();
        });

        assertThatThrownBy(() -> service.delete(operationId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Solo se pueden eliminar operaciones pendientes de validación");

        assertThat(operationRepository.findById(operationId)).isPresent();
        assertThat(auditRepository.findAll()).isEmpty();
    }
}
