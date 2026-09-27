package com.sistemadeoperaciones.transferencias;

import com.sistemadeoperaciones.shared.dto.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Transferencias entre cuentas propias. Vive fuera de {@code /api/bank-movements}, que es
 * de sólo consulta por diseño: aquí nace la fuente y allá sólo se lee.
 */
@RestController
@RequestMapping("/api/bank-transfers")
@PreAuthorize("hasAnyRole('ADMIN', 'JEFA_CUENTAS', 'GERENTE', 'DIRECCION', 'AUXILIAR_CUENTAS')")
public class BankTransferController {
    private final BankTransferService service;

    public BankTransferController(BankTransferService service) { this.service = service; }

    @GetMapping
    public ApiResponse<List<BankTransferView>> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return new ApiResponse<>(true, "Transferencias obtenidas", service.list(desde, hasta), null);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFA_CUENTAS')")
    public ApiResponse<BankTransferView> create(@RequestBody BankTransferRequest request) {
        return new ApiResponse<>(true, "Transferencia registrada", service.create(request), null);
    }
}
