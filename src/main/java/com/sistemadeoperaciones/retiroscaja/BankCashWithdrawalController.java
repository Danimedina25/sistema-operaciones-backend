package com.sistemadeoperaciones.retiroscaja;

import com.sistemadeoperaciones.shared.dto.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Retiros de efectivo hacia Caja General. Cuentas los registra y cancela; Cajas los confirma
 * o rechaza. Los dos lados, y quienes consultan ambos módulos, pueden verlos.
 */
@RestController
@RequestMapping("/api/bank-cash-withdrawals")
@PreAuthorize("hasAnyRole('ADMIN', 'JEFA_CUENTAS', 'AUXILIAR_CUENTAS', 'JEFA_CAJAS', 'GERENTE', 'DIRECCION')")
public class BankCashWithdrawalController {
    private final BankCashWithdrawalService service;

    public BankCashWithdrawalController(BankCashWithdrawalService service) { this.service = service; }

    @GetMapping("/pending")
    public ApiResponse<List<BankCashWithdrawalView>> pending() { return ok(service.pending()); }

    @GetMapping
    public ApiResponse<List<BankCashWithdrawalView>> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ok(service.list(desde, hasta));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFA_CUENTAS')")
    public ApiResponse<BankCashWithdrawalView> register(@RequestBody RegisterWithdrawalRequest request) {
        return ok(service.register(request));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFA_CUENTAS')")
    public ApiResponse<BankCashWithdrawalView> cancel(@PathVariable Long id, @RequestBody ResolveWithdrawalRequest request) {
        return ok(service.cancel(id, request));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFA_CAJAS')")
    public ApiResponse<BankCashWithdrawalView> confirm(@PathVariable Long id, @RequestBody ConfirmWithdrawalRequest request) {
        return ok(service.confirm(id, request));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFA_CAJAS')")
    public ApiResponse<BankCashWithdrawalView> reject(@PathVariable Long id, @RequestBody ResolveWithdrawalRequest request) {
        return ok(service.reject(id, request));
    }

    private <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(true, "Retiros a Caja General", data, null); }
}
