package com.sistemadeoperaciones.corte.service;

import com.sistemadeoperaciones.cajageneral.enums.CashMovementConcept;
import com.sistemadeoperaciones.cajageneral.enums.CashMovementDirection;
import com.sistemadeoperaciones.cajageneral.model.CashGeneralDay;
import com.sistemadeoperaciones.cajageneral.model.CashGeneralMovement;
import com.sistemadeoperaciones.cajageneral.repository.CashGeneralDayRepository;
import com.sistemadeoperaciones.cajageneral.repository.CashGeneralMovementRepository;
import com.sistemadeoperaciones.corte.dto.BankAccountBalanceResponseDto;
import com.sistemadeoperaciones.corte.dto.BankLedgerRowDto;
import com.sistemadeoperaciones.corte.enums.BankLedgerDirection;
import com.sistemadeoperaciones.corte.enums.BankLedgerOrigin;
import com.sistemadeoperaciones.retiroscaja.BankCashWithdrawalRepository;
import com.sistemadeoperaciones.retiroscaja.WithdrawalStatus;
import com.sistemadeoperaciones.envioscaja.CashBankDepositRepository;
import com.sistemadeoperaciones.envioscaja.CashBankDepositStatus;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeneralCutReadServiceTest {
    @Mock CashGeneralDayRepository days;
    @Mock CashGeneralMovementRepository cashMovements;
    @Mock BankAccountDailyCutService bankCuts;
    @Mock BankLedgerService bankLedger;
    @Mock BankCashWithdrawalRepository withdrawals;
    @Mock CashBankDepositRepository deposits;

    @Test void consolidatesExternalMoneyOnceAndKeepsInternalTransfersSeparate() {
        LocalDate start = LocalDate.of(2026, 10, 1), end = LocalDate.of(2026, 10, 2);
        var before = new CashGeneralDay(); before.setSaldoActual(new BigDecimal("100.00"));
        var last = new CashGeneralDay(); last.setSaldoActual(new BigDecimal("160.00"));
        when(days.findByFechaBetweenOrderByFechaAsc(start, end)).thenReturn(List.of());
        when(days.findFirstByFechaLessThanEqualOrderByFechaDesc(start.minusDays(1))).thenReturn(Optional.of(before));
        when(days.findFirstByFechaLessThanEqualOrderByFechaDesc(end)).thenReturn(Optional.of(last));
        when(bankCuts.calculateBalances(start.minusDays(1))).thenReturn(List.of(balance("200.00")));
        when(bankCuts.calculateBalances(end)).thenReturn(List.of(balance("170.00")));
        when(bankCuts.calculateBalancesGrouped(end)).thenReturn(List.of());
        when(withdrawals.findByEstatusOrderByRegistradoEnAscIdAsc(WithdrawalStatus.PENDIENTE)).thenReturn(List.of());
        when(deposits.findByEstatusOrderByRegistradoEnAscIdAsc(CashBankDepositStatus.PENDIENTE)).thenReturn(List.of());
        when(deposits.findByEstatusOrderByRegistradoEnAscIdAsc(CashBankDepositStatus.RECHAZADO_PENDIENTE_DEVOLUCION)).thenReturn(List.of());

        when(cashMovements.findByDiaFechaBetweenOrderByIdAsc(start, end)).thenReturn(List.of(
                cash(1, CashMovementConcept.EFECTIVO, CashMovementDirection.ENTRADA, "50.00"),
                cash(2, CashMovementConcept.RETIRO_CON_TARJETA, CashMovementDirection.ENTRADA, "20.00"),
                cash(3, CashMovementConcept.EFECTIVO, CashMovementDirection.SALIDA, "10.00")));
        when(bankLedger.search(any(), eq(0), eq(100))).thenReturn(new PageImpl<>(List.of(
                bank(1, BankLedgerOrigin.PAGO, BankLedgerDirection.ENTRADA, "30.00"),
                bank(2, BankLedgerOrigin.CAJA_GENERAL, BankLedgerDirection.SALIDA, "20.00"),
                bank(3, BankLedgerOrigin.COMISION, BankLedgerDirection.SALIDA, "40.00"))));

        var result = new GeneralCutReadService(days, cashMovements, bankCuts, bankLedger, withdrawals, deposits, new ReturnCustodyQuery(org.mockito.Mockito.mock(com.sistemadeoperaciones.pagos.repository.OperationReturnInstallmentRepository.class)))
                .read(start, end, 0, 30);
        assertEquals(new BigDecimal("300.00"), result.totalInitial());
        assertEquals(new BigDecimal("80.00"), result.totalIncome());
        assertEquals(new BigDecimal("50.00"), result.totalOutflow());
        assertEquals(new BigDecimal("330.00"), result.totalFinal());
        assertEquals(0, result.difference().signum());
        assertEquals(new BigDecimal("20.00"), result.cashInternal());
        assertEquals(new BigDecimal("-20.00"), result.bankInternal());
        assertEquals(6, result.totalMovements());
    }

    @Test void rejectsFutureAndInvalidPeriods() {
        var service = new GeneralCutReadService(days, cashMovements, bankCuts, bankLedger, withdrawals, deposits, new ReturnCustodyQuery(org.mockito.Mockito.mock(com.sistemadeoperaciones.pagos.repository.OperationReturnInstallmentRepository.class)));
        assertThrows(BadRequestException.class, () -> service.read(LocalDate.now(), LocalDate.now().plusDays(1), 0, 30));
        assertThrows(BadRequestException.class, () -> service.read(LocalDate.now(), LocalDate.now().minusDays(1), 0, 30));
    }

    @Test void cashToBankDepositIsInternalAndKeepsTheCombinedBalance() {
        LocalDate start = LocalDate.of(2026, 10, 1), end = LocalDate.of(2026, 10, 2);
        var before = new CashGeneralDay(); before.setSaldoActual(new BigDecimal("100"));
        var after = new CashGeneralDay(); after.setSaldoActual(new BigDecimal("70"));
        when(days.findByFechaBetweenOrderByFechaAsc(start, end)).thenReturn(List.of());
        when(days.findFirstByFechaLessThanEqualOrderByFechaDesc(start.minusDays(1))).thenReturn(Optional.of(before));
        when(days.findFirstByFechaLessThanEqualOrderByFechaDesc(end)).thenReturn(Optional.of(after));
        when(bankCuts.calculateBalances(start.minusDays(1))).thenReturn(List.of(balance("200")));
        when(bankCuts.calculateBalances(end)).thenReturn(List.of(balance("230")));
        when(bankCuts.calculateBalancesGrouped(end)).thenReturn(List.of());
        when(withdrawals.findByEstatusOrderByRegistradoEnAscIdAsc(WithdrawalStatus.PENDIENTE)).thenReturn(List.of());
        when(deposits.findByEstatusOrderByRegistradoEnAscIdAsc(CashBankDepositStatus.PENDIENTE)).thenReturn(List.of());
        when(deposits.findByEstatusOrderByRegistradoEnAscIdAsc(CashBankDepositStatus.RECHAZADO_PENDIENTE_DEVOLUCION)).thenReturn(List.of());
        when(cashMovements.findByDiaFechaBetweenOrderByIdAsc(start, end)).thenReturn(List.of(
                cash(1, CashMovementConcept.ENVIO_A_CUENTA, CashMovementDirection.SALIDA, "30")));
        when(bankLedger.search(any(), eq(0), eq(100))).thenReturn(new PageImpl<>(List.of(
                bank(1, BankLedgerOrigin.CAJA_GENERAL, BankLedgerDirection.ENTRADA, "30"))));

        var result = new GeneralCutReadService(days, cashMovements, bankCuts, bankLedger, withdrawals, deposits, new ReturnCustodyQuery(org.mockito.Mockito.mock(com.sistemadeoperaciones.pagos.repository.OperationReturnInstallmentRepository.class)))
                .read(start, end, 0, 30);
        assertEquals(0, result.difference().signum());
        assertEquals(new BigDecimal("-30"), result.cashInternal());
        assertEquals(new BigDecimal("30"), result.bankInternal());
        assertEquals(0, result.totalInternal().signum());
        assertEquals(0, result.totalIncome().signum());
        assertEquals(0, result.totalOutflow().signum());
    }

    @Test void countDifferencesAtCloseAreIncomeOrOutflowAndKeepThePeriodBalanced() {
        LocalDate start = LocalDate.of(2026, 10, 1), end = LocalDate.of(2026, 10, 3);
        // El cierre previo contó 5 de más: el periodo arranca con lo contado, no con el sistema.
        var before = closedDay(0L, "100", "105");
        var sobrante = closedDay(1L, "145", "185");
        var faltante = closedDay(2L, "185", "175");
        var abierto = new CashGeneralDay(); abierto.setId(3L); abierto.setSaldoInicial(new BigDecimal("175"));
        abierto.setSaldoActual(new BigDecimal("195"));
        when(days.findByFechaBetweenOrderByFechaAsc(start, end)).thenReturn(List.of(sobrante, faltante, abierto));
        when(days.findFirstByFechaLessThanEqualOrderByFechaDesc(start.minusDays(1))).thenReturn(Optional.of(before));
        when(days.findFirstByFechaLessThanEqualOrderByFechaDesc(end)).thenReturn(Optional.of(abierto));
        when(bankCuts.calculateBalances(any())).thenReturn(List.of());
        when(bankCuts.calculateBalancesGrouped(end)).thenReturn(List.of());
        when(withdrawals.findByEstatusOrderByRegistradoEnAscIdAsc(WithdrawalStatus.PENDIENTE)).thenReturn(List.of());
        when(deposits.findByEstatusOrderByRegistradoEnAscIdAsc(any())).thenReturn(List.of());
        when(cashMovements.findByDiaFechaBetweenOrderByIdAsc(start, end)).thenReturn(List.of(
                cash(1, CashMovementConcept.EFECTIVO, CashMovementDirection.ENTRADA, "40"),
                cash(2, CashMovementConcept.EFECTIVO, CashMovementDirection.ENTRADA, "20")));
        when(bankLedger.search(any(), eq(0), eq(100))).thenReturn(new PageImpl<>(List.of()));

        var result = new GeneralCutReadService(days, cashMovements, bankCuts, bankLedger, withdrawals, deposits, new ReturnCustodyQuery(org.mockito.Mockito.mock(com.sistemadeoperaciones.pagos.repository.OperationReturnInstallmentRepository.class)))
                .read(start, end, 0, 30);
        assertEquals(new BigDecimal("105"), result.cashInitial());
        assertEquals(new BigDecimal("100"), result.cashIncome());
        assertEquals(new BigDecimal("10"), result.cashOutflow());
        assertEquals(new BigDecimal("195"), result.cashFinal());
        assertEquals(0, result.difference().signum());
        assertTrue(result.movements().stream().anyMatch(m -> m.concept().equals("Diferencia de arqueo · sobrante")
                && m.type().equals("ENTRADA") && m.amount().compareTo(new BigDecimal("40")) == 0));
        assertTrue(result.movements().stream().anyMatch(m -> m.concept().equals("Diferencia de arqueo · faltante")
                && m.type().equals("SALIDA") && m.amount().compareTo(new BigDecimal("10")) == 0));
    }

    private CashGeneralDay closedDay(long id, String system, String counted) {
        var day = new CashGeneralDay(); day.setId(id);
        day.setSaldoActual(new BigDecimal(system)); day.setSaldoContado(new BigDecimal(counted));
        day.setDiferencia(new BigDecimal(counted).subtract(new BigDecimal(system)));
        day.setClosedAt(LocalDateTime.of(2026, 10, 1, 20, 0).plusDays(id));
        return day;
    }

    private BankAccountBalanceResponseDto balance(String amount) {
        var row = new BankAccountBalanceResponseDto(); row.setSaldoFinal(new BigDecimal(amount)); return row;
    }

    @Test void preparedCardlessCashRemainsInBusinessTotalUntilPhysicalDelivery() {
        var today = LocalDate.now();
        when(bankCuts.calculateBalances(today.minusDays(1))).thenReturn(List.of(balance("1000")));
        when(bankCuts.calculateBalances(today)).thenReturn(List.of(balance("700")));
        var row = new com.sistemadeoperaciones.pagos.model.OperationReturnInstallment();
        row.setId(42L); row.setMonto(new BigDecimal("300")); row.setFechaPreparacionBancaria(today.atTime(10, 0));
        var request = new com.sistemadeoperaciones.pagos.model.OperationReturnPayment();
        var operation = new com.sistemadeoperaciones.pagos.model.PaymentOperation(); operation.setId(7L); request.setOperacion(operation); row.setSolicitud(request);
        var repo = org.mockito.Mockito.mock(com.sistemadeoperaciones.pagos.repository.OperationReturnInstallmentRepository.class);
        when(repo.findByFechaPreparacionBancariaIsNotNull()).thenReturn(List.of(row));
        when(bankLedger.search(any(), eq(0), eq(100))).thenReturn(new PageImpl<>(List.of(new BankLedgerRowDto("42", BankLedgerOrigin.RETORNO, 42L,
            today.atTime(10, 0), BankLedgerDirection.SALIDA, "RETIRO_SIN_TARJETA", "Retiro", new BigDecimal("300"),
            1L, "BANORTE", "Titular", "••8998", true, 7L, 42L, null, 1L, "Cajas"))));
        var service = new GeneralCutReadService(days, cashMovements, bankCuts, bankLedger, withdrawals, deposits, new ReturnCustodyQuery(repo));
        var prepared = service.read(today, today, 0, 30);
        assertEquals(0, prepared.totalFinal().compareTo(new BigDecimal("1000")));
        assertEquals(0, prepared.custodyFinal().compareTo(new BigDecimal("300")));
        assertEquals(0, prepared.totalOutflow().signum()); assertEquals(0, prepared.difference().signum());
        assertEquals(0, prepared.cashFinal().signum()); assertEquals(0, prepared.totalInternal().signum());
        row.setFechaEntrega(today.atTime(11, 0));
        var delivered = service.read(today, today, 0, 30);
        assertEquals(0, delivered.totalFinal().compareTo(new BigDecimal("700")));
        assertEquals(0, delivered.totalOutflow().compareTo(new BigDecimal("300")));
        assertEquals(0, delivered.custodyFinal().signum()); assertEquals(0, delivered.difference().signum());
    }
    private CashGeneralMovement cash(long id, CashMovementConcept type, CashMovementDirection direction, String amount) {
        var row = new CashGeneralMovement(); row.setId(id); row.setTipo(type); row.setDireccion(direction);
        row.setMontoManual(new BigDecimal(amount)); row.setConcepto("Movimiento"); return row;
    }
    private BankLedgerRowDto bank(long id, BankLedgerOrigin origin, BankLedgerDirection direction, String amount) {
        return new BankLedgerRowDto(String.valueOf(id), origin, id, LocalDateTime.of(2026, 10, 1, 12, 0),
                direction, "TRANSFERENCIA", "Movimiento", new BigDecimal(amount), 1L, "BANORTE",
                "Cuenta", "••8998", true, null, null, null, null, null);
    }
}
