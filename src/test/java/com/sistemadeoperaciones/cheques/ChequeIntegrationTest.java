package com.sistemadeoperaciones.cheques;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistemadeoperaciones.cajageneral.dto.*;
import com.sistemadeoperaciones.cajageneral.enums.*;
import com.sistemadeoperaciones.cajageneral.repository.*;
import com.sistemadeoperaciones.cajageneral.service.CashGeneralService;
import com.sistemadeoperaciones.clientes.model.Clientes;
import com.sistemadeoperaciones.comisionessocioscomerciales.service.CommercialPartnerCommissionService;
import com.sistemadeoperaciones.configuraciones.service.ConfiguracionGeneralService;
import com.sistemadeoperaciones.corte.dto.BankLedgerFilter;
import com.sistemadeoperaciones.envioscaja.CashBankDepositService;
import com.sistemadeoperaciones.envioscaja.CashBankDepositRepository;
import com.sistemadeoperaciones.envioscaja.CashBankDepositRequests;
import com.sistemadeoperaciones.envioscaja.CashBankDepositStatus;
import com.sistemadeoperaciones.corte.service.*;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.notifications.service.NotificationService;
import com.sistemadeoperaciones.pagos.dto.*;
import com.sistemadeoperaciones.pagos.enums.*;
import com.sistemadeoperaciones.pagos.model.*;
import com.sistemadeoperaciones.pagos.repository.*;
import com.sistemadeoperaciones.pagos.service.PaymentOperationServiceImpl;
import com.sistemadeoperaciones.shared.audit.service.DeletionAuditService;
import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.enums.RoleName;
import com.sistemadeoperaciones.shared.exception.*;
import com.sistemadeoperaciones.usuarios.model.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest(showSql=false)
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@Import({ChequeService.class, ChequeNotifications.class, ChequeIntegrationTest.JsonConfig.class, PaymentOperationServiceImpl.class,
        CashGeneralService.class, BankLedgerQuery.class, DeletionAuditService.class, CashBankDepositService.class})
@Transactional(propagation=Propagation.NOT_SUPPORTED)
class ChequeIntegrationTest {
    @TestConfiguration static class JsonConfig { @Bean ObjectMapper chequeMapper() { return new ObjectMapper().findAndRegisterModules(); } }
    @DynamicPropertySource static void isolatedDatabase(DynamicPropertyRegistry registry) {
        String url = System.getProperty("cheques.test.mysql-url");
        if (url != null) {
            if (!url.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/sdo_cheques_test(\\?.*)?")) throw new IllegalArgumentException("Only an isolated loopback test schema is allowed");
            registry.add("spring.datasource.url", () -> url);
            registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
            registry.add("spring.datasource.username", () -> "root");
            registry.add("spring.datasource.password", () -> "");
            registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
        }
    }
    @Autowired ChequeService service;
    @Autowired CashBankDepositService depositsService;
    @Autowired CashBankDepositRepository deposits;
    @Autowired PaymentOperationServiceImpl payments;
    @Autowired CashGeneralService cash;
    @Autowired CashGeneralMovementRepository movements;
    @Autowired CashGeneralDayRepository days;
    @Autowired OperationPaymentRepository paymentRepository;
    @Autowired ChequeAuditRepository audits;
    @Autowired BankLedgerQuery ledger;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @MockBean AuthenticatedUserService auth;
    @MockBean NotificationService notifications;
    @MockBean CommercialPartnerCommissionService commissions;
    @MockBean ConfiguracionGeneralService configuration;
    @MockBean BankAccountDailyCutService bankCuts;
    @MockBean BusinessAuditService businessAuditService;
    User actor; Long operationId; Long bankId;
    <T> T tx(Supplier<T> work) { return new TransactionTemplate(transactions).execute(s -> work.get()); }
    void run(Runnable work) { tx(() -> { work.run(); return null; }); }
    @BeforeEach void setup() {
        run(() -> {
            Role role = em.createQuery("select r from Role r where r.name=:name", Role.class).setParameter("name", RoleName.ADMIN).getResultStream().findFirst().orElseGet(() -> { Role r = new Role(); r.setName(RoleName.ADMIN); em.persist(r); return r; });
            actor = new User(); actor.setNombre("Admin cheques"); actor.setCorreo(UUID.randomUUID()+"@example.test"); actor.setRoles(new HashSet<>(Set.of(role))); em.persist(actor);
            Clientes c = new Clientes(); c.setNombre("Cliente cheques"); c.setUser(actor); c.setPorcentajeComisionSocio(BigDecimal.ZERO); c.setPorcentajeComisionOficina(BigDecimal.ZERO); em.persist(c);
            PaymentOperation op = new PaymentOperation(); op.setCliente(c); op.setSocioComercial(actor); op.setMontoTotal(new BigDecimal("100.00")); op.setMontoValidado(BigDecimal.ZERO); op.setPorcentajeComisionSocio(BigDecimal.ZERO); op.setPorcentajeComisionOficina(BigDecimal.ZERO); op.setEstatus(OperationStatus.PENDIENTE_VALIDACION); em.persist(op); operationId=op.getId();
            BankAccount b = new BankAccount(); b.setBanco("Banco"); b.setTitular("Empresa"); b.setNumeroCuenta(UUID.randomUUID().toString()); b.setClabe(UUID.randomUUID().toString()); b.setActivo(true); em.persist(b); bankId=b.getId();
        });
        when(auth.getCurrentUser()).thenReturn(actor);
    }
    @AfterEach void cleanupCash() { run(() -> { movements.deleteAll(); movements.flush(); days.deleteAll(); days.flush(); }); }
    String proof() { return "https://firebasestorage.googleapis.com/v0/b/test/o/comprobantes%2Foperaciones%2F"+operationId+"%2Fproof.pdf?alt=media"; }
    CreateOperationPaymentRequestDto request() {
        var r = new CreateOperationPaymentRequestDto(); r.setOperacionId(operationId); r.setMonto(new BigDecimal("100.00")); r.setTipoPago(PaymentType.CHEQUE); r.setNumeroCheque("001"); r.setBancoEmisor("Banco emisor"); r.setEmisor("Cliente"); r.setBeneficiario("Empresa"); r.setComprobanteUrl(proof()); r.setFechaComprobante(LocalDateTime.now()); return r;
    }
    ChequeView received() { return service.byPayment(payments.addPayment(request()).getId()); }
    ChequeCommand command(ChequeView p, ChequeAction action) { return new ChequeCommand(UUID.randomUUID(), p.version(), action, LocalDate.now(), (action==ChequeAction.DEPOSITAR || action==ChequeAction.COBRAR_BANCO) ? bankId : null, proof(), "Motivo", null, null); }
    Long depositId(Long paymentId) { return deposits.findByChequePaymentIdOrderByIdDesc(paymentId).get(0).getId(); }
    ChequeView confirmCheque(ChequeView cheque) {
        depositsService.confirm(depositId(cheque.id()), new CashBankDepositRequests.Confirm(proof()));
        return service.byPayment(cheque.id());
    }
    long bankRows() { return tx(() -> ledger.search(new BankLedgerFilter(LocalDate.now(),LocalDate.now(),bankId,null,null,null), PageRequest.of(0,20)).getTotalElements()); }
    Map<CashDenomination,Integer> counts(int hundreds) { var m = new EnumMap<CashDenomination,Integer>(CashDenomination.class); for (var d:CashDenomination.values()) m.put(d,0); m.put(CashDenomination.D100,hundreds); return m; }
    @Test void receiptAndDepositDoNotRecognizeMoney() {
        var p=received(); assertThat(p.cuentaDestinoId()).isNull(); assertThat(p.estado()).isEqualTo("POR_COBRAR"); assertThat(bankRows()).isZero();
        var deposited=service.act(p.id(),command(p,ChequeAction.DEPOSITAR)); assertThat(deposited.estado()).isEqualTo("DEPOSITADO"); assertThat(bankRows()).isZero();
        assertThat(tx(() -> paymentRepository.findById(p.id()).orElseThrow().getEstatus())).isEqualTo(PaymentStatus.PENDIENTE_VALIDACION);
        verifyNoInteractions(commissions);
    }
    @Test void depositReplaysExactlyAndRejectsChangedBody() {
        var p=received(); var c=command(p,ChequeAction.DEPOSITAR); var deposited=service.act(p.id(),c);
        assertThat(service.act(p.id(),c)).isEqualTo(deposited);
        assertThat(deposits.findByChequePaymentIdOrderByIdDesc(p.id())).hasSize(1);
        assertThat(bankRows()).isZero();
        assertThatThrownBy(() -> service.act(p.id(),new ChequeCommand(c.requestId(),c.version(),ChequeAction.COBRAR_BANCO,c.fecha(),bankId,proof(),"Motivo",null,null))).isInstanceOf(ConflictException.class);
    }
    @Test void onlyLegacyDepositedChequeCanUseDirectBankCollection() {
        var received=received();
        assertThatThrownBy(() -> service.act(received.id(),command(received,ChequeAction.COBRAR_BANCO)))
                .isInstanceOf(ConflictException.class);
        run(() -> {
            var p=paymentRepository.findById(received.id()).orElseThrow();
            p.setChequeEstado("DEPOSITADO"); p.setChequeFechaDeposito(LocalDate.now().atStartOfDay());
            p.setCuentaDestino(em.getReference(BankAccount.class,bankId)); p.setChequeVersion(p.getChequeVersion()+1);
        });
        var legacy=service.byPayment(received.id());
        assertThat(legacy.depositoCompartido()).isFalse();
        assertThat(legacy.depositoId()).isNull();
        var confirmed=service.act(legacy.id(),command(legacy,ChequeAction.COBRAR_BANCO));
        assertThat(confirmed.estado()).isEqualTo("COBRADO");
        assertThat(bankRows()).isEqualTo(1);
    }
    @Test void depositedThenCollectedHasOneBankEntryAndTotalsIgnoreStateFilter() {
        var p=received(); p=service.act(p.id(),command(p,ChequeAction.DEPOSITAR));
        assertThat(p.depositoCompartido()).isTrue();
        assertThat(p.depositoId()).isEqualTo(depositId(p.id()));
        var pending=p;
        assertThatThrownBy(() -> service.act(pending.id(),command(pending,ChequeAction.COBRAR_BANCO))).isInstanceOf(ConflictException.class);
        p=confirmCheque(p);
        assertThat(p.estado()).isEqualTo("COBRADO"); assertThat(bankRows()).isEqualTo(1);
        assertThat(depositsService.confirm(depositId(p.id()), new CashBankDepositRequests.Confirm(proof())).estatus())
                .isEqualTo(CashBankDepositStatus.CONFIRMADO);
        assertThat(bankRows()).isEqualTo(1);
        assertThat(movements.findByPagoId(p.id())).isEmpty();
        var page=service.list("POR_COBRAR","","",operationId,"",null,null,0);
        assertThat(page.content()).isEmpty(); assertThat(page.totales().get(0).cobrados()).isEqualByComparingTo("100");
    }
    @Test void cashHeadValidatesReceivedChequeWithCountInOneStep() {
        var day=cash.open(new OpenCashDayRequest(LocalDate.now(),BigDecimal.ZERO,counts(0)));
        var p=received(); var cashRole=new Role(); cashRole.setName(RoleName.JEFA_CAJAS);
        actor.setRoles(Set.of(cashRole));
        var decision=new ChequeCommand(UUID.randomUUID(),p.version(),ChequeAction.CONFIRMAR_COBRO_EFECTIVO,
                LocalDate.now(),null,proof(),null,day.id(),counts(1));
        var result=service.act(p.id(),decision);
        assertThat(result.estado()).isEqualTo("COBRADO");
        assertThat(result.destinoCobro()).isEqualTo("EFECTIVO");
        assertThat(cash.latest().saldoActual()).isEqualByComparingTo("100");
        assertThat(bankRows()).isZero();
        assertThat(deposits.findByChequePaymentIdOrderByIdDesc(p.id())).isEmpty();
    }
    @Test void concurrentBankConfirmationsCreditChequeOnlyOnce() throws Exception {
        var received=received(); var pending=service.act(received.id(),command(received,ChequeAction.DEPOSITAR));
        Long id=depositId(pending.id());
        var results=parallel(() -> depositsService.confirm(id,new CashBankDepositRequests.Confirm(proof())),
                () -> depositsService.confirm(id,new CashBankDepositRequests.Confirm(proof())));
        assertThat(results).allMatch(com.sistemadeoperaciones.envioscaja.CashBankDepositView.class::isInstance);
        assertThat(bankRows()).isEqualTo(1);
        assertThat(service.byPayment(pending.id()).estado()).isEqualTo("COBRADO");
        assertThat(movements.findByPagoId(pending.id())).isEmpty();
        verify(commissions,times(1)).generateCommissionsForOperation(operationId);
    }
    @Test void cashCollectionCreatesOnlyCashEntry() {
        var day=cash.open(new OpenCashDayRequest(LocalDate.now(),BigDecimal.ZERO,counts(0))); var received=received();
        var p=service.act(received.id(),command(received,ChequeAction.ASIGNAR_COBRO_EFECTIVO));
        assertThat(p.estado()).isEqualTo("PENDIENTE_COBRO_EFECTIVO"); assertThat(movements.findByPagoId(p.id())).isEmpty();
        var c=new ChequeCommand(UUID.randomUUID(),p.version(),ChequeAction.CONFIRMAR_COBRO_EFECTIVO,LocalDate.now(),null,proof(),null,day.id(),counts(1));
        var result=service.act(p.id(),c); assertThat(result.destinoCobro()).isEqualTo("EFECTIVO"); assertThat(bankRows()).isZero();
        var movement=tx(() -> movements.findByPagoId(p.id()).orElseThrow()); assertThat(movement.getTipo()).isEqualTo(CashMovementConcept.COBRO_CHEQUE_CLIENTE); assertThat(movement.getCuentaBancaria()).isNull();
        assertThat(cash.latest().saldoActual()).isEqualByComparingTo("100");
    }
    @Test void aChequeWithARecordedCollectionCannotBeCollectedAgainEvenIfItsStateWasAltered() {
        var day=cash.open(new OpenCashDayRequest(LocalDate.now(),BigDecimal.ZERO,counts(0))); var received=received();
        var assigned=service.act(received.id(),command(received,ChequeAction.ASIGNAR_COBRO_EFECTIVO));
        service.act(assigned.id(),new ChequeCommand(UUID.randomUUID(),assigned.version(),ChequeAction.CONFIRMAR_COBRO_EFECTIVO,LocalDate.now(),null,proof(),null,day.id(),counts(1)));
        // Datos alterados fuera del flujo: el cheque vuelve a "pendiente" y sin movimiento de caja.
        run(() -> {
            movements.findByPagoId(received.id()).ifPresent(movements::delete);
            var p=paymentRepository.findById(received.id()).orElseThrow(); p.setChequeEstado("PENDIENTE_COBRO_EFECTIVO"); p.setEstatus(PaymentStatus.PENDIENTE_VALIDACION); paymentRepository.save(p);
        });
        var altered=service.byPayment(received.id());
        assertThatThrownBy(() -> service.act(altered.id(),new ChequeCommand(UUID.randomUUID(),altered.version(),ChequeAction.CONFIRMAR_COBRO_EFECTIVO,LocalDate.now(),null,proof(),null,day.id(),counts(1))))
                .isInstanceOf(ConflictException.class).hasMessage("Este cheque ya tiene un cobro registrado");
        assertThat(movements.findByPagoId(received.id())).isEmpty();
        assertThat(service.byPayment(received.id()).historial()).extracting(ChequeView.History::accion)
                .filteredOn("CONFIRMAR_COBRO_EFECTIVO"::equals).hasSize(1);
    }
    @Test void cashAssignsTheChequeAndCanReturnItWithoutRejectingThePayment() {
        var p=received();
        var cashRole=new Role(); cashRole.setName(RoleName.JEFA_CAJAS); actor.setRoles(Set.of(cashRole));
        var assigned=service.act(p.id(),new ChequeCommand(UUID.randomUUID(),p.version(),ChequeAction.ASIGNAR_COBRO_EFECTIVO,
                LocalDate.now(),null,null,"Cobrar en ventanilla",null,null));
        assertThat(assigned.estado()).isEqualTo("PENDIENTE_COBRO_EFECTIVO");
        assertThat(tx(() -> paymentRepository.findById(p.id()).orElseThrow().getEstatus())).isEqualTo(PaymentStatus.PENDIENTE_VALIDACION);
        var returned=service.act(p.id(),new ChequeCommand(UUID.randomUUID(),assigned.version(),ChequeAction.DEVOLVER_A_CUENTAS,
                LocalDate.now(),null,null,"Solo permite abono en cuenta",null,null));
        assertThat(returned.estado()).isEqualTo("POR_COBRAR");
        assertThat(returned.historial()).extracting(ChequeView.History::accion)
                .containsSubsequence("ASIGNAR_COBRO_EFECTIVO","DEVOLVER_A_CUENTAS");
        assertThat(movements.findByPagoId(p.id())).isEmpty();
        assertThat(tx(() -> paymentRepository.findById(p.id()).orElseThrow().getEstatus())).isEqualTo(PaymentStatus.PENDIENTE_VALIDACION);
    }
    @Test void cashFailureRollsBackPaymentAndAudit() {
        var received=received(); var p=service.act(received.id(),command(received,ChequeAction.ASIGNAR_COBRO_EFECTIVO));
        var c=new ChequeCommand(UUID.randomUUID(),p.version(),ChequeAction.CONFIRMAR_COBRO_EFECTIVO,LocalDate.now(),null,proof(),null,999L,counts(1));
        assertThatThrownBy(() -> service.act(p.id(),c)).isInstanceOf(BusinessException.class);
        assertThat(service.byPayment(p.id()).estado()).isEqualTo("PENDIENTE_COBRO_EFECTIVO"); assertThat(audits.findByRequestId(c.requestId().toString())).isEmpty(); verifyNoInteractions(commissions);
    }
    @Test void downstreamFailureRollsBackCollection() {
        var p=received(); p=service.act(p.id(),command(p,ChequeAction.DEPOSITAR));
        clearInvocations(notifications); doThrow(new IllegalStateException("commission failure")).when(commissions).generateCommissionsForOperation(operationId);
        Long id=depositId(p.id());
        assertThatThrownBy(() -> depositsService.confirm(id,new CashBankDepositRequests.Confirm(proof()))).isInstanceOf(IllegalStateException.class);
        assertThat(service.byPayment(p.id()).estado()).isEqualTo("DEPOSITADO");
        assertThat(deposits.findById(id).orElseThrow().getEstatus()).isEqualTo(CashBankDepositStatus.PENDIENTE);
        assertThat(bankRows()).isZero(); verifyNoInteractions(notifications);
    }
    @Test void rejectionReleasesReservationForReplacement() {
        var p=received(); var deposited=service.act(p.id(),command(p,ChequeAction.DEPOSITAR));
        var rejected=depositsService.reject(depositId(p.id()),new CashBankDepositRequests.Reject("No acreditado"));
        assertThat(rejected.estatus()).isEqualTo(CashBankDepositStatus.CANCELADO);
        assertThatThrownBy(() -> depositsService.confirmReturn(rejected.id(),
                new CashBankDepositRequests.ConfirmReturn(counts(1)))).isInstanceOf(BadRequestException.class);
        assertThat(service.byPayment(p.id()).estado()).isEqualTo("POR_COBRAR");
        assertThat(bankRows()).isZero();
        var retry=service.act(p.id(),command(service.byPayment(p.id()),ChequeAction.DEPOSITAR));
        assertThat(retry.estado()).isEqualTo("DEPOSITADO");
        assertThat(deposits.findByChequePaymentIdOrderByIdDesc(p.id())).hasSize(2);
    }
    @Test void legacyValidationRemainsInLedgerButCannotBeCollectedAgain() {
        var p=received(); run(() -> { var entity=paymentRepository.findById(p.id()).orElseThrow(); entity.setChequeEstado(null); entity.setEstatus(PaymentStatus.VALIDADA); entity.setCuentaDestino(em.getReference(BankAccount.class,bankId)); entity.setFechaValidacion(LocalDateTime.now()); });
        assertThat(service.byPayment(p.id()).requiereConciliacion()).isTrue(); assertThat(bankRows()).isEqualTo(1);
        assertThatThrownBy(() -> service.act(p.id(),command(p,ChequeAction.COBRAR_BANCO))).isInstanceOf(ConflictException.class);
    }
    @Test void genericValidationAndEditingCollectedChequeAreBlocked() {
        var p=received(); var r=new UpdatePaymentStatusRequestDto(); r.setComprobanteValidacionUrl(proof());
        assertThatThrownBy(() -> payments.validatePayment(p.id(),r)).isInstanceOf(ConflictException.class);
        service.act(p.id(),command(p,ChequeAction.DEPOSITAR));
        var edit=new UpdateOperationPaymentRequestDto(); edit.setComprobanteUrl(proof()); edit.setTipoPago(PaymentType.CHEQUE); edit.setMonto(new BigDecimal("100"));
        assertThatThrownBy(() -> payments.updatePayment(p.id(),edit)).isInstanceOf(ConflictException.class);
    }
    @Test void bankValidationUsesAccountsRoleAndRecordsBeforeAndAfter() {
        var capture = new CreateOperationPaymentRequestDto();
        capture.setOperacionId(operationId);
        capture.setMonto(new BigDecimal("100.00"));
        capture.setTipoPago(PaymentType.TRANSFERENCIA);
        capture.setCuentaDestinoId(bankId);
        capture.setComprobanteUrl(proof());
        capture.setFechaComprobante(LocalDateTime.now());
        var paymentId = payments.addPayment(capture).getId();
        var decision = new UpdatePaymentStatusRequestDto();
        decision.setComprobanteValidacionUrl(proof());

        var manager = new Role(); manager.setName(RoleName.GERENTE); actor.setRoles(Set.of(manager));
        assertThatThrownBy(() -> payments.validatePayment(paymentId, decision)).isInstanceOf(BusinessException.class);
        assertThat(tx(() -> paymentRepository.findById(paymentId).orElseThrow().getEstatus())).isEqualTo(PaymentStatus.PENDIENTE_VALIDACION);

        var accounts = new Role(); accounts.setName(RoleName.JEFA_CUENTAS); actor.setRoles(Set.of(accounts));
        payments.validatePayment(paymentId, decision);
        assertThat(tx(() -> paymentRepository.findById(paymentId).orElseThrow().getEstatus())).isEqualTo(PaymentStatus.VALIDADA);
        verify(businessAuditService).record(eq(actor.getId()), eq("PAYMENT_VALIDATE"), eq("OPERATION_PAYMENT"), eq(paymentId),
                argThat(before -> ((Map<?, ?>) before).get("estatus") == PaymentStatus.PENDIENTE_VALIDACION),
                argThat(after -> ((Map<?, ?>) after).get("estatus") == PaymentStatus.VALIDADA));
    }
    @Test void inactiveAccountFailsWithoutEffects() {
        var p=received(); run(() -> em.find(BankAccount.class,bankId).setActivo(false));
        assertThatThrownBy(() -> service.act(p.id(),command(p,ChequeAction.DEPOSITAR))).isInstanceOf(BusinessException.class); assertThat(bankRows()).isZero();
    }
    @ParameterizedTest @EnumSource(value=RoleName.class,names={"SOCIO_COMERCIAL","GERENTE","DIRECCION","JEFA_CUENTAS","AUXILIAR_CUENTAS"})
    void unauthorizedBankActionsAreRejected(RoleName role) {
        var p=received(); var r=new Role(); r.setName(role); actor.setRoles(Set.of(r));
        assertThatThrownBy(() -> service.act(p.id(),command(p,ChequeAction.DEPOSITAR))).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
    @Test void captureRejectsDestinationAndRetainsMetadataOnEdit() {
        var request=request(); request.setCuentaDestinoId(bankId);
        assertThatThrownBy(() -> payments.addPayment(request)).isInstanceOf(BusinessException.class);
        var p=received(); var edit=new UpdateOperationPaymentRequestDto(); edit.setMonto(new BigDecimal("100")); edit.setTipoPago(PaymentType.CHEQUE);
        edit.setNumeroCheque("002"); edit.setBancoEmisor("Nuevo banco"); edit.setEmisor("Cliente"); edit.setBeneficiario("Empresa"); edit.setComprobanteUrl(proof()); edit.setFechaComprobante(LocalDateTime.now());
        var updated=payments.updatePayment(p.id(),edit); assertThat(updated.getCuentaDestinoId()).isNull(); assertThat(updated.getNumeroCheque()).isEqualTo("002");
        assertThat(service.byPayment(p.id()).version()).isGreaterThan(p.version());
    }
    @Test void fractionalDenominationsAreRejectedAtJsonBoundary() throws Exception {
        var json=new ObjectMapper().findAndRegisterModules();
        assertThatThrownBy(() -> json.readValue("{\"denominaciones\":{\"D100\":1.5}}",ChequeCommand.class)).isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);
    }
    @Test void wrongProofAndDatesCannotCreateFinancialEntries() {
        var p=received(); var c=command(p,ChequeAction.DEPOSITAR);
        assertThatThrownBy(() -> service.act(p.id(),new ChequeCommand(c.requestId(),p.version(),c.accion(),c.fecha(),bankId,"https://example.com/proof",null,null,null))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.act(p.id(),new ChequeCommand(c.requestId(),p.version(),c.accion(),LocalDate.now().plusDays(1),bankId,proof(),null,null,null))).isInstanceOf(BusinessException.class);
        assertThat(bankRows()).isZero();
    }
    @Test void closedPeriodRejectsChequeDispatchAndCollection() {
        var p=received();
        Long cutId=tx(() -> { var cut=new com.sistemadeoperaciones.corte.model.DailyCashCut(); cut.setFecha(LocalDate.now()); cut.setEstatus(com.sistemadeoperaciones.corte.enums.DailyCashCutStatus.CERRADO); em.persist(cut); return cut.getId(); });
        try {
            assertThatThrownBy(() -> service.act(p.id(),command(p,ChequeAction.DEPOSITAR))).isInstanceOf(ConflictException.class);
            assertThat(service.byPayment(p.id()).estado()).isEqualTo("POR_COBRAR");
        } finally { run(() -> em.remove(em.find(com.sistemadeoperaciones.corte.model.DailyCashCut.class,cutId))); }
    }
    @Test void closedCashAndIncorrectDenominationsLeaveTheChequePending() {
        var day=cash.open(new OpenCashDayRequest(LocalDate.now(),BigDecimal.ZERO,counts(0))); var received=received();
        var p=service.act(received.id(),command(received,ChequeAction.ASIGNAR_COBRO_EFECTIVO));
        var bad=new ChequeCommand(UUID.randomUUID(),p.version(),ChequeAction.CONFIRMAR_COBRO_EFECTIVO,LocalDate.now(),null,proof(),null,day.id(),counts(0));
        assertThatThrownBy(() -> service.act(p.id(),bad)).isInstanceOf(BusinessException.class);
        cash.close(day.id(),new CloseCashDayRequest(BigDecimal.ZERO,cash.latest().version(),counts(0),null));
        var closed=new ChequeCommand(UUID.randomUUID(),p.version(),ChequeAction.CONFIRMAR_COBRO_EFECTIVO,LocalDate.now(),null,proof(),null,day.id(),counts(1));
        assertThatThrownBy(() -> service.act(p.id(),closed)).isInstanceOf(BusinessException.class);
        assertThat(service.byPayment(p.id()).estado()).isEqualTo("PENDIENTE_COBRO_EFECTIVO");
    }
    @ParameterizedTest @EnumSource(value=RoleName.class,names={"JEFA_CUENTAS","AUXILIAR_CUENTAS","GERENTE","DIRECCION"})
    void accountingAndReadOnlyRolesCannotReceiveCash(RoleName role) {
        var p=received(); var r=new Role(); r.setName(role); actor.setRoles(Set.of(r));
        assertThatThrownBy(() -> service.act(p.id(),command(p,ChequeAction.CONFIRMAR_COBRO_EFECTIVO))).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
    @Test void concurrentDistinctRequestsCollectOnce() throws Exception {
        var p=received(); var c1=command(p,ChequeAction.DEPOSITAR); var c2=command(p,ChequeAction.DEPOSITAR);
        List<Object> results=parallel(() -> service.act(p.id(),c1), () -> service.act(p.id(),c2));
        assertThat(results.stream().filter(ChequeView.class::isInstance).count()).isEqualTo(1);
        assertThat(deposits.findByChequePaymentIdOrderByIdDesc(p.id())).hasSize(1);
        assertThat(bankRows()).isZero();
    }
    @Test void cashCloseRacingCollectionHasExactlyOneWinner() throws Exception {
        var day=cash.open(new OpenCashDayRequest(LocalDate.now(),BigDecimal.ZERO,counts(0))); var received=received();
        var p=service.act(received.id(),command(received,ChequeAction.ASIGNAR_COBRO_EFECTIVO));
        var c=new ChequeCommand(UUID.randomUUID(),p.version(),ChequeAction.CONFIRMAR_COBRO_EFECTIVO,LocalDate.now(),null,proof(),null,day.id(),counts(1));
        var results=parallel(() -> service.act(p.id(),c), () -> cash.close(day.id(),new CloseCashDayRequest(BigDecimal.ZERO,day.version(),counts(0),null)));
        assertThat(results.stream().filter(BusinessException.class::isInstance).count()).isEqualTo(1);
        if ("COBRADO".equals(service.byPayment(p.id()).estado())) {
            assertThat(cash.latest().closedAt()).isNull(); assertThat(cash.latest().saldoActual()).isEqualByComparingTo("100");
        } else { assertThat(cash.latest().closedAt()).isNotNull(); assertThat(cash.latest().saldoActual()).isEqualByComparingTo("0"); }
    }
    @Test void distinctOperationsDoNotDeadlockOnMissingIdempotencyKeys() throws Exception {
        var p1=received(); var c1=command(p1,ChequeAction.DEPOSITAR);
        operationId=tx(() -> {
            var original=em.find(PaymentOperation.class,operationId);
            var op=new PaymentOperation(); op.setCliente(original.getCliente()); op.setSocioComercial(original.getSocioComercial());
            op.setMontoTotal(new BigDecimal("100")); op.setMontoValidado(BigDecimal.ZERO); op.setPorcentajeComisionOficina(BigDecimal.ZERO); op.setPorcentajeComisionSocio(BigDecimal.ZERO); op.setEstatus(OperationStatus.PENDIENTE_VALIDACION); em.persist(op); return op.getId();
        });
        var p2=received(); var c2=command(p2,ChequeAction.DEPOSITAR);
        var results=parallel(() -> service.act(p1.id(),c1), () -> service.act(p2.id(),c2));
        assertThat(results).allMatch(ChequeView.class::isInstance); assertThat(bankRows()).isZero();
        assertThat(deposits.findByChequePaymentIdOrderByIdDesc(p1.id())).hasSize(1);
        assertThat(deposits.findByChequePaymentIdOrderByIdDesc(p2.id())).hasSize(1);
    }
    @Test void concurrentPartialChequesRecalculateTheSameOperationUsingCurrentRows() throws Exception {
        var r1=request(); r1.setMonto(new BigDecimal("50")); r1.setComprobanteUrl(proof()+"&part=1");
        var r2=request(); r2.setMonto(new BigDecimal("50")); r2.setComprobanteUrl(proof()+"&part=2");
        var p1=service.byPayment(payments.addPayment(r1).getId()); var p2=service.byPayment(payments.addPayment(r2).getId());
        var d1=service.act(p1.id(),command(p1,ChequeAction.DEPOSITAR));
        var d2=service.act(p2.id(),command(p2,ChequeAction.DEPOSITAR));
        var results=parallel(() -> depositsService.confirm(depositId(d1.id()),new CashBankDepositRequests.Confirm(proof())),
                () -> depositsService.confirm(depositId(d2.id()),new CashBankDepositRequests.Confirm(proof())));
        assertThat(results).allMatch(com.sistemadeoperaciones.envioscaja.CashBankDepositView.class::isInstance);
        assertThat(tx(() -> em.find(PaymentOperation.class,operationId).getMontoValidado())).isEqualByComparingTo("100");
        verify(commissions,times(1)).generateCommissionsForOperation(operationId);
    }
    @Test void concurrentSameRequestReturnsSameResult() throws Exception {
        var p=received(); var c=command(p,ChequeAction.DEPOSITAR);
        List<Object> results=parallel(() -> service.act(p.id(),c), () -> service.act(p.id(),c));
        assertThat(results.get(0)).isInstanceOf(ChequeView.class).isEqualTo(results.get(1));
        assertThat(deposits.findByChequePaymentIdOrderByIdDesc(p.id())).hasSize(1); assertThat(bankRows()).isZero();
    }
    List<Object> parallel(Callable<?> first, Callable<?> second) throws Exception {
        var pool=Executors.newFixedThreadPool(2); var start=new CountDownLatch(1);
        try {
            var tasks=List.of(first,second).stream().map(task -> pool.submit(() -> { start.await(); try { return task.call(); } catch (BusinessException ex) { return ex; } })).toList();
            start.countDown(); return List.of(tasks.get(0).get(20,TimeUnit.SECONDS),tasks.get(1).get(20,TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
    }
}
