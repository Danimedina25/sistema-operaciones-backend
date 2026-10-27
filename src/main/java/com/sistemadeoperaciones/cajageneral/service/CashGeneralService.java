package com.sistemadeoperaciones.cajageneral.service;

import com.sistemadeoperaciones.cajageneral.dto.*;
import com.sistemadeoperaciones.cajageneral.enums.*;
import com.sistemadeoperaciones.cajageneral.exceptions.InvalidCashGeneralException;
import com.sistemadeoperaciones.cajageneral.model.*;
import com.sistemadeoperaciones.cajageneral.repository.*;
import com.sistemadeoperaciones.corte.service.BankAccountDailyCutService;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.cuentasbancarias.repository.BankAccountRepository;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.enums.ReturnInstallmentStatus;
import com.sistemadeoperaciones.pagos.model.OperationPayment;
import com.sistemadeoperaciones.pagos.model.OperationReturnInstallment;
import com.sistemadeoperaciones.pagos.repository.OperationReturnInstallmentRepository;
import com.sistemadeoperaciones.cuentasbancarias.service.BankNames;
import com.sistemadeoperaciones.retiroscaja.BankCashWithdrawal;
import com.sistemadeoperaciones.retiroscaja.BankCashWithdrawalRepository;
import com.sistemadeoperaciones.retiroscaja.WithdrawalStatus;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
import com.sistemadeoperaciones.envioscaja.CashBankDepositRepository;
import com.sistemadeoperaciones.shared.exception.ConflictException;
import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class CashGeneralService {
    private final CashGeneralRegisterRepository register;
    private final CashGeneralDayRepository days;
    private final CashGeneralMovementRepository movements;
    private final OperationReturnInstallmentRepository installments;
    private final CashGeneralDeletionAuditRepository deletionAudits;
    private final BankAccountRepository bankAccounts;
    private final BankAccountDailyCutService bankCuts;
    private final AuthenticatedUserService auth;
    private final BankCashWithdrawalRepository withdrawals;
    private final BusinessAuditService businessAuditService;
    private final CashBankDepositRepository cashBankDeposits;
    // TODO: confirmar con negocio PDF descargable o impresión del navegador para los tres formatos (Fase 2).
    // TODO: confirmar con negocio historial completo de asignaciones de tarjetas o sólo estado actual (Fase 2).
    /**
     * Lo único que se captura a mano en Caja General es efectivo. El efectivo retirado de una
     * cuenta —cheque, retiro con o sin tarjeta— lo registra Cuentas y entra aquí al confirmarlo
     * ({@link #recordBankWithdrawal}); capturarlo también a mano lo contaría dos veces.
     */
    private static final Set<CashMovementConcept> PHYSICAL_CONCEPTS = EnumSet.of(CashMovementConcept.EFECTIVO);
    /**
     * Conceptos que sacan efectivo de una cuenta bancaria y lo meten a la caja. Todos exigen la
     * cuenta real: esa fila es a la vez la entrada de efectivo y la salida bancaria.
     */
    private static final Set<CashMovementConcept> BANK_WITHDRAWALS = EnumSet.of(
            CashMovementConcept.CHEQUE, CashMovementConcept.RETIRO_SIN_TARJETA, CashMovementConcept.RETIRO_CON_TARJETA);
    public CashGeneralService(CashGeneralRegisterRepository register, CashGeneralDayRepository days,
            CashGeneralMovementRepository movements, OperationReturnInstallmentRepository installments,
            AuthenticatedUserService auth, CashGeneralDeletionAuditRepository deletionAudits,
            BankAccountRepository bankAccounts, BankAccountDailyCutService bankCuts,
            BankCashWithdrawalRepository withdrawals, BusinessAuditService businessAuditService,
            CashBankDepositRepository cashBankDeposits) {
        this.register = register; this.days = days; this.movements = movements;
        this.installments = installments; this.auth = auth;
        this.deletionAudits = deletionAudits;
        this.bankAccounts = bankAccounts; this.bankCuts = bankCuts;
        this.withdrawals = withdrawals;
        this.businessAuditService = businessAuditService;
        this.cashBankDeposits = cashBankDeposits;
    }

    // Orden único para TODAS las escrituras: registro global -> día -> movimiento.
    // El lock también protege la ausencia de un día en la primera apertura y entre fechas distintas.
    private void lock() { register.ensureRegister(); register.lockRegister(); }

    private void requireUnreserved(BigDecimal resultingBalance) {
        if (resultingBalance.compareTo(cashBankDeposits.sumReservedCash()) < 0)
            throw new InvalidCashGeneralException("Saldo disponible insuficiente: hay efectivo reservado en envíos a cuentas");
    }

    private CashMovementResponse saveMovement(CashGeneralMovement movement) {
        CashGeneralMovement saved = movements.saveAndFlush(movement);
        businessAuditService.record(saved.getCreadoPor().getId(), "CASH_MOVEMENT_CREATE", "CASH_MOVEMENT",
                saved.getId(), null, movementAuditSnapshot(saved));
        return movementDto(saved);
    }

    private Map<String, Object> movementAuditSnapshot(CashGeneralMovement movement) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("dayId", movement.getDia().getId());
        snapshot.put("direction", movement.getDireccion());
        snapshot.put("type", movement.getTipo());
        snapshot.put("amount", movement.importe());
        snapshot.put("balanceAfter", movement.getSaldoAcumulado());
        snapshot.put("denominations", new LinkedHashMap<>(movement.getDenominaciones()));
        snapshot.put("bankAccountId", movement.getCuentaBancaria() == null ? null : movement.getCuentaBancaria().getId());
        snapshot.put("paymentId", movement.getPago() == null ? null : movement.getPago().getId());
        snapshot.put("installmentId", movement.getParcialidad() == null ? null : movement.getParcialidad().getId());
        return snapshot;
    }

    private Map<String, Object> dayAuditSnapshot(CashGeneralDay day) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("date", day.getFecha());
        snapshot.put("openingBalance", day.getSaldoInicial());
        snapshot.put("currentBalance", day.getSaldoActual());
        snapshot.put("countedBalance", day.getSaldoContado());
        snapshot.put("difference", day.getDiferencia());
        snapshot.put("openingCounts", new LinkedHashMap<>(day.getApertura()));
        snapshot.put("closingCounts", new LinkedHashMap<>(day.getCierre()));
        snapshot.put("closedAt", day.getClosedAt());
        return snapshot;
    }

    public CashDayResponse latest() { return days.findFirstByOrderByFechaDesc().map(this::dayDto).orElse(null); }

    /**
     * Registra la salida en la misma transacción que la entrega física. El ID
     * determinista vuelve la operación idempotente ante reintentos HTTP.
     */
    @Transactional
    public CashMovementResponse recordCashDelivery(
            OperationReturnInstallment installment,
            Map<CashDenomination, Integer> denominations
    ) {
        lock();
        if (installment.getTipoPago() != PaymentType.EFECTIVO) {
            throw new InvalidCashGeneralException("Solo los retornos en efectivo salen de Caja General");
        }
        if (movements.existsByParcialidadId(installment.getId())) {
            return movements.findByParcialidadId(installment.getId())
                    .map(this::movementDto)
                    .orElseThrow(() -> new ConflictException("La entrega ya está vinculada a Caja General"));
        }
        CashGeneralDay day = days.findFirstByOrderByFechaDesc()
                .orElseThrow(() -> new InvalidCashGeneralException("Abre la Caja General antes de entregar efectivo"));
        if (day.getClosedAt() != null || !day.getFecha().equals(LocalDate.now())) {
            throw new InvalidCashGeneralException("Debe existir una Caja General abierta para el día de hoy");
        }
        BigDecimal amount = CashGeneralAmounts.money(installment.getMonto(), true);
        CashGeneralAmounts.requireTotal(denominations, amount);
        BigDecimal balance = CashGeneralAmounts.balance(day.getSaldoActual(), BigDecimal.ZERO, amount);
        requireUnreserved(balance);

        CashGeneralMovement movement = new CashGeneralMovement();
        movement.setDia(day);
        movement.setRequestId(UUID.nameUUIDFromBytes(
                ("cash-delivery:" + installment.getId()).getBytes(java.nio.charset.StandardCharsets.UTF_8)
        ).toString());
        movement.setDireccion(CashMovementDirection.SALIDA);
        movement.setTipo(CashMovementConcept.EFECTIVO);
        movement.setConcepto("Retorno en efectivo · Operación #" + installment.getSolicitud().getOperacion().getId());
        movement.setMontoManual(null);
        movement.setParcialidad(installment);
        movement.setSaldoAcumulado(balance);
        movement.setDenominaciones(new EnumMap<>(denominations));
        movement.setComprobanteUrl(installment.getComprobanteEntregaUrl());
        movement.setCreadoPor(auth.getCurrentUser());
        day.setSaldoActual(balance);
        days.saveAndFlush(day);
        return saveMovement(movement);
    }

    /** Salida física y entrada bancaria del mismo envío confirmado, en una transacción. */
    @Transactional
    public CashMovementResponse recordBankDeposit(Long depositId, BankAccount account, BigDecimal amount,
            Map<CashDenomination, Integer> denominations, String outgoingProof) {
        lock();
        String requestId = UUID.nameUUIDFromBytes(
                ("cash-bank-deposit:" + depositId).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        Optional<CashGeneralMovement> existing = movements.findByRequestId(requestId);
        if (existing.isPresent()) return movementDto(existing.get());
        CashGeneralDay day = days.findFirstByOrderByFechaDesc()
                .orElseThrow(() -> new InvalidCashGeneralException("Abre la Caja General antes de confirmar el envío"));
        if (day.getClosedAt() != null || !day.getFecha().equals(LocalDate.now()))
            throw new InvalidCashGeneralException("Debe existir una Caja General abierta para el día de hoy");
        BigDecimal value = CashGeneralAmounts.money(amount, true);
        CashGeneralAmounts.requireTotal(denominations, value);
        BigDecimal balance = CashGeneralAmounts.balance(day.getSaldoActual(), BigDecimal.ZERO, value);
        // Este envío aún integra la reserva; se libera al confirmar en la misma transacción.
        BigDecimal otherReservations = cashBankDeposits.sumReservedCash().subtract(value);
        if (balance.compareTo(otherReservations) < 0)
            throw new InvalidCashGeneralException("Saldo insuficiente, considerando otros envíos en tránsito");
        CashGeneralMovement movement = new CashGeneralMovement();
        movement.setDia(day); movement.setRequestId(requestId);
        movement.setDireccion(CashMovementDirection.SALIDA);
        movement.setTipo(CashMovementConcept.ENVIO_A_CUENTA);
        movement.setConcepto("Envío de efectivo a " + account.getBanco() + " ••••"
                + account.getNumeroCuenta().substring(Math.max(0, account.getNumeroCuenta().length() - 4)));
        movement.setBanco(account.getBanco()); movement.setCuentaBancaria(account);
        movement.setMontoManual(value); movement.setSaldoAcumulado(balance);
        movement.setDenominaciones(new EnumMap<>(denominations));
        movement.setComprobanteUrl(outgoingProof);
        movement.setCreadoPor(auth.getCurrentUser());
        day.setSaldoActual(balance);
        days.saveAndFlush(day);
        return saveMovement(movement);
    }

    /**
     * Registra la entrada de efectivo en la misma transacción que la validación del pago.
     * Es el espejo de {@link #recordCashDelivery}: allí el efectivo sale de la caja y aquí
     * entra, y en los dos casos el ID determinista vuelve la operación idempotente ante
     * reintentos HTTP.
     *
     * <p>Exige la caja abierta del día, igual que cualquier otro movimiento: si el dinero
     * entra físicamente al efectivo, tiene que haber una caja donde entre. Si algo falla
     * aquí, la validación del pago se revierte completa.
     */
    @Transactional
    public CashMovementResponse recordCashPayment(
            OperationPayment payment,
            Map<CashDenomination, Integer> denominations
    ) {
        lock();
        if (payment.getTipoPago() != PaymentType.EFECTIVO) {
            throw new InvalidCashGeneralException("Solo los pagos en efectivo entran a Caja General");
        }
        Optional<CashGeneralMovement> existing = movements.findByPagoId(payment.getId());
        if (existing.isPresent()) {
            return movementDto(existing.get());
        }
        CashGeneralDay day = days.findFirstByOrderByFechaDesc()
                .orElseThrow(() -> new InvalidCashGeneralException("Abre la Caja General antes de validar un pago en efectivo"));
        if (day.getClosedAt() != null || !day.getFecha().equals(LocalDate.now())) {
            throw new InvalidCashGeneralException("Debe existir una Caja General abierta para el día de hoy");
        }
        BigDecimal amount = CashGeneralAmounts.money(payment.getMonto(), true);
        CashGeneralAmounts.requireTotal(denominations, amount);
        BigDecimal balance = CashGeneralAmounts.balance(day.getSaldoActual(), amount, BigDecimal.ZERO);

        CashGeneralMovement movement = new CashGeneralMovement();
        movement.setDia(day);
        movement.setRequestId(UUID.nameUUIDFromBytes(
                ("cash-payment:" + payment.getId()).getBytes(java.nio.charset.StandardCharsets.UTF_8)
        ).toString());
        movement.setDireccion(CashMovementDirection.ENTRADA);
        movement.setTipo(CashMovementConcept.EFECTIVO);
        movement.setConcepto("Pago en efectivo · Operación #" + payment.getOperacion().getId());
        movement.setMontoManual(amount);
        movement.setPago(payment);
        movement.setSaldoAcumulado(balance);
        movement.setDenominaciones(new EnumMap<>(denominations));
        movement.setComprobanteUrl(payment.getComprobanteValidacionUrl());
        movement.setCreadoPor(auth.getCurrentUser());
        day.setSaldoActual(balance);
        days.saveAndFlush(day);
        return saveMovement(movement);
    }

    /**
     * Entrada del efectivo retirado de una cuenta, al confirmarlo Cajas. La misma fila es la
     * salida bancaria del libro derivado. El banco agrupado y el total de cuentas de ese banco
     * se guardan como foto para que el "Retiro N de M" de un día pasado se siga leyendo igual.
     *
     * <p>El ID determinista por retiro vuelve la entrada idempotente ante reintentos.
     */
    @Transactional
    public CashMovementResponse recordBankWithdrawal(
            Long withdrawalId, BankAccount account, CashMovementConcept tipo, BigDecimal monto, String concepto,
            String comprobanteUrl, Map<CashDenomination, Integer> denominations, Long expectedDayId,
            String grupoBanco, int totalCuentasBanco
    ) {
        lock();
        if (!BANK_WITHDRAWALS.contains(tipo))
            throw new InvalidCashGeneralException("Concepto de retiro bancario no válido");
        if (account == null || !Boolean.TRUE.equals(account.getActivo()))
            throw new InvalidCashGeneralException("La cuenta bancaria está inactiva");
        String requestId = UUID.nameUUIDFromBytes(
                ("bank-withdrawal:" + withdrawalId).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        Optional<CashGeneralMovement> existing = movements.findByRequestId(requestId);
        if (existing.isPresent()) return movementDto(existing.get());

        CashGeneralDay day = days.findFirstByOrderByFechaDesc()
                .orElseThrow(() -> new InvalidCashGeneralException("Abre la Caja General antes de confirmar un retiro"));
        if (day.getClosedAt() != null || !day.getFecha().equals(LocalDate.now()))
            throw new InvalidCashGeneralException("Debe existir una Caja General abierta para el día de hoy");
        if (!day.getId().equals(expectedDayId))
            throw new InvalidCashGeneralException("La caja cambió. Actualiza la página y vuelve a confirmar");
        BigDecimal amount = CashGeneralAmounts.money(monto, true);
        CashGeneralAmounts.requireTotal(denominations, amount);
        BigDecimal balance = CashGeneralAmounts.balance(day.getSaldoActual(), amount, BigDecimal.ZERO);

        CashGeneralMovement movement = new CashGeneralMovement();
        movement.setDia(day);
        movement.setRequestId(requestId);
        movement.setDireccion(CashMovementDirection.ENTRADA);
        movement.setTipo(tipo);
        movement.setConcepto(requiredText(concepto, 300, "concepto"));
        movement.setBanco(optionalText(account.getBanco() == null ? null
                : account.getBanco().substring(0, Math.min(50, account.getBanco().length())), 50));
        movement.setCuentaBancaria(account);
        movement.setGrupoBanco(optionalText(grupoBanco, 60));
        movement.setTotalCuentasBanco(totalCuentasBanco);
        movement.setMontoManual(amount);
        movement.setSaldoAcumulado(balance);
        movement.setDenominaciones(new EnumMap<>(denominations));
        movement.setComprobanteUrl(optionalText(comprobanteUrl, 500));
        movement.setCreadoPor(auth.getCurrentUser());
        day.setSaldoActual(balance);
        days.saveAndFlush(day);
        return saveMovement(movement);
    }

    @Transactional
    public CashMovementResponse recordChequePayment(
            OperationPayment payment,
            Map<CashDenomination, Integer> denominations, Long expectedDayId, LocalDate effectiveDate
    ) {
        lock();
        if (payment.getTipoPago() != PaymentType.CHEQUE || !"COBRADO".equals(payment.getChequeEstado()) || !"EFECTIVO".equals(payment.getChequeDestinoCobro())) {
            throw new InvalidCashGeneralException("El cheque debe estar cobrado en efectivo");
        }
        Optional<CashGeneralMovement> existing = movements.findByPagoId(payment.getId());
        if (existing.isPresent()) {
            return movementDto(existing.get());
        }
        CashGeneralDay day = days.findTopByOrderByFechaDesc()
                .orElseThrow(() -> new InvalidCashGeneralException("Abre la Caja General antes de validar un pago en efectivo"));
        if (day.getClosedAt() != null || !day.getFecha().equals(LocalDate.now())) {
            throw new InvalidCashGeneralException("Debe existir una Caja General abierta para el día de hoy");
        }
        if (!day.getId().equals(expectedDayId) || !day.getFecha().equals(effectiveDate))
            throw new InvalidCashGeneralException("La fecha y caja deben corresponder al día abierto");
        BigDecimal amount = CashGeneralAmounts.money(payment.getMonto(), true);
        CashGeneralAmounts.requireTotal(denominations, amount);
        BigDecimal balance = CashGeneralAmounts.balance(day.getSaldoActual(), amount, BigDecimal.ZERO);

        CashGeneralMovement movement = new CashGeneralMovement();
        movement.setDia(day);
        movement.setRequestId(UUID.nameUUIDFromBytes(
                ("cheque-payment:" + payment.getId()).getBytes(java.nio.charset.StandardCharsets.UTF_8)
        ).toString());
        movement.setDireccion(CashMovementDirection.ENTRADA);
        movement.setTipo(CashMovementConcept.COBRO_CHEQUE_CLIENTE);
        movement.setConcepto("Cobro de cheque de cliente · Operación #" + payment.getOperacion().getId());
        movement.setMontoManual(amount);
        movement.setPago(payment);
        movement.setSaldoAcumulado(balance);
        movement.setDenominaciones(new EnumMap<>(denominations));
        movement.setComprobanteUrl(payment.getComprobanteValidacionUrl());
        movement.setCreadoPor(auth.getCurrentUser());
        day.setSaldoActual(balance);
        days.saveAndFlush(day);
        return saveMovement(movement);
    }

    @Transactional
    public CashDayResponse open(OpenCashDayRequest request) {
        lock();
        if (request.fecha() == null || !request.fecha().equals(LocalDate.now()))
            throw new InvalidCashGeneralException("La caja solo puede abrirse en la fecha actual");
        BigDecimal amount = CashGeneralAmounts.money(request.saldoInicial(), false);
        CashGeneralAmounts.requireTotal(request.denominaciones(), amount);
        days.findFirstByOrderByFechaDesc().ifPresent(previous -> {
            if (previous.getClosedAt() == null) throw new ConflictException("Cierra la caja abierta antes de abrir otra");
            if (!request.fecha().isAfter(previous.getFecha()))
                throw new ConflictException("La apertura debe ser posterior al último corte");
            if (amount.compareTo(previous.getSaldoContado()) != 0)
                throw new InvalidCashGeneralException("El saldo inicial debe coincidir con el contado del último cierre");
        });
        // TODO: confirmar con negocio la migración histórica. No se importan los Excel automáticamente.
        CashGeneralDay day = new CashGeneralDay();
        day.setFecha(request.fecha()); day.setSaldoInicial(amount); day.setSaldoActual(amount);
        day.setApertura(new EnumMap<>(request.denominaciones())); day.setCierre(new EnumMap<>(CashDenomination.class));
        day.setAbiertoPor(auth.getCurrentUser());
        CashGeneralDay saved = days.saveAndFlush(day);
        businessAuditService.record(saved.getAbiertoPor().getId(), "CASH_DAY_OPEN", "CASH_DAY",
                saved.getId(), null, dayAuditSnapshot(saved));
        return dayDto(saved);
    }

    @Transactional
    public CashMovementResponse createMovement(Long dayId, CreateCashMovementRequest request) {
        lock();
        if (request.requestId() == null) throw new InvalidCashGeneralException("Falta identificador de solicitud");
        Optional<CashGeneralMovement> existing = movements.findByRequestId(request.requestId().toString());
        if (existing.isPresent()) {
            CashGeneralMovement movement = existing.get();
            if (!Objects.equals(movement.getDia().getId(), dayId) || !sameRequest(movement, request))
                throw new ConflictException("El identificador de solicitud ya se utilizó con otros datos");
            return movementDto(movement);
        }
        CashGeneralDay day = openDay(dayId);
        if (request.direccion() == null || request.tipo() == null)
            throw new InvalidCashGeneralException("Indica dirección y tipo del movimiento");
        if (!PHYSICAL_CONCEPTS.contains(request.tipo()))
            throw new InvalidCashGeneralException("Caja General sólo admite efectivo. Los cheques y retiros de una cuenta los registra Cuentas y se confirman en Retiros por confirmar");
        String concept = requiredText(request.concepto(), 300, "concepto");
        String bank = optionalText(request.banco(), 50);
        BankAccount account = resolveBankAccount(request, bank);
        if (account != null) bank = optionalText(account.getBanco(), 50);
        String proof = optionalText(request.comprobanteUrl(), 500);
        if (proof != null && !proof.startsWith("https://"))
            throw new InvalidCashGeneralException("El comprobante debe ser una URL HTTPS");
        OperationReturnInstallment installment = null;
        BigDecimal amount;
        if (request.parcialidadId() != null) {
            if (request.direccion() != CashMovementDirection.SALIDA || request.tipo() != CashMovementConcept.EFECTIVO || request.monto() != null)
                throw new InvalidCashGeneralException("La entrega vinculada exige salida en efectivo sin recapturar monto");
            installment = installments.findById(request.parcialidadId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parcialidad no encontrada"));
            // Las COMPLETADA son terminales: cancelInstallment sólo admite PROGRAMADA.
            if (installment.getTipoPago() != PaymentType.EFECTIVO || installment.getEstatus() != ReturnInstallmentStatus.COMPLETADA)
                throw new InvalidCashGeneralException("Solo se vinculan entregas EFECTIVO completadas");
            if (installment.getFechaRealizacion() == null || installment.getFechaRealizacion().toLocalDate().isAfter(day.getFecha()))
                throw new InvalidCashGeneralException("La entrega no puede ser posterior al día de caja");
            if (movements.existsByParcialidadId(installment.getId()))
                throw new ConflictException("La entrega ya está vinculada a Caja General");
            amount = CashGeneralAmounts.money(installment.getMonto(), true);
        } else {
            amount = CashGeneralAmounts.money(request.monto(), true);
        }
        // Cada movimiento representa efectivo físico y exige su desglose completo.
        CashGeneralAmounts.requireTotal(request.denominaciones(), amount);
        boolean incoming = request.direccion() == CashMovementDirection.ENTRADA;
        BigDecimal balance = CashGeneralAmounts.balance(day.getSaldoActual(), incoming ? amount : BigDecimal.ZERO,
                incoming ? BigDecimal.ZERO : amount);
        if (!incoming) requireUnreserved(balance);
        CashGeneralMovement movement = new CashGeneralMovement();
        movement.setDia(day); movement.setRequestId(request.requestId().toString());
        movement.setDireccion(request.direccion()); movement.setTipo(request.tipo()); movement.setConcepto(concept);
        movement.setBanco(bank); movement.setCuentaBancaria(account);
        movement.setMontoManual(installment == null ? amount : null);
        movement.setParcialidad(installment); movement.setSaldoAcumulado(balance);
        movement.setDenominaciones(new EnumMap<>(request.denominaciones())); movement.setComprobanteUrl(proof);
        movement.setCreadoPor(auth.getCurrentUser()); day.setSaldoActual(balance);
        days.saveAndFlush(day);
        return saveMovement(movement);
    }

    @Transactional
    public CashDayResponse close(Long dayId, CloseCashDayRequest request) {
        lock();
        CashGeneralDay day = openDayForClose(dayId);
        Map<String, Object> beforeAudit = dayAuditSnapshot(day);
        if (!Objects.equals(day.getVersion(), request.version()))
            throw new ConflictException("La caja cambió durante el conteo. Actualiza y revisa el cierre");
        BigDecimal counted = CashGeneralAmounts.money(request.saldoContado(), false);
        CashGeneralAmounts.requireTotal(request.denominaciones(), counted);
        BigDecimal difference = counted.subtract(day.getSaldoActual());
        String note = optionalText(request.observaciones(), 500);
        if (difference.signum() != 0 && note == null)
            throw new InvalidCashGeneralException("Explica la diferencia antes de cerrar la caja");
        day.setSaldoContado(counted); day.setDiferencia(difference);
        day.setCierre(new EnumMap<>(request.denominaciones())); day.setObservacionesCierre(note);
        day.setCerradoPor(auth.getCurrentUser()); day.setClosedAt(LocalDateTime.now());
        CashGeneralDay saved = days.saveAndFlush(day);
        businessAuditService.record(saved.getCerradoPor().getId(), "CASH_DAY_CLOSE", "CASH_DAY",
                saved.getId(), beforeAudit, dayAuditSnapshot(saved));
        return dayDto(saved);
    }

    public CashLedgerResponse ledger(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start) || end.isAfter(start.plusYears(1))
                || end.isAfter(LocalDate.now()))
            throw new InvalidCashGeneralException("Selecciona un rango ordenado, no futuro y de máximo un año");
        List<CashGeneralMovement> rangeMovements = movements.findByDiaFechaBetweenOrderByIdAsc(start, end);
        return new CashLedgerResponse(days.findByFechaBetweenOrderByFechaAsc(start, end).stream().map(this::dayDto).toList(),
                rangeMovements.stream().map(this::movementDto).toList(), withdrawalGroups(rangeMovements));
    }

    /**
     * Agrupa las entradas de efectivo retirado de un banco como las lleva negocio: por día, forma
     * y banco. Los cheques de clientes cobrados en efectivo se agrupan con los cheques de las
     * cuentas propias, por el banco emisor. Los movimientos sin cuenta ni cheque de cliente —el
     * histórico que sólo guardó texto— no se agrupan.
     */
    private List<CashWithdrawalGroup> withdrawalGroups(List<CashGeneralMovement> rangeMovements) {
        List<BankAccount> allAccounts = bankAccounts.findAll();
        BankNames catalog = BankNames.of(allAccounts.stream().map(BankAccount::getBanco).toList());
        Map<String, Long> activeByBank = allAccounts.stream().filter(a -> Boolean.TRUE.equals(a.getActivo()))
                .collect(java.util.stream.Collectors.groupingBy(a -> catalog.display(a.getBanco()), java.util.stream.Collectors.counting()));

        Map<List<Object>, List<CashGeneralMovement>> grouped = new LinkedHashMap<>();
        Map<List<Object>, String> bankOf = new HashMap<>();
        for (CashGeneralMovement m : rangeMovements) {
            if (m.getDireccion() != CashMovementDirection.ENTRADA) continue;
            String forma = switch (m.getTipo()) {
                case RETIRO_CON_TARJETA -> m.getCuentaBancaria() == null ? null : "TD";
                case RETIRO_SIN_TARJETA -> m.getCuentaBancaria() == null ? null : "RST";
                case CHEQUE -> m.getCuentaBancaria() == null ? null : "CHEQUE";
                case COBRO_CHEQUE_CLIENTE -> "CHEQUE";
                default -> null;
            };
            if (forma == null) continue;
            String banco = m.getGrupoBanco() != null ? m.getGrupoBanco()
                    : m.getCuentaBancaria() != null ? catalog.display(m.getCuentaBancaria().getBanco())
                    : catalog.display(m.getPago() == null ? null : m.getPago().getBancoEmisor());
            if (banco.isEmpty()) banco = "SIN BANCO";
            List<Object> key = List.of(m.getDia().getId(), forma, banco);
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(m);
            bankOf.put(key, banco);
        }

        List<CashWithdrawalGroup> groups = new ArrayList<>();
        grouped.forEach((key, members) -> {
            String forma = (String) key.get(1);
            String banco = bankOf.get(key);
            CashGeneralMovement last = members.get(members.size() - 1);
            BigDecimal total = members.stream().map(CashGeneralMovement::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
            boolean cheque = forma.equals("CHEQUE");
            Integer cuentas = cheque ? null : (int) members.stream().map(m -> m.getCuentaBancaria().getId()).distinct().count();
            Integer totalCuentas = cheque ? null : last.getTotalCuentasBanco() != null ? last.getTotalCuentasBanco()
                    : activeByBank.getOrDefault(banco, 0L).intValue();
            String etiqueta = cheque ? "Cheques " + banco
                    : "Retiro " + cuentas + " de " + Math.max(totalCuentas, cuentas) + " " + forma + " " + banco;
            groups.add(new CashWithdrawalGroup(last.getDia().getId(), last.getDia().getFecha(), forma, banco, cuentas,
                    totalCuentas, members.size(), total, members.stream().map(CashGeneralMovement::getId).toList(),
                    last.getId(), etiqueta));
        });
        return groups;
    }

    public List<CashDeliveryResponse> deliveries(int page) {
        if (page < 0) throw new InvalidCashGeneralException("Página inválida");
        return movements.findUnlinkedDeliveries(PageRequest.of(page, 50)).stream()
                .map(i -> new CashDeliveryResponse(i.getId(), i.getSolicitud().getOperacion().getId(), i.getMonto(),
                        i.getFechaRealizacion(), i.getPersonaQueRecibioEfectivo())).toList();
    }

    @Transactional
    public void deleteDay(Long dayId, DeleteCashDayRequest request) {
        lock();
        if (request == null || !"ELIMINAR".equals(request.confirmacion())) {
            throw new InvalidCashGeneralException("Escribe ELIMINAR para confirmar");
        }
        String reason = requiredText(request.motivo(), 500, "el motivo de eliminación");
        CashGeneralDay day = days.findById(dayId)
                .orElseThrow(() -> new ResourceNotFoundException("Corte de Caja General no encontrado"));
        if (cashBankDeposits.existsByMovimientoCajaDiaId(dayId))
            throw new ConflictException("El día tiene envíos confirmados a bancos y no puede eliminarse sin reversión auditada");
        Map<String, Object> beforeAudit = dayAuditSnapshot(day);
        if (!Objects.equals(day.getVersion(), request.version())) {
            throw new ConflictException("El corte cambió. Actualiza la página antes de eliminarlo");
        }
        List<CashGeneralMovement> dayMovements = movements.findByDiaIdOrderByIdAsc(dayId);
        if (dayMovements.stream().anyMatch(m -> m.getTipo() == CashMovementConcept.COBRO_CHEQUE_CLIENTE))
            throw new ConflictException("El día contiene cheques de clientes cobrados; requiere reversión auditada");
        // Los cheques cobrados del día son salidas bancarias. Al borrarlos hay que rehacer los
        // cortes bancarios ya persistidos de esas cuentas o quedarían con una salida sin origen.
        Set<Long> affectedAccounts = dayMovements.stream()
                .map(CashGeneralMovement::getCuentaBancaria)
                .filter(Objects::nonNull)
                .map(BankAccount::getId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        CashGeneralDeletionAudit audit = new CashGeneralDeletionAudit();
        audit.setDeletedDayId(day.getId());
        audit.setFecha(day.getFecha());
        audit.setSaldoInicial(day.getSaldoInicial());
        audit.setSaldoEsperado(day.getSaldoActual());
        audit.setSaldoContado(day.getSaldoContado());
        audit.setMovementCount(dayMovements.size());
        audit.setMotivo(reason);
        audit.setDeletedBy(auth.getCurrentUser());
        // Los retiros confirmados ese día regresan a pendientes: el efectivo no desaparece, sólo
        // su registro en esta caja, y Cajas lo vuelve a confirmar en la caja que corresponda.
        List<BankCashWithdrawal> linked = dayMovements.isEmpty() ? List.of()
                : withdrawals.findByMovimientoCajaIdIn(dayMovements.stream().map(CashGeneralMovement::getId).toList());
        for (BankCashWithdrawal withdrawal : linked) {
            withdrawal.setMovimientoCaja(null);
            withdrawal.setEstatus(WithdrawalStatus.PENDIENTE);
            withdrawal.setResueltoPor(null);
            withdrawal.setResueltoEn(null);
        }
        if (!linked.isEmpty()) withdrawals.saveAllAndFlush(linked);
        movements.deleteAll(dayMovements);
        movements.flush();
        days.delete(day);
        days.flush();
        int recalculated = 0;
        for (Long accountId : affectedAccounts) {
            recalculated += bankCuts.recalculateFrom(accountId, day.getFecha());
        }
        audit.setCortesBancariosRecalculados(recalculated);
        deletionAudits.save(audit);
        businessAuditService.record(audit.getDeletedBy().getId(), "CASH_DAY_DELETE", "CASH_DAY",
                day.getId(), beforeAudit, null);
    }

    /** Para capturar movimientos: sólo la caja de hoy, o quedarían fechados en otro día. */
    private CashGeneralDay openDay(Long id) {
        CashGeneralDay day = openDayForClose(id);
        if (!day.getFecha().equals(LocalDate.now()))
            throw new InvalidCashGeneralException("Solo se pueden capturar movimientos en la caja del día actual");
        return day;
    }

    /**
     * Para cerrar: se admite una caja de un día anterior que quedó abierta por olvido.
     *
     * <p>Antes exigía que fuera la de hoy y eso trababa la operación: no se podía cerrar la de
     * ayer por no ser hoy, y no se podía abrir la de hoy porque la anterior seguía abierta.
     * Contar el efectivo hoy es válido porque desde que venció ese día ya no se le pudo
     * capturar ningún movimiento, así que el dinero no se movió; si hay diferencia, el cierre
     * la registra y exige explicarla como siempre.
     */
    private CashGeneralDay openDayForClose(Long id) {
        CashGeneralDay day = days.findById(id).orElseThrow(() -> new ResourceNotFoundException("Caja no encontrada"));
        if (day.getClosedAt() != null) throw new ConflictException("La caja está cerrada");
        if (day.getFecha().isAfter(LocalDate.now()))
            throw new InvalidCashGeneralException("La caja no puede cerrarse antes de su fecha");
        return day;
    }

    /**
     * Desglose que debería haber en la caja: apertura + entradas − salidas, denominación por
     * denominación. Sirve para prellenar el cierre y que sólo se valide contra el conteo real.
     *
     * <p>Alguna denominación puede salir negativa si durante el día se cambió físicamente un
     * billete por otros: el importe total sigue cuadrando con el saldo, pero el detalle no.
     * No se corrige ni se bloquea, porque el conteo real es el que manda.
     */
    private Map<CashDenomination, Integer> expectedCounts(CashGeneralDay day) {
        if (day.getClosedAt() != null) return null;

        Map<CashDenomination, Integer> expected = new EnumMap<>(CashDenomination.class);
        for (CashDenomination denomination : CashDenomination.values()) {
            expected.put(denomination, day.getApertura().getOrDefault(denomination, 0));
        }
        for (CashGeneralMovement movement : movements.findByDiaIdOrderByIdAsc(day.getId())) {
            int sign = movement.getDireccion() == CashMovementDirection.ENTRADA ? 1 : -1;
            movement.getDenominaciones()
                    .forEach((denomination, quantity) -> expected.merge(denomination, sign * quantity, Integer::sum));
        }
        return expected;
    }
    private String requiredText(String value, int max, String label) {
        String text = optionalText(value, max);
        if (text == null) throw new InvalidCashGeneralException("Captura " + label);
        return text;
    }
    private String optionalText(String value, int max) {
        if (value == null || value.isBlank()) return null;
        if (value.length() > max) throw new InvalidCashGeneralException("Texto demasiado largo");
        return value.trim();
    }
    private boolean sameRequest(CashGeneralMovement m, CreateCashMovementRequest r) {
        return m.getDireccion() == r.direccion() && m.getTipo() == r.tipo()
                && Objects.equals(m.getConcepto(), optionalText(r.concepto(), 300))
                && Objects.equals(m.getCuentaBancaria() == null ? null : m.getCuentaBancaria().getId(), r.bankAccountId())
                && (m.getCuentaBancaria() != null || Objects.equals(m.getBanco(), optionalText(r.banco(), 50)))
                && Objects.equals(m.getComprobanteUrl(), optionalText(r.comprobanteUrl(), 500))
                && Objects.equals(m.getDenominaciones(), r.denominaciones())
                && Objects.equals(m.getParcialidad() == null ? null : m.getParcialidad().getId(), r.parcialidadId())
                && (m.getMontoManual() == null ? r.monto() == null : r.monto() != null && m.getMontoManual().compareTo(r.monto()) == 0);
    }
    private CashDayResponse dayDto(CashGeneralDay d) {
        Map<CashDenomination, Integer> expected = expectedCounts(d);
        return new CashDayResponse(d.getId(), d.getFecha(), d.getVersion(), d.getSaldoInicial(), d.getSaldoActual(),
                d.getSaldoContado(), d.getDiferencia(), Map.copyOf(d.getApertura()), Map.copyOf(d.getCierre()),
                expected == null ? null : Map.copyOf(expected),
                d.getObservacionesCierre(), d.getCreatedAt(), d.getClosedAt(), d.getAbiertoPor().getId(),
                d.getAbiertoPor().getNombre(),
                d.getCerradoPor() == null ? null : d.getCerradoPor().getId());
    }
    private CashMovementResponse movementDto(CashGeneralMovement m) {
        OperationReturnInstallment i = m.getParcialidad();
        BankAccount c = m.getCuentaBancaria();
        Long operacionId = i != null ? i.getSolicitud().getOperacion().getId()
                : m.getPago() != null ? m.getPago().getOperacion().getId() : null;
        return new CashMovementResponse(m.getId(), m.getDia().getId(), m.getDia().getFecha(), m.getCreatedAt(),
                m.getDireccion(), m.getTipo(), m.getConcepto(), m.getBanco(),
                c == null ? null : c.getId(), c == null ? null : c.getBanco(), c == null ? null : c.getTitular(),
                c == null ? null : c.getNumeroCuenta(), c == null ? null : c.getActivo(),
                m.importe(), m.getSaldoAcumulado(),
                i == null ? null : i.getId(), operacionId,
                Map.copyOf(m.getDenominaciones()), m.getComprobanteUrl(), m.getCreadoPor().getId());
    }

    /**
     * La captura manual sólo admite efectivo, y el efectivo nunca lleva banco ni cuenta. El
     * efectivo retirado de una cuenta entra por {@link #recordBankWithdrawal}.
     */
    private BankAccount resolveBankAccount(CreateCashMovementRequest request, String bank) {
        if (bank != null || request.bankAccountId() != null)
            throw new InvalidCashGeneralException("Los movimientos en efectivo no admiten banco ni cuenta bancaria");
        return null;
    }
}
