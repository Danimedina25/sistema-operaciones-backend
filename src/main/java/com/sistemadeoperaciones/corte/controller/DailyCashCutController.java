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
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION', 'AUXILIAR_CUENTAS')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION')")
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
     * Rehace los cortes ya registrados desde una fecha en adelante y vuelve a encadenar sus
     * saldos con la definición contable vigente.
     *
     * <p>Reescribe filas financieras ya cerradas, así que queda restringido a Administración.
     * Es determinista: los importes se recalculan desde los pagos, retornos y cheques
     * originales, nunca desde los agregados guardados, de modo que ejecutarlo dos veces da el
     * mismo resultado.
     */
    @PostMapping("/recalculate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> recalculateFrom(
            @RequestParam LocalDate desde
    ) {

        int recalculados = dailyCashCutService.recalculateFrom(desde);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Cortes diarios recalculados: " + recalculados,
                        recalculados,
                        null
                )
        );
    }

    /**
     * Borra los cortes bancarios del rango —el global y el de cada cuenta— y los vuelve a
     * calcular desde las operaciones registradas.
     *
     * <p>Destructivo: reescribe filas financieras cerradas, así que queda restringido a
     * Administración. {@code saldoInicial} sólo se usa cuando no hay ningún corte anterior a
     * {@code desde}. El día de hoy nunca se registra: siempre se calcula en vivo.
     */
    @PostMapping("/rebuild")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> rebuildRange(
            @RequestParam LocalDate desde,
            @RequestParam(required = false) LocalDate hasta,
            @RequestParam(required = false) java.math.BigDecimal saldoInicial
    ) {

        int dias = dailyCashCutService.rebuildRange(desde, hasta, saldoInicial);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Cortes bancarios reconstruidos: " + dias + " días",
                        dias,
                        null
                )
        );
    }

    /**
     * Registra el corte diario con datos adicionales.
     * Útil para primer corte, observaciones o saldo inicial manual.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION', 'AUXILIAR_CUENTAS')")
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