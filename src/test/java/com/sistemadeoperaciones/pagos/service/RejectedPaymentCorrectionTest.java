package com.sistemadeoperaciones.pagos.service;

import com.sistemadeoperaciones.cajageneral.service.CashGeneralService;
import com.sistemadeoperaciones.clientes.model.Clientes;
import com.sistemadeoperaciones.comisionessocioscomerciales.service.CommercialPartnerCommissionService;
import com.sistemadeoperaciones.configuraciones.service.ConfiguracionGeneralService;
import com.sistemadeoperaciones.notifications.service.NotificationService;
import com.sistemadeoperaciones.pagos.dto.UpdateOperationPaymentRequestDto;
import com.sistemadeoperaciones.pagos.enums.OperationStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.model.OperationPayment;
import com.sistemadeoperaciones.pagos.model.PaymentOperation;
import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** El Socio corrige un comprobante rechazado sobre el mismo pago, que vuelve a validación. */
@DataJpaTest
@Import(PaymentOperationServiceImpl.class)
class RejectedPaymentCorrectionTest {
    @Autowired PaymentOperationService service;
    @PersistenceContext EntityManager em;

    @MockBean CashGeneralService cashGeneralService;
    @MockBean BusinessAuditService businessAuditService;
    @MockBean AuthenticatedUserService authenticatedUserService;
    @MockBean NotificationService notificationService;
    @MockBean CommercialPartnerCommissionService commercialPartnerCommissionService;
    @MockBean ConfiguracionGeneralService configuracionGeneralService;
    @MockBean DeletionAuditService deletionAuditService;

    User socio;
    User cajas;
    PaymentOperation operation;

    @BeforeEach
    void setUp() {
        socio = user("rejected-owner@example.test");
        cajas = user("rejected-cajas@example.test");
        Clientes cliente = new Clientes();
        cliente.setNombre("QA-Cliente");
        cliente.setUser(socio);
        cliente.setPorcentajeComisionSocio(BigDecimal.ZERO);
        cliente.setPorcentajeComisionOficina(BigDecimal.ZERO);
        em.persist(cliente);

        operation = new PaymentOperation();
        operation.setCliente(cliente);
        operation.setSocioComercial(socio);
        operation.setMontoTotal(new BigDecimal("12000.00"));
        operation.setMontoValidado(BigDecimal.ZERO);
        operation.setPorcentajeComisionSocio(BigDecimal.ZERO);
        operation.setPorcentajeComisionOficina(BigDecimal.ZERO);
        operation.setEstatus(OperationStatus.PENDIENTE_VALIDACION);
        operation.setActivo(true);
        em.persist(operation);
        actAs(socio);
    }

    @Test
    void correctingARejectedPaymentReturnsItToValidationAndIsAudited() {
        OperationPayment rejected = payment("3000", PaymentStatus.RECHAZADA);

        var result = service.updatePayment(rejected.getId(), edit("3000", "https://example.com/nuevo.jpg"));

        assertThat(result.getEstatus()).isEqualTo(PaymentStatus.PENDIENTE_VALIDACION);
        em.flush();
        em.clear();
        OperationPayment stored = em.find(OperationPayment.class, rejected.getId());
        assertThat(stored.getEstatus()).isEqualTo(PaymentStatus.PENDIENTE_VALIDACION);
        assertThat(stored.getComprobanteUrl()).isEqualTo("https://example.com/nuevo.jpg");
        assertThat(stored.getValidadoPor()).isNull();
        assertThat(stored.getFechaValidacion()).isNull();
        assertThat(stored.getObservaciones()).isNull();
        verify(businessAuditService).record(anyLong(), eq("PAYMENT_RESUBMIT"), eq("OPERATION_PAYMENT"),
                eq(rejected.getId()), any(), any());
    }

    @Test
    void aValidatedPaymentStillCannotBeEdited() {
        OperationPayment validated = payment("3000", PaymentStatus.VALIDADA);
        assertThatThrownBy(() -> service.updatePayment(validated.getId(), edit("3000", "https://example.com/otro.jpg")))
                .hasMessageContaining("pendientes de validación o rechazados");
    }

    @Test
    void anotherPartnerCannotCorrectIt() {
        OperationPayment rejected = payment("3000", PaymentStatus.RECHAZADA);
        actAs(user("rejected-other@example.test"));
        assertThatThrownBy(() -> service.updatePayment(rejected.getId(), edit("3000", "https://example.com/ajeno.jpg")))
                .hasMessageContaining("No tienes permiso");
        em.flush();
        em.clear();
        assertThat(em.find(OperationPayment.class, rejected.getId()).getEstatus()).isEqualTo(PaymentStatus.RECHAZADA);
    }

    @Test
    void theCorrectionCannotExceedTheOperationTotal() {
        OperationPayment rejected = payment("3000", PaymentStatus.RECHAZADA);
        // Mientras estaba rechazado, el Socio registró el resto.
        payment("10000", PaymentStatus.PENDIENTE_VALIDACION);
        assertThatThrownBy(() -> service.updatePayment(rejected.getId(), edit("3000", "https://example.com/excede.jpg")))
                .isInstanceOf(com.sistemadeoperaciones.pagos.exceptions.PaymentAmountExceededException.class);
    }

    private void actAs(User user) {
        User current = mock(User.class);
        Role role = new Role();
        role.setName(RoleName.SOCIO_COMERCIAL);
        when(current.getId()).thenReturn(user.getId());
        when(current.getNombre()).thenReturn(user.getNombre());
        when(current.getRoles()).thenReturn(Set.of(role));
        when(authenticatedUserService.getCurrentUser()).thenReturn(current);
    }

    private UpdateOperationPaymentRequestDto edit(String monto, String comprobante) {
        UpdateOperationPaymentRequestDto request = new UpdateOperationPaymentRequestDto();
        request.setMonto(new BigDecimal(monto));
        request.setTipoPago(PaymentType.EFECTIVO);
        request.setComprobanteUrl(comprobante);
        request.setFechaComprobante(LocalDateTime.now());
        return request;
    }

    private User user(String correo) {
        User user = new User();
        user.setNombre("Usuario " + correo);
        user.setCorreo(correo);
        em.persist(user);
        return user;
    }

    private OperationPayment payment(String monto, PaymentStatus status) {
        OperationPayment payment = new OperationPayment();
        payment.setOperacion(operation);
        payment.setMonto(new BigDecimal(monto));
        payment.setTipoPago(PaymentType.EFECTIVO);
        payment.setComprobanteUrl("https://example.com/" + System.nanoTime() + ".jpg");
        payment.setEstatus(status);
        payment.setRegistradoPor(socio);
        payment.setFechaPago(LocalDateTime.now());
        payment.setFechaComprobante(LocalDateTime.now());
        if (status != PaymentStatus.PENDIENTE_VALIDACION) {
            payment.setValidadoPor(cajas);
            payment.setFechaValidacion(LocalDateTime.now());
        }
        if (status == PaymentStatus.RECHAZADA) payment.setObservaciones("Comprobante ilegible");
        em.persist(payment);
        em.flush();
        return payment;
    }
}
