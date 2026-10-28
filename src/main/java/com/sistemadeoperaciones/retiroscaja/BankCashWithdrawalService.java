package com.sistemadeoperaciones.retiroscaja;

import com.sistemadeoperaciones.cajageneral.dto.CashMovementResponse;
import com.sistemadeoperaciones.cajageneral.repository.CashGeneralMovementRepository;
import com.sistemadeoperaciones.cajageneral.repository.CashGeneralRegisterRepository;
import com.sistemadeoperaciones.cajageneral.service.CashGeneralService;
import com.sistemadeoperaciones.corte.enums.DailyCashCutStatus;
import com.sistemadeoperaciones.corte.repository.BankAccountDailyCutRepository;
import com.sistemadeoperaciones.corte.repository.DailyCashCutRepository;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.cuentasbancarias.repository.BankAccountRepository;
import com.sistemadeoperaciones.cuentasbancarias.service.BankNames;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
import com.sistemadeoperaciones.shared.exception.ConflictException;
import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;
import com.sistemadeoperaciones.usuarios.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Objects;

/**
 * Retiros de efectivo de las cuentas hacia Caja General.
 *
 * <p>Orden de bloqueos, igual que en Caja General y cheques: primero el registro global y
 * después la fila del retiro. Todas las escrituras toman el registro global, así que el saldo
 * disponible que se valida no puede cambiar entre la validación y el guardado.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankCashWithdrawalService {
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999999.99");
    private static final BigDecimal FIFTY_CENTS = new BigDecimal("50");

    private final BankCashWithdrawalRepository withdrawals;
    private final BankAccountRepository accounts;
    private final BankAvailability availability;
    private final CashGeneralService cash;
    private final CashGeneralMovementRepository cashMovements;
    private final CashGeneralRegisterRepository register;
    private final BankAccountDailyCutRepository bankCuts;
    private final DailyCashCutRepository dailyCuts;
    private final AuthenticatedUserService auth;
    private final ApplicationEventPublisher events;
    private final BusinessAuditService businessAuditService;

    private void lock() { register.ensureRegister(); register.lockRegister(); }

    @Transactional
    public BankCashWithdrawalView register(RegisterWithdrawalRequest request) {
        if (request == null || request.requestId() == null)
            throw new BadRequestException("Falta identificador de solicitud");
        lock();

        var existing = withdrawals.findByRequestId(request.requestId().toString());
        if (existing.isPresent()) {
            if (!sameRequest(existing.get(), request))
                throw new ConflictException("El identificador de solicitud ya se utilizó con otros datos");
            return view(existing.get(), catalog());
        }

        if (request.bankAccountId() == null) throw new BadRequestException("Selecciona la cuenta de la que sale el efectivo");
        if (request.forma() == null) throw new BadRequestException("Indica la forma del retiro");
        BigDecimal amount = amount(request.monto());
        String reference = optionalText(request.referencia(), 300);
        String proof = optionalText(request.comprobanteUrl(), 500);
        if (proof != null && !proof.startsWith("https://"))
            throw new BadRequestException("El comprobante debe ser una URL HTTPS");

        BankAccount account = accounts.findById(request.bankAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta bancaria no encontrada"));
        if (!Boolean.TRUE.equals(account.getActivo()))
            throw new BadRequestException("La cuenta bancaria está inactiva");
        requireAvailable(account, amount, availability.available(account.getId()), true);

        BankCashWithdrawal withdrawal = new BankCashWithdrawal();
        withdrawal.setRequestId(request.requestId().toString());
        withdrawal.setCuentaBancaria(account);
        withdrawal.setBancoSnapshot(account.getBanco());
        withdrawal.setForma(request.forma());
        withdrawal.setMonto(amount);
        withdrawal.setReferencia(reference);
        withdrawal.setComprobanteUrl(proof);
        withdrawal.setRegistradoPor(auth.getCurrentUser());
        BankCashWithdrawal saved = withdrawals.saveAndFlush(withdrawal);
        businessAuditService.record(saved.getRegistradoPor().getId(), "BANK_WITHDRAWAL_REGISTER",
                "BANK_WITHDRAWAL", saved.getId(), null, auditSnapshot(saved));
        events.publishEvent(new WithdrawalChanged(saved.getId(), WithdrawalStatus.PENDIENTE, saved.getRegistradoPor().getNombre()));
        return view(saved, catalog());
    }

    @Transactional
    public BankCashWithdrawalView edit(Long id, EditWithdrawalRequest request) {
        if (request == null || request.version() == null) throw new BadRequestException("Falta la versión del retiro");
        lock();
        BankCashWithdrawal withdrawal = forUpdate(id);
        if (withdrawal.getEstatus() != WithdrawalStatus.PENDIENTE)
            throw new ConflictException("Solo se puede editar un retiro pendiente");
        if (!request.version().equals(withdrawal.getVersion()))
            throw new ConflictException("El retiro cambió; actualiza la consulta antes de editar");
        if (request.bankAccountId() == null) throw new BadRequestException("Selecciona la cuenta de la que sale el efectivo");
        if (request.forma() == null) throw new BadRequestException("Indica la forma del retiro");
        BigDecimal amount = amount(request.monto());
        String reference = optionalText(request.referencia(), 300);
        String proof = optionalText(request.comprobanteUrl(), 500);
        if (proof != null && !proof.startsWith("https://"))
            throw new BadRequestException("El comprobante debe ser una URL HTTPS");
        BankAccount account = accounts.findById(request.bankAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta bancaria no encontrada"));
        if (!Boolean.TRUE.equals(account.getActivo())) throw new BadRequestException("La cuenta bancaria está inactiva");
        if (bankCuts.findFirstByBankAccountIdAndFechaGreaterThanEqualOrderByFechaAsc(account.getId(), LocalDate.now()).isPresent())
            throw new ConflictException("La cuenta ya tiene registrado el corte de hoy");
        BigDecimal available = availability.available(account.getId());
        if (account.getId().equals(withdrawal.getCuentaBancaria().getId()))
            available = available.add(withdrawal.getMonto());
        requireAvailable(account, amount, available, true);
        User actor = auth.getCurrentUser();
        Map<String, Object> before = auditSnapshot(withdrawal);
        boolean proofChanged = !Objects.equals(withdrawal.getComprobanteUrl(), proof);
        withdrawal.setCuentaBancaria(account); withdrawal.setBancoSnapshot(account.getBanco());
        withdrawal.setForma(request.forma()); withdrawal.setMonto(amount);
        withdrawal.setReferencia(reference); withdrawal.setComprobanteUrl(proof);
        BankCashWithdrawal saved = withdrawals.saveAndFlush(withdrawal);
        Map<String, Object> after = auditSnapshot(saved);
        after.put("proofReplaced", proofChanged);
        businessAuditService.record(actor.getId(), "BANK_WITHDRAWAL_EDIT", "BANK_WITHDRAWAL", id, before, after);
        events.publishEvent(new WithdrawalChanged(id, WithdrawalStatus.PENDIENTE, actor.getNombre()));
        return view(saved, catalog());
    }

    /**
     * Cajas recibió y contó el efectivo: entra a la caja abierta de hoy y, en la misma fila,
     * sale del banco. Repetir la confirmación de un retiro ya confirmado devuelve el mismo
     * resultado sin volver a registrarlo.
     */
    @Transactional
    public BankCashWithdrawalView confirm(Long id, ConfirmWithdrawalRequest request) {
        if (request == null || request.diaCajaId() == null)
            throw new BadRequestException("Indica la caja en la que entra el efectivo");
        lock();
        BankCashWithdrawal withdrawal = forUpdate(id);
        BankNames catalog = catalog();
        if (withdrawal.getEstatus() == WithdrawalStatus.CONFIRMADO) return view(withdrawal, catalog);
        if (withdrawal.getEstatus() != WithdrawalStatus.PENDIENTE)
            throw new ConflictException("El retiro ya fue " + withdrawal.getEstatus().name().toLowerCase(Locale.ROOT));

        BankAccount account = withdrawal.getCuentaBancaria();
        if (!Boolean.TRUE.equals(account.getActivo()))
            throw new ConflictException("La cuenta bancaria se desactivó; recházalo para que Cuentas lo revise");
        LocalDate hoy = LocalDate.now();
        if (dailyCuts.findFirstByFechaGreaterThanEqualAndEstatusOrderByFechaAsc(hoy, DailyCashCutStatus.CERRADO).isPresent())
            throw new ConflictException("El periodo está cerrado");
        if (bankCuts.findFirstByBankAccountIdAndFechaGreaterThanEqualOrderByFechaAsc(account.getId(), hoy).isPresent())
            throw new ConflictException("La cuenta ya tiene registrado el corte de hoy");
        // Al registrarlo ya se validó contra el disponible; aquí se vuelve a validar contra el
        // saldo real por si otra salida lo consumió mientras estaba en tránsito.
        requireAvailable(account, withdrawal.getMonto(), availability.balance(account.getId()), false);

        String banco = catalog.display(account.getBanco());
        int totalCuentas = (int) accounts.findByActivoTrue().stream()
                .filter(a -> catalog.display(a.getBanco()).equals(banco)).count();
        CashMovementResponse movement = cash.recordBankWithdrawal(withdrawal.getId(), account, withdrawal.getForma().cashConcept(),
                withdrawal.getMonto(), concept(withdrawal, banco), withdrawal.getComprobanteUrl(),
                request.denominaciones(), request.diaCajaId(), banco, totalCuentas);

        User user = auth.getCurrentUser();
        Map<String, Object> beforeAudit = auditSnapshot(withdrawal);
        withdrawal.setEstatus(WithdrawalStatus.CONFIRMADO);
        withdrawal.setMovimientoCaja(cashMovements.getReferenceById(movement.id()));
        withdrawal.setResueltoPor(user);
        withdrawal.setResueltoEn(LocalDateTime.now());
        BankCashWithdrawal saved = withdrawals.saveAndFlush(withdrawal);
        businessAuditService.record(user.getId(), "BANK_WITHDRAWAL_CONFIRM", "BANK_WITHDRAWAL",
                saved.getId(), beforeAudit, auditSnapshot(saved));
        events.publishEvent(new WithdrawalChanged(saved.getId(), WithdrawalStatus.CONFIRMADO, user.getNombre()));
        return view(saved, catalog);
    }

    @Transactional
    public BankCashWithdrawalView reject(Long id, ResolveWithdrawalRequest request) {
        return close(id, request, WithdrawalStatus.RECHAZADO);
    }

    @Transactional
    public BankCashWithdrawalView cancel(Long id, ResolveWithdrawalRequest request) {
        return close(id, request, WithdrawalStatus.CANCELADO);
    }

    private BankCashWithdrawalView close(Long id, ResolveWithdrawalRequest request, WithdrawalStatus target) {
        String reason = optionalText(request == null ? null : request.motivo(), 500);
        if (reason == null) throw new BadRequestException("Indica el motivo");
        lock();
        BankCashWithdrawal withdrawal = forUpdate(id);
        if (withdrawal.getEstatus() == target) return view(withdrawal, catalog());
        if (withdrawal.getEstatus() != WithdrawalStatus.PENDIENTE)
            throw new ConflictException("Sólo se puede " + (target == WithdrawalStatus.RECHAZADO ? "rechazar" : "cancelar")
                    + " un retiro pendiente");
        User user = auth.getCurrentUser();
        Map<String, Object> beforeAudit = auditSnapshot(withdrawal);
        withdrawal.setEstatus(target);
        withdrawal.setMotivo(reason);
        withdrawal.setResueltoPor(user);
        withdrawal.setResueltoEn(LocalDateTime.now());
        BankCashWithdrawal saved = withdrawals.saveAndFlush(withdrawal);
        businessAuditService.record(user.getId(), target == WithdrawalStatus.RECHAZADO
                        ? "BANK_WITHDRAWAL_REJECT" : "BANK_WITHDRAWAL_CANCEL",
                "BANK_WITHDRAWAL", saved.getId(), beforeAudit, auditSnapshot(saved));
        events.publishEvent(new WithdrawalChanged(saved.getId(), target, user.getNombre()));
        return view(saved, catalog());
    }

    /** Pendientes, sin límite de fechas: es la bandeja de Cajas y lo que va "en tránsito", el más reciente primero. */
    public List<BankCashWithdrawalView> pending() {
        BankNames catalog = catalog();
        return withdrawals.findByEstatusOrderByRegistradoEnDescIdDesc(WithdrawalStatus.PENDIENTE)
                .stream().map(w -> view(w, catalog)).toList();
    }

    public List<BankCashWithdrawalView> list(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde) || hasta.isAfter(LocalDate.now())
                || hasta.isAfter(desde.plusYears(1)))
            throw new BadRequestException("Selecciona un rango ordenado, no futuro y de máximo un año");
        BankNames catalog = catalog();
        return withdrawals.findByRegistradoEnGreaterThanEqualAndRegistradoEnLessThanOrderByRegistradoEnDescIdDesc(
                        desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay())
                .stream().map(w -> view(w, catalog)).toList();
    }

    private BankCashWithdrawal forUpdate(Long id) {
        return withdrawals.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Retiro no encontrado"));
    }

    private Map<String, Object> auditSnapshot(BankCashWithdrawal withdrawal) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("bankAccountId", withdrawal.getCuentaBancaria().getId());
        snapshot.put("amount", withdrawal.getMonto());
        snapshot.put("method", withdrawal.getForma());
        snapshot.put("reference", withdrawal.getReferencia());
        snapshot.put("proofPresent", withdrawal.getComprobanteUrl() != null);
        snapshot.put("status", withdrawal.getEstatus());
        snapshot.put("cashMovementId", withdrawal.getMovimientoCaja() == null ? null : withdrawal.getMovimientoCaja().getId());
        return snapshot;
    }

    private BankNames catalog() {
        return BankNames.of(accounts.findAll().stream().map(BankAccount::getBanco).toList());
    }

    private static void requireAvailable(BankAccount account, BigDecimal amount, BigDecimal available, boolean includesTransit) {
        if (available.compareTo(amount) < 0) {
            NumberFormat mxn = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX"));
            throw new BadRequestException("Saldo insuficiente en " + account.getBanco() + " ••••" + last4(account.getNumeroCuenta())
                    + ": " + (includesTransit ? "disponible " : "saldo ") + mxn.format(available.max(BigDecimal.ZERO))
                    + (includesTransit ? " (descontando el efectivo en tránsito)" : ""));
        }
    }

    private static String concept(BankCashWithdrawal w, String banco) {
        return w.getForma().label() + " · " + banco + " ••••" + last4(w.getCuentaBancaria().getNumeroCuenta());
    }

    private static String last4(String number) {
        if (number == null) return "";
        String digits = number.trim();
        return digits.length() <= 4 ? digits : digits.substring(digits.length() - 4);
    }

    private static BigDecimal amount(BigDecimal value) {
        if (value == null || value.signum() <= 0) throw new BadRequestException("Captura un monto mayor a cero");
        if (value.stripTrailingZeros().scale() > 2) throw new BadRequestException("El monto admite máximo dos decimales");
        if (value.compareTo(MAX_AMOUNT) > 0) throw new BadRequestException("El monto excede el máximo permitido");
        // Es efectivo: Cajas lo confirma contando billetes y monedas, y la moneda más chica es de
        // 50 centavos. Otro importe nunca podría cuadrar con el desglose.
        if (value.movePointRight(2).remainder(FIFTY_CENTS).signum() != 0)
            throw new BadRequestException("El efectivo sólo admite centavos en múltiplos de 50: .00 o .50");
        return value.setScale(2);
    }

    private static String optionalText(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String text = value.trim();
        if (text.length() > max) throw new BadRequestException("Texto demasiado largo");
        return text;
    }

    private static boolean sameRequest(BankCashWithdrawal w, RegisterWithdrawalRequest r) {
        return Objects.equals(w.getCuentaBancaria().getId(), r.bankAccountId())
                && w.getForma() == r.forma()
                && r.monto() != null && w.getMonto().compareTo(r.monto()) == 0
                && Objects.equals(w.getReferencia(), optionalText(r.referencia(), 300))
                && Objects.equals(w.getComprobanteUrl(), optionalText(r.comprobanteUrl(), 500));
    }

    private static BankCashWithdrawalView view(BankCashWithdrawal w, BankNames catalog) {
        BankAccount a = w.getCuentaBancaria();
        return new BankCashWithdrawalView(w.getId(), w.getRegistradoEn(), a.getId(),
                a.getTitular() + " — " + a.getBanco() + " — " + a.getNumeroCuenta(), a.getTitular(), a.getNumeroCuenta(),
                catalog.display(w.getBancoSnapshot()), w.getForma(), w.getForma().label(), w.getMonto(),
                w.getReferencia(), w.getComprobanteUrl(), w.getEstatus(), w.getMotivo(),
                w.getRegistradoPor().getId(), w.getRegistradoPor().getNombre(),
                w.getResueltoPor() == null ? null : w.getResueltoPor().getNombre(), w.getResueltoEn(),
                w.getMovimientoCaja() == null ? null : w.getMovimientoCaja().getId(), w.getVersion());
    }
}
