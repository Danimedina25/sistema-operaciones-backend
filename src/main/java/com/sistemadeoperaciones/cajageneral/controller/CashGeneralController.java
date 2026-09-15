package com.sistemadeoperaciones.cajageneral.controller;
import com.sistemadeoperaciones.cajageneral.dto.*;
import com.sistemadeoperaciones.cajageneral.service.CashGeneralService;
import com.sistemadeoperaciones.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/caja-general")
@PreAuthorize("hasAnyRole('ADMIN', 'JEFA_CAJAS', 'GERENTE', 'DIRECCION')")
public class CashGeneralController {
    private final CashGeneralService service;
    public CashGeneralController(CashGeneralService service) { this.service = service; }
    @GetMapping("/latest") public ApiResponse<CashDayResponse> latest() { return ok(service.latest()); }
    @GetMapping("/ledger") public ApiResponse<CashLedgerResponse> ledger(@RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
        return ok(service.ledger(startDate, endDate));
    }
    @GetMapping("/deliveries") public ApiResponse<List<CashDeliveryResponse>> deliveries(@RequestParam(defaultValue = "0") int page) {
        return ok(service.deliveries(page));
    }
    @PostMapping("/days")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFA_CAJAS')")
    public ApiResponse<CashDayResponse> open(@Valid @RequestBody OpenCashDayRequest request) { return ok(service.open(request)); }
    @PostMapping("/days/{id}/movements")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFA_CAJAS')")
    public ApiResponse<CashMovementResponse> movement(@PathVariable Long id, @Valid @RequestBody CreateCashMovementRequest request) {
        return ok(service.createMovement(id, request));
    }
    @PostMapping("/days/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFA_CAJAS')")
    public ApiResponse<CashDayResponse> close(@PathVariable Long id, @Valid @RequestBody CloseCashDayRequest request) { return ok(service.close(id, request)); }
    @DeleteMapping("/days/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id, @Valid @RequestBody DeleteCashDayRequest request) {
        service.deleteDay(id, request);
        return new ApiResponse<>(true, "Corte eliminado correctamente", null, null);
    }
    private <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(true, "Caja General actualizada", data, null); }
}
