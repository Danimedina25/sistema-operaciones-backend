package com.sistemadeoperaciones.pagos.service;

import com.sistemadeoperaciones.clientes.model.Clientes;
import com.sistemadeoperaciones.pagos.enums.CommissionStatus;
import com.sistemadeoperaciones.comisionessocioscomerciales.models.CommercialPartnerCommission;
import com.sistemadeoperaciones.comisionessocioscomerciales.service.CommercialPartnerCommissionService;
import com.sistemadeoperaciones.configuraciones.service.ConfiguracionGeneralService;
import com.sistemadeoperaciones.notifications.enums.NotificationModule;
import com.sistemadeoperaciones.notifications.enums.NotificationReferenceType;
import com.sistemadeoperaciones.notifications.enums.NotificationType;
import com.sistemadeoperaciones.notifications.models.Notification;
import com.sistemadeoperaciones.notifications.models.UserNotification;
import com.sistemadeoperaciones.notifications.service.NotificationService;
import com.sistemadeoperaciones.pagos.enums.OperationStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.enums.ReturnInstallmentStatus;
import com.sistemadeoperaciones.pagos.enums.ReturnPaymentStatus;
import com.sistemadeoperaciones.pagos.exceptions.PaymentOperationNotFoundException;
import com.sistemadeoperaciones.pagos.model.OperationPayment;
import com.sistemadeoperaciones.pagos.model.OperationReturnInstallment;
import com.sistemadeoperaciones.pagos.model.OperationReturnPayment;
import com.sistemadeoperaciones.pagos.model.PaymentOperation;
import com.sistemadeoperaciones.pagos.repository.OperationPaymentRepository;
import com.sistemadeoperaciones.pagos.repository.PaymentOperationRepository;
import com.sistemadeoperaciones.shared.audit.repository.DeletionAuditLogRepository;
import com.sistemadeoperaciones.shared.audit.service.DeletionAuditService;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.exception.ConflictException;
import com.sistemadeoperaciones.shared.exception.EntityHasDependenciesException;
import com.sistemadeoperaciones.socioscomerciales.models.CommercialPartner;
import com.sistemadeoperaciones.usuarios.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Reglas de negocio del borrado físico de operaciones contra H2.
 *
 * El caso permitido es estrecho a propósito: operación en PENDIENTE_VALIDACION y
 * sin ningún movimiento financiero. Sus comprobantes no financieros (pendientes,
 * en proceso o rechazados) se arrastran junto con las notificaciones que los
 * referenciaban.
 */
@DataJpaTest
@Import({ PaymentOperationServiceImpl.class, DeletionAuditService.class })
class PaymentOperationDeleteTest {

    @Autowired PaymentOperationService service;
    @Autowired PaymentOperationRepository operationRepository;
    @Autowired OperationPaymentRepository paymentRepository;
    @Autowired DeletionAuditLogRepository auditRepository;

    @PersistenceContext EntityManager em;

    @MockBean AuthenticatedUserService authenticatedUserService;
    @MockBean NotificationService notificationService;
    @MockBean CommercialPartnerCommissionService commercialPartnerCommissionService;
    @MockBean ConfiguracionGeneralService configuracionGeneralService;

    private User actor;
    private Clientes cliente;

    @BeforeEach
    void setUp() {
        actor = persistUser("Ana Admin");
        cliente = new Clientes();
        cliente.setNombre("Cliente Demo");
        cliente.setUser(actor);
        cliente.setPorcentajeComisionSocio(BigDecimal.ZERO);
        cliente.setPorcentajeComisionOficina(BigDecimal.ZERO);
        em.persist(cliente);

        when(authenticatedUserService.getCurrentUser()).thenReturn(actor);
    }

    private User persistUser(String nombre) {
        User u = new User();
        u.setNombre(nombre);
        u.setCorreo("user+" + System.nanoTime() + "@example.com");
        em.persist(u);
        return u;
    }

    private PaymentOperation persistOperation(OperationStatus estatus, BigDecimal montoValidado) {
        PaymentOperation op = new PaymentOperation();
        op.setCliente(cliente);
        op.setSocioComercial(actor);
        op.setMontoTotal(new BigDecimal("10000.00"));
        op.setMontoValidado(montoValidado);
        op.setPorcentajeComisionSocio(BigDecimal.ZERO);
        op.setPorcentajeComisionOficina(BigDecimal.ZERO);
        op.setEstatus(estatus);
        em.persist(op);
        return op;
    }

    private PaymentOperation persistPendingOperation() {
        return persistOperation(OperationStatus.PENDIENTE_VALIDACION, BigDecimal.ZERO);
    }

    private OperationPayment persistPayment(PaymentOperation op, PaymentStatus estatus) {
        OperationPayment p = new OperationPayment();
        p.setOperacion(op);
        p.setMonto(new BigDecimal("1000.00"));
        p.setTipoPago(PaymentType.TRANSFERENCIA);
        p.setEstatus(estatus);
        p.setComprobanteUrl("https://files.example/" + System.nanoTime() + ".pdf");
        p.setFechaComprobante(LocalDateTime.now());
        p.setRegistradoPor(actor);
        em.persist(p);
        return p;
    }

    private Notification persistNotification(NotificationReferenceType type, Long referenceId) {
        Notification n = new Notification();
        n.setTitulo("Aviso");
        n.setMensaje("Mensaje de prueba");
        n.setTipo(NotificationType.OPERATION_CREATED);
        n.setModulo(NotificationModule.OPERACIONES);
        n.setReferenceType(type);
        n.setReferenceId(referenceId);
        n.setActionUrl("/operaciones/" + referenceId);
        em.persist(n);

        UserNotification un = new UserNotification();
        un.setNotification(n);
        un.setUsuario(actor);
        em.persist(un);
        return n;
    }

    private OperationReturnPayment persistReturn(PaymentOperation op) {
        OperationReturnPayment r = new OperationReturnPayment();
        r.setOperacion(op);
        r.setMonto(new BigDecimal("500.00"));
        r.setTipoPago(PaymentType.TRANSFERENCIA);
        r.setSolicitadoPor(actor);
        r.setEstatus(ReturnPaymentStatus.SOLICITADO);
        em.persist(r);
        return r;
    }

    private CommercialPartner persistPartner() {
        CommercialPartner cp = new CommercialPartner();
        cp.setNombre("Socio Demo");
        cp.setActivo(true);
        cp.setPorcentajeComision(BigDecimal.ZERO);
        cp.setSocioComercial(actor);
        em.persist(cp);
        return cp;
    }

    // ==========================================================
    // Caso permitido
    // ==========================================================

    @Test
    void deletesPendingOperationAlongWithItsNonFinancialReceipts() {
        PaymentOperation op = persistPendingOperation();
        OperationPayment pendiente = persistPayment(op, PaymentStatus.PENDIENTE_VALIDACION);
        OperationPayment enProceso = persistPayment(op, PaymentStatus.EN_PROCESO);
        OperationPayment rechazado = persistPayment(op, PaymentStatus.RECHAZADA);
        em.flush();

        Long operationId = op.getId();
        List<Long> paymentIds = List.of(pendiente.getId(), enProceso.getId(), rechazado.getId());

        service.delete(operationId);
        em.clear();

        assertThat(operationRepository.findById(operationId)).isEmpty();
        assertThat(paymentRepository.findAllById(paymentIds)).isEmpty();
    }

    @Test
    void removesNotificationsPointingToTheOperationAndItsReceipts() {
        PaymentOperation op = persistPendingOperation();
        OperationPayment pago = persistPayment(op, PaymentStatus.PENDIENTE_VALIDACION);
        em.flush();

        Notification deOperacion = persistNotification(NotificationReferenceType.PAYMENT_OPERATION, op.getId());
        Notification deComprobante = persistNotification(NotificationReferenceType.OPERATION_PAYMENT, pago.getId());
        Notification ajena = persistNotification(NotificationReferenceType.PAYMENT_OPERATION, 999_999L);
        em.flush();

        Long idOperacion = deOperacion.getId();
        Long idComprobante = deComprobante.getId();
        Long idAjena = ajena.getId();

        service.delete(op.getId());
        em.clear();

        assertThat(em.find(Notification.class, idOperacion)).isNull();
        assertThat(em.find(Notification.class, idComprobante)).isNull();
        // No debe tocar notificaciones de otras operaciones.
        assertThat(em.find(Notification.class, idAjena)).isNotNull();

        Long userNotifications = em.createQuery(
                        "SELECT COUNT(un) FROM UserNotification un WHERE un.notification.id IN :ids", Long.class)
                .setParameter("ids", List.of(idOperacion, idComprobante))
                .getSingleResult();
        assertThat(userNotifications).isZero();
    }

    @Test
    void recordsAnAuditEntryForTheDeletedOperation() {
        PaymentOperation op = persistPendingOperation();
        persistPayment(op, PaymentStatus.RECHAZADA);
        em.flush();
        Long operationId = op.getId();

        service.delete(operationId);
        em.clear();

        var logs = auditRepository.findAll();
        assertThat(logs).hasSize(1);
        var log = logs.get(0);
        assertThat(log.getEntityType()).isEqualTo("PAYMENT_OPERATION");
        assertThat(log.getEntityId()).isEqualTo(operationId);
        assertThat(log.getDeletedByUserId()).isEqualTo(actor.getId());
        assertThat(log.getEntityLabel())
                .contains("Op #" + operationId)
                .contains("Cliente Demo")
                .contains("10000.00")
                .contains("PENDIENTE_VALIDACION")
                .contains("Comprobantes eliminados: 1");
        assertThat(log.getEntityLabel().length()).isLessThanOrEqualTo(255);
    }

    // ==========================================================
    // Estatus
    // ==========================================================

    @ParameterizedTest
    @EnumSource(value = OperationStatus.class, names = "PENDIENTE_VALIDACION", mode = EnumSource.Mode.EXCLUDE)
    void rejectsOperationsThatAreNoLongerPendingValidation(OperationStatus estatus) {
        PaymentOperation op = persistOperation(estatus, BigDecimal.ZERO);
        em.flush();

        assertThatThrownBy(() -> service.delete(op.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Solo se pueden eliminar operaciones pendientes de validación");
    }

    @Test
    void deletesPendingOperationEvenWhenInactive() {
        PaymentOperation op = persistPendingOperation();
        op.setActivo(false);
        em.flush();
        Long operationId = op.getId();

        service.delete(operationId);
        em.clear();

        assertThat(operationRepository.findById(operationId)).isEmpty();
    }

    @Test
    void missingOperationIsNotFound() {
        assertThatThrownBy(() -> service.delete(999_999L))
                .isInstanceOf(PaymentOperationNotFoundException.class);
    }

    // ==========================================================
    // Dependencias financieras
    // ==========================================================

    @Test
    void rejectsOperationWithValidatedPayment() {
        PaymentOperation op = persistPendingOperation();
        persistPayment(op, PaymentStatus.VALIDADA);
        em.flush();

        assertThatThrownBy(() -> service.delete(op.getId()))
                .isInstanceOf(EntityHasDependenciesException.class)
                .hasMessage("La operación no puede eliminarse porque ya tiene movimientos financieros asociados");
    }

    @Test
    void rejectsOperationWithValidatedAmount() {
        PaymentOperation op = persistOperation(OperationStatus.PENDIENTE_VALIDACION, new BigDecimal("2500.00"));
        em.flush();

        var error = catchThrowableOfType(
                () -> service.delete(op.getId()), EntityHasDependenciesException.class);

        assertThat(error.getDependencies()).containsKey("montoValidado");
    }

    @Test
    void rejectsOperationWithReturnRequest() {
        PaymentOperation op = persistPendingOperation();
        persistReturn(op);
        em.flush();

        var error = catchThrowableOfType(
                () -> service.delete(op.getId()), EntityHasDependenciesException.class);

        assertThat(error.getDependencies()).containsKey("retornos");
    }

    @Test
    void rejectsOperationWithReturnInstallment() {
        PaymentOperation op = persistPendingOperation();
        OperationReturnPayment solicitud = persistReturn(op);

        OperationReturnInstallment parcialidad = new OperationReturnInstallment();
        parcialidad.setSolicitud(solicitud);
        parcialidad.setMonto(new BigDecimal("100.00"));
        parcialidad.setTipoPago(PaymentType.EFECTIVO);
        parcialidad.setEstatus(ReturnInstallmentStatus.PROGRAMADA);
        parcialidad.setCreadoPor(actor);
        em.persist(parcialidad);
        em.flush();

        var error = catchThrowableOfType(
                () -> service.delete(op.getId()), EntityHasDependenciesException.class);

        assertThat(error.getDependencies()).containsKey("parcialidadesDeRetorno");
    }

    @Test
    void rejectsOperationWithModernCommission() {
        PaymentOperation op = persistPendingOperation();

        CommercialPartnerCommission comision = new CommercialPartnerCommission();
        comision.setOperation(op);
        comision.setCommercialPartner(persistPartner());
        comision.setCommissionAmount(new BigDecimal("50.00"));
        comision.setCommissionPercentage(new BigDecimal("1.00"));
        comision.setBaseAmount(new BigDecimal("5000.00"));
        comision.setStatus(CommissionStatus.GENERADA);
        comision.setNivel(1);
        em.persist(comision);
        em.flush();

        var error = catchThrowableOfType(
                () -> service.delete(op.getId()), EntityHasDependenciesException.class);

        assertThat(error.getDependencies()).containsKey("comisiones");
    }

    @Test
    void rejectsOperationWithLegacyCommission() {
        PaymentOperation op = persistPendingOperation();
        CommercialPartner partner = persistPartner();
        em.flush();

        // operation_commissions es una tabla viva sin repositorio ni setters:
        // se inserta con SQL nativo, igual que la consultaría UserDeletionGuard.
        em.createNativeQuery("""
                INSERT INTO operation_commissions
                    (operacion_id, socio_comercial_id, nivel, porcentaje, monto, estatus)
                VALUES (?, ?, 1, 1.00, 50.00, 'GENERADA')
                """)
                .setParameter(1, op.getId())
                .setParameter(2, partner.getId())
                .executeUpdate();

        var error = catchThrowableOfType(
                () -> service.delete(op.getId()), EntityHasDependenciesException.class);

        assertThat(error.getDependencies()).containsKey("comisionesHistoricas");
    }

    @Test
    void doesNotLeaveAnAuditEntryWhenDeletionIsRejected() {
        PaymentOperation op = persistPendingOperation();
        persistPayment(op, PaymentStatus.VALIDADA);
        em.flush();

        assertThatThrownBy(() -> service.delete(op.getId()))
                .isInstanceOf(EntityHasDependenciesException.class);

        assertThat(auditRepository.findAll()).isEmpty();
        assertThat(operationRepository.findById(op.getId())).isPresent();
    }
}
