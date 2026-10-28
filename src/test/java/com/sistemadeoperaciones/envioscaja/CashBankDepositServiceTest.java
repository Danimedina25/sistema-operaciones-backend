package com.sistemadeoperaciones.envioscaja;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistemadeoperaciones.cajageneral.dto.OpenCashDayRequest;
import com.sistemadeoperaciones.cajageneral.dto.CreateCashMovementRequest;
import com.sistemadeoperaciones.cajageneral.enums.CashDenomination;
import com.sistemadeoperaciones.cajageneral.enums.CashMovementConcept;
import com.sistemadeoperaciones.cajageneral.enums.CashMovementDirection;
import com.sistemadeoperaciones.cajageneral.service.CashGeneralService;
import com.sistemadeoperaciones.corte.model.BankAccountDailyCut;
import com.sistemadeoperaciones.corte.model.DailyCashCut;
import com.sistemadeoperaciones.corte.enums.DailyCashCutStatus;
import com.sistemadeoperaciones.corte.service.BankAccountDailyCutServiceImpl;
import com.sistemadeoperaciones.corte.service.DailyCashCutServiceImpl;
import com.sistemadeoperaciones.corte.service.BankLedgerQuery;
import com.sistemadeoperaciones.pagos.service.PaymentOperationServiceImpl;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.exception.ConflictException;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
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
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({CashBankDepositService.class, CashGeneralService.class,
        BankAccountDailyCutServiceImpl.class, BankLedgerQuery.class, DailyCashCutServiceImpl.class})
class CashBankDepositServiceTest {
    @Autowired TestEntityManager em;
    @Autowired CashBankDepositService service;
    @Autowired CashGeneralService cash;
    @Autowired BankLedgerQuery ledger;
    @Autowired DailyCashCutServiceImpl bankWideCut;
    @MockBean AuthenticatedUserService auth;
    @MockBean BusinessAuditService audit;
    @MockBean PaymentOperationServiceImpl paymentService;
    User user;
    BankAccount account;

    @BeforeEach void setup() {
        user = new User(); user.setNombre("Jefa"); user.setCorreo("jefa+" + System.nanoTime() + "@example.test");
        em.persist(user); when(auth.getCurrentUser()).thenReturn(user);
        account = new BankAccount();
        account.setBanco("BANORTE"); account.setTitular("Oficina");
        account.setNumeroCuenta("123456789012345678"); account.setClabe("123456789012345678"); account.setActivo(true);
        em.persist(account);
        BankAccountDailyCut previous = new BankAccountDailyCut();
        previous.setBankAccount(account); previous.setFecha(LocalDate.now().minusDays(1));
        previous.setSaldoInicial(new BigDecimal("100")); previous.setSaldoFinal(new BigDecimal("100"));
        em.persist(previous);
        DailyCashCut globalPrevious = new DailyCashCut();
        globalPrevious.setFecha(LocalDate.now().minusDays(1));
        globalPrevious.setSaldoInicial(new BigDecimal("100"));
        globalPrevious.setSaldoFinal(new BigDecimal("100"));
        globalPrevious.setEstatus(DailyCashCutStatus.CERRADO);
        em.persist(globalPrevious);
        em.flush();
        cash.open(new OpenCashDayRequest(LocalDate.now(), new BigDecimal("1000"), hundreds(10)));
    }

    static Map<CashDenomination, Integer> hundreds(int count) {
        var values = new EnumMap<CashDenomination, Integer>(CashDenomination.class);
        for (var d : CashDenomination.values()) values.put(d, 0);
        values.put(CashDenomination.D100, count);
        return values;
    }
    CashBankDepositView register(int hundreds) {
        return service.register(new CashBankDepositRequests.Register(UUID.randomUUID(), account.getId(),
                new BigDecimal(hundreds * 100), "VENTANILLA", "REF-1",
                "https://example.test/salida.pdf", hundreds(hundreds)));
    }

    @Test void theTrayListsTheMostRecentDepositFirstWhileTheSummaryKeepsTheOldest() {
        var first = register(2);
        var second = register(3);

        assertThat(service.pending()).extracting(CashBankDepositView::id).containsExactly(second.id(), first.id());
        assertThat(service.pendingSummary().oldest()).isEqualTo(first.registradoEn());
    }

    @Test void pendingReservesWithoutChangingEitherBalance() {
        var d = register(6);
        assertThat(d.estatus()).isEqualTo(CashBankDepositStatus.PENDIENTE);
        assertThat(cash.latest().saldoActual()).isEqualByComparingTo("1000");
        assertThat(ledger.bucketsForAccount(account.getId(), LocalDate.now()).entradasDeposito())
                .isEqualByComparingTo("0");
        assertThatThrownBy(() -> register(5)).isInstanceOf(ConflictException.class)
                .hasMessageContaining("disponible");
    }

    @Test void editingPendingDepositReplacesReservationAndConfirmationUsesEditedAmount() throws Exception {
        var original = register(6);
        var edited = service.edit(original.id(), new CashBankDepositRequests.Edit(original.version(),
                account.getId(), new BigDecimal("700"), "CAJERO", "REF-2",
                "https://example.test/edited.pdf", hundreds(7)));
        assertThat(edited.version()).isGreaterThan(original.version());
        assertThat(edited.denominaciones().get(CashDenomination.D100)).isEqualTo(7);
        assertThat(service.preview(null, null).reservadoEnTransito()).isEqualByComparingTo("700");
        assertThat(cash.latest().saldoActual()).isEqualByComparingTo("1000");
        service.confirm(edited.id(), new CashBankDepositRequests.Confirm("https://example.test/banco.pdf"));
        assertThat(cash.latest().saldoActual()).isEqualByComparingTo("300");
        assertThat(ledger.bucketsForAccount(account.getId(), LocalDate.now()).entradasDeposito())
                .isEqualByComparingTo("700");
        ArgumentCaptor<Object> before = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> after = ArgumentCaptor.forClass(Object.class);
        verify(audit).record(eq(user.getId()), eq("CASH_BANK_DEPOSIT_EDIT"),
                eq("CASH_BANK_DEPOSIT"), eq(original.id()), before.capture(), after.capture());
        em.clear();
        assertThat(new ObjectMapper().writeValueAsString(before.getValue())).contains("\"counts\"");
        assertThat(new ObjectMapper().writeValueAsString(after.getValue())).contains("\"counts\"");
    }

    @Test void editingDepositRejectsInsufficientCashStaleVersionAndResolvedStatus() {
        var original = register(6);
        assertThatThrownBy(() -> service.edit(original.id(), new CashBankDepositRequests.Edit(original.version(),
                account.getId(), new BigDecimal("1100"), "VENTANILLA", null,
                "https://example.test/salida.pdf", hundreds(11))))
                .isInstanceOf(ConflictException.class).hasMessageContaining("insuficiente");
        var edited = service.edit(original.id(), new CashBankDepositRequests.Edit(original.version(),
                account.getId(), new BigDecimal("500"), "VENTANILLA", null,
                "https://example.test/salida.pdf", hundreds(5)));
        assertThatThrownBy(() -> service.edit(original.id(), new CashBankDepositRequests.Edit(original.version(),
                account.getId(), new BigDecimal("400"), "VENTANILLA", null,
                "https://example.test/salida.pdf", hundreds(4))))
                .isInstanceOf(ConflictException.class).hasMessageContaining("cambió");
        service.confirm(edited.id(), new CashBankDepositRequests.Confirm("https://example.test/banco.pdf"));
        assertThatThrownBy(() -> service.edit(original.id(), new CashBankDepositRequests.Edit(edited.version(),
                account.getId(), new BigDecimal("400"), "VENTANILLA", null,
                "https://example.test/salida.pdf", hundreds(4))))
                .isInstanceOf(ConflictException.class).hasMessageContaining("pendiente");
    }

    @Test void reservedCashCannotLeaveThroughAnotherCashMovement() {
        register(8);
        var manual = new CreateCashMovementRequest(UUID.randomUUID(), CashMovementDirection.SALIDA,
                CashMovementConcept.EFECTIVO, "Salida", null, null, new BigDecimal("300"),
                null, hundreds(3), null);
        assertThatThrownBy(() -> cash.createMovement(cash.latest().id(), manual))
                .hasMessageContaining("reservado");
    }

    @Test void requiresOutgoingProofAndValidAccount() {
        var noProof = new CashBankDepositRequests.Register(UUID.randomUUID(), account.getId(),
                new BigDecimal("100"), "VENTANILLA", null, null, hundreds(1));
        assertThatThrownBy(() -> service.register(noProof)).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("comprobante");
        account.setActivo(false); em.flush();
        assertThatThrownBy(() -> register(1)).isInstanceOf(ConflictException.class)
                .hasMessageContaining("inactiva");
    }

    @Test void acceptsOnlyTheThreeDepositMethods() {
        for (String method : new String[]{"VENTANILLA", "CAJERO", "OXXO"}) {
            var request = new CashBankDepositRequests.Register(UUID.randomUUID(), account.getId(),
                    new BigDecimal("100"), method, null, "https://example.test/salida.pdf", hundreds(1));
            assertThat(service.register(request).forma()).isEqualTo(method);
        }
        var invalid = new CashBankDepositRequests.Register(UUID.randomUUID(), account.getId(),
                new BigDecimal("100"), "TRANSFERENCIA", null, "https://example.test/salida.pdf", hundreds(1));
        assertThatThrownBy(() -> service.register(invalid)).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("forma de depósito válida");
    }

    @Test void confirmingMovesCashAndBankOnce() {
        var d = register(3);
        var first = service.confirm(d.id(), new CashBankDepositRequests.Confirm("https://example.test/banco.pdf"));
        var again = service.confirm(d.id(), new CashBankDepositRequests.Confirm("https://example.test/banco.pdf"));
        assertThat(first.cashMovementId()).isNotNull().isEqualTo(again.cashMovementId());
        assertThat(cash.latest().saldoActual()).isEqualByComparingTo("700");
        assertThat(ledger.bucketsForAccount(account.getId(), LocalDate.now()).entradasDeposito())
                .isEqualByComparingTo("300");
        assertThat(service.pending()).isEmpty();
        verify(audit, times(1)).record(eq(user.getId()), eq("CASH_BANK_DEPOSIT_CONFIRM"),
                eq("CASH_BANK_DEPOSIT"), eq(d.id()), any(), any());
    }

    @Test void confirmedDepositAppearsInBankWideCutExactlyOnce() {
        var d = register(3);
        service.confirm(d.id(), new CashBankDepositRequests.Confirm("https://example.test/banco.pdf"));
        var cut = bankWideCut.calculateDailyCut(LocalDate.now());
        assertThat(cut.getEntradasDeposito()).isEqualByComparingTo("300");
        assertThat(cut.getSaldoFinal()).isEqualByComparingTo("400");
    }

    @Test void rejectionKeepsReservationUntilCountedReturn() {
        var d = register(8);
        service.reject(d.id(), new CashBankDepositRequests.Reject("No llegó al banco"));
        assertThatThrownBy(() -> register(3)).isInstanceOf(ConflictException.class);
        service.confirmReturn(d.id(), new CashBankDepositRequests.ConfirmReturn(hundreds(8)));
        assertThat(service.rejectedForReturn()).isEmpty();
        assertThat(cash.latest().saldoActual()).isEqualByComparingTo("1000");
        assertThat(ledger.bucketsForAccount(account.getId(), LocalDate.now()).entradasDeposito())
                .isEqualByComparingTo("0");
        register(3);
    }

    @Test void requestIdIsIdempotentAndRejectsDifferentPayload() {
        UUID id = UUID.randomUUID();
        var request = new CashBankDepositRequests.Register(id, account.getId(), new BigDecimal("200"),
                "VENTANILLA", null, "https://example.test/proof", hundreds(2));
        assertThat(service.register(request).id()).isEqualTo(service.register(request).id());
        var altered = new CashBankDepositRequests.Register(id, account.getId(), new BigDecimal("300"),
                "VENTANILLA", null, "https://example.test/proof", hundreds(3));
        assertThatThrownBy(() -> service.register(altered)).isInstanceOf(ConflictException.class);
    }
}
