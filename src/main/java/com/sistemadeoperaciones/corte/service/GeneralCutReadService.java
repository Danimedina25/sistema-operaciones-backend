package com.sistemadeoperaciones.corte.service;

import com.sistemadeoperaciones.cajageneral.enums.CashMovementConcept;
import com.sistemadeoperaciones.cajageneral.enums.CashMovementDirection;
import com.sistemadeoperaciones.cajageneral.model.CashGeneralDay;
import com.sistemadeoperaciones.cajageneral.model.CashGeneralMovement;
import com.sistemadeoperaciones.cajageneral.repository.CashGeneralDayRepository;
import com.sistemadeoperaciones.cajageneral.repository.CashGeneralMovementRepository;
import com.sistemadeoperaciones.corte.dto.BankAccountBalanceResponseDto;
import com.sistemadeoperaciones.corte.dto.BankLedgerFilter;
import com.sistemadeoperaciones.corte.dto.BankLedgerRowDto;
import com.sistemadeoperaciones.corte.enums.BankLedgerDirection;
import com.sistemadeoperaciones.corte.enums.BankLedgerOrigin;
import com.sistemadeoperaciones.retiroscaja.BankCashWithdrawalRepository;
import com.sistemadeoperaciones.retiroscaja.WithdrawalStatus;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** A read-only consolidation of the existing cash and bank books. */
@Service
@Transactional(readOnly = true)
public class GeneralCutReadService {
    private final CashGeneralDayRepository days;
    private final CashGeneralMovementRepository cashMovements;
    private final BankAccountDailyCutService bankCuts;
    private final BankLedgerService bankLedger;
    private final BankCashWithdrawalRepository withdrawals;

    public GeneralCutReadService(CashGeneralDayRepository days, CashGeneralMovementRepository cashMovements,
                                 BankAccountDailyCutService bankCuts, BankLedgerService bankLedger,
                                 BankCashWithdrawalRepository withdrawals) {
        this.days = days;
        this.cashMovements = cashMovements;
        this.bankCuts = bankCuts;
        this.bankLedger = bankLedger;
        this.withdrawals = withdrawals;
    }

    public record Movement(String id, LocalDateTime date, String concept, String source, String direction,
                           String type, BigDecimal amount) {}
    public record Transit(Long id, LocalDateTime date, String bank, String accountLastFour, BigDecimal amount) {}
    public record BankBalance(String bank, int accounts, BigDecimal amount) {}
    public record GeneralCut(LocalDate startDate, LocalDate endDate,
                             BigDecimal cashInitial, BigDecimal cashIncome, BigDecimal cashOutflow,
                             BigDecimal cashInternal, BigDecimal cashFinal,
                             BigDecimal bankInitial, BigDecimal bankIncome, BigDecimal bankOutflow,
                             BigDecimal bankInternal, BigDecimal bankFinal, BigDecimal totalInternal,
                             BigDecimal totalInitial, BigDecimal totalIncome, BigDecimal totalOutflow,
                             BigDecimal totalFinal, BigDecimal difference,
                             List<BankBalance> banks, List<Transit> transit,
                             List<Movement> movements, int totalMovements, int totalPages) {}

    public GeneralCut read(LocalDate startDate, LocalDate endDate, int page, int size) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)
                || endDate.isAfter(LocalDate.now()) || endDate.isAfter(startDate.plusYears(1))) {
            throw new BadRequestException("Selecciona un periodo válido de hasta un año, sin fechas futuras");
        }
        if (page < 0 || size < 1 || size > 100) throw new BadRequestException("Página o tamaño inválido");

        var cashDays = days.findByFechaBetweenOrderByFechaAsc(startDate, endDate);
        var before = days.findFirstByFechaLessThanEqualOrderByFechaDesc(startDate.minusDays(1));
        var atEnd = days.findFirstByFechaLessThanEqualOrderByFechaDesc(endDate);
        BigDecimal cashInitial = before.map(CashGeneralDay::getSaldoActual)
                .orElseGet(() -> cashDays.isEmpty() ? BigDecimal.ZERO : cashDays.get(0).getSaldoInicial());
        BigDecimal cashFinal = atEnd.map(CashGeneralDay::getSaldoActual).orElse(BigDecimal.ZERO);
        BigDecimal bankInitial = sum(bankCuts.calculateBalances(startDate.minusDays(1)).stream()
                .map(BankAccountBalanceResponseDto::getSaldoFinal).toList());
        var bankEnd = bankCuts.calculateBalances(endDate);
        BigDecimal bankFinal = sum(bankEnd.stream().map(BankAccountBalanceResponseDto::getSaldoFinal).toList());

        BigDecimal cashIncome = BigDecimal.ZERO, cashOutflow = BigDecimal.ZERO, cashInternal = BigDecimal.ZERO;
        BigDecimal bankIncome = BigDecimal.ZERO, bankOutflow = BigDecimal.ZERO, bankInternal = BigDecimal.ZERO;
        List<Movement> book = new ArrayList<>();

        for (CashGeneralMovement row : cashMovements.findByDiaFechaBetweenOrderByIdAsc(startDate, endDate)) {
            BigDecimal amount = money(row.importe());
            boolean internal = row.getTipo() == CashMovementConcept.CHEQUE
                    || row.getTipo() == CashMovementConcept.RETIRO_CON_TARJETA
                    || row.getTipo() == CashMovementConcept.RETIRO_SIN_TARJETA;
            if (internal) cashInternal = cashInternal.add(row.getDireccion() == CashMovementDirection.ENTRADA ? amount : amount.negate());
            else if (row.getDireccion() == CashMovementDirection.ENTRADA) cashIncome = cashIncome.add(amount);
            else cashOutflow = cashOutflow.add(amount);
            book.add(new Movement("cash-" + row.getId(), row.getCreatedAt(), row.getConcepto(), "Caja General",
                    row.getDireccion().name(), internal ? "INTERNO" : row.getDireccion().name(), amount));
        }

        BankLedgerFilter filter = new BankLedgerFilter(startDate, endDate, null, null, null, null);
        int bankPage = 0;
        org.springframework.data.domain.Page<BankLedgerRowDto> slice;
        do {
            slice = bankLedger.search(filter, bankPage++, 100);
            for (BankLedgerRowDto row : slice.getContent()) {
                BigDecimal amount = money(row.monto());
                boolean internal = row.origen() == BankLedgerOrigin.CAJA_GENERAL
                        || row.origen() == BankLedgerOrigin.TRANSFERENCIA_INTERNA;
                if (internal) bankInternal = bankInternal.add(row.direccion() == BankLedgerDirection.ENTRADA ? amount : amount.negate());
                else if (row.direccion() == BankLedgerDirection.ENTRADA) bankIncome = bankIncome.add(amount);
                else bankOutflow = bankOutflow.add(amount);
                book.add(new Movement("bank-" + row.id(), row.fecha(), row.concepto(), row.cuentaBanco(),
                        row.direccion().name(), internal ? "INTERNO" : row.direccion().name(), amount));
            }
        } while (slice.hasNext());

        BigDecimal totalInitial = cashInitial.add(bankInitial);
        BigDecimal totalIncome = cashIncome.add(bankIncome);
        BigDecimal totalOutflow = cashOutflow.add(bankOutflow);
        BigDecimal totalFinal = cashFinal.add(bankFinal);
        BigDecimal difference = totalInitial.add(totalIncome).subtract(totalOutflow).subtract(totalFinal);
        var balances = bankCuts.calculateBalancesGrouped(endDate).stream()
                .map(group -> new BankBalance(group.getBanco(), group.getTotalCuentas(), money(group.getSaldoTotalBanco())))
                .toList();
        var transit = withdrawals.findByEstatusOrderByRegistradoEnAscIdAsc(WithdrawalStatus.PENDIENTE).stream()
                .map(row -> new Transit(row.getId(), row.getRegistradoEn(), row.getBancoSnapshot(),
                        lastFour(row.getCuentaBancaria().getNumeroCuenta()), row.getMonto())).toList();

        book.sort(Comparator.comparing(Movement::date, Comparator.nullsLast(Comparator.naturalOrder())).reversed()
                .thenComparing(Movement::id));
        int count = book.size();
        int from = (int) Math.min((long) count, (long) page * size);
        int to = Math.min(count, from + size);
        return new GeneralCut(startDate, endDate, cashInitial, cashIncome, cashOutflow, cashInternal, cashFinal,
                bankInitial, bankIncome, bankOutflow, bankInternal, bankFinal, cashInternal.add(bankInternal), totalInitial, totalIncome,
                totalOutflow, totalFinal, difference, balances, transit, book.subList(from, to),
                count, (count + size - 1) / size);
    }

    private static BigDecimal money(BigDecimal amount) { return amount == null ? BigDecimal.ZERO : amount; }
    private static BigDecimal sum(List<BigDecimal> amounts) {
        return amounts.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    private static String lastFour(String account) {
        if (account == null) return "";
        return account.substring(Math.max(0, account.length() - 4));
    }
}
