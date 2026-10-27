package com.sistemadeoperaciones.retiroscaja;

import com.sistemadeoperaciones.corte.service.BankAccountDailyCutService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Saldo disponible de una cuenta, como en un banco: el saldo de hoy menos el efectivo que ya
 * se retiró y todavía no confirma Caja General. Ninguna salida registrada por Cuentas puede
 * dejar una cuenta en negativo.
 */
@Component
@RequiredArgsConstructor
public class BankAvailability {
    private final BankAccountDailyCutService balances;
    private final BankCashWithdrawalRepository withdrawals;
    private final com.sistemadeoperaciones.pagos.repository.OperationReturnInstallmentRepository returnInstallments;

    public BigDecimal balance(Long accountId) {
        return balances.calculateBalance(accountId, LocalDate.now()).getSaldoFinal();
    }

    public BigDecimal inTransit(Long accountId) {
        return withdrawals.sumPendingByAccount(accountId);
    }

    public BigDecimal available(Long accountId) {
        return balance(accountId).subtract(inTransit(accountId)).subtract(java.util.Objects.requireNonNullElse(returnInstallments.sumReservedCodes(accountId), BigDecimal.ZERO));
    }
}
