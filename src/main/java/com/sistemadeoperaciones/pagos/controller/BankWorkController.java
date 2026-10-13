package com.sistemadeoperaciones.pagos.controller;

import com.sistemadeoperaciones.pagos.dto.retornos.ReturnPaymentResponseDto;
import com.sistemadeoperaciones.pagos.enums.PaymentStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.enums.ReturnPaymentStatus;
import com.sistemadeoperaciones.pagos.model.OperationPayment;
import com.sistemadeoperaciones.pagos.repository.OperationPaymentRepository;
import com.sistemadeoperaciones.pagos.repository.OperationReturnPaymentRepository;
import com.sistemadeoperaciones.pagos.service.ReturnPaymentDtoMapper;
import com.sistemadeoperaciones.retiroscaja.BankCashWithdrawalRepository;
import com.sistemadeoperaciones.retiroscaja.WithdrawalStatus;
import com.sistemadeoperaciones.shared.dto.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** Read-only work queues for Cuentas. Every row represents one payment or return request. */
@RestController
@RequestMapping("/api/bank-work")
@PreAuthorize("hasAnyRole('ADMIN', 'JEFA_CUENTAS', 'AUXILIAR_CUENTAS', 'GERENTE', 'DIRECCION')")
@Transactional(readOnly = true)
public class BankWorkController {
    private final OperationPaymentRepository payments;
    private final OperationReturnPaymentRepository returns;
    private final ReturnPaymentDtoMapper returnMapper;
    private final BankCashWithdrawalRepository withdrawals;

    public BankWorkController(OperationPaymentRepository payments, OperationReturnPaymentRepository returns,
                              ReturnPaymentDtoMapper returnMapper, BankCashWithdrawalRepository withdrawals) {
        this.payments = payments;
        this.returns = returns;
        this.returnMapper = returnMapper;
        this.withdrawals = withdrawals;
    }

    public record WorkTotal(long count, BigDecimal amount, LocalDateTime oldest) {}
    public record WorkSummary(WorkTotal income, WorkTotal outflow, WorkTotal withdrawalsInTransit) {}
    public record WorkPage<T>(List<T> content, long totalElements, int totalPages, BigDecimal totalAmount) {}
    public record IncomeRow(Long id, Long operationId, String clienteNombre, String socioComercialNombre,
                            BigDecimal monto, PaymentType tipoPago, PaymentStatus estatus, String chequeEstado,
                            String cuentaDestino, LocalDateTime fechaPago) {}

    private List<IncomeRow> incomeRows() {
        return payments.findAll((root, query, cb) -> cb.and(
                        cb.isTrue(root.get("operacion").get("activo")),
                        root.get("estatus").in(PaymentStatus.PENDIENTE_VALIDACION, PaymentStatus.EN_PROCESO),
                        cb.or(root.get("tipoPago").in(PaymentType.TRANSFERENCIA, PaymentType.DEPOSITO),
                                cb.and(cb.equal(root.get("tipoPago"), PaymentType.CHEQUE),
                                        root.get("chequeEstado").in("POR_COBRAR", "DEPOSITADO")))))
                .stream().map(this::incomeRow)
                .sorted(Comparator.comparing(IncomeRow::fechaPago, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(IncomeRow::id).reversed()).toList();
    }

    private IncomeRow incomeRow(OperationPayment payment) {
        var operation = payment.getOperacion();
        var account = payment.getCuentaDestino();
        return new IncomeRow(payment.getId(), operation.getId(), operation.getCliente().getNombre(),
                operation.getSocioComercial().getNombre(), payment.getMonto(), payment.getTipoPago(),
                payment.getEstatus(), payment.getChequeEstado(),
                account == null ? null : account.getBanco() + " · " + account.getTitular(), payment.getFechaPago());
    }

    private List<ReturnPaymentResponseDto> outflowRows() {
        return returns.findAll((root, query, cb) -> cb.and(
                        cb.isTrue(root.get("operacion").get("activo")),
                        cb.equal(root.get("tipoPago"), PaymentType.TRANSFERENCIA),
                        cb.notEqual(root.get("estatus"), ReturnPaymentStatus.RETORNADO)))
                .stream().map(returnMapper::toDto)
                .filter(item -> amount(item.getMontoDisponible()).signum() > 0)
                .sorted(Comparator.comparing(ReturnPaymentResponseDto::getFechaSolicitud,
                        Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(ReturnPaymentResponseDto::getId).reversed())
                .toList();
    }

    @GetMapping("/summary")
    public ApiResponse<WorkSummary> summary() {
        var income = incomeRows();
        var outflow = outflowRows();
        var pendingWithdrawals = withdrawals.findByEstatusOrderByRegistradoEnAscIdAsc(WithdrawalStatus.PENDIENTE);
        return ok(new WorkSummary(total(income, IncomeRow::monto, IncomeRow::fechaPago),
                total(outflow, ReturnPaymentResponseDto::getMontoDisponible, ReturnPaymentResponseDto::getFechaSolicitud),
                new WorkTotal(pendingWithdrawals.size(), pendingWithdrawals.stream()
                        .map(item -> amount(item.getMonto())).reduce(BigDecimal.ZERO, BigDecimal::add),
                        pendingWithdrawals.isEmpty() ? null : pendingWithdrawals.get(0).getRegistradoEn())));
    }

    @GetMapping("/income")
    public ApiResponse<WorkPage<IncomeRow>> income(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size,
                                                   @RequestParam(defaultValue = "") String search,
                                                   @RequestParam(defaultValue = "") String method) {
        var rows = incomeRows().stream().filter(row -> method.isBlank() || row.tipoPago().name().equals(method))
                .filter(row -> matches(search, row.operationId(), row.clienteNombre(), row.socioComercialNombre(), row.cuentaDestino()))
                .toList();
        return ok(paginate(rows, page, size, IncomeRow::monto));
    }

    @GetMapping("/outflow")
    public ApiResponse<WorkPage<ReturnPaymentResponseDto>> outflow(@RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size,
                                                                    @RequestParam(defaultValue = "") String search) {
        var rows = outflowRows().stream()
                .filter(row -> matches(search, row.getOperationId(), row.getClienteNombre(), row.getSocioComercialNombre(), row.getCuentaDestinoTitular()))
                .toList();
        return ok(paginate(rows, page, size, ReturnPaymentResponseDto::getMontoDisponible));
    }

    private static <T> WorkTotal total(List<T> rows, Function<T, BigDecimal> money, Function<T, LocalDateTime> date) {
        return new WorkTotal(rows.size(), rows.stream().map(row -> amount(money.apply(row))).reduce(BigDecimal.ZERO, BigDecimal::add),
                rows.stream().map(date).filter(java.util.Objects::nonNull).min(LocalDateTime::compareTo).orElse(null));
    }

    private static <T> WorkPage<T> paginate(List<T> rows, int page, int size, Function<T, BigDecimal> money) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        long from = (long) safePage * safeSize;
        var content = from >= rows.size() ? List.<T>of() : rows.subList((int) from, Math.min(rows.size(), (int) from + safeSize));
        var sum = rows.stream().map(row -> amount(money.apply(row))).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new WorkPage<>(content, rows.size(), (rows.size() + safeSize - 1) / safeSize, sum);
    }

    private static BigDecimal amount(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }

    private static boolean matches(String search, Long id, String... values) {
        String term = search.trim().toLowerCase(Locale.ROOT);
        if (term.isBlank() || String.valueOf(id).contains(term)) return true;
        for (String value : values) if (value != null && value.toLowerCase(Locale.ROOT).contains(term)) return true;
        return false;
    }

    private <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(true, "Trabajo de cuentas", data, null); }
}
