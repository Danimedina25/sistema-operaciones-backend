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
import com.sistemadeoperaciones.envioscaja.CashBankDepositRepository;
import com.sistemadeoperaciones.envioscaja.CashBankDepositStatus;
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
    private final CashBankDepositRepository deposits;
    private final ReturnCustodyQuery returnCustody;

    public GeneralCutReadService(CashGeneralDayRepository days, CashGeneralMovementRepository cashMovements,
                                 BankAccountDailyCutService bankCuts, BankLedgerService bankLedger,
                                 BankCashWithdrawalRepository withdrawals, CashBankDepositRepository deposits, ReturnCustodyQuery returnCustody) {
        this.days = days;
        this.cashMovements = cashMovements;
        this.bankCuts = bankCuts;
        this.bankLedger = bankLedger;
        this.withdrawals = withdrawals;
        this.deposits = deposits;
        this.returnCustody = returnCustody;
    }

    public record Movement(String id, LocalDateTime date, String concept, String source, String direction,
                           String type, BigDecimal amount) {}
    public record Transit(Long id, LocalDateTime date, String bank, String accountLastFour,
                          BigDecimal amount, String origin) {}
    public record BankBalance(String bank, int accounts, BigDecimal amount) {}
    public record GeneralCut(LocalDate startDate, LocalDate endDate,
                             BigDecimal cashInitial, BigDecimal cashIncome, BigDecimal cashOutflow,
                             BigDecimal cashInternal, BigDecimal cashFinal,
                             BigDecimal bankInitial, BigDecimal bankIncome, BigDecimal bankOutflow,
                             BigDecimal bankInternal, BigDecimal bankFinal, BigDecimal totalInternal,
                             BigDecimal totalInitial, BigDecimal totalIncome, BigDecimal totalOutflow,
                             BigDecimal totalFinal, BigDecimal difference,
                             List<BankBalance> banks, List<Transit> transit,
                             List<Movement> movements, int totalMovements, int totalPages,
                             BigDecimal custodyInitial, BigDecimal custodyPrepared, BigDecimal custodyDelivered, BigDecimal custodyFinal) {}

    public GeneralCut read(LocalDate startDate, LocalDate endDate, int page, int size) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)
                || endDate.isAfter(LocalDate.now()) || endDate.isAfter(startDate.plusYears(1))) {
            throw new BadRequestException("Selecciona un periodo válido de hasta un año, sin fechas futuras");
        }
        if (page < 0 || size < 1 || size > 100) throw new BadRequestException("Página o tamaño inválido");

        var cashDays = days.findByFechaBetweenOrderByFechaAsc(startDate, endDate);
        var before = days.findFirstByFechaLessThanEqualOrderByFechaDesc(startDate.minusDays(1));
        var atEnd = days.findFirstByFechaLessThanEqualOrderByFechaDesc(endDate);
        BigDecimal cashInitial = before.map(GeneralCutReadService::closingBalance)
                .orElseGet(() -> cashDays.isEmpty() ? BigDecimal.ZERO : cashDays.get(0).getSaldoInicial());
        BigDecimal cashFinal = atEnd.map(GeneralCutReadService::closingBalance).orElse(BigDecimal.ZERO);
        BigDecimal bankInitial = sum(bankCuts.calculateBalances(startDate.minusDays(1)).stream()
                .map(BankAccountBalanceResponseDto::getSaldoFinal).toList());
        var bankEnd = bankCuts.calculateBalances(endDate);
        BigDecimal bankFinal = sum(bankEnd.stream().map(BankAccountBalanceResponseDto::getSaldoFinal).toList());

        BigDecimal cashIncome = BigDecimal.ZERO, cashOutflow = BigDecimal.ZERO, cashInternal = BigDecimal.ZERO;
        BigDecimal bankIncome = BigDecimal.ZERO, bankOutflow = BigDecimal.ZERO, bankInternal = BigDecimal.ZERO;
        List<Movement> book = new ArrayList<>();
        var custody = returnCustody.read(startDate, endDate);
        var preparedIds = custody.rows().stream().map(row -> row.getId()).collect(java.util.stream.Collectors.toSet());
        for (var row : custody.rows()) {
            if (!row.getFechaPreparacionBancaria().toLocalDate().isBefore(startDate) && !row.getFechaPreparacionBancaria().toLocalDate().isAfter(endDate))
                book.add(new Movement("custody-in-" + row.getId(), row.getFechaPreparacionBancaria(), "Retiro preparado · operación #" + row.getSolicitud().getOperacion().getId(), "Custodia de retornos", "ENTRADA", "INTERNO", row.getMonto()));
            if (row.getFechaEntrega() != null && !row.getFechaEntrega().toLocalDate().isBefore(startDate) && !row.getFechaEntrega().toLocalDate().isAfter(endDate))
                book.add(new Movement("custody-out-" + row.getId(), row.getFechaEntrega(), "Retorno entregado · operación #" + row.getSolicitud().getOperacion().getId(), "Custodia de retornos", "SALIDA", "SALIDA", row.getMonto()));
        }

        for (CashGeneralMovement row : cashMovements.findByDiaFechaBetweenOrderByIdAsc(startDate, endDate)) {
            BigDecimal amount = money(row.importe());
            boolean internal = row.getTipo() == CashMovementConcept.CHEQUE
                    || row.getTipo() == CashMovementConcept.RETIRO_CON_TARJETA
                    || row.getTipo() == CashMovementConcept.RETIRO_SIN_TARJETA
                    || row.getTipo() == CashMovementConcept.ENVIO_A_CUENTA;
            if (internal) cashInternal = cashInternal.add(row.getDireccion() == CashMovementDirection.ENTRADA ? amount : amount.negate());
            else if (row.getDireccion() == CashMovementDirection.ENTRADA) cashIncome = cashIncome.add(amount);
            else cashOutflow = cashOutflow.add(amount);
            book.add(new Movement("cash-" + row.getId(), row.getCreatedAt(), row.getConcepto(), "Caja General",
                    row.getDireccion().name(), internal ? "INTERNO" : row.getDireccion().name(), amount));
        }

        // El sobrante o faltante del conteo de cierre es dinero real: el día siguiente abre con lo contado.
        for (CashGeneralDay day : cashDays) {
            BigDecimal diff = money(day.getDiferencia());
            if (day.getSaldoContado() == null || diff.signum() == 0) continue;
            String direction = diff.signum() > 0 ? "ENTRADA" : "SALIDA";
            if (diff.signum() > 0) cashIncome = cashIncome.add(diff);
            else cashOutflow = cashOutflow.add(diff.negate());
            book.add(new Movement("cash-count-" + day.getId(), day.getClosedAt(),
                    "Diferencia de arqueo · " + (diff.signum() > 0 ? "sobrante" : "faltante"), "Caja General",
                    direction, direction, diff.abs()));
        }

        BankLedgerFilter filter = new BankLedgerFilter(startDate, endDate, null, null, null, null);
        int bankPage = 0;
        org.springframework.data.domain.Page<BankLedgerRowDto> slice;
        do {
            slice = bankLedger.search(filter, bankPage++, 100);
            for (BankLedgerRowDto row : slice.getContent()) {
                BigDecimal amount = money(row.monto());
                boolean internal = row.origen() == BankLedgerOrigin.CAJA_GENERAL
                        || row.origen() == BankLedgerOrigin.TRANSFERENCIA_INTERNA
                        || row.parcialidadId() != null && preparedIds.contains(row.parcialidadId()) && row.origen() == BankLedgerOrigin.RETORNO;
                if (internal) bankInternal = bankInternal.add(row.direccion() == BankLedgerDirection.ENTRADA ? amount : amount.negate());
                else if (row.direccion() == BankLedgerDirection.ENTRADA) bankIncome = bankIncome.add(amount);
                else bankOutflow = bankOutflow.add(amount);
                book.add(new Movement("bank-" + row.id(), row.fecha(), row.concepto(), row.cuentaBanco(),
                        row.direccion().name(), internal ? "INTERNO" : row.direccion().name(), amount));
            }
        } while (slice.hasNext());

        BigDecimal totalInitial = cashInitial.add(bankInitial).add(custody.initial());
        BigDecimal totalIncome = cashIncome.add(bankIncome);
        BigDecimal totalOutflow = cashOutflow.add(bankOutflow).add(custody.delivered());
        BigDecimal totalFinal = cashFinal.add(bankFinal).add(custody.balance());
        BigDecimal difference = totalInitial.add(totalIncome).subtract(totalOutflow).subtract(totalFinal);
        var balances = bankCuts.calculateBalancesGrouped(endDate).stream()
                .map(group -> new BankBalance(group.getBanco(), group.getTotalCuentas(), money(group.getSaldoTotalBanco())))
                .toList();
        List<Transit> transit = new ArrayList<>(withdrawals.findByEstatusOrderByRegistradoEnAscIdAsc(WithdrawalStatus.PENDIENTE).stream()
                .map(row -> new Transit(row.getId(), row.getRegistradoEn(), row.getBancoSnapshot(),
                        lastFour(row.getCuentaBancaria().getNumeroCuenta()), row.getMonto(), "CUENTAS_A_CAJA")).toList());
        deposits.findByEstatusOrderByRegistradoEnAscIdAsc(CashBankDepositStatus.PENDIENTE).stream()
                .map(row -> new Transit(row.getId(), row.getRegistradoEn(), row.getCuentaBancaria().getBanco(),
                        lastFour(row.getCuentaBancaria().getNumeroCuenta()), row.getMonto(), "CAJA_A_CUENTAS"))
                .forEach(transit::add);
        deposits.findByEstatusOrderByRegistradoEnAscIdAsc(CashBankDepositStatus.RECHAZADO_PENDIENTE_DEVOLUCION).stream()
                .map(row -> new Transit(row.getId(), row.getRegistradoEn(), row.getCuentaBancaria().getBanco(),
                        lastFour(row.getCuentaBancaria().getNumeroCuenta()), row.getMonto(), "DEVOLUCION_PENDIENTE"))
                .forEach(transit::add);

        book.sort(Comparator.comparing(Movement::date, Comparator.nullsLast(Comparator.naturalOrder())).reversed()
                .thenComparing(Movement::id));
        int count = book.size();
        int from = (int) Math.min((long) count, (long) page * size);
        int to = Math.min(count, from + size);
        return new GeneralCut(startDate, endDate, cashInitial, cashIncome, cashOutflow, cashInternal, cashFinal,
                bankInitial, bankIncome, bankOutflow, bankInternal, bankFinal, cashInternal.add(bankInternal).add(custody.prepared()), totalInitial, totalIncome,
                totalOutflow, totalFinal, difference, balances, transit, book.subList(from, to),
                count, (count + size - 1) / size, custody.initial(), custody.prepared(), custody.delivered(), custody.balance());
    }

    private static BigDecimal money(BigDecimal amount) { return amount == null ? BigDecimal.ZERO : amount; }
    /** Un día cerrado termina con lo contado; uno abierto, con el saldo del sistema. */
    private static BigDecimal closingBalance(CashGeneralDay day) {
        return day.getSaldoContado() != null ? day.getSaldoContado() : day.getSaldoActual();
    }
    private static BigDecimal sum(List<BigDecimal> amounts) {
        return amounts.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    private static String lastFour(String account) {
        if (account == null) return "";
        return account.substring(Math.max(0, account.length() - 4));
    }
}
