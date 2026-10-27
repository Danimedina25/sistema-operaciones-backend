package com.sistemadeoperaciones.transferencias;

import com.sistemadeoperaciones.cajageneral.repository.CashGeneralRegisterRepository;
import com.sistemadeoperaciones.corte.repository.BankAccountDailyCutRepository;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.cuentasbancarias.repository.BankAccountRepository;
import com.sistemadeoperaciones.retiroscaja.BankAvailability;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
import com.sistemadeoperaciones.shared.exception.ConflictException;
import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankTransferService {
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999999.99");

    private final BankTransferRepository transfers;
    private final BankAccountRepository accounts;
    private final BankAccountDailyCutRepository bankCuts;
    private final BankAvailability availability;
    // Mismo candado global que el registro de cortes y los cheques: una transferencia no
    // puede colarse entre el cálculo y el guardado de un corte bancario.
    private final CashGeneralRegisterRepository periodLock;
    private final AuthenticatedUserService auth;
    private final BusinessAuditService businessAuditService;

    @Transactional
    public BankTransferView create(BankTransferRequest request) {
        if (request == null || request.requestId() == null)
            throw new BadRequestException("Falta identificador de solicitud");
        periodLock.ensureRegister();
        periodLock.lockRegister();

        var existing = transfers.findByRequestId(request.requestId().toString());
        if (existing.isPresent()) {
            if (!sameRequest(existing.get(), request))
                throw new ConflictException("El identificador de solicitud ya se utilizó con otros datos");
            return view(existing.get(), null);
        }

        if (request.cuentaOrigenId() == null || request.cuentaDestinoId() == null)
            throw new BadRequestException("Selecciona la cuenta origen y la cuenta beneficiaria");
        if (request.cuentaOrigenId().equals(request.cuentaDestinoId()))
            throw new BadRequestException("La cuenta origen y la beneficiaria deben ser distintas");
        BigDecimal amount = amount(request.monto());
        String reference = optionalText(request.referencia(), 300);
        String proof = optionalText(request.comprobanteUrl(), 500);
        if (proof != null && !proof.startsWith("https://"))
            throw new BadRequestException("El comprobante debe ser una URL HTTPS");

        BankAccount origen = activeAccount(request.cuentaOrigenId(), "origen");
        BankAccount destino = activeAccount(request.cuentaDestinoId(), "beneficiaria");

        // La transferencia se fecha hoy. Si alguna de las dos cuentas ya tiene su corte de hoy
        // registrado, ese corte quedaría sin este movimiento.
        LocalDate hoy = LocalDate.now();
        for (BankAccount account : List.of(origen, destino)) {
            if (bankCuts.findFirstByBankAccountIdAndFechaGreaterThanEqualOrderByFechaAsc(account.getId(), hoy).isPresent())
                throw new ConflictException("La cuenta " + label(account) + " ya tiene registrado el corte de hoy");
        }

        // Como en un banco: no se transfiere más de lo disponible, y el efectivo que ya se retiró
        // hacia Caja General y sigue en tránsito tampoco está disponible.
        BigDecimal disponible = availability.available(origen.getId());
        if (disponible.compareTo(amount) < 0) {
            NumberFormat mxn = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX"));
            throw new BadRequestException("Saldo insuficiente en la cuenta origen: disponible "
                    + mxn.format(disponible.max(BigDecimal.ZERO)));
        }

        BankTransfer transfer = new BankTransfer();
        transfer.setRequestId(request.requestId().toString());
        transfer.setCuentaOrigen(origen);
        transfer.setCuentaDestino(destino);
        transfer.setMonto(amount);
        transfer.setReferencia(reference);
        transfer.setComprobanteUrl(proof);
        transfer.setRegistradoPor(auth.getCurrentUser());
        BankTransfer saved = transfers.saveAndFlush(transfer);
        businessAuditService.record(saved.getRegistradoPor().getId(), "BANK_TRANSFER_CREATE", "BANK_TRANSFER",
                saved.getId(), null, Map.of("originAccountId", origen.getId(),
                        "destinationAccountId", destino.getId(), "amount", amount));

        return view(saved, availability.balance(origen.getId()));
    }

    public List<BankTransferView> list(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde) || hasta.isAfter(LocalDate.now())
                || hasta.isAfter(desde.plusYears(1)))
            throw new BadRequestException("Selecciona un rango ordenado, no futuro y de máximo un año");
        return transfers.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDescIdDesc(
                        desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay())
                .stream().map(t -> view(t, null)).toList();
    }

    private BankAccount activeAccount(Long id, String rol) {
        BankAccount account = accounts.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta " + rol + " no encontrada"));
        if (!Boolean.TRUE.equals(account.getActivo()))
            throw new BadRequestException("La cuenta " + rol + " está inactiva");
        return account;
    }

    private static BigDecimal amount(BigDecimal value) {
        if (value == null || value.signum() <= 0)
            throw new BadRequestException("Captura un monto mayor a cero");
        if (value.stripTrailingZeros().scale() > 2)
            throw new BadRequestException("El monto admite máximo dos decimales");
        if (value.compareTo(MAX_AMOUNT) > 0)
            throw new BadRequestException("El monto excede el máximo permitido");
        return value.setScale(2);
    }

    private static String optionalText(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String text = value.trim();
        if (text.length() > max) throw new BadRequestException("Texto demasiado largo");
        return text;
    }

    private static boolean sameRequest(BankTransfer t, BankTransferRequest r) {
        return Objects.equals(t.getCuentaOrigen().getId(), r.cuentaOrigenId())
                && Objects.equals(t.getCuentaDestino().getId(), r.cuentaDestinoId())
                && r.monto() != null && t.getMonto().compareTo(r.monto()) == 0
                && Objects.equals(t.getReferencia(), optionalText(r.referencia(), 300))
                && Objects.equals(t.getComprobanteUrl(), optionalText(r.comprobanteUrl(), 500));
    }

    /** {@code Titular — Banco — Número}, igual que {@code bank-account-label.ts} en el frontend. */
    private static String label(BankAccount a) {
        return a.getTitular() + " — " + a.getBanco() + " — " + a.getNumeroCuenta();
    }

    private static BankTransferView view(BankTransfer t, BigDecimal saldoOrigen) {
        return new BankTransferView(t.getId(), t.getCreatedAt(),
                t.getCuentaOrigen().getId(), label(t.getCuentaOrigen()),
                t.getCuentaDestino().getId(), label(t.getCuentaDestino()),
                t.getMonto(), t.getReferencia(), t.getComprobanteUrl(),
                t.getRegistradoPor().getId(), t.getRegistradoPor().getNombre(), saldoOrigen);
    }
}
