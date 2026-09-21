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
import com.sistemadeoperaciones.corte.service.*;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.notifications.service.NotificationService;
import com.sistemadeoperaciones.pagos.dto.*;
import com.sistemadeoperaciones.pagos.enums.*;
import com.sistemadeoperaciones.pagos.model.*;
import com.sistemadeoperaciones.pagos.repository.*;
import com.sistemadeoperaciones.pagos.service.PaymentOperationServiceImpl;
import com.sistemadeoperaciones.shared.audit.service.DeletionAuditService;
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
        CashGeneralService.class, BankLedgerQuery.class, DeletionAuditService.class})
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
    long bankRows() { return tx(() -> ledger.search(new BankLedgerFilter(LocalDate.now(),LocalDate.now(),bankId,null,null,null), PageRequest.of(0,20)).getTotalElements()); }
    Map<CashDenomination,Integer> counts(int hundreds) { var m = new EnumMap<CashDenomination,Integer>(CashDenomination.class); for (var d:CashDenomination.values()) m.put(d,0); m.put(CashDenomination.D100,hundreds); return m; }
    @Test void receiptAndDepositDoNotRecognizeMoney() {
        var p=received(); assertThat(p.cuentaDestinoId()).isNull(); assertThat(p.estado()).isEqualTo("POR_COBRAR"); assertThat(bankRows()).isZero();
        var deposited=service.act(p.id(),command(p,ChequeAction.DEPOSITAR)); assertThat(deposited.estado()).isEqualTo("DEPOSITADO"); assertThat(bankRows()).isZero();
        assertThat(tx(() -> paymentRepository.findById(p.id()).orElseThrow().getEstatus())).isEqualTo(PaymentStatus.PENDIENTE_VALIDACION);
        verifyNoInteractions(commissions);
    }
    @Test void directBankCollectionReplaysExactlyAndRejectsChangedBody() {
        var p=received(); var c=command(p,ChequeAction.COBRAR_BANCO); var collected=service.act(p.id(),c);
        assertThat(service.act(p.id(),c)).isEqualTo(collected); assertThat(bankRows()).isEqualTo(1);
        verify(commissions,times(1)).generateCommissionsForOperation(operationId);
        assertThatThrownBy(() -> service.act(p.id(),new ChequeCommand(c.requestId(),c.version(),ChequeAction.DEPOSITAR,c.fecha(),bankId,proof(),"Motivo",null,null))).isInstanceOf(ConflictException.class);
    }
    @Test void depositedThenCollectedHasOneBankEntryAndTotalsIgnoreStateFilter() {
        var p=received(); p=service.act(p.id(),command(p,ChequeAction.DEPOSITAR)); p=service.act(p.id(),command(p,ChequeAction.COBRAR_BANCO));
        assertThat(p.estado()).isEqualTo("COBRADO"); assertThat(bankRows()).isEqualTo(1);
        var page=service.list("POR_COBRAR","","",operationId,"",null,null,0);
        assertThat(page.content()).isEmpty(); assertThat(page.totales().get(0).cobrados()).isEqualByComparingTo("100");
    }
    @Test void cashCollectionCreatesOnlyCashEntry() {
        var day=cash.open(new OpenCashDayRequest(LocalDate.now(),BigDecimal.ZERO,counts(0))); var p=received();
        var c=new ChequeCommand(UUID.randomUUID(),p.version(),ChequeAction.COBRAR_EFECTIVO,LocalDate.now(),null,proof(),null,day.id(),counts(1));
        var result=service.act(p.id(),c); assertThat(result.destinoCobro()).isEqualTo("EFECTIVO"); assertThat(bankRows()).isZero();
        var movement=tx(() -> movements.findByPagoId(p.id()).orElseThrow()); assertThat(movement.getTipo()).isEqualTo(CashMovementConcept.COBRO_CHEQUE_CLIENTE); assertThat(movement.getCuentaBancaria()).isNull();
        assertThat(cash.latest().saldoActual()).isEqualByComparingTo("100");
    }
    @Test void cashFailureRollsBackPaymentAndAudit() {
        var p=received(); var c=new ChequeCommand(UUID.randomUUID(),p.version(),ChequeAction.COBRAR_EFECTIVO,LocalDate.now(),null,proof(),null,999L,counts(1));
        assertThatThrownBy(() -> service.act(p.id(),c)).isInstanceOf(BusinessException.class);
        assertThat(service.byPayment(p.id()).estado()).isEqualTo("POR_COBRAR"); assertThat(audits.findByRequestId(c.requestId().toString())).isEmpty(); verifyNoInteractions(commissions);
    }
    @Test void downstreamFailureRollsBackCollection() {
        var p=received(); clearInvocations(notifications); doThrow(new IllegalStateException("commission failure")).when(commissions).generateCommissionsForOperation(operationId);
        var c=command(p,ChequeAction.COBRAR_BANCO); assertThatThrownBy(() -> service.act(p.id(),c)).isInstanceOf(IllegalStateException.class);
        assertThat(service.byPayment(p.id()).estado()).isEqualTo("POR_COBRAR"); assertThat(bankRows()).isZero(); assertThat(audits.findByRequestId(c.requestId().toString())).isEmpty(); verifyNoInteractions(notifications);
    }
    @Test void rejectionReleasesReservationForReplacement() {
        var p=received(); service.act(p.id(),command(p,ChequeAction.DEVOLVER));
        assertThat(payments.addPayment(request()).getId()).isNotEqualTo(p.id()); assertThat(bankRows()).isZero();
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
    @Test void inactiveAccountFailsWithoutEffects() {
        var p=received(); run(() -> em.find(BankAccount.class,bankId).setActivo(false));
        assertThatThrownBy(() -> service.act(p.id(),command(p,ChequeAction.COBRAR_BANCO))).isInstanceOf(BusinessException.class); assertThat(bankRows()).isZero();
    }
    @ParameterizedTest @EnumSource(value=RoleName.class,names={"SOCIO_COMERCIAL","GERENTE","DIRECCION","JEFA_CAJAS"})
    void unauthorizedBankActionsAreRejected(RoleName role) {
        var p=received(); var r=new Role(); r.setName(role); actor.setRoles(Set.of(r));
        assertThatThrownBy(() -> service.act(p.id(),command(p,ChequeAction.COBRAR_BANCO))).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
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
        var p=received(); var c=command(p,ChequeAction.COBRAR_BANCO);
        assertThatThrownBy(() -> service.act(p.id(),new ChequeCommand(c.requestId(),p.version(),c.accion(),c.fecha(),bankId,"https://example.com/proof",null,null,null))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.act(p.id(),new ChequeCommand(c.requestId(),p.version(),c.accion(),LocalDate.now().plusDays(1),bankId,proof(),null,null,null))).isInstanceOf(BusinessException.class);
        assertThat(bankRows()).isZero();
    }
    @Test void closedPeriodRejectsCollectionButStillAllowsRecordingADeposit() {
        var p=received();
        Long cutId=tx(() -> { var cut=new com.sistemadeoperaciones.corte.model.DailyCashCut(); cut.setFecha(LocalDate.now()); cut.setEstatus(com.sistemadeoperaciones.corte.enums.DailyCashCutStatus.CERRADO); em.persist(cut); return cut.getId(); });
        try {
            assertThatThrownBy(() -> service.act(p.id(),command(p,ChequeAction.COBRAR_BANCO))).isInstanceOf(ConflictException.class);
            assertThat(service.act(p.id(),command(p,ChequeAction.DEPOSITAR)).estado()).isEqualTo("DEPOSITADO");
        } finally { run(() -> em.remove(em.find(com.sistemadeoperaciones.corte.model.DailyCashCut.class,cutId))); }
    }
    @Test void closedCashAndIncorrectDenominationsLeaveTheChequePending() {
        var day=cash.open(new OpenCashDayRequest(LocalDate.now(),BigDecimal.ZERO,counts(0))); var p=received();
        var bad=new ChequeCommand(UUID.randomUUID(),p.version(),ChequeAction.COBRAR_EFECTIVO,LocalDate.now(),null,proof(),null,day.id(),counts(0));
        assertThatThrownBy(() -> service.act(p.id(),bad)).isInstanceOf(BusinessException.class);
        cash.close(day.id(),new CloseCashDayRequest(BigDecimal.ZERO,cash.latest().version(),counts(0),null));
        var closed=new ChequeCommand(UUID.randomUUID(),p.version(),ChequeAction.COBRAR_EFECTIVO,LocalDate.now(),null,proof(),null,day.id(),counts(1));
        assertThatThrownBy(() -> service.act(p.id(),closed)).isInstanceOf(BusinessException.class);
        assertThat(service.byPayment(p.id()).estado()).isEqualTo("POR_COBRAR");
    }
    @ParameterizedTest @EnumSource(value=RoleName.class,names={"JEFA_CUENTAS","AUXILIAR_CUENTAS","GERENTE","DIRECCION"})
    void accountingAndReadOnlyRolesCannotReceiveCash(RoleName role) {
        var p=received(); var r=new Role(); r.setName(role); actor.setRoles(Set.of(r));
        assertThatThrownBy(() -> service.act(p.id(),command(p,ChequeAction.COBRAR_EFECTIVO))).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
    @Test void concurrentDistinctRequestsCollectOnce() throws Exception {
        var p=received(); var c1=command(p,ChequeAction.COBRAR_BANCO); var c2=command(p,ChequeAction.COBRAR_BANCO);
        List<Object> results=parallel(() -> service.act(p.id(),c1), () -> service.act(p.id(),c2));
        assertThat(results.stream().filter(ChequeView.class::isInstance).count()).isEqualTo(1); assertThat(bankRows()).isEqualTo(1); verify(commissions,times(1)).generateCommissionsForOperation(operationId);
    }
    @Test void cashCloseRacingCollectionHasExactlyOneWinner() throws Exception {
        var day=cash.open(new OpenCashDayRequest(LocalDate.now(),BigDecimal.ZERO,counts(0))); var p=received();
        var c=new ChequeCommand(UUID.randomUUID(),p.version(),ChequeAction.COBRAR_EFECTIVO,LocalDate.now(),null,proof(),null,day.id(),counts(1));
        var results=parallel(() -> service.act(p.id(),c), () -> cash.close(day.id(),new CloseCashDayRequest(BigDecimal.ZERO,day.version(),counts(0),null)));
        assertThat(results.stream().filter(BusinessException.class::isInstance).count()).isEqualTo(1);
        if ("COBRADO".equals(service.byPayment(p.id()).estado())) {
            assertThat(cash.latest().closedAt()).isNull(); assertThat(cash.latest().saldoActual()).isEqualByComparingTo("100");
        } else { assertThat(cash.latest().closedAt()).isNotNull(); assertThat(cash.latest().saldoActual()).isEqualByComparingTo("0"); }
    }
    @Test void distinctOperationsDoNotDeadlockOnMissingIdempotencyKeys() throws Exception {
        var p1=received(); var c1=command(p1,ChequeAction.COBRAR_BANCO);
        operationId=tx(() -> {
            var original=em.find(PaymentOperation.class,operationId);
            var op=new PaymentOperation(); op.setCliente(original.getCliente()); op.setSocioComercial(original.getSocioComercial());
            op.setMontoTotal(new BigDecimal("100")); op.setMontoValidado(BigDecimal.ZERO); op.setPorcentajeComisionOficina(BigDecimal.ZERO); op.setPorcentajeComisionSocio(BigDecimal.ZERO); op.setEstatus(OperationStatus.PENDIENTE_VALIDACION); em.persist(op); return op.getId();
        });
        var p2=received(); var c2=command(p2,ChequeAction.COBRAR_BANCO);
        var results=parallel(() -> service.act(p1.id(),c1), () -> service.act(p2.id(),c2));
        assertThat(results).allMatch(ChequeView.class::isInstance); assertThat(bankRows()).isEqualTo(2);
    }
    @Test void concurrentPartialChequesRecalculateTheSameOperationUsingCurrentRows() throws Exception {
        var r1=request(); r1.setMonto(new BigDecimal("50")); r1.setComprobanteUrl(proof()+"&part=1");
        var r2=request(); r2.setMonto(new BigDecimal("50")); r2.setComprobanteUrl(proof()+"&part=2");
        var p1=service.byPayment(payments.addPayment(r1).getId()); var p2=service.byPayment(payments.addPayment(r2).getId());
        var results=parallel(() -> service.act(p1.id(),command(p1,ChequeAction.COBRAR_BANCO)), () -> service.act(p2.id(),command(p2,ChequeAction.COBRAR_BANCO)));
        assertThat(results).allMatch(ChequeView.class::isInstance);
        assertThat(tx(() -> em.find(PaymentOperation.class,operationId).getMontoValidado())).isEqualByComparingTo("100");
        verify(commissions,times(1)).generateCommissionsForOperation(operationId);
    }
    @Test void concurrentSameRequestReturnsSameResult() throws Exception {
        var p=received(); var c=command(p,ChequeAction.COBRAR_BANCO);
        List<Object> results=parallel(() -> service.act(p.id(),c), () -> service.act(p.id(),c));
        assertThat(results.get(0)).isInstanceOf(ChequeView.class).isEqualTo(results.get(1)); assertThat(bankRows()).isEqualTo(1);
    }
    List<Object> parallel(Callable<?> first, Callable<?> second) throws Exception {
        var pool=Executors.newFixedThreadPool(2); var start=new CountDownLatch(1);
        try {
            var tasks=List.of(first,second).stream().map(task -> pool.submit(() -> { start.await(); try { return task.call(); } catch (BusinessException ex) { return ex; } })).toList();
            start.countDown(); return List.of(tasks.get(0).get(20,TimeUnit.SECONDS),tasks.get(1).get(20,TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
    }
}
