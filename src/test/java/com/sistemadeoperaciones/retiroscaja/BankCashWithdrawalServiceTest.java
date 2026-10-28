package com.sistemadeoperaciones.retiroscaja;

import com.sistemadeoperaciones.cajageneral.dto.CashDayResponse;
import com.sistemadeoperaciones.cajageneral.dto.CashWithdrawalGroup;
import com.sistemadeoperaciones.cajageneral.dto.OpenCashDayRequest;
import com.sistemadeoperaciones.cajageneral.enums.CashDenomination;
import com.sistemadeoperaciones.cajageneral.enums.CashMovementConcept;
import com.sistemadeoperaciones.cajageneral.service.CashGeneralService;
import com.sistemadeoperaciones.corte.model.BankAccountDailyCut;
import com.sistemadeoperaciones.corte.service.BankAccountDailyCutServiceImpl;
import com.sistemadeoperaciones.corte.service.BankLedgerQuery;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.*;

/**
 * El retiro de una cuenta hacia Caja General de punta a punta: Cuentas lo registra (el saldo
 * no baja, el disponible sí), Cajas lo confirma (entra a la caja y sale del banco en la misma
 * fila) y el libro de caja lo agrupa como "Retiro N de M TD/RST BANCO".
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({BankCashWithdrawalService.class, BankAvailability.class, CashGeneralService.class,
        BankAccountDailyCutServiceImpl.class, BankLedgerQuery.class})
class BankCashWithdrawalServiceTest {

    @Autowired TestEntityManager em;
    @Autowired BankCashWithdrawalService service;
    @Autowired BankAvailability availability;
    @Autowired CashGeneralService cash;
    @Autowired BankLedgerQuery ledger;
    @MockBean AuthenticatedUserService auth;
    @MockBean BusinessAuditService businessAuditService;

    static final LocalDate HOY = LocalDate.now();

    User user;
    BankAccount bbva;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setNombre("Jefa");
        user.setCorreo("jefa+" + System.nanoTime() + "@example.com");
        em.persist(user);
        when(auth.getCurrentUser()).thenReturn(user);
        bbva = account("BBVA", "1000");
    }

    BankAccount account(String banco, String saldo) {
        BankAccount a = new BankAccount();
        a.setBanco(banco);
        a.setTitular("Operaciones SA");
        a.setNumeroCuenta(UUID.randomUUID().toString().replaceAll("[^0-9]", "3").substring(0, 18));
        a.setClabe(UUID.randomUUID().toString().replaceAll("[^0-9]", "0").substring(0, 18));
        a.setActivo(true);
        em.persist(a);
        BankAccountDailyCut cut = new BankAccountDailyCut();
        cut.setBankAccount(a);
        cut.setFecha(HOY.minusDays(1));
        cut.setSaldoInicial(new BigDecimal(saldo));
        cut.setSaldoFinal(new BigDecimal(saldo));
        em.persist(cut);
        em.flush();
        return a;
    }

    /** Desglose en billetes de $100. */
    static Map<CashDenomination, Integer> hundreds(int count) {
        Map<CashDenomination, Integer> values = new EnumMap<>(CashDenomination.class);
        for (var d : CashDenomination.values()) values.put(d, 0);
        values.put(CashDenomination.D100, count);
        return values;
    }

    CashDayResponse openCash() {
        return cash.open(new OpenCashDayRequest(HOY, new BigDecimal("100"), hundreds(1)));
    }

    BankCashWithdrawalView register(BankAccount account, WithdrawalMethod forma, String monto) {
        return service.register(new RegisterWithdrawalRequest(UUID.randomUUID(), account.getId(), forma,
                new BigDecimal(monto), null, null));
    }

    BankCashWithdrawalView confirm(BankCashWithdrawalView w, CashDayResponse day) {
        return service.confirm(w.id(), new ConfirmWithdrawalRequest(day.id(), hundreds(w.monto().intValue() / 100)));
    }

    @Test
    void theCashTrayListsTheMostRecentWithdrawalFirst() {
        var first = register(bbva, WithdrawalMethod.RETIRO_SIN_TARJETA, "100");
        var second = register(bbva, WithdrawalMethod.RETIRO_SIN_TARJETA, "200");

        assertThat(service.pending()).extracting(BankCashWithdrawalView::id).containsExactly(second.id(), first.id());
    }

    @Test
    void aPendingWithdrawalKeepsTheBalanceButIsNoLongerAvailable() {
        var w = register(bbva, WithdrawalMethod.RETIRO_SIN_TARJETA, "300");

        assertThat(w.estatus()).isEqualTo(WithdrawalStatus.PENDIENTE);
        assertThat(availability.balance(bbva.getId())).isEqualByComparingTo("1000");
        assertThat(availability.inTransit(bbva.getId())).isEqualByComparingTo("300");
        assertThat(availability.available(bbva.getId())).isEqualByComparingTo("700");
        assertThat(service.pending()).extracting(BankCashWithdrawalView::id).containsExactly(w.id());
    }

    @Test
    void editingPendingWithdrawalReplacesReservationAndConfirmationUsesEditedAmount() {
        var day = openCash();
        var original = register(bbva, WithdrawalMethod.RETIRO_SIN_TARJETA, "300");
        var edited = service.edit(original.id(), new EditWithdrawalRequest(original.version(), bbva.getId(),
                WithdrawalMethod.RETIRO_CON_TARJETA, new BigDecimal("500"), "REF-2", null));
        assertThat(edited.version()).isGreaterThan(original.version());
        assertThat(availability.available(bbva.getId())).isEqualByComparingTo("500");
        assertThat(availability.balance(bbva.getId())).isEqualByComparingTo("1000");
        service.confirm(edited.id(), new ConfirmWithdrawalRequest(day.id(), hundreds(5)));
        assertThat(availability.balance(bbva.getId())).isEqualByComparingTo("500");
        assertThat(cash.latest().saldoActual()).isEqualByComparingTo("600");
        verify(businessAuditService).record(eq(user.getId()), eq("BANK_WITHDRAWAL_EDIT"),
                eq("BANK_WITHDRAWAL"), eq(original.id()), any(), any());
    }

    @Test
    void editingWithdrawalRejectsInsufficientFundsStaleVersionAndResolvedStatus() {
        var original = register(bbva, WithdrawalMethod.RETIRO_SIN_TARJETA, "300");
        assertThatThrownBy(() -> service.edit(original.id(), new EditWithdrawalRequest(original.version(),
                bbva.getId(), WithdrawalMethod.RETIRO_CON_TARJETA, new BigDecimal("1100"), null, null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Saldo insuficiente");
        var edited = service.edit(original.id(), new EditWithdrawalRequest(original.version(),
                bbva.getId(), WithdrawalMethod.RETIRO_CON_TARJETA, new BigDecimal("200"), null, null));
        assertThatThrownBy(() -> service.edit(original.id(), new EditWithdrawalRequest(original.version(),
                bbva.getId(), WithdrawalMethod.RETIRO_CON_TARJETA, new BigDecimal("100"), null, null)))
                .isInstanceOf(ConflictException.class).hasMessageContaining("cambió");
        service.cancel(edited.id(), new ResolveWithdrawalRequest("Corrección"));
        assertThatThrownBy(() -> service.edit(original.id(), new EditWithdrawalRequest(edited.version(),
                bbva.getId(), WithdrawalMethod.RETIRO_CON_TARJETA, new BigDecimal("100"), null, null)))
                .isInstanceOf(ConflictException.class).hasMessageContaining("pendiente");
    }

    @Test
    void cannotWithdrawMoreThanIsAvailable() {
        register(bbva, WithdrawalMethod.RETIRO_SIN_TARJETA, "800");
        assertThatThrownBy(() -> register(bbva, WithdrawalMethod.RETIRO_CON_TARJETA, "300"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Saldo insuficiente")
                .hasMessageContaining("$200.00");
    }

    @Test
    void confirmingEntersCashAndLeavesTheBankInTheSameRow() {
        var day = openCash();
        var w = register(bbva, WithdrawalMethod.RETIRO_SIN_TARJETA, "300");

        var confirmed = confirm(w, day);

        assertThat(confirmed.estatus()).isEqualTo(WithdrawalStatus.CONFIRMADO);
        assertThat(confirmed.cashMovementId()).isNotNull();
        assertThat(cash.latest().saldoActual()).isEqualByComparingTo("400");
        assertThat(availability.balance(bbva.getId())).isEqualByComparingTo("700");
        assertThat(availability.inTransit(bbva.getId())).isEqualByComparingTo("0");
        assertThat(ledger.bucketsForAccount(bbva.getId(), HOY).salidasCajaGeneral()).isEqualByComparingTo("300");
        assertThat(service.pending()).isEmpty();
    }

    @Test
    void aCardWithdrawalAlsoLeavesTheBank() {
        var day = openCash();
        confirm(register(bbva, WithdrawalMethod.RETIRO_CON_TARJETA, "200"), day);
        assertThat(ledger.bucketsForAccount(bbva.getId(), HOY).salidasCajaGeneral()).isEqualByComparingTo("200");
    }

    @Test
    void confirmingTwiceEntersOnce() {
        var day = openCash();
        var w = register(bbva, WithdrawalMethod.RETIRO_SIN_TARJETA, "300");
        var first = confirm(w, day);
        var again = confirm(w, day);

        assertThat(again.cashMovementId()).isEqualTo(first.cashMovementId());
        assertThat(cash.latest().saldoActual()).isEqualByComparingTo("400");
        verify(businessAuditService, times(1)).record(eq(user.getId()), eq("BANK_WITHDRAWAL_CONFIRM"),
                eq("BANK_WITHDRAWAL"), eq(w.id()), any(), any());
    }

    @Test
    void theCashLedgerGroupsByDayMethodAndBank() {
        BankAccount banorte1 = account("BANORTE", "1000");
        BankAccount banorte2 = account("Banorte ", "1000");
        BankAccount banorte3 = account("banorte", "1000");
        var day = openCash();

        confirm(register(banorte1, WithdrawalMethod.RETIRO_SIN_TARJETA, "100"), day);
        confirm(register(banorte2, WithdrawalMethod.RETIRO_SIN_TARJETA, "200"), day);
        // La misma cuenta otra vez: suma el monto, no la cuenta.
        confirm(register(banorte1, WithdrawalMethod.RETIRO_SIN_TARJETA, "100"), day);
        confirm(register(banorte3, WithdrawalMethod.RETIRO_CON_TARJETA, "300"), day);
        confirm(register(bbva, WithdrawalMethod.COBRO_CHEQUE, "500"), day);

        var grupos = cash.ledger(HOY, HOY).grupos();
        assertThat(grupos).extracting(CashWithdrawalGroup::etiqueta).containsExactlyInAnyOrder(
                "Retiro 2 de 3 RST BANORTE", "Retiro 1 de 3 TD BANORTE", "Cheques BBVA");

        CashWithdrawalGroup rst = grupos.stream().filter(g -> g.forma().equals("RST")).findFirst().orElseThrow();
        assertThat(rst.cuentas()).isEqualTo(2);
        assertThat(rst.totalCuentas()).isEqualTo(3);
        assertThat(rst.movimientos()).isEqualTo(3);
        assertThat(rst.total()).isEqualByComparingTo("400");
        assertThat(rst.ultimoMovimientoId()).isEqualTo(rst.movementIds().get(2));

        CashWithdrawalGroup cheques = grupos.stream().filter(g -> g.forma().equals("CHEQUE")).findFirst().orElseThrow();
        assertThat(cheques.cuentas()).isNull();
        assertThat(cheques.total()).isEqualByComparingTo("500");
        assertThat(cash.ledger(HOY, HOY).movimientos()).extracting(m -> m.tipo())
                .contains(CashMovementConcept.CHEQUE, CashMovementConcept.RETIRO_CON_TARJETA);
    }

    @Test
    void cajasRejectsAndCuentasCancelsOnlyPendingWithdrawals() {
        var day = openCash();
        var rejected = service.reject(register(bbva, WithdrawalMethod.RETIRO_SIN_TARJETA, "300").id(),
                new ResolveWithdrawalRequest("No llegó el efectivo"));
        assertThat(rejected.estatus()).isEqualTo(WithdrawalStatus.RECHAZADO);
        assertThat(rejected.motivo()).isEqualTo("No llegó el efectivo");
        assertThat(availability.available(bbva.getId())).isEqualByComparingTo("1000");
        assertThatThrownBy(() -> confirm(rejected, day)).isInstanceOf(ConflictException.class);

        var cancelled = service.cancel(register(bbva, WithdrawalMethod.RETIRO_SIN_TARJETA, "100").id(),
                new ResolveWithdrawalRequest("Capturado dos veces"));
        assertThat(cancelled.estatus()).isEqualTo(WithdrawalStatus.CANCELADO);

        var confirmed = confirm(register(bbva, WithdrawalMethod.RETIRO_SIN_TARJETA, "100"), day);
        assertThatThrownBy(() -> service.cancel(confirmed.id(), new ResolveWithdrawalRequest("Tarde")))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.reject(confirmed.id(), new ResolveWithdrawalRequest(" ")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("motivo");
    }

    @Test
    void confirmationNeedsTheAccountStillActive() {
        var day = openCash();
        var w = register(bbva, WithdrawalMethod.RETIRO_SIN_TARJETA, "300");
        bbva.setActivo(false);
        em.persistAndFlush(bbva);

        assertThatThrownBy(() -> confirm(w, day))
                .isInstanceOf(ConflictException.class).hasMessageContaining("desactivó");
    }

    @Test
    void aRetriedRegistrationDoesNotDuplicate() {
        UUID id = UUID.randomUUID();
        var request = new RegisterWithdrawalRequest(id, bbva.getId(), WithdrawalMethod.COBRO_CHEQUE,
                new BigDecimal("300"), "Cheque 123", null);
        var first = service.register(request);
        assertThat(service.register(request).id()).isEqualTo(first.id());
        assertThat(availability.inTransit(bbva.getId())).isEqualByComparingTo("300");
        verify(businessAuditService, times(1)).record(eq(user.getId()), eq("BANK_WITHDRAWAL_REGISTER"),
                eq("BANK_WITHDRAWAL"), eq(first.id()), isNull(), any());

        var changed = new RegisterWithdrawalRequest(id, bbva.getId(), WithdrawalMethod.COBRO_CHEQUE,
                new BigDecimal("301"), "Cheque 123", null);
        assertThatThrownBy(() -> service.register(changed)).isInstanceOf(ConflictException.class);
    }

    @Test
    void rejectsInvalidRegistrations() {
        assertThatThrownBy(() -> register(bbva, WithdrawalMethod.RETIRO_SIN_TARJETA, "0"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> register(bbva, WithdrawalMethod.RETIRO_SIN_TARJETA, "10.001"))
                .isInstanceOf(BadRequestException.class);
        // La moneda más chica es de 50 centavos: el retiro tiene que poder contarse en efectivo.
        assertThatThrownBy(() -> register(bbva, WithdrawalMethod.RETIRO_SIN_TARJETA, "100.25"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("múltiplos de 50");
        assertThatThrownBy(() -> register(bbva, WithdrawalMethod.RETIRO_SIN_TARJETA, "100.01"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("múltiplos de 50");
        assertThat(register(bbva, WithdrawalMethod.RETIRO_SIN_TARJETA, "100.50").monto()).isEqualByComparingTo("100.50");
        assertThat(register(bbva, WithdrawalMethod.RETIRO_SIN_TARJETA, "100.5").monto()).isEqualByComparingTo("100.50");
        assertThatThrownBy(() -> service.register(new RegisterWithdrawalRequest(UUID.randomUUID(), bbva.getId(), null,
                BigDecimal.TEN, null, null))).isInstanceOf(BadRequestException.class);
        BankAccount inactive = account("HSBC", "1000");
        inactive.setActivo(false);
        em.persistAndFlush(inactive);
        assertThatThrownBy(() -> register(inactive, WithdrawalMethod.RETIRO_SIN_TARJETA, "100"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("inactiva");
    }
}
