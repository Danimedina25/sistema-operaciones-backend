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

        when(cashMovements.findByDiaFechaBetweenOrderByIdAsc(start, end)).thenReturn(List.of(
                cash(1, CashMovementConcept.EFECTIVO, CashMovementDirection.ENTRADA, "50.00"),
                cash(2, CashMovementConcept.RETIRO_CON_TARJETA, CashMovementDirection.ENTRADA, "20.00"),
                cash(3, CashMovementConcept.EFECTIVO, CashMovementDirection.SALIDA, "10.00")));
        when(bankLedger.search(any(), eq(0), eq(100))).thenReturn(new PageImpl<>(List.of(
                bank(1, BankLedgerOrigin.PAGO, BankLedgerDirection.ENTRADA, "30.00"),
                bank(2, BankLedgerOrigin.CAJA_GENERAL, BankLedgerDirection.SALIDA, "20.00"),
                bank(3, BankLedgerOrigin.COMISION, BankLedgerDirection.SALIDA, "40.00"))));

        var result = new GeneralCutReadService(days, cashMovements, bankCuts, bankLedger, withdrawals)
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
        var service = new GeneralCutReadService(days, cashMovements, bankCuts, bankLedger, withdrawals);
        assertThrows(BadRequestException.class, () -> service.read(LocalDate.now(), LocalDate.now().plusDays(1), 0, 30));
        assertThrows(BadRequestException.class, () -> service.read(LocalDate.now(), LocalDate.now().minusDays(1), 0, 30));
    }

    private BankAccountBalanceResponseDto balance(String amount) {
        var row = new BankAccountBalanceResponseDto(); row.setSaldoFinal(new BigDecimal(amount)); return row;
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
