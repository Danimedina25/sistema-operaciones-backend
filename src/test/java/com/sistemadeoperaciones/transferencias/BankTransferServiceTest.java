package com.sistemadeoperaciones.transferencias;

import com.sistemadeoperaciones.corte.model.BankAccountDailyCut;
import com.sistemadeoperaciones.corte.dto.BankLedgerFilter;
import com.sistemadeoperaciones.corte.enums.BankLedgerDirection;
import com.sistemadeoperaciones.corte.enums.BankLedgerOrigin;
import com.sistemadeoperaciones.corte.service.BankAccountDailyCutService;
import com.sistemadeoperaciones.corte.service.BankAccountDailyCutServiceImpl;
import com.sistemadeoperaciones.corte.service.BankLedgerQuery;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.retiroscaja.BankAvailability;
import com.sistemadeoperaciones.retiroscaja.BankCashWithdrawal;
import com.sistemadeoperaciones.retiroscaja.WithdrawalMethod;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
import com.sistemadeoperaciones.shared.exception.ConflictException;
import com.sistemadeoperaciones.usuarios.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.*;

/**
 * Transferencias entre cuentas propias: mueven los dos saldos desde una sola fila, nunca dejan
 * la cuenta origen en negativo, son idempotentes y nunca modifican un corte ya registrado.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({BankTransferService.class, BankAccountDailyCutServiceImpl.class, BankLedgerQuery.class, BankAvailability.class})
class BankTransferServiceTest {

    @Autowired TestEntityManager em;
    @Autowired BankTransferService service;
    @Autowired BankAccountDailyCutService balances;
    @Autowired BankLedgerQuery ledger;
    @MockBean AuthenticatedUserService auth;
    @MockBean BusinessAuditService businessAuditService;

    static final LocalDate HOY = LocalDate.now();

    User user;
    BankAccount origen;
    BankAccount destino;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setNombre("Jefa de Cuentas");
        user.setCorreo("cuentas+" + System.nanoTime() + "@example.com");
        em.persist(user);
        when(auth.getCurrentUser()).thenReturn(user);
        origen = account("BBVA", true);
        destino = account("Banorte", true);
        fund(origen, "1000");
    }

    /** Saldo de cierre de ayer: el saldo inicial de hoy. */
    void fund(BankAccount account, String saldo) {
        BankAccountDailyCut cut = new BankAccountDailyCut();
        cut.setBankAccount(account);
        cut.setFecha(HOY.minusDays(1));
        cut.setSaldoInicial(new BigDecimal(saldo));
        cut.setSaldoFinal(new BigDecimal(saldo));
        em.persist(cut);
        em.flush();
    }

    BankAccount account(String banco, boolean activa) {
        BankAccount a = new BankAccount();
        a.setBanco(banco);
        a.setTitular("Operaciones SA");
        a.setNumeroCuenta(UUID.randomUUID().toString().replaceAll("[^0-9]", "7").substring(0, 18));
        a.setClabe(UUID.randomUUID().toString().replaceAll("[^0-9]", "0").substring(0, 18));
        a.setActivo(activa);
        em.persist(a);
        return a;
    }

    BankTransferRequest request(UUID id, BankAccount from, BankAccount to, String monto) {
        return new BankTransferRequest(id, from.getId(), to.getId(),
                monto == null ? null : new BigDecimal(monto), "SPEI 123", null);
    }

    @Test
    void movesMoneyFromOneAccountToTheOther() {
        BankTransferView view = service.create(request(UUID.randomUUID(), origen, destino, "300"));

        assertThat(view.monto()).isEqualByComparingTo("300");
        assertThat(balances.calculateBalance(origen.getId(), HOY).getSaldoFinal()).isEqualByComparingTo("700");
        assertThat(balances.calculateBalance(destino.getId(), HOY).getSaldoFinal()).isEqualByComparingTo("300");

        var origenHoy = balances.calculateBalance(origen.getId(), HOY);
        assertThat(origenHoy.getSalidasTransferenciaInterna()).isEqualByComparingTo("300");
        assertThat(origenHoy.getTotalSalidas()).isEqualByComparingTo("300");
        var destinoHoy = balances.calculateBalance(destino.getId(), HOY);
        assertThat(destinoHoy.getEntradasTransferenciaInterna()).isEqualByComparingTo("300");
        assertThat(destinoHoy.getTotalEntradas()).isEqualByComparingTo("300");
    }

    @Test
    void appearsInTheLedgerAsTwoRowsWithDistinctIds() {
        service.create(request(UUID.randomUUID(), origen, destino, "300"));

        var rows = ledger.search(new BankLedgerFilter(HOY, HOY, null, null, null, null), PageRequest.of(0, 20));
        assertThat(rows.getContent()).hasSize(2)
                .allSatisfy(r -> assertThat(r.origen()).isEqualTo(BankLedgerOrigin.TRANSFERENCIA_INTERNA));
        assertThat(rows.getContent()).extracting(r -> r.id()).doesNotHaveDuplicates();
        var salida = rows.getContent().stream().filter(r -> r.direccion() == BankLedgerDirection.SALIDA).findFirst().orElseThrow();
        assertThat(salida.bankAccountId()).isEqualTo(origen.getId());
        assertThat(salida.concepto()).startsWith("Transferencia a Banorte");

        // En el total general la transferencia interna es neutra.
        var totals = ledger.totals(new BankLedgerFilter(HOY, HOY, null, null, null, null));
        assertThat(totals.variacionNeta()).isEqualByComparingTo("0");
    }

    @Test
    void reportsTheResultingOriginBalance() {
        BankTransferView view = service.create(request(UUID.randomUUID(), origen, destino, "1000"));
        assertThat(view.saldoOrigenResultante()).isEqualByComparingTo("0");
    }

    @Test
    void neverLeavesTheOriginAccountNegative() {
        assertThatThrownBy(() -> service.create(request(UUID.randomUUID(), origen, destino, "1000.01")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Saldo insuficiente");
        // Una cuenta sin saldo tampoco puede transferir.
        assertThatThrownBy(() -> service.create(request(UUID.randomUUID(), destino, origen, "1")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Saldo insuficiente");
        assertThat(service.list(HOY, HOY)).isEmpty();
    }

    @Test
    void cashInTransitToCajaGeneralIsNotAvailable() {
        BankCashWithdrawal pending = new BankCashWithdrawal();
        pending.setRequestId(UUID.randomUUID().toString());
        pending.setCuentaBancaria(origen);
        pending.setBancoSnapshot(origen.getBanco());
        pending.setForma(WithdrawalMethod.RETIRO_SIN_TARJETA);
        pending.setMonto(new BigDecimal("800"));
        pending.setRegistradoPor(user);
        em.persist(pending);
        em.flush();

        assertThatThrownBy(() -> service.create(request(UUID.randomUUID(), origen, destino, "300")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("disponible $200.00");
        assertThat(service.create(request(UUID.randomUUID(), origen, destino, "200")).monto()).isEqualByComparingTo("200");
    }

    @Test
    void aRetryWithTheSameRequestIdDoesNotDuplicate() {
        UUID id = UUID.randomUUID();
        BankTransferView first = service.create(request(id, origen, destino, "300"));
        BankTransferView retry = service.create(request(id, origen, destino, "300.00"));

        assertThat(retry.id()).isEqualTo(first.id());
        assertThat(balances.calculateBalance(destino.getId(), HOY).getSaldoFinal()).isEqualByComparingTo("300");
        verify(businessAuditService, times(1)).record(eq(user.getId()), eq("BANK_TRANSFER_CREATE"),
                eq("BANK_TRANSFER"), eq(first.id()), isNull(), any());
    }

    @Test
    void aReusedRequestIdWithOtherDataIsRejected() {
        UUID id = UUID.randomUUID();
        service.create(request(id, origen, destino, "300"));
        assertThatThrownBy(() -> service.create(request(id, origen, destino, "301")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void rejectsInvalidRequests() {
        assertThatThrownBy(() -> service.create(request(UUID.randomUUID(), origen, origen, "300")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("distintas");
        assertThatThrownBy(() -> service.create(request(UUID.randomUUID(), origen, destino, "0")))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.create(request(UUID.randomUUID(), origen, destino, "-5")))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.create(request(UUID.randomUUID(), origen, destino, "10.001")))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.create(request(UUID.randomUUID(), origen, destino, null)))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.create(request(null, origen, destino, "300")))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.create(new BankTransferRequest(UUID.randomUUID(), origen.getId(), destino.getId(),
                BigDecimal.TEN, null, "http://inseguro.com/x.pdf")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsAnInactiveAccount() {
        BankAccount inactiva = account("HSBC", false);
        assertThatThrownBy(() -> service.create(request(UUID.randomUUID(), origen, inactiva, "300")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("inactiva");
        assertThatThrownBy(() -> service.create(request(UUID.randomUUID(), inactiva, destino, "300")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("inactiva");
    }

    @Test
    void neverChangesACutAlreadyRegisteredForToday() {
        BankAccountDailyCut cut = new BankAccountDailyCut();
        cut.setBankAccount(destino);
        cut.setFecha(HOY);
        em.persist(cut);
        em.flush();

        assertThatThrownBy(() -> service.create(request(UUID.randomUUID(), origen, destino, "300")))
                .isInstanceOf(ConflictException.class).hasMessageContaining("corte de hoy");
    }

    @Test
    void listsTransfersOfTheRange() {
        service.create(request(UUID.randomUUID(), origen, destino, "300"));
        service.create(request(UUID.randomUUID(), destino, origen, "100"));

        var list = service.list(HOY, HOY);
        assertThat(list).hasSize(2);
        assertThat(list.get(0).monto()).isEqualByComparingTo("100");
        assertThat(list.get(0).registradoPorNombre()).isEqualTo("Jefa de Cuentas");
        assertThatThrownBy(() -> service.list(HOY, HOY.plusDays(1))).isInstanceOf(BadRequestException.class);
    }
}
