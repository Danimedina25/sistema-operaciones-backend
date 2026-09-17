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
import com.sistemadeoperaciones.pagos.model.OperationReturnInstallment;
import com.sistemadeoperaciones.pagos.repository.OperationReturnInstallmentRepository;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
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
    // TODO: confirmar con negocio PDF descargable o impresión del navegador para los tres formatos (Fase 2).
    // TODO: confirmar con negocio historial completo de asignaciones de tarjetas o sólo estado actual (Fase 2).
    private static final Set<CashMovementConcept> PHYSICAL_CONCEPTS = EnumSet.of(
            CashMovementConcept.EFECTIVO, CashMovementConcept.CHEQUE, CashMovementConcept.RETIRO_SIN_TARJETA);
    /**
     * Conceptos que sacan efectivo de una cuenta bancaria y lo meten a la caja. Los dos
     * exigen la cuenta real: el cheque se cobra contra una cuenta concreta y el retiro sin
     * tarjeta necesita esa cuenta para generar su código.
     */
    private static final Set<CashMovementConcept> BANK_WITHDRAWALS = EnumSet.of(
            CashMovementConcept.CHEQUE, CashMovementConcept.RETIRO_SIN_TARJETA);
    public CashGeneralService(CashGeneralRegisterRepository register, CashGeneralDayRepository days,
            CashGeneralMovementRepository movements, OperationReturnInstallmentRepository installments,
            AuthenticatedUserService auth, CashGeneralDeletionAuditRepository deletionAudits,
            BankAccountRepository bankAccounts, BankAccountDailyCutService bankCuts) {
        this.register = register; this.days = days; this.movements = movements;
        this.installments = installments; this.auth = auth;
        this.deletionAudits = deletionAudits;
        this.bankAccounts = bankAccounts; this.bankCuts = bankCuts;
    }

    // Orden único para TODAS las escrituras: registro global -> día -> movimiento.
    // El lock también protege la ausencia de un día en la primera apertura y entre fechas distintas.
    private void lock() { register.ensureRegister(); register.lockRegister(); }

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
        return movementDto(movements.saveAndFlush(movement));
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
        return dayDto(days.saveAndFlush(day));
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
            throw new InvalidCashGeneralException("Caja General solo admite efectivo, cheque cobrado o retiro sin tarjeta");
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
        CashGeneralMovement movement = new CashGeneralMovement();
        movement.setDia(day); movement.setRequestId(request.requestId().toString());
        movement.setDireccion(request.direccion()); movement.setTipo(request.tipo()); movement.setConcepto(concept);
        movement.setBanco(bank); movement.setCuentaBancaria(account);
        movement.setMontoManual(installment == null ? amount : null);
        movement.setParcialidad(installment); movement.setSaldoAcumulado(balance);
        movement.setDenominaciones(new EnumMap<>(request.denominaciones())); movement.setComprobanteUrl(proof);
        movement.setCreadoPor(auth.getCurrentUser()); day.setSaldoActual(balance);
        days.saveAndFlush(day);
        return movementDto(movements.saveAndFlush(movement));
    }

    @Transactional
    public CashDayResponse close(Long dayId, CloseCashDayRequest request) {
        lock();
        CashGeneralDay day = openDayForClose(dayId);
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
        return dayDto(days.saveAndFlush(day));
    }

    public CashLedgerResponse ledger(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start) || end.isAfter(start.plusYears(1))
                || end.isAfter(LocalDate.now()))
            throw new InvalidCashGeneralException("Selecciona un rango ordenado, no futuro y de máximo un año");
        return new CashLedgerResponse(days.findByFechaBetweenOrderByFechaAsc(start, end).stream().map(this::dayDto).toList(),
                movements.findByDiaFechaBetweenOrderByIdAsc(start, end).stream().map(this::movementDto).toList());
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
        if (!Objects.equals(day.getVersion(), request.version())) {
            throw new ConflictException("El corte cambió. Actualiza la página antes de eliminarlo");
        }
        List<CashGeneralMovement> dayMovements = movements.findByDiaIdOrderByIdAsc(dayId);
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
        return new CashMovementResponse(m.getId(), m.getDia().getId(), m.getDia().getFecha(), m.getCreatedAt(),
                m.getDireccion(), m.getTipo(), m.getConcepto(), m.getBanco(),
                c == null ? null : c.getId(), c == null ? null : c.getBanco(), c == null ? null : c.getTitular(),
                c == null ? null : c.getNumeroCuenta(), c == null ? null : c.getActivo(),
                m.importe(), m.getSaldoAcumulado(),
                i == null ? null : i.getId(), i == null ? null : i.getSolicitud().getOperacion().getId(),
                Map.copyOf(m.getDenominaciones()), m.getComprobanteUrl(), m.getCreadoPor().getId());
    }

    /**
     * El cheque cobrado saca efectivo de una cuenta bancaria real: exige la FK, sólo existe
     * como ENTRADA de efectivo y la cuenta debe estar activa al capturar. El retiro con tarjeta
     * conserva el catálogo fijo de nombres y no afecta todavía ningún saldo bancario.
     */
    private BankAccount resolveBankAccount(CreateCashMovementRequest request, String bank) {
        if (request.tipo() == CashMovementConcept.EFECTIVO) {
            if (bank != null || request.bankAccountId() != null)
                throw new InvalidCashGeneralException("Los movimientos en efectivo no admiten banco ni cuenta bancaria");
            return null;
        }
        if (!BANK_WITHDRAWALS.contains(request.tipo()))
            throw new InvalidCashGeneralException("Concepto no admitido en Caja General");

        String concepto = request.tipo() == CashMovementConcept.CHEQUE
                ? "El cheque cobrado"
                : "El retiro sin tarjeta";
        if (bank != null)
            throw new InvalidCashGeneralException(concepto + " se captura con la cuenta bancaria, no con el nombre del banco");
        // Retirar dinero del banco sólo puede meter efectivo a la caja, nunca sacarlo.
        if (request.direccion() != CashMovementDirection.ENTRADA)
            throw new InvalidCashGeneralException(concepto + " sólo se registra como entrada de efectivo");
        if (request.bankAccountId() == null)
            throw new InvalidCashGeneralException("Selecciona la cuenta bancaria de la que salió el dinero");
        BankAccount account = bankAccounts.findById(request.bankAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta bancaria no encontrada"));
        if (!Boolean.TRUE.equals(account.getActivo()))
            throw new InvalidCashGeneralException("La cuenta bancaria está inactiva");
        return account;
    }
}
