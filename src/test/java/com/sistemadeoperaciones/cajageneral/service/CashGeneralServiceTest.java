package com.sistemadeoperaciones.cajageneral.service;

import com.sistemadeoperaciones.cajageneral.dto.*;
import com.sistemadeoperaciones.cajageneral.enums.*;
import com.sistemadeoperaciones.cajageneral.model.*;
import com.sistemadeoperaciones.cajageneral.repository.*;
import com.sistemadeoperaciones.corte.service.BankAccountDailyCutService;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.cuentasbancarias.repository.BankAccountRepository;
import com.sistemadeoperaciones.shared.exception.ConflictException;
import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;
import com.sistemadeoperaciones.pagos.enums.*;
import com.sistemadeoperaciones.pagos.model.*;
import com.sistemadeoperaciones.pagos.repository.OperationReturnInstallmentRepository;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.usuarios.model.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class CashGeneralServiceTest {
    @Mock CashGeneralRegisterRepository register;
    @Mock CashGeneralDayRepository days;
    @Mock CashGeneralMovementRepository movements;
    @Mock OperationReturnInstallmentRepository installments;
    @Mock AuthenticatedUserService auth;
    @Mock CashGeneralDeletionAuditRepository deletionAudits;
    @Mock BankAccountRepository bankAccounts;
    @Mock BankAccountDailyCutService bankCuts;
    @InjectMocks CashGeneralService service;
    CashGeneralDay day;
    User user;
    static BigDecimal money(String value) { return new BigDecimal(value); }
    static Map<CashDenomination, Integer> counts(int hundreds, int halves) {
        Map<CashDenomination, Integer> values = new EnumMap<>(CashDenomination.class);
        for (var d : CashDenomination.values()) values.put(d, 0);
        values.put(CashDenomination.D100, hundreds); values.put(CashDenomination.D050, halves);
        return values;
    }
    @BeforeEach void setup() {
        user = new User(); user.setId(7L);
        day = new CashGeneralDay(); day.setId(1L); day.setVersion(0L); day.setFecha(LocalDate.now());
        day.setSaldoInicial(money("100")); day.setSaldoActual(money("100"));
        day.setApertura(counts(1,0)); day.setCierre(new EnumMap<>(CashDenomination.class)); day.setAbiertoPor(user);
        lenient().when(auth.getCurrentUser()).thenReturn(user);
        lenient().when(days.findById(1L)).thenReturn(Optional.of(day));
        lenient().when(days.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(movements.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }
    CreateCashMovementRequest request(CashMovementDirection direction, BigDecimal amount, Long reference, int hundreds) {
        return new CreateCashMovementRequest(UUID.randomUUID(), direction, CashMovementConcept.EFECTIVO,
                "Movimiento", null, null, amount, reference, counts(hundreds,0), null);
    }
    /** Cuenta bancaria activa disponible para los cheques cobrados. */
    BankAccount activeAccount() {
        BankAccount account = new BankAccount();
        account.setId(4L); account.setBanco("BBVA"); account.setTitular("Operaciones SA");
        account.setNumeroCuenta("00012345678"); account.setActivo(true);
        lenient().when(bankAccounts.findById(4L)).thenReturn(Optional.of(account));
        return account;
    }
    CreateCashMovementRequest cheque(CashMovementDirection direction, Long bankAccountId) {
        return new CreateCashMovementRequest(UUID.randomUUID(), direction, CashMovementConcept.CHEQUE,
                "Cheque cobrado", null, bankAccountId, money("100"), null, counts(1,0), null);
    }
    @Test void denominationsAreExactIncludingFiftyCents() {
        assertThat(CashGeneralAmounts.total(counts(2,3))).isEqualByComparingTo("201.50");
        assertThatThrownBy(() -> CashGeneralAmounts.requireTotal(counts(2,3), money("201.51"))).hasMessageContaining("exactamente");
        assertThatThrownBy(() -> CashGeneralAmounts.total(counts(-1,0))).hasMessageContaining("no negativos");
        assertThatThrownBy(() -> CashGeneralAmounts.total(Map.of())).hasMessageContaining("11 denominaciones");
        assertThatThrownBy(() -> CashGeneralAmounts.money(money("1.001"),true)).hasMessageContaining("dos decimales");
    }
    @Test void ledgerMaintainsRunningBalanceAndAllowsZero() {
        var incoming = service.createMovement(1L, request(CashMovementDirection.ENTRADA,money("200"),null,2));
        assertThat(incoming.saldoAcumulado()).isEqualByComparingTo("300");
        var outgoing = service.createMovement(1L, request(CashMovementDirection.SALIDA,money("300"),null,3));
        assertThat(outgoing.saldoAcumulado()).isEqualByComparingTo("0");
    }
    @Test void overdraftDoesNotWrite() {
        assertThatThrownBy(() -> service.createMovement(1L,request(CashMovementDirection.SALIDA,money("200"),null,2)))
                .hasMessageContaining("Saldo insuficiente");
        verify(movements, never()).saveAndFlush(any());
        assertThat(day.getSaldoActual()).isEqualByComparingTo("100");
    }
    @Test void mismatchedMovementDoesNotWrite() {
        assertThatThrownBy(() -> service.createMovement(1L,request(CashMovementDirection.ENTRADA,money("101"),null,1)))
                .hasMessageContaining("exactamente");
        verify(movements,never()).saveAndFlush(any());
    }
    @Test void acceptsPhysicalConceptsAndRejectsBankOnlyMovements() {
        var nonCash = new CreateCashMovementRequest(UUID.randomUUID(), CashMovementDirection.ENTRADA,
                CashMovementConcept.DEPOSITO, "Depósito", "BBVA", null, money("100"), null, counts(1,0), null);
        assertThatThrownBy(() -> service.createMovement(1L, nonCash))
                .hasMessageContaining("efectivo, cheque cobrado o retiro sin tarjeta");

        var cashWithBank = new CreateCashMovementRequest(UUID.randomUUID(), CashMovementDirection.ENTRADA,
                CashMovementConcept.EFECTIVO, "Efectivo", "BBVA", null, money("100"), null, counts(1,0), null);
        assertThatThrownBy(() -> service.createMovement(1L, cashWithBank))
                .hasMessageContaining("no admiten banco");

        activeAccount();
        var chequeResponse = service.createMovement(1L, cheque(CashMovementDirection.ENTRADA, 4L));
        assertThat(chequeResponse.tipo()).isEqualTo(CashMovementConcept.CHEQUE);
        assertThat(chequeResponse.bankAccountId()).isEqualTo(4L);

        // El retiro sin tarjeta exige cuenta real, igual que el cheque.
        var withdrawalWithoutAccount = new CreateCashMovementRequest(UUID.randomUUID(), CashMovementDirection.ENTRADA,
                CashMovementConcept.RETIRO_SIN_TARJETA, "Retiro sin tarjeta", null, null, money("100"), null, counts(1,0), null);
        assertThatThrownBy(() -> service.createMovement(1L, withdrawalWithoutAccount))
                .hasMessageContaining("Selecciona la cuenta bancaria");

        // El retiro con tarjeta nunca existió en la operación: ya no se puede capturar.
        var card = new CreateCashMovementRequest(UUID.randomUUID(), CashMovementDirection.ENTRADA,
                CashMovementConcept.RETIRO_CON_TARJETA, "Retiro con tarjeta", null, null, money("100"), null, counts(1,0), null);
        assertThatThrownBy(() -> service.createMovement(1L, card))
                .hasMessageContaining("efectivo, cheque cobrado o retiro sin tarjeta");
        verify(movements, times(1)).saveAndFlush(any());
    }
    OperationReturnInstallment completed() {
        var op = new PaymentOperation(); op.setId(5L);
        var req = new OperationReturnPayment(); req.setOperacion(op);
        var i = new OperationReturnInstallment(); i.setId(9L); i.setSolicitud(req);
        i.setTipoPago(PaymentType.EFECTIVO); i.setEstatus(ReturnInstallmentStatus.COMPLETADA);
        i.setMonto(money("100")); i.setFechaRealizacion(LocalDateTime.now());
        lenient().when(installments.findById(9L)).thenReturn(Optional.of(i)); return i;
    }
    @Test void linkedDeliveryUsesForeignKeyWithoutDuplicatingAmount() {
        completed();
        var result = service.createMovement(1L,request(CashMovementDirection.SALIDA,null,9L,1));
        assertThat(result.monto()).isEqualByComparingTo("100"); assertThat(result.operacionId()).isEqualTo(5L);
        var capture = ArgumentCaptor.forClass(CashGeneralMovement.class);
        verify(movements).saveAndFlush(capture.capture());
        assertThat(capture.getValue().getMontoManual()).isNull();
        assertThat(capture.getValue().getParcialidad().getId()).isEqualTo(9L);
    }
    @Test void automaticCashDeliveryCreatesLinkedExitAndDecreasesBalance() {
        var installment = completed();
        installment.setComprobanteEntregaUrl("https://example.com/entrega.jpg");
        when(days.findFirstByOrderByFechaDesc()).thenReturn(Optional.of(day));

        var result = service.recordCashDelivery(installment, counts(1,0));

        assertThat(result.direccion()).isEqualTo(CashMovementDirection.SALIDA);
        assertThat(result.monto()).isEqualByComparingTo("100");
        assertThat(result.saldoAcumulado()).isEqualByComparingTo("0");
        var capture = ArgumentCaptor.forClass(CashGeneralMovement.class);
        verify(movements).saveAndFlush(capture.capture());
        assertThat(capture.getValue().getMontoManual()).isNull();
        assertThat(capture.getValue().getParcialidad()).isSameAs(installment);
        assertThat(capture.getValue().getComprobanteUrl()).isEqualTo("https://example.com/entrega.jpg");
        assertThat(day.getSaldoActual()).isEqualByComparingTo("0");
    }
    @Test void automaticCashDeliveryRequiresTodayOpenCashExactBreakdownAndBalance() {
        var installment = completed();
        when(days.findFirstByOrderByFechaDesc()).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.recordCashDelivery(installment, counts(1,0)))
                .hasMessageContaining("Abre la Caja General");

        when(days.findFirstByOrderByFechaDesc()).thenReturn(Optional.of(day));
        assertThatThrownBy(() -> service.recordCashDelivery(installment, counts(0,0)))
                .hasMessageContaining("exactamente");
        installment.setMonto(money("200"));
        assertThatThrownBy(() -> service.recordCashDelivery(installment, counts(2,0)))
                .hasMessageContaining("Saldo insuficiente");
        verify(movements, never()).saveAndFlush(any());
    }
    @Test void withdrawalWithoutCardNeverCreatesCashExit() {
        var installment = completed();
        installment.setTipoPago(PaymentType.RETIRO_SIN_TARJETA);
        assertThatThrownBy(() -> service.recordCashDelivery(installment, counts(1,0)))
                .hasMessageContaining("efectivo");
        verify(movements, never()).saveAndFlush(any());
    }
    @Test void duplicateReferenceRejected() {
        completed(); when(movements.existsByParcialidadId(9L)).thenReturn(true);
        assertThatThrownBy(() -> service.createMovement(1L,request(CashMovementDirection.SALIDA,null,9L,1))).hasMessageContaining("ya está vinculada");
    }
    @Test void rstIsNotACashDelivery() {
        completed().setTipoPago(PaymentType.RETIRO_SIN_TARJETA);
        assertThatThrownBy(() -> service.createMovement(1L,request(CashMovementDirection.SALIDA,null,9L,1))).hasMessageContaining("EFECTIVO completadas");
    }
    @Test void unconfirmedDeliveryRejected() {
        completed().setEstatus(ReturnInstallmentStatus.ENTREGADA);
        assertThatThrownBy(() -> service.createMovement(1L,request(CashMovementDirection.SALIDA,null,9L,1))).hasMessageContaining("completadas");
    }
    @Test void closeStoresDifferenceAndRequiresExplanation() {
        var request = new CloseCashDayRequest(money("99.50"),0L,counts(0,199),null);
        assertThatThrownBy(() -> service.close(1L,request)).hasMessageContaining("Explica la diferencia");
        var result = service.close(1L,new CloseCashDayRequest(money("99.50"),0L,counts(0,199),"Faltante contado"));
        assertThat(result.diferencia()).isEqualByComparingTo("-0.50");
        assertThat(result.saldoActual()).isEqualByComparingTo("100"); assertThat(result.closedAt()).isNotNull();
    }
    @Test void exactCloseAndStaleVersion() {
        assertThatThrownBy(() -> service.close(1L,new CloseCashDayRequest(money("100"),2L,counts(1,0),null))).hasMessageContaining("cambió");
        assertThat(service.close(1L,new CloseCashDayRequest(money("100"),0L,counts(1,0),null)).diferencia()).isEqualByComparingTo("0");
    }
    @Test void closedDayCannotChange() {
        day.setClosedAt(LocalDateTime.now());
        assertThatThrownBy(() -> service.createMovement(1L,request(CashMovementDirection.ENTRADA,money("100"),null,1))).hasMessageContaining("cerrada");
        assertThatThrownBy(() -> service.close(1L,new CloseCashDayRequest(money("100"),0L,counts(1,0),null))).hasMessageContaining("cerrada");
    }
    @Test void pastOpenDayCannotReceiveMovements() {
        day.setFecha(LocalDate.now().minusDays(1));
        assertThatThrownBy(() -> service.createMovement(1L,request(CashMovementDirection.ENTRADA,money("100"),null,1)))
                .hasMessageContaining("movimientos en la caja del día actual");
        verify(movements, never()).saveAndFlush(any());
    }
    @Test void openingOnlyAcceptsToday() {
        assertThatThrownBy(() -> service.open(new OpenCashDayRequest(LocalDate.now().minusDays(1),money("100"),counts(1,0))))
                .hasMessageContaining("fecha actual");
        assertThatThrownBy(() -> service.open(new OpenCashDayRequest(LocalDate.now().plusDays(1),money("100"),counts(1,0))))
                .hasMessageContaining("fecha actual");
        verify(days, never()).saveAndFlush(any());
    }
    @Test void openingCarriesCountedBalanceAndRejectsOpenOrEarlierDay() {
        when(days.findFirstByOrderByFechaDesc()).thenReturn(Optional.of(day));
        var request = new OpenCashDayRequest(LocalDate.now(),money("100"),counts(1,0));
        assertThatThrownBy(() -> service.open(request)).hasMessageContaining("Cierra");
        day.setClosedAt(LocalDateTime.now()); day.setSaldoContado(money("99.50"));
        assertThatThrownBy(() -> service.open(request)).hasMessageContaining("posterior");
        day.setFecha(LocalDate.now().minusDays(1));
        assertThatThrownBy(() -> service.open(request)).hasMessageContaining("contado");
        var result = service.open(new OpenCashDayRequest(LocalDate.now(),money("99.50"),counts(0,199)));
        assertThat(result.saldoInicial()).isEqualByComparingTo("99.50");
    }
    @Test void retryIsIdempotentEvenAfterClosingButChangedPayloadConflicts() {
        var r = request(CashMovementDirection.ENTRADA,money("100"),null,1);
        service.createMovement(1L,r);
        var capture = ArgumentCaptor.forClass(CashGeneralMovement.class); verify(movements).saveAndFlush(capture.capture());
        when(movements.findByRequestId(r.requestId().toString())).thenReturn(Optional.of(capture.getValue()));
        day.setClosedAt(LocalDateTime.now());
        assertThat(service.createMovement(1L,r).saldoAcumulado()).isEqualByComparingTo("200");
        verify(movements,times(1)).saveAndFlush(any());
        var changed = new CreateCashMovementRequest(r.requestId(),r.direccion(),r.tipo(),"Otro",null,null,r.monto(),null,r.denominaciones(),null);
        assertThatThrownBy(() -> service.createMovement(1L,changed)).hasMessageContaining("otros datos");
    }
    @Test void chequeRequiresAnExistingActiveAccountAndOnlyAsIncome() {
        assertThatThrownBy(() -> service.createMovement(1L, cheque(CashMovementDirection.SALIDA, 4L)))
                .hasMessageContaining("sólo se registra como entrada");

        assertThatThrownBy(() -> service.createMovement(1L, cheque(CashMovementDirection.ENTRADA, null)))
                .hasMessageContaining("Selecciona la cuenta bancaria");

        when(bankAccounts.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createMovement(1L, cheque(CashMovementDirection.ENTRADA, 99L)))
                .isInstanceOf(ResourceNotFoundException.class);

        BankAccount inactive = new BankAccount();
        inactive.setId(5L); inactive.setBanco("Banorte"); inactive.setActivo(false);
        when(bankAccounts.findById(5L)).thenReturn(Optional.of(inactive));
        assertThatThrownBy(() -> service.createMovement(1L, cheque(CashMovementDirection.ENTRADA, 5L)))
                .hasMessageContaining("inactiva");

        var withFreeText = new CreateCashMovementRequest(UUID.randomUUID(), CashMovementDirection.ENTRADA,
                CashMovementConcept.CHEQUE, "Cheque cobrado", "BBVA", 4L, money("100"), null, counts(1,0), null);
        assertThatThrownBy(() -> service.createMovement(1L, withFreeText))
                .hasMessageContaining("no con el nombre del banco");

        verify(movements, never()).saveAndFlush(any());
        assertThat(day.getSaldoActual()).isEqualByComparingTo("100");
    }
    @Test void cashNeverCarriesABankAccount() {
        var cashWithAccount = new CreateCashMovementRequest(UUID.randomUUID(), CashMovementDirection.ENTRADA,
                CashMovementConcept.EFECTIVO, "Efectivo", null, 4L, money("100"), null, counts(1,0), null);
        assertThatThrownBy(() -> service.createMovement(1L, cashWithAccount))
                .hasMessageContaining("no admiten banco ni cuenta bancaria");

        verify(movements, never()).saveAndFlush(any());
    }
    @Test void aCardlessWithdrawalBehavesLikeACashedCheque() {
        BankAccount account = activeAccount();
        var withdrawal = new CreateCashMovementRequest(UUID.randomUUID(), CashMovementDirection.ENTRADA,
                CashMovementConcept.RETIRO_SIN_TARJETA, "Retiro sin tarjeta", null, 4L, money("100"), null, counts(1,0), null);

        var response = service.createMovement(1L, withdrawal);

        assertThat(response.bankAccountId()).isEqualTo(4L);
        assertThat(response.saldoAcumulado()).isEqualByComparingTo("200");
        var capture = ArgumentCaptor.forClass(CashGeneralMovement.class);
        verify(movements).saveAndFlush(capture.capture());
        assertThat(capture.getValue().getCuentaBancaria()).isSameAs(account);
    }
    @Test void aCardlessWithdrawalOnlyEntersCash() {
        activeAccount();
        var asExit = new CreateCashMovementRequest(UUID.randomUUID(), CashMovementDirection.SALIDA,
                CashMovementConcept.RETIRO_SIN_TARJETA, "Retiro sin tarjeta", null, 4L, money("100"), null, counts(1,0), null);
        assertThatThrownBy(() -> service.createMovement(1L, asExit))
                .hasMessageContaining("sólo se registra como entrada");

        var withFreeText = new CreateCashMovementRequest(UUID.randomUUID(), CashMovementDirection.ENTRADA,
                CashMovementConcept.RETIRO_SIN_TARJETA, "Retiro sin tarjeta", "BBVA", 4L, money("100"), null, counts(1,0), null);
        assertThatThrownBy(() -> service.createMovement(1L, withFreeText))
                .hasMessageContaining("no con el nombre del banco");

        verify(movements, never()).saveAndFlush(any());
    }
    @Test void chequeStoresTheAccountAndSnapshotsItsBankName() {
        BankAccount account = activeAccount();
        var response = service.createMovement(1L, cheque(CashMovementDirection.ENTRADA, 4L));

        assertThat(response.saldoAcumulado()).isEqualByComparingTo("200");
        assertThat(response.cuentaTitular()).isEqualTo("Operaciones SA");
        assertThat(response.cuentaNumero()).isEqualTo("00012345678");

        var capture = ArgumentCaptor.forClass(CashGeneralMovement.class);
        verify(movements).saveAndFlush(capture.capture());
        assertThat(capture.getValue().getCuentaBancaria()).isSameAs(account);
        assertThat(capture.getValue().getBanco()).isEqualTo("BBVA");
    }
    @Test void retryingAChequeWithAnotherAccountIsRejected() {
        activeAccount();
        var original = cheque(CashMovementDirection.ENTRADA, 4L);
        service.createMovement(1L, original);
        var capture = ArgumentCaptor.forClass(CashGeneralMovement.class);
        verify(movements).saveAndFlush(capture.capture());
        when(movements.findByRequestId(original.requestId().toString())).thenReturn(Optional.of(capture.getValue()));

        assertThat(service.createMovement(1L, original).bankAccountId()).isEqualTo(4L);
        verify(movements, times(1)).saveAndFlush(any());

        var otherAccount = new CreateCashMovementRequest(original.requestId(), original.direccion(), original.tipo(),
                original.concepto(), null, 5L, original.monto(), null, original.denominaciones(), null);
        assertThatThrownBy(() -> service.createMovement(1L, otherAccount))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("otros datos");
    }
    @Test void dateRangeRejectsReversalAndExcess() {
        var today = LocalDate.now();
        assertThatThrownBy(() -> service.ledger(today,today.minusDays(1))).hasMessageContaining("rango");
        assertThatThrownBy(() -> service.ledger(today,today.plusYears(2))).hasMessageContaining("rango");
        assertThatThrownBy(() -> service.ledger(today,today.plusDays(1))).hasMessageContaining("futuro");
    }
    @Test void adminDeletionAuditsAndDeletesMovementsBeforeDay() {
        CashGeneralMovement movement = new CashGeneralMovement();
        movement.setId(30L);
        when(movements.findByDiaIdOrderByIdAsc(1L)).thenReturn(List.of(movement));

        service.deleteDay(1L, new DeleteCashDayRequest("ELIMINAR", "Captura duplicada", 0L));

        var audit = ArgumentCaptor.forClass(CashGeneralDeletionAudit.class);
        verify(deletionAudits).save(audit.capture());
        assertThat(audit.getValue().getDeletedDayId()).isEqualTo(1L);
        assertThat(audit.getValue().getFecha()).isEqualTo(day.getFecha());
        assertThat(audit.getValue().getMovementCount()).isEqualTo(1);
        assertThat(audit.getValue().getMotivo()).isEqualTo("Captura duplicada");
        assertThat(audit.getValue().getDeletedBy()).isSameAs(user);
        verify(movements).deleteAll(List.of(movement));
        verify(movements).flush();
        verify(days).delete(day);
        verify(days).flush();
    }
    @Test void deletingADayWithChequesRebuildsTheAffectedBankCuts() {
        BankAccount account = activeAccount();
        BankAccount otra = new BankAccount(); otra.setId(5L); otra.setBanco("Banorte"); otra.setActivo(true);

        CashGeneralMovement cheque = new CashGeneralMovement();
        cheque.setId(30L); cheque.setCuentaBancaria(account);
        CashGeneralMovement otroCheque = new CashGeneralMovement();
        otroCheque.setId(31L); otroCheque.setCuentaBancaria(otra);
        CashGeneralMovement efectivo = new CashGeneralMovement();
        efectivo.setId(32L);
        when(movements.findByDiaIdOrderByIdAsc(1L)).thenReturn(List.of(cheque, otroCheque, efectivo));
        when(bankCuts.recalculateFrom(4L, day.getFecha())).thenReturn(2);
        when(bankCuts.recalculateFrom(5L, day.getFecha())).thenReturn(1);

        service.deleteDay(1L, new DeleteCashDayRequest("ELIMINAR", "Captura duplicada", 0L));

        // Una vez por cuenta afectada, nunca por la salida en efectivo.
        verify(bankCuts).recalculateFrom(4L, day.getFecha());
        verify(bankCuts).recalculateFrom(5L, day.getFecha());
        verifyNoMoreInteractions(bankCuts);

        var audit = ArgumentCaptor.forClass(CashGeneralDeletionAudit.class);
        verify(deletionAudits).save(audit.capture());
        assertThat(audit.getValue().getCortesBancariosRecalculados()).isEqualTo(3);
    }
    @Test void deletingADayWithoutChequesTouchesNoBankCut() {
        CashGeneralMovement efectivo = new CashGeneralMovement();
        efectivo.setId(32L);
        when(movements.findByDiaIdOrderByIdAsc(1L)).thenReturn(List.of(efectivo));

        service.deleteDay(1L, new DeleteCashDayRequest("ELIMINAR", "Captura duplicada", 0L));

        verifyNoInteractions(bankCuts);
        var audit = ArgumentCaptor.forClass(CashGeneralDeletionAudit.class);
        verify(deletionAudits).save(audit.capture());
        assertThat(audit.getValue().getCortesBancariosRecalculados()).isZero();
    }
    @Test void deletionRequiresExactConfirmationReasonAndCurrentVersion() {
        assertThatThrownBy(() -> service.deleteDay(1L, new DeleteCashDayRequest("eliminar", "Error", 0L)))
                .hasMessageContaining("ELIMINAR");
        assertThatThrownBy(() -> service.deleteDay(1L, new DeleteCashDayRequest("ELIMINAR", "  ", 0L)))
                .hasMessageContaining("motivo");
        assertThatThrownBy(() -> service.deleteDay(1L, new DeleteCashDayRequest("ELIMINAR", "Error", 5L)))
                .hasMessageContaining("cambió");
        verify(deletionAudits, never()).save(any());
        verify(days, never()).delete(any());
    }
    @Test void aDayLeftOpenFromAnEarlierDateCanStillBeClosed() {
        // El bloqueo anterior: no se podía cerrar ayer por no ser hoy, y no se podía abrir hoy
        // porque la anterior seguía abierta. La operación quedaba trabada.
        day.setFecha(LocalDate.now().minusDays(1));

        var result = service.close(1L, new CloseCashDayRequest(money("100"), 0L, counts(1,0), null));

        assertThat(result.closedAt()).isNotNull();
        assertThat(result.saldoContado()).isEqualByComparingTo("100");
        assertThat(result.diferencia()).isEqualByComparingTo("0");
    }
    @Test void aFutureCashBoxCannotBeClosed() {
        day.setFecha(LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> service.close(1L, new CloseCashDayRequest(money("100"), 0L, counts(1,0), null)))
                .hasMessageContaining("antes de su fecha");
    }
    @Test void theExpectedBreakdownFollowsTheMovements() {
        // Apertura: 1 billete de 100. Entra 1 de 100, sale 1 de 100 en dos monedas de 50.
        var entrada = new CashGeneralMovement();
        entrada.setDireccion(CashMovementDirection.ENTRADA);
        entrada.setDenominaciones(counts(1, 0));
        var salida = new CashGeneralMovement();
        salida.setDireccion(CashMovementDirection.SALIDA);
        salida.setDenominaciones(counts(0, 2));
        when(movements.findByDiaIdOrderByIdAsc(1L)).thenReturn(List.of(entrada, salida));

        when(days.findFirstByOrderByFechaDesc()).thenReturn(Optional.of(day));

        var dto = service.latest();

        // Se informa aunque alguna denominación quede negativa —se cambió un billete—:
        // el conteo real es el que manda.
        assertThat(dto.denominacionesEsperadas())
                .containsEntry(CashDenomination.D100, 2)
                .containsEntry(CashDenomination.D050, -2);
    }
    @Test void aClosedDayReportsNoExpectedBreakdown() {
        day.setClosedAt(LocalDateTime.now());
        when(days.findFirstByOrderByFechaDesc()).thenReturn(Optional.of(day));

        assertThat(service.latest().denominacionesEsperadas()).isNull();
        verify(movements, never()).findByDiaIdOrderByIdAsc(any());
    }

}
