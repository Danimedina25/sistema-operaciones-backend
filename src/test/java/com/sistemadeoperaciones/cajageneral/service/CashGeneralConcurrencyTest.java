package com.sistemadeoperaciones.cajageneral.service;

import com.sistemadeoperaciones.cajageneral.dto.*;
import com.sistemadeoperaciones.cajageneral.enums.*;
import com.sistemadeoperaciones.cajageneral.repository.*;
import com.sistemadeoperaciones.corte.dto.BankLedgerFilter;
import com.sistemadeoperaciones.corte.service.BankAccountDailyCutService;
import com.sistemadeoperaciones.corte.service.BankLedgerQuery;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.usuarios.model.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:caja_concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({CashGeneralService.class, BankLedgerQuery.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CashGeneralConcurrencyTest {
    @Autowired CashGeneralService service;
    @Autowired CashGeneralMovementRepository movements;
    @Autowired CashGeneralDeletionAuditRepository deletionAudits;
    @Autowired PlatformTransactionManager manager;
    @Autowired EntityManager em;
    @Autowired BankLedgerQuery ledger;
    // El corte bancario se recalcula sólo al eliminar un día; aquí no se ejercita.
    @MockBean BankAccountDailyCutService bankCuts;
    @MockBean AuthenticatedUserService auth;
    @BeforeEach void setup() {
        User user = new TransactionTemplate(manager).execute(status -> {
            User u = new User(); u.setNombre("Jefa Caja"); u.setCorreo(UUID.randomUUID()+"@example.com"); em.persist(u); return u;
        });
        when(auth.getCurrentUser()).thenReturn(user);
    }
    @AfterEach void cleanup() {
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            for (String table : new String[]{"cash_general_movement_counts", "cash_general_movements", "cash_general_opening_counts", "cash_general_closing_counts", "cash_general_days", "cash_general_register", "bank_accounts"})
                em.createNativeQuery("delete from " + table).executeUpdate();
        });
    }
    OpenCashDayRequest opening() { return new OpenCashDayRequest(LocalDate.now(), new BigDecimal("100"), CashGeneralServiceTest.counts(1,0)); }
    CreateCashMovementRequest expense(UUID requestId) {
        return new CreateCashMovementRequest(requestId,CashMovementDirection.SALIDA,CashMovementConcept.EFECTIVO,
                "Salida concurrente",null,null,new BigDecimal("100"),null,CashGeneralServiceTest.counts(1,0),null);
    }
    int parallel(Runnable work) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        try {
            Callable<Void> task = () -> {
                start.await();
                try { work.run(); succeeded.incrementAndGet(); }
                catch (com.sistemadeoperaciones.shared.exception.BusinessException expected) { /* Saldo o apertura ya ocupada. */ }
                return null;
            };
            Future<Void> first = pool.submit(task), second = pool.submit(task); start.countDown();
            first.get(20,TimeUnit.SECONDS); second.get(20,TimeUnit.SECONDS);
            return succeeded.get();
        } finally { pool.shutdownNow(); }
    }
    @Test void concurrentFirstOpeningCreatesOnlyOneDay() throws Exception {
        assertThat(parallel(() -> service.open(opening()))).isEqualTo(1);
        assertThat(service.ledger(LocalDate.now(),LocalDate.now()).dias()).hasSize(1);
    }
    @Test void concurrentExpensesCannotOverdraw() throws Exception {
        Long id = service.open(opening()).id();
        assertThat(parallel(() -> service.createMovement(id,expense(UUID.randomUUID())))).isEqualTo(1);
        assertThat(service.latest().saldoActual()).isEqualByComparingTo("0");
        var ledger = service.ledger(LocalDate.now(),LocalDate.now());
        assertThat(ledger.movimientos()).hasSize(1);
        assertThat(ledger.movimientos().get(0).saldoAcumulado()).isEqualByComparingTo("0");
    }
    @Test void concurrentRetriesPostExactlyOneMovement() throws Exception {
        Long id = service.open(opening()).id(); UUID key = UUID.randomUUID();
        assertThat(parallel(() -> service.createMovement(id,expense(key)))).isEqualTo(2);
        assertThat(movements.count()).isEqualTo(1);
        assertThat(service.latest().saldoActual()).isEqualByComparingTo("0");
    }
    @Test void ledgerKeepsOpeningAndClosingCountsAndVersionDetectsStaleClose() {
        var day = service.open(opening());
        service.createMovement(day.id(),expense(UUID.randomUUID()));
        assertThatThrownBy(() -> service.close(day.id(),new CloseCashDayRequest(BigDecimal.ZERO,day.version(),CashGeneralServiceTest.counts(0,0),null)))
                .hasMessageContaining("cambió");
        var updated = service.latest();
        service.close(day.id(),new CloseCashDayRequest(BigDecimal.ZERO,updated.version(),CashGeneralServiceTest.counts(0,0),null));
        var result = service.ledger(LocalDate.now(),LocalDate.now()).dias().get(0);
        assertThat(result.apertura().get(CashDenomination.D100)).isEqualTo(1);
        assertThat(result.cierre()).hasSize(11);
        assertThat(result.diferencia()).isEqualByComparingTo("0");
    }
    @Test void deletionRemovesFullCashDayButPreservesAudit() {
        var day = service.open(opening());
        service.createMovement(day.id(), expense(UUID.randomUUID()));
        var current = service.latest();

        service.deleteDay(day.id(), new DeleteCashDayRequest("ELIMINAR", "Prueba de eliminación", current.version()));

        assertThat(service.latest()).isNull();
        assertThat(movements.count()).isZero();
        assertThat(deletionAudits.findAll()).singleElement().satisfies(audit -> {
            assertThat(audit.getDeletedDayId()).isEqualTo(day.id());
            assertThat(audit.getMovementCount()).isEqualTo(1);
            assertThat(audit.getMotivo()).isEqualTo("Prueba de eliminación");
        });
    }
    @Test void completedDeliveryIsLinkedOnceAndForeignKeyProtectsItsSource() {
        var day = service.open(opening());
        Long installmentId = new TransactionTemplate(manager).execute(status -> {
            var user = auth.getCurrentUser();
            var client = new com.sistemadeoperaciones.clientes.model.Clientes();
            client.setNombre("Cliente Caja"); client.setUser(user);
            client.setPorcentajeComisionSocio(BigDecimal.ZERO); client.setPorcentajeComisionOficina(BigDecimal.ZERO); em.persist(client);
            var op = new com.sistemadeoperaciones.pagos.model.PaymentOperation();
            op.setCliente(client); op.setSocioComercial(user); op.setMontoTotal(new BigDecimal("100")); op.setMontoValidado(new BigDecimal("100"));
            op.setPorcentajeComisionSocio(BigDecimal.ZERO); op.setPorcentajeComisionOficina(BigDecimal.ZERO); em.persist(op);
            var req = new com.sistemadeoperaciones.pagos.model.OperationReturnPayment();
            req.setOperacion(op); req.setMonto(new BigDecimal("100")); req.setTipoPago(com.sistemadeoperaciones.pagos.enums.PaymentType.EFECTIVO);
            req.setSolicitadoPor(user); em.persist(req);
            var i = new com.sistemadeoperaciones.pagos.model.OperationReturnInstallment();
            i.setSolicitud(req); i.setMonto(new BigDecimal("100")); i.setTipoPago(com.sistemadeoperaciones.pagos.enums.PaymentType.EFECTIVO);
            i.setEstatus(com.sistemadeoperaciones.pagos.enums.ReturnInstallmentStatus.COMPLETADA);
            i.setFechaRealizacion(java.time.LocalDateTime.now()); i.setCreadoPor(user); em.persist(i); return i.getId();
        });
        assertThat(service.deliveries(0)).extracting(CashDeliveryResponse::id).contains(installmentId);
        var request = new CreateCashMovementRequest(UUID.randomUUID(),CashMovementDirection.SALIDA,CashMovementConcept.EFECTIVO,
                "Entrega vinculada",null,null,null,installmentId,CashGeneralServiceTest.counts(1,0),null);
        service.createMovement(day.id(),request);
        assertThat(service.deliveries(0)).extracting(CashDeliveryResponse::id).doesNotContain(installmentId);
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            Object amount = em.createNativeQuery("select monto_manual from cash_general_movements where installment_id = :id")
                    .setParameter("id",installmentId).getSingleResult();
            assertThat(amount).isNull();
        });
        assertThatThrownBy(() -> new TransactionTemplate(manager).executeWithoutResult(status ->
                em.createNativeQuery("delete from operation_return_installments where id = :id").setParameter("id",installmentId).executeUpdate()))
                .isInstanceOf(RuntimeException.class);
        assertThat(service.ledger(LocalDate.now(),LocalDate.now()).movimientos().get(0).monto()).isEqualByComparingTo("100");
    }
    /**
     * El cheque cobrado es una sola fila que vale por las dos patas: entrada de efectivo en
     * Caja General y salida de la cuenta bancaria. Dos reintentos simultáneos con el mismo
     * requestId no pueden duplicar ninguna de las dos.
     */
    @Test void concurrentChequeRetriesPostOneCashEntryAndOneBankExit() throws Exception {
        Long accountId = new TransactionTemplate(manager).execute(status -> {
            BankAccount account = new BankAccount();
            account.setBanco("BBVA"); account.setTitular("Operaciones SA");
            account.setNumeroCuenta(UUID.randomUUID().toString().substring(0, 18));
            account.setClabe(UUID.randomUUID().toString().replaceAll("[^0-9]", "0").substring(0, 18));
            account.setActivo(true);
            em.persist(account); return account.getId();
        });
        var day = service.open(opening());

        UUID requestId = UUID.randomUUID();
        CreateCashMovementRequest cheque = new CreateCashMovementRequest(requestId, CashMovementDirection.ENTRADA,
                CashMovementConcept.CHEQUE, "Cheque cobrado", null, accountId,
                new BigDecimal("100"), null, CashGeneralServiceTest.counts(1, 0), null);

        int applied = parallel(() -> service.createMovement(day.id(), cheque));

        assertThat(applied).isEqualTo(2);              // ambos hilos responden 200
        assertThat(movements.count()).isEqualTo(1);    // pero sólo existe un movimiento
        assertThat(service.latest().saldoActual()).isEqualByComparingTo("200");

        var filter = new BankLedgerFilter(LocalDate.now(), LocalDate.now(), accountId, null, null, null);
        var totals = ledger.totals(filter);
        assertThat(totals.totalMovimientos()).isEqualTo(1);
        assertThat(totals.totalSalidas()).isEqualByComparingTo("100");
        assertThat(totals.totalEntradas()).isEqualByComparingTo("0");
        assertThat(totals.variacionNeta()).isEqualByComparingTo("-100");

        var buckets = ledger.bucketsForAccount(accountId, LocalDate.now());
        assertThat(buckets.salidasCheque()).isEqualByComparingTo("100");
        assertThat(buckets.totalEntradas()).isEqualByComparingTo("0");

        var rows = ledger.search(filter, org.springframework.data.domain.PageRequest.of(0, 20));
        assertThat(rows.getTotalElements()).isEqualTo(1);
        assertThat(rows.getContent().get(0).origen().name()).isEqualTo("CAJA_GENERAL");
        assertThat(rows.getContent().get(0).concepto()).isEqualTo("Cheque cobrado");
        // El libro no publica el número completo de la cuenta.
        assertThat(rows.getContent().get(0).cuentaNumero()).startsWith("\u2022\u2022\u2022\u2022");
    }

}
