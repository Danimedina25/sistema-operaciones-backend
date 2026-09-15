package com.sistemadeoperaciones.cajageneral.service;

import com.sistemadeoperaciones.cajageneral.dto.*;
import com.sistemadeoperaciones.cajageneral.enums.*;
import com.sistemadeoperaciones.cajageneral.model.*;
import com.sistemadeoperaciones.cajageneral.repository.*;
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
                "Movimiento", null, amount, reference, counts(hundreds,0), null);
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
        var changed = new CreateCashMovementRequest(r.requestId(),r.direccion(),r.tipo(),"Otro",null,r.monto(),null,r.denominaciones(),null);
        assertThatThrownBy(() -> service.createMovement(1L,changed)).hasMessageContaining("otros datos");
    }
    @Test void dateRangeRejectsReversalAndExcess() {
        var today = LocalDate.now();
        assertThatThrownBy(() -> service.ledger(today,today.minusDays(1))).hasMessageContaining("rango");
        assertThatThrownBy(() -> service.ledger(today,today.plusYears(2))).hasMessageContaining("rango");
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
}
