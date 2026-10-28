package com.sistemadeoperaciones.envioscaja;

import com.sistemadeoperaciones.cajageneral.dto.CashMovementResponse;
import com.sistemadeoperaciones.cajageneral.model.CashGeneralDay;
import com.sistemadeoperaciones.cajageneral.repository.CashGeneralDayRepository;
import com.sistemadeoperaciones.cajageneral.repository.CashGeneralMovementRepository;
import com.sistemadeoperaciones.cajageneral.repository.CashGeneralRegisterRepository;
import com.sistemadeoperaciones.cajageneral.service.CashGeneralAmounts;
import com.sistemadeoperaciones.cajageneral.service.CashGeneralService;
import com.sistemadeoperaciones.corte.enums.DailyCashCutStatus;
import com.sistemadeoperaciones.corte.repository.BankAccountDailyCutRepository;
import com.sistemadeoperaciones.corte.repository.DailyCashCutRepository;
import com.sistemadeoperaciones.corte.service.BankAccountDailyCutService;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.cuentasbancarias.repository.BankAccountRepository;
import com.sistemadeoperaciones.cheques.ChequeAudit;
import com.sistemadeoperaciones.cheques.ChequeAuditRepository;
import com.sistemadeoperaciones.cheques.ChequeService;
import com.sistemadeoperaciones.pagos.enums.PaymentStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.model.OperationPayment;
import com.sistemadeoperaciones.pagos.repository.OperationPaymentRepository;
import com.sistemadeoperaciones.pagos.repository.PaymentOperationRepository;
import com.sistemadeoperaciones.pagos.service.PaymentOperationServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
import com.sistemadeoperaciones.shared.exception.ConflictException;
import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;
import com.sistemadeoperaciones.usuarios.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Caja → cuenta: ambos saldos cambian juntos al confirmar el depósito bancario. */
@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class CashBankDepositService {
    private final CashBankDepositRepository deposits;
    private final OperationPaymentRepository payments;
    private final PaymentOperationRepository operations;
    private final PaymentOperationServiceImpl paymentService;
    private final ChequeAuditRepository chequeAudits;
    private final EntityManager em;
    private final BankAccountRepository accounts;
    private final CashGeneralDayRepository days;
    private final CashGeneralMovementRepository movements;
    private final CashGeneralRegisterRepository register;
    private final CashGeneralService cash;
    private final BankAccountDailyCutRepository bankCuts;
    private final DailyCashCutRepository dailyCuts;
    private final BankAccountDailyCutService bankBalanceService;
    private final AuthenticatedUserService auth;
    private final BusinessAuditService audit;
    private final ApplicationEventPublisher events;

    public record Preview(BigDecimal saldoCaja, BigDecimal saldoCuenta, BigDecimal reservadoEnTransito,
                          BigDecimal disponible, BigDecimal monto,
                          BigDecimal saldoAlConfirmar, boolean suficiente) {}
    public record PendingSummary(long count, BigDecimal amount, LocalDateTime oldest) {}

    public PendingSummary pendingSummary() {
        var rows = deposits.findByEstatusOrderByRegistradoEnAscIdAsc(CashBankDepositStatus.PENDIENTE);
        return new PendingSummary(rows.size(), rows.stream().map(CashBankDeposit::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add),
                rows.isEmpty() ? null : rows.get(0).getRegistradoEn());
    }

    public Preview preview(Long accountId, BigDecimal requested) {
        BigDecimal amount = requested == null ? BigDecimal.ZERO : CashGeneralAmounts.money(requested, false);
        BigDecimal bankBalance = null;
        if (accountId != null) {
            BankAccount account = accounts.findById(accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cuenta bancaria no encontrada"));
            if (!Boolean.TRUE.equals(account.getActivo())) throw new ConflictException("La cuenta está inactiva");
            bankBalance = bankBalanceService.calculateBalance(accountId, LocalDate.now()).getSaldoFinal();
        }
        BigDecimal balance = days.findFirstByOrderByFechaDesc().map(CashGeneralDay::getSaldoActual)
                .orElse(BigDecimal.ZERO);
        BigDecimal reserved = deposits.sumReservedCash();
        BigDecimal available = balance.subtract(reserved).max(BigDecimal.ZERO);
        return new Preview(balance, bankBalance, reserved, available, amount, balance.subtract(amount),
                available.compareTo(amount) >= 0);
    }

    private void lock() { register.ensureRegister(); register.lockRegister(); }
    private static String proof(String value, String name) {
        if (value == null || value.isBlank() || value.length() > 500 || !value.startsWith("https://"))
            throw new BadRequestException("Adjunta el comprobante " + name + " mediante una URL HTTPS");
        return value.trim();
    }
    private static String optional(String value, int max) {
        if (value == null || value.isBlank()) return null;
        if (value.trim().length() > max) throw new BadRequestException("Texto demasiado largo");
        return value.trim();
    }
    private CashGeneralDay openDay() {
        CashGeneralDay day = days.findTopByOrderByFechaDesc()
                .orElseThrow(() -> new ConflictException("Abre la Caja General antes de registrar el envío"));
        if (day.getClosedAt() != null || !day.getFecha().equals(LocalDate.now()))
            throw new ConflictException("La Caja General debe estar abierta para el día de hoy");
        return day;
    }
    private void openPeriod(Long accountId) {
        LocalDate today = LocalDate.now();
        if (dailyCuts.findFirstByFechaGreaterThanEqualAndEstatusOrderByFechaAsc(today, DailyCashCutStatus.CERRADO).isPresent())
            throw new ConflictException("El periodo está cerrado");
        if (bankCuts.findFirstByBankAccountIdAndFechaGreaterThanEqualOrderByFechaAsc(accountId, today).isPresent())
            throw new ConflictException("La cuenta ya tiene registrado el corte de hoy");
    }
    private CashBankDeposit get(Long id) {
        return deposits.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Envío no encontrado"));
    }
    private Map<String, Object> snapshot(CashBankDeposit d) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("origin", d.getOrigen()); row.put("accountId", d.getCuentaBancaria().getId());
        row.put("chequePaymentId", d.getChequePayment() == null ? null : d.getChequePayment().getId());
        if (d.getChequePayment() != null) {
            row.put("chequeState", d.getChequePayment().getChequeEstado());
            row.put("paymentStatus", d.getChequePayment().getEstatus());
            row.put("chequeVersion", d.getChequePayment().getChequeVersion());
        }
        row.put("amount", d.getMonto()); row.put("method", d.getForma());
        // The audit serializer must receive a plain snapshot, never Hibernate's lazy PersistentMap.
        row.put("reference", d.getReferencia()); row.put("counts", new LinkedHashMap<>(d.getDenominaciones()));
        row.put("status", d.getEstatus());
        row.put("cashMovementId", d.getMovimientoCaja() == null ? null : d.getMovimientoCaja().getId());
        row.put("outgoingProofPresent", d.getComprobanteSalidaUrl() != null);
        row.put("bankProofPresent", d.getComprobanteBancoUrl() != null);
        row.put("reason", d.getMotivo());
        return row;
    }
    private CashBankDepositView view(CashBankDeposit d) {
        BankAccount a = d.getCuentaBancaria();
        String number = a.getNumeroCuenta();
        String masked = a.getBanco() + " ••••" + number.substring(Math.max(0, number.length() - 4));
        return new CashBankDepositView(d.getId(), d.getOrigen(), a.getId(), masked, a.getTitular(),
                d.getMonto(), d.getForma(), d.getReferencia(), d.getComprobanteSalidaUrl(),
                d.getComprobanteBancoUrl(), d.getEstatus(), d.getMotivo(), d.getRegistradoEn(),
                d.getRegistradoPor().getNombre(), d.getResueltoEn(),
                d.getResueltoPor() == null ? null : d.getResueltoPor().getNombre(),
                d.getMovimientoCaja() == null ? null : d.getMovimientoCaja().getId(),
                d.getChequePayment() == null ? null : d.getChequePayment().getId(),
                d.getChequePayment() == null ? null : d.getChequePayment().getOperacion().getId(),
                d.getChequePayment() == null ? null : d.getChequePayment().getNumeroCheque(),
                d.getVersion(), d.getOrigen() == CashBankDepositOrigin.EFECTIVO_CAJA
                        ? Map.copyOf(d.getDenominaciones()) : Map.of());
    }

    @Transactional
    public CashBankDepositView register(CashBankDepositRequests.Register request) {
        if (request == null || request.requestId() == null) throw new BadRequestException("Falta identificador de solicitud");
        lock();
        Optional<CashBankDeposit> existing = deposits.findByRequestId(request.requestId().toString());
        if (existing.isPresent()) {
            CashBankDeposit d = existing.get();
            if (!Objects.equals(d.getCuentaBancaria().getId(), request.bankAccountId())
                    || request.monto() == null || d.getMonto().compareTo(request.monto()) != 0
                    || !Objects.equals(d.getForma(), request.forma())
                    || !Objects.equals(d.getReferencia(), optional(request.referencia(), 300))
                    || !Objects.equals(d.getDenominaciones(), request.denominaciones())
                    || !Objects.equals(d.getComprobanteSalidaUrl(), request.comprobanteSalidaUrl()))
                throw new ConflictException("El identificador de solicitud ya se utilizó con otros datos");
            return view(d);
        }
        CashGeneralDay day = openDay();
        if (request.bankAccountId() == null) throw new BadRequestException("Selecciona la cuenta destino");
        BankAccount account = accounts.findById(request.bankAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta bancaria no encontrada"));
        if (!Boolean.TRUE.equals(account.getActivo())) throw new ConflictException("La cuenta está inactiva");
        openPeriod(account.getId());
        BigDecimal amount = CashGeneralAmounts.money(request.monto(), true);
        CashGeneralAmounts.requireTotal(request.denominaciones(), amount);
        BigDecimal available = day.getSaldoActual().subtract(deposits.sumReservedCash());
        if (available.compareTo(amount) < 0) throw new ConflictException("Saldo disponible de Caja insuficiente, considerando los envíos en tránsito");
        if (request.forma() == null || !Set.of("VENTANILLA", "CAJERO", "OXXO").contains(request.forma()))
            throw new BadRequestException("Selecciona una forma de depósito válida");
        CashBankDeposit d = new CashBankDeposit();
        d.setRequestId(request.requestId().toString()); d.setOrigen(CashBankDepositOrigin.EFECTIVO_CAJA);
        d.setCuentaBancaria(account); d.setMonto(amount); d.setForma(request.forma());
        d.setReferencia(optional(request.referencia(), 300));
        d.setComprobanteSalidaUrl(proof(request.comprobanteSalidaUrl(), "de salida"));
        d.setDenominaciones(new EnumMap<>(request.denominaciones()));
        d.setRegistradoPor(auth.getCurrentUser());
        CashBankDeposit saved = deposits.saveAndFlush(d);
        audit.record(saved.getRegistradoPor().getId(), "CASH_BANK_DEPOSIT_REGISTER", "CASH_BANK_DEPOSIT",
                saved.getId(), null, snapshot(saved));
        events.publishEvent(new CashBankDepositChanged(saved.getId(), saved.getEstatus(),
                saved.getRegistradoPor().getNombre()));
        return view(saved);
    }

    @Transactional
    public CashBankDepositView edit(Long id, CashBankDepositRequests.Edit request) {
        if (request == null || request.version() == null) throw new BadRequestException("Falta la versión del envío");
        lock();
        CashBankDeposit d = get(id);
        if (d.getOrigen() != CashBankDepositOrigin.EFECTIVO_CAJA)
            throw new ConflictException("Los cheques se corrigen desde su operación");
        if (d.getEstatus() != CashBankDepositStatus.PENDIENTE)
            throw new ConflictException("Solo se puede editar un envío pendiente");
        if (!request.version().equals(d.getVersion()))
            throw new ConflictException("El envío cambió; actualiza la consulta antes de editar");
        CashGeneralDay day = openDay();
        if (request.bankAccountId() == null) throw new BadRequestException("Selecciona la cuenta destino");
        BankAccount account = accounts.findById(request.bankAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta bancaria no encontrada"));
        if (!Boolean.TRUE.equals(account.getActivo())) throw new ConflictException("La cuenta está inactiva");
        openPeriod(account.getId());
        BigDecimal amount = CashGeneralAmounts.money(request.monto(), true);
        CashGeneralAmounts.requireTotal(request.denominaciones(), amount);
        BigDecimal available = day.getSaldoActual().subtract(deposits.sumReservedCash()).add(d.getMonto());
        if (available.compareTo(amount) < 0)
            throw new ConflictException("Saldo disponible de Caja insuficiente, considerando los envíos en tránsito");
        if (request.forma() == null || !Set.of("VENTANILLA", "CAJERO", "OXXO").contains(request.forma()))
            throw new BadRequestException("Selecciona una forma de depósito válida");
        String outgoingProof = proof(request.comprobanteSalidaUrl(), "de salida");
        Map<String, Object> before = snapshot(d);
        boolean proofChanged = !Objects.equals(d.getComprobanteSalidaUrl(), outgoingProof);
        d.setCuentaBancaria(account); d.setMonto(amount); d.setForma(request.forma());
        d.setReferencia(optional(request.referencia(), 300));
        d.setComprobanteSalidaUrl(outgoingProof);
        d.setDenominaciones(new EnumMap<>(request.denominaciones()));
        CashBankDeposit saved = deposits.saveAndFlush(d);
        Map<String, Object> after = snapshot(saved);
        after.put("outgoingProofReplaced", proofChanged);
        User user = auth.getCurrentUser();
        audit.record(user.getId(), "CASH_BANK_DEPOSIT_EDIT", "CASH_BANK_DEPOSIT", id, before, after);
        events.publishEvent(new CashBankDepositChanged(id, saved.getEstatus(), user.getNombre()));
        return view(saved);
    }

    /** Called inside the cheque action transaction, after its operation and cash mutex are locked. */
    @Transactional(propagation = Propagation.MANDATORY)
    public CashBankDepositView registerCheque(OperationPayment payment, UUID requestId, User user, String outgoingProof) {
        if (payment.getTipoPago() != PaymentType.CHEQUE || !"DEPOSITADO".equals(payment.getChequeEstado())
                || payment.getCuentaDestino() == null || payment.getEstatus() != PaymentStatus.PENDIENTE_VALIDACION)
            throw new ConflictException("El cheque no está listo para enviarse a la cuenta");
        if (deposits.existsByChequePaymentIdAndEstatus(payment.getId(), CashBankDepositStatus.PENDIENTE))
            throw new ConflictException("El cheque ya tiene un envío pendiente");
        if (deposits.existsByChequePaymentIdAndEstatus(payment.getId(), CashBankDepositStatus.CONFIRMADO))
            throw new ConflictException("El cheque ya fue acreditado");
        openPeriod(payment.getCuentaDestino().getId());
        CashBankDeposit d = new CashBankDeposit();
        d.setRequestId(requestId.toString()); d.setOrigen(CashBankDepositOrigin.CHEQUE);
        d.setChequePayment(payment); d.setCuentaBancaria(payment.getCuentaDestino());
        d.setMonto(payment.getMonto()); d.setForma("CHEQUE");
        d.setReferencia(optional(payment.getNumeroCheque(), 300));
        d.setComprobanteSalidaUrl(proof(outgoingProof, "de envío del cheque"));
        d.setDenominaciones(Collections.emptyMap()); d.setRegistradoPor(user);
        CashBankDeposit saved = deposits.saveAndFlush(d);
        audit.record(user.getId(), "CASH_BANK_DEPOSIT_REGISTER_CHEQUE", "CASH_BANK_DEPOSIT",
                saved.getId(), null, snapshot(saved));
        events.publishEvent(new CashBankDepositChanged(saved.getId(), saved.getEstatus(), user.getNombre()));
        return view(saved);
    }

    private CashBankDeposit lockedDeposit(Long id) {
        CashBankDepositOrigin origin = deposits.findOriginById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Envío no encontrado"));
        if (origin == CashBankDepositOrigin.CHEQUE) {
            Long paymentId = deposits.findChequePaymentIdById(id)
                    .orElseThrow(() -> new ConflictException("El envío no tiene cheque asociado"));
            OperationPayment payment = payments.findById(paymentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cheque no encontrado"));
            operations.findByIdForUpdate(payment.getOperacion().getId()).orElseThrow();
            em.refresh(payment, LockModeType.PESSIMISTIC_WRITE);
        }
        lock();
        return get(id);
    }

    private void requirePendingCheque(CashBankDeposit d) {
        OperationPayment p = d.getChequePayment();
        if (p == null || p.getTipoPago() != PaymentType.CHEQUE || !"DEPOSITADO".equals(p.getChequeEstado())
                || p.getEstatus() != PaymentStatus.PENDIENTE_VALIDACION
                || p.getCuentaDestino() == null || !p.getCuentaDestino().getId().equals(d.getCuentaBancaria().getId())
                || p.getMonto().compareTo(d.getMonto()) != 0)
            throw new ConflictException("El cheque ya no coincide con el envío pendiente");
    }

    private void chequeHistory(OperationPayment p, User actor, String action, String proofUrl, String reason) {
        ChequeAudit history = new ChequeAudit();
        history.setPagoId(p.getId()); history.setRequestId(UUID.randomUUID().toString());
        history.setAccion(action); history.setFecha(LocalDate.now().atStartOfDay());
        history.setRegistradoEn(LocalDateTime.now()); history.setUsuarioId(actor.getId());
        history.setUsuarioNombre(actor.getNombre()); history.setComprobanteUrl(proofUrl);
        history.setMotivo(reason); history.setDetalle(ChequeService.snapshot(p));
        chequeAudits.saveAndFlush(history);
        events.publishEvent(new ChequeService.Changed(p.getId(), action, actor.getNombre()));
    }

    @Transactional
    public CashBankDepositView confirm(Long id, CashBankDepositRequests.Confirm request) {
        CashBankDeposit d = lockedDeposit(id);
        if (d.getEstatus() == CashBankDepositStatus.CONFIRMADO) return view(d);
        if (d.getEstatus() != CashBankDepositStatus.PENDIENTE)
            throw new ConflictException("Solo se puede confirmar un envío pendiente");
        String bankProof = proof(request == null ? null : request.comprobanteBancoUrl(), "bancario");
        if (!Boolean.TRUE.equals(d.getCuentaBancaria().getActivo())) throw new ConflictException("La cuenta está inactiva");
        openPeriod(d.getCuentaBancaria().getId());
        Map<String, Object> before = snapshot(d);
        User user = auth.getCurrentUser();
        if (d.getOrigen() == CashBankDepositOrigin.CHEQUE) {
            requirePendingCheque(d);
            OperationPayment p = d.getChequePayment();
            ChequeService.validateProof(bankProof, p.getOperacion().getId());
            p.setChequeEstado("COBRADO"); p.setChequeDestinoCobro("CUENTA_BANCARIA");
            p.setChequeFechaCobro(LocalDate.now().atStartOfDay()); p.setChequeVersion(p.getChequeVersion() + 1);
            p.setEstatus(PaymentStatus.VALIDADA); p.setFechaValidacion(LocalDateTime.now());
            p.setValidadoPor(user); p.setComprobanteValidacionUrl(bankProof);
            p.setEnProcesoPor(null); p.setFechaEnProceso(null);
            payments.saveAndFlush(p);
            paymentService.recalculateAfterCheque(p);
            chequeHistory(p, user, "COBRAR_BANCO", bankProof, null);
        } else {
            openDay();
            CashMovementResponse movement = cash.recordBankDeposit(d.getId(), d.getCuentaBancaria(),
                    d.getMonto(), d.getDenominaciones(), d.getComprobanteSalidaUrl());
            d.setMovimientoCaja(movements.getReferenceById(movement.id()));
        }
        d.setComprobanteBancoUrl(bankProof); d.setEstatus(CashBankDepositStatus.CONFIRMADO);
        d.setResueltoPor(user); d.setResueltoEn(LocalDateTime.now());
        CashBankDeposit saved = deposits.saveAndFlush(d);
        audit.record(user.getId(), "CASH_BANK_DEPOSIT_CONFIRM", "CASH_BANK_DEPOSIT",
                id, before, snapshot(saved));
        events.publishEvent(new CashBankDepositChanged(id, saved.getEstatus(), user.getNombre()));
        return view(saved);
    }

    @Transactional
    public CashBankDepositView reject(Long id, CashBankDepositRequests.Reject request) {
        CashBankDeposit d = lockedDeposit(id);
        if (d.getEstatus() == CashBankDepositStatus.RECHAZADO_PENDIENTE_DEVOLUCION
                || (d.getOrigen() == CashBankDepositOrigin.CHEQUE && d.getEstatus() == CashBankDepositStatus.CANCELADO)) return view(d);
        if (d.getEstatus() != CashBankDepositStatus.PENDIENTE)
            throw new ConflictException("Solo se puede rechazar un envío pendiente");
        String reason = optional(request == null ? null : request.motivo(), 500);
        if (reason == null) throw new BadRequestException("Indica el motivo del rechazo");
        Map<String, Object> before = snapshot(d);
        User user = auth.getCurrentUser();
        if (d.getOrigen() == CashBankDepositOrigin.CHEQUE) {
            requirePendingCheque(d);
            OperationPayment p = d.getChequePayment();
            p.setChequeEstado("POR_COBRAR"); p.setChequeFechaDeposito(null);
            p.setCuentaDestino(null); p.setObservaciones(reason);
            p.setChequeVersion(p.getChequeVersion() + 1);
            payments.saveAndFlush(p);
            chequeHistory(p, user, "DEPOSITO_RECHAZADO", null, reason);
            d.setEstatus(CashBankDepositStatus.CANCELADO);
        } else d.setEstatus(CashBankDepositStatus.RECHAZADO_PENDIENTE_DEVOLUCION);
        d.setMotivo(reason); d.setResueltoPor(user); d.setResueltoEn(LocalDateTime.now());
        CashBankDeposit saved = deposits.saveAndFlush(d);
        audit.record(user.getId(), "CASH_BANK_DEPOSIT_REJECT", "CASH_BANK_DEPOSIT", id, before, snapshot(saved));
        events.publishEvent(new CashBankDepositChanged(id, saved.getEstatus(), user.getNombre()));
        return view(saved);
    }

    @Transactional
    public CashBankDepositView confirmReturn(Long id, CashBankDepositRequests.ConfirmReturn request) {
        lock();
        CashBankDeposit d = get(id);
        if (d.getOrigen() != CashBankDepositOrigin.EFECTIVO_CAJA)
            throw new BadRequestException("La devolución con conteo solo aplica a envíos de efectivo");
        if (d.getEstatus() == CashBankDepositStatus.CANCELADO) return view(d);
        if (d.getEstatus() != CashBankDepositStatus.RECHAZADO_PENDIENTE_DEVOLUCION)
            throw new ConflictException("Solo se puede devolver un envío rechazado");
        openDay();
        CashGeneralAmounts.requireTotal(request == null ? null : request.denominaciones(), d.getMonto());
        Map<String, Object> before = snapshot(d);
        User user = auth.getCurrentUser();
        d.setDenominacionesDevolucion(new EnumMap<>(request.denominaciones()));
        d.setEstatus(CashBankDepositStatus.CANCELADO);
        d.setDevueltoPor(user); d.setDevueltoEn(LocalDateTime.now());
        CashBankDeposit saved = deposits.saveAndFlush(d);
        audit.record(user.getId(), "CASH_BANK_DEPOSIT_RETURN", "CASH_BANK_DEPOSIT", id, before, snapshot(saved));
        events.publishEvent(new CashBankDepositChanged(id, saved.getEstatus(), user.getNombre()));
        return view(saved);
    }

    /** Bandejas de trabajo: el envío más reciente primero. El resumen sigue midiendo el más antiguo. */
    public List<CashBankDepositView> pending() {
        return deposits.findByEstatusOrderByRegistradoEnDescIdDesc(CashBankDepositStatus.PENDIENTE)
                .stream().map(this::view).toList();
    }
    public List<CashBankDepositView> rejectedForReturn() {
        return deposits.findByEstatusOrderByRegistradoEnDescIdDesc(CashBankDepositStatus.RECHAZADO_PENDIENTE_DEVOLUCION)
                .stream().map(this::view).toList();
    }
    public List<CashBankDepositView> list(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from) || to.isAfter(LocalDate.now()) || to.isAfter(from.plusYears(1)))
            throw new BadRequestException("Selecciona un rango válido de máximo un año");
        return deposits.findByRegistradoEnGreaterThanEqualAndRegistradoEnLessThanOrderByRegistradoEnDescIdDesc(
                from.atStartOfDay(), to.plusDays(1).atStartOfDay()).stream().map(this::view).toList();
    }
}
