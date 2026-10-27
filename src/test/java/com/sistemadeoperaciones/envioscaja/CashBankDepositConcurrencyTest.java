package com.sistemadeoperaciones.envioscaja;

import com.sistemadeoperaciones.cajageneral.dto.OpenCashDayRequest;
import com.sistemadeoperaciones.cajageneral.enums.CashDenomination;
import com.sistemadeoperaciones.cajageneral.repository.CashGeneralMovementRepository;
import com.sistemadeoperaciones.cajageneral.service.CashGeneralService;
import com.sistemadeoperaciones.corte.model.BankAccountDailyCut;
import com.sistemadeoperaciones.corte.service.BankAccountDailyCutServiceImpl;
import com.sistemadeoperaciones.corte.service.BankLedgerQuery;
import com.sistemadeoperaciones.pagos.service.PaymentOperationServiceImpl;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.exception.BusinessException;
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
import java.util.EnumMap;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:cash_bank_deposit_concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({CashBankDepositService.class, CashGeneralService.class, BankAccountDailyCutServiceImpl.class, BankLedgerQuery.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CashBankDepositConcurrencyTest {
    @Autowired CashBankDepositService deposits;
    @Autowired CashBankDepositRepository repository;
    @Autowired CashGeneralService cash;
    @Autowired CashGeneralMovementRepository movements;
    @Autowired BankLedgerQuery ledger;
    @Autowired PlatformTransactionManager manager;
    @Autowired EntityManager em;
    @MockBean AuthenticatedUserService auth;
    @MockBean BusinessAuditService audit;
    @MockBean PaymentOperationServiceImpl paymentService;
    Long accountId;
    @BeforeEach void setup() {
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            User u = new User(); u.setNombre("Jefa"); u.setCorreo(UUID.randomUUID() + "@example.test"); em.persist(u);
            when(auth.getCurrentUser()).thenReturn(u);
            BankAccount a = new BankAccount(); a.setBanco("BANORTE"); a.setTitular("Oficina");
            a.setNumeroCuenta("123456789012345678"); a.setClabe("123456789012345678"); a.setActivo(true);
            em.persist(a); accountId = a.getId();
            BankAccountDailyCut cut = new BankAccountDailyCut(); cut.setBankAccount(a);
            cut.setFecha(LocalDate.now().minusDays(1)); cut.setSaldoInicial(new BigDecimal("100"));
            cut.setSaldoFinal(new BigDecimal("100")); em.persist(cut);
        });
        cash.open(new OpenCashDayRequest(LocalDate.now(), new BigDecimal("100"), hundreds(1)));
    }
    @AfterEach void cleanup() {
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            for (String table : new String[]{"cash_bank_deposit_counts", "cash_bank_deposit_return_counts",
                    "cash_bank_deposits", "cash_general_movement_counts", "cash_general_movements",
                    "cash_general_opening_counts", "cash_general_closing_counts", "cash_general_days",
                    "cash_general_register", "bank_account_daily_cuts", "bank_accounts", "users"})
                em.createNativeQuery("delete from " + table).executeUpdate();
        });
    }
    static EnumMap<CashDenomination, Integer> hundreds(int quantity) {
        var counts = new EnumMap<CashDenomination, Integer>(CashDenomination.class);
        for (var d : CashDenomination.values()) counts.put(d, 0);
        counts.put(CashDenomination.D100, quantity); return counts;
    }
    CashBankDepositRequests.Register request(UUID id) {
        return new CashBankDepositRequests.Register(id, accountId, new BigDecimal("100"),
                "VENTANILLA", null, "https://example.test/salida.pdf", hundreds(1));
    }
    int parallel(Runnable work) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        try {
            Callable<Void> task = () -> {
                start.await();
                try { work.run(); succeeded.incrementAndGet(); }
                catch (BusinessException expected) { /* El saldo ya quedó reservado. */ }
                return null;
            };
            Future<Void> first = pool.submit(task), second = pool.submit(task);
            start.countDown(); first.get(20, TimeUnit.SECONDS); second.get(20, TimeUnit.SECONDS);
            return succeeded.get();
        } finally { pool.shutdownNow(); }
    }
    @Test void simultaneousRegistrationsCannotReserveTheSameCashTwice() throws Exception {
        assertThat(parallel(() -> deposits.register(request(UUID.randomUUID())))).isEqualTo(1);
        assertThat(repository.count()).isEqualTo(1);
        assertThat(cash.latest().saldoActual()).isEqualByComparingTo("100");
    }
    @Test void simultaneousConfirmationRetriesMoveTheMoneyOnce() throws Exception {
        var registered = deposits.register(request(UUID.randomUUID()));
        assertThat(parallel(() -> deposits.confirm(registered.id(),
                new CashBankDepositRequests.Confirm("https://example.test/banco.pdf")))).isEqualTo(2);
        assertThat(movements.count()).isEqualTo(1);
        assertThat(cash.latest().saldoActual()).isEqualByComparingTo("0");
        assertThat(ledger.bucketsForAccount(accountId, LocalDate.now()).entradasDeposito()).isEqualByComparingTo("100");
    }
}
