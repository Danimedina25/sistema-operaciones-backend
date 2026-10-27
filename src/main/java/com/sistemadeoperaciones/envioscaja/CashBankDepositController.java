package com.sistemadeoperaciones.envioscaja;

import com.sistemadeoperaciones.shared.dto.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/cash-bank-deposits")
@PreAuthorize("hasAnyRole('ADMIN','JEFA_CAJAS','JEFA_CUENTAS','GERENTE','DIRECCION')")
public class CashBankDepositController {
    private final CashBankDepositService service;
    public CashBankDepositController(CashBankDepositService service) { this.service = service; }
    @GetMapping("/pending") public ApiResponse<List<CashBankDepositView>> pending() { return ok(service.pending()); }
    @GetMapping("/pending-summary") public ApiResponse<CashBankDepositService.PendingSummary> summary() { return ok(service.pendingSummary()); }
    @GetMapping("/return-pending") public ApiResponse<List<CashBankDepositView>> returns() { return ok(service.rejectedForReturn()); }
    @GetMapping("/preview") public ApiResponse<CashBankDepositService.Preview> preview(
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(required = false) BigDecimal monto) { return ok(service.preview(bankAccountId, monto)); }
    @GetMapping public ApiResponse<List<CashBankDepositView>> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ok(service.list(desde, hasta));
    }
    @PostMapping @PreAuthorize("hasAnyRole('ADMIN','JEFA_CAJAS')")
    public ApiResponse<CashBankDepositView> register(@RequestBody CashBankDepositRequests.Register request) { return ok(service.register(request)); }
    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','JEFA_CAJAS')")
    public ApiResponse<CashBankDepositView> edit(@PathVariable Long id, @RequestBody CashBankDepositRequests.Edit request) { return ok(service.edit(id, request)); }
    @PostMapping("/{id}/confirm") @PreAuthorize("hasAnyRole('ADMIN','JEFA_CUENTAS')")
    public ApiResponse<CashBankDepositView> confirm(@PathVariable Long id, @RequestBody CashBankDepositRequests.Confirm request) { return ok(service.confirm(id, request)); }
    @PostMapping("/{id}/reject") @PreAuthorize("hasAnyRole('ADMIN','JEFA_CUENTAS')")
    public ApiResponse<CashBankDepositView> reject(@PathVariable Long id, @RequestBody CashBankDepositRequests.Reject request) { return ok(service.reject(id, request)); }
    @PostMapping("/{id}/confirm-return") @PreAuthorize("hasAnyRole('ADMIN','JEFA_CAJAS')")
    public ApiResponse<CashBankDepositView> confirmReturn(@PathVariable Long id, @RequestBody CashBankDepositRequests.ConfirmReturn request) { return ok(service.confirmReturn(id, request)); }
    private <T> ApiResponse<T> ok(T value) { return new ApiResponse<>(true, "Envíos de Caja a cuentas", value, null); }
}
