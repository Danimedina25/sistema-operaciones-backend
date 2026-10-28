package com.sistemadeoperaciones.pagos.service;

import com.sistemadeoperaciones.cajageneral.service.CashGeneralService;
import com.sistemadeoperaciones.clientes.model.Clientes;
import com.sistemadeoperaciones.comisionessocioscomerciales.service.CommercialPartnerCommissionService;
import com.sistemadeoperaciones.configuraciones.service.ConfiguracionGeneralService;
import com.sistemadeoperaciones.notifications.enums.NotificationType;
import com.sistemadeoperaciones.notifications.service.NotificationService;
import com.sistemadeoperaciones.pagos.enums.OperationStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.model.OperationPayment;
import com.sistemadeoperaciones.pagos.model.PaymentOperation;
import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
import com.sistemadeoperaciones.shared.audit.service.DeletionAuditService;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.enums.RoleName;
import com.sistemadeoperaciones.usuarios.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Los avisos del cheque llegan a quien tiene trabajo que hacer con él: Cajas lo valida, Cuentas solo si se deposita. */
@DataJpaTest
@Import(PaymentOperationServiceImpl.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED) // notifyChequeChanged abre su propia transacción
class ChequeNotificationRecipientsTest {
    @Autowired PaymentOperationServiceImpl service;
    @Autowired JpaTransactionManager jpaTxManager;

    @MockBean CashGeneralService cashGeneralService;
    @MockBean BusinessAuditService businessAuditService;
    @MockBean AuthenticatedUserService authenticatedUserService;
    @MockBean NotificationService notificationService;
    @MockBean CommercialPartnerCommissionService commercialPartnerCommissionService;
    @MockBean ConfiguracionGeneralService configuracionGeneralService;
    @MockBean DeletionAuditService deletionAuditService;

    @Test
    void aNewChequeIsAnnouncedToCajasNotToCuentas() {
        Long paymentId = persistCheque(PaymentStatus.PENDIENTE_VALIDACION);

        service.notifyChequeChanged(paymentId, "REGISTRAR", "Socio de prueba");

        verify(notificationService).createForRoles(eq(List.of(RoleName.JEFA_CAJAS, RoleName.ADMIN)),
                eq("Nuevo comprobante pendiente de validación"), anyString(), eq(NotificationType.PAYMENT_SUBMITTED),
                any(), any(), anyLong(), anyString(), any());
    }

    @Test
    void collectingAChequeInCashOnlyNotifiesThePartner() {
        Long paymentId = persistCheque(PaymentStatus.VALIDADA);

        service.notifyChequeChanged(paymentId, "CONFIRMAR_COBRO_EFECTIVO", "Jefa de Cajas");

        verify(notificationService).createForUser(anyLong(), eq("Comprobante validado"), anyString(),
                eq(NotificationType.PAYMENT_VALIDATED), any(), any(), eq(paymentId), anyString(), any());
        verify(notificationService, never()).createForRoles(anyList(), anyString(), anyString(), any(),
                any(), any(), anyLong(), anyString(), any());
    }

    private Long persistCheque(PaymentStatus status) {
        var em = jpaTxManager.getEntityManagerFactory().createEntityManager();
        em.getTransaction().begin();
        User socio = new User();
        socio.setNombre("Socio de prueba");
        socio.setCorreo("cheque-notif+" + System.nanoTime() + "@example.test");
        em.persist(socio);

        Clientes cliente = new Clientes();
        cliente.setNombre("QA-Cliente");
        cliente.setUser(socio);
        cliente.setPorcentajeComisionSocio(BigDecimal.ZERO);
        cliente.setPorcentajeComisionOficina(BigDecimal.ZERO);
        em.persist(cliente);

        PaymentOperation operation = new PaymentOperation();
        operation.setCliente(cliente);
        operation.setSocioComercial(socio);
        operation.setMontoTotal(new BigDecimal("1500.00"));
        operation.setMontoValidado(BigDecimal.ZERO);
        operation.setPorcentajeComisionSocio(BigDecimal.ZERO);
        operation.setPorcentajeComisionOficina(BigDecimal.ZERO);
        operation.setEstatus(OperationStatus.PENDIENTE_VALIDACION);
        operation.setActivo(true);
        em.persist(operation);

        OperationPayment payment = new OperationPayment();
        payment.setOperacion(operation);
        payment.setMonto(new BigDecimal("1500.00"));
        payment.setTipoPago(PaymentType.CHEQUE);
        payment.setNumeroCheque("123");
        payment.setComprobanteUrl("https://example.com/cheque.jpg");
        payment.setEstatus(status);
        payment.setRegistradoPor(socio);
        payment.setFechaPago(LocalDateTime.now());
        payment.setFechaComprobante(LocalDateTime.now());
        em.persist(payment);
        em.getTransaction().commit();
        em.close();
        return payment.getId();
    }
}
