package com.sistemadeoperaciones.pagos.controller;
import com.sistemadeoperaciones.pagos.service.CardlessReturnFlow;
import com.sistemadeoperaciones.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/operations/returns/requests")
public class CardlessReturnController {
    private final CardlessReturnFlow flow;
    public CardlessReturnController(CardlessReturnFlow flow) { this.flow = flow; }
    public record Assignment(@NotNull Long cuentaOrigenId) {}
    @PatchMapping("/{id}/cardless-account")
    @PreAuthorize("hasAnyRole('JEFA_CUENTAS', 'ADMIN')")
    public ApiResponse<?> assign(@PathVariable Long id, @Valid @RequestBody Assignment body) {
        return new ApiResponse<>(true, "Cuenta origen asignada", flow.assign(id, body.cuentaOrigenId()), null);
    }
}
