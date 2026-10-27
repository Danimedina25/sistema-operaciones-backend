package com.sistemadeoperaciones.pagos.service;

import com.sistemadeoperaciones.cajageneral.service.CashGeneralService;
import com.sistemadeoperaciones.clientes.model.Clientes;
import com.sistemadeoperaciones.comisionessocioscomerciales.service.CommercialPartnerCommissionService;
import com.sistemadeoperaciones.configuraciones.service.ConfiguracionGeneralService;
import com.sistemadeoperaciones.notifications.service.NotificationService;
import com.sistemadeoperaciones.pagos.dto.PaymentOperationFilterDto;
import com.sistemadeoperaciones.pagos.enums.OperationStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.model.OperationPayment;
import com.sistemadeoperaciones.pagos.model.PaymentOperation;
import com.sistemadeoperaciones.shared.audit.service.DeletionAuditService;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.enums.RoleName;
import com.sistemadeoperaciones.usuarios.model.Role;
import com.sistemadeoperaciones.usuarios.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * "Pagos de ingreso por registrar" del Socio: depende de lo registrado, no de lo validado.
 * Una operación sin ningún pago validado ya le exige registrar el faltante.
 */
@DataJpaTest
@Import(PaymentOperationServiceImpl.class)
class PaymentOperationMissingIncomeSearchTest {
    @Autowired PaymentOperationService service;
    @PersistenceContext EntityManager em;

    @MockBean CashGeneralService cashGeneralService;
    @MockBean com.sistemadeoperaciones.shared.audit.service.BusinessAuditService businessAuditService;
    @MockBean AuthenticatedUserService authenticatedUserService;
    @MockBean NotificationService notificationService;
    @MockBean CommercialPartnerCommissionService commercialPartnerCommissionService;
    @MockBean ConfiguracionGeneralService configuracionGeneralService;
    @MockBean DeletionAuditService deletionAuditService;

    User socio;
    User otroSocio;
    Clientes cliente;

    @BeforeEach
    void setUp() {
        socio = user("missing-income@example.test");
        otroSocio = user("missing-income-other@example.test");
        cliente = new Clientes();
        cliente.setNombre("Cliente QA");
        cliente.setUser(socio);
        cliente.setPorcentajeComisionSocio(BigDecimal.ZERO);
        cliente.setPorcentajeComisionOficina(BigDecimal.ZERO);
        em.persist(cliente);

        User current = mock(User.class);
        Role role = new Role();
        role.setName(RoleName.SOCIO_COMERCIAL);
        when(current.getId()).thenReturn(socio.getId());
        when(current.getRoles()).thenReturn(Set.of(role));
        when(authenticatedUserService.getCurrentUser()).thenReturn(current);
    }

    @Test
    void listsOperationsWithUnregisteredIncomeEvenBeforeAnyValidation() {
        PaymentOperation sinValidar = operation(socio);
        payment(sinValidar, "6000", PaymentStatus.PENDIENTE_VALIDACION);

        PaymentOperation parcial = operation(socio);
        parcial.setEstatus(OperationStatus.INGRESO_PARCIAL);
        payment(parcial, "3000", PaymentStatus.VALIDADA);

        PaymentOperation conRechazo = operation(socio);
        payment(conRechazo, "9000", PaymentStatus.VALIDADA);
        payment(conRechazo, "3000", PaymentStatus.RECHAZADA);

        PaymentOperation todoRegistrado = operation(socio);
        todoRegistrado.setEstatus(OperationStatus.INGRESO_PARCIAL);
        payment(todoRegistrado, "6000", PaymentStatus.VALIDADA);
        payment(todoRegistrado, "6000", PaymentStatus.PENDIENTE_VALIDACION);

        PaymentOperation ajena = operation(otroSocio);
        payment(ajena, "1000", PaymentStatus.PENDIENTE_VALIDACION);
        em.flush();
        em.clear();

        assertThat(search(true).getContent()).extracting("id")
                .containsExactlyInAnyOrder(sinValidar.getId(), parcial.getId(), conRechazo.getId());
        // Sin el filtro, el listado no cambia.
        assertThat(search(null).getTotalElements()).isEqualTo(4);
    }

    private org.springframework.data.domain.Page<?> search(Boolean missingIncome) {
        PaymentOperationFilterDto filter = new PaymentOperationFilterDto();
        filter.setMissingIncome(missingIncome);
        return service.findMyOperations(filter, PageRequest.of(0, 20));
    }

    private User user(String correo) {
        User user = new User();
        user.setNombre("Socio de prueba");
        user.setCorreo(correo);
        em.persist(user);
        return user;
    }

    private PaymentOperation operation(User owner) {
        PaymentOperation operation = new PaymentOperation();
        operation.setCliente(cliente);
        operation.setSocioComercial(owner);
        operation.setMontoTotal(new BigDecimal("12000.00"));
        operation.setMontoValidado(BigDecimal.ZERO);
        operation.setPorcentajeComisionSocio(BigDecimal.ZERO);
        operation.setPorcentajeComisionOficina(BigDecimal.ZERO);
        operation.setEstatus(OperationStatus.PENDIENTE_VALIDACION);
        operation.setActivo(true);
        em.persist(operation);
        return operation;
    }

    private void payment(PaymentOperation operation, String monto, PaymentStatus status) {
        OperationPayment payment = new OperationPayment();
        payment.setOperacion(operation);
        payment.setMonto(new BigDecimal(monto));
        payment.setTipoPago(PaymentType.TRANSFERENCIA);
        payment.setComprobanteUrl("https://example.com/c.jpg");
        payment.setEstatus(status);
        payment.setRegistradoPor(operation.getSocioComercial());
        payment.setFechaPago(LocalDateTime.now());
        payment.setFechaComprobante(LocalDateTime.now());
        em.persist(payment);
    }
}
