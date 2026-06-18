package com.sistemadeoperaciones.corte.controller;

import com.sistemadeoperaciones.corte.dto.CashCutRangeResponse;
import com.sistemadeoperaciones.corte.dto.DailyCashCutRequest;
import com.sistemadeoperaciones.corte.dto.DailyCashCutResponse;
import com.sistemadeoperaciones.corte.service.DailyCashCutService;
import com.sistemadeoperaciones.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/daily-cash-cuts")
public class DailyCashCutController {

    private final DailyCashCutService dailyCashCutService;

    public DailyCashCutController(
            DailyCashCutService dailyCashCutService
    ) {
        this.dailyCashCutService = dailyCashCutService;
    }

    /**
     * Calcula el corte de un día sin guardarlo.
     * Útil para mostrar el corte en vivo del día actual.
     */
    @GetMapping("/daily")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'AUXILIAR_CUENTAS')")
    public ResponseEntity<ApiResponse<DailyCashCutResponse>>
    calculateDailyCut(
            @RequestParam LocalDate fecha
    ) {

        DailyCashCutResponse response =
                dailyCashCutService.calculateDailyCut(fecha);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Corte diario calculado exitosamente",
                        response,
                        null
                )
        );
    }

    /**
     * Registra el corte diario en la tabla daily_cash_cuts.
     * Normalmente lo usará el scheduler automático.
     */
    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse<DailyCashCutResponse>>
    registerDailyCut(
            @RequestParam LocalDate fecha
    ) {

        DailyCashCutResponse response =
                dailyCashCutService.registerDailyCut(fecha);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Corte diario registrado exitosamente",
                        response,
                        null
                )
        );
    }

    /**
     * Registra el corte diario con datos adicionales.
     * Útil para primer corte, observaciones o saldo inicial manual.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse<DailyCashCutResponse>>
    registerDailyCutWithRequest(
            @Valid @RequestBody DailyCashCutRequest request
    ) {

        DailyCashCutResponse response =
                dailyCashCutService.registerDailyCut(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Corte diario registrado exitosamente",
                        response,
                        null
                )
        );
    }

    /**
     * Consulta un resumen por rango de fechas sin guardar nada.
     * Sirve para reportes semanales, mensuales, anuales o personalizados.
     */
    @GetMapping("/range")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'AUXILIAR_CUENTAS')")
    public ResponseEntity<ApiResponse<CashCutRangeResponse>>
    calculateRangeCut(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {

        CashCutRangeResponse response =
                dailyCashCutService.calculateRangeCut(
                        startDate,
                        endDate
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Resumen de corte obtenido exitosamente",
                        response,
                        null
                )
        );
    }
}