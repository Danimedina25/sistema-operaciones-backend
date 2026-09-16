package com.sistemadeoperaciones.corte.controller;

import com.sistemadeoperaciones.corte.dto.BankLedgerFilter;
import com.sistemadeoperaciones.corte.dto.BankLedgerRowDto;
import com.sistemadeoperaciones.corte.dto.BankLedgerTotalsDto;
import com.sistemadeoperaciones.corte.enums.BankLedgerDirection;
import com.sistemadeoperaciones.corte.service.BankLedgerService;
import com.sistemadeoperaciones.shared.dto.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Historial general de movimientos bancarios. Sólo consulta: este controlador no expone
 * ningún POST, PUT ni DELETE a propósito.
 */
@RestController
@RequestMapping("/api/bank-movements")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION', 'AUXILIAR_CUENTAS')")
public class BankLedgerController {

    private final BankLedgerService bankLedgerService;

    public BankLedgerController(BankLedgerService bankLedgerService) {
        this.bankLedgerService = bankLedgerService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BankLedgerRowDto>>> search(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(required = false) String banco,
            @RequestParam(required = false) BankLedgerDirection direccion,
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<BankLedgerRowDto> response = bankLedgerService.search(
                new BankLedgerFilter(desde, hasta, bankAccountId, banco, direccion, tipo),
                page,
                size
        );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Movimientos bancarios obtenidos exitosamente",
                        response,
                        null
                )
        );
    }

    /**
     * Totales sobre todo el filtro. El frontend nunca suma la página que tiene a la vista.
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<BankLedgerTotalsDto>> summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(required = false) String banco,
            @RequestParam(required = false) BankLedgerDirection direccion,
            @RequestParam(required = false) String tipo
    ) {
        BankLedgerTotalsDto response = bankLedgerService.totals(
                new BankLedgerFilter(desde, hasta, bankAccountId, banco, direccion, tipo)
        );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Totales de movimientos bancarios obtenidos exitosamente",
                        response,
                        null
                )
        );
    }
}
