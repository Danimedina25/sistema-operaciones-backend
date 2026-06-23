package com.sistemadeoperaciones.corte.controller;

import com.sistemadeoperaciones.corte.dto.BankAccountBalanceDetailResponseDto;
import com.sistemadeoperaciones.corte.dto.BankAccountBalanceResponseDto;
import com.sistemadeoperaciones.corte.dto.BankGroupBalanceResponseDto;
import com.sistemadeoperaciones.corte.service.BankAccountDailyCutService;
import com.sistemadeoperaciones.shared.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bank-account-daily-cuts")
public class BankAccountDailyCutController {

    private final BankAccountDailyCutService bankAccountDailyCutService;

    public BankAccountDailyCutController(
            BankAccountDailyCutService bankAccountDailyCutService
    ) {
        this.bankAccountDailyCutService = bankAccountDailyCutService;
    }

    /**
     * Consulta el saldo detallado de una cuenta bancaria específica.
     */
    @GetMapping("/account/{bankAccountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'AUXILIAR_CUENTAS')")
    public ResponseEntity<ApiResponse<BankAccountBalanceDetailResponseDto>>
    calculateBalance(
            @PathVariable Long bankAccountId,
            @RequestParam LocalDate fecha
    ) {

        BankAccountBalanceDetailResponseDto response =
                bankAccountDailyCutService.calculateBalance(
                        bankAccountId,
                        fecha
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Saldo de cuenta bancaria obtenido exitosamente",
                        response,
                        null
                )
        );
    }

    /**
     * Consulta el saldo de todas las cuentas bancarias activas.
     */
    @GetMapping("/accounts")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'AUXILIAR_CUENTAS')")
    public ResponseEntity<ApiResponse<List<BankAccountBalanceResponseDto>>>
    calculateBalances(
            @RequestParam(required = false) LocalDate fecha
    ) {

        List<BankAccountBalanceResponseDto> response =
                bankAccountDailyCutService.calculateBalances(fecha);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Saldos de cuentas bancarias obtenidos exitosamente",
                        response,
                        null
                )
        );
    }

    /**
     * Consulta saldos agrupados por banco.
     */
    @GetMapping("/grouped")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'AUXILIAR_CUENTAS')")
    public ResponseEntity<ApiResponse<List<BankGroupBalanceResponseDto>>>
    calculateBalancesGrouped(
            @RequestParam(required = false) LocalDate fecha
    ) {

        List<BankGroupBalanceResponseDto> response =
                bankAccountDailyCutService.calculateBalancesGrouped(fecha);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Saldos agrupados por banco obtenidos exitosamente",
                        response,
                        null
                )
        );
    }

    /**
     * Registra el corte diario de todas las cuentas bancarias activas.
     * Normalmente lo usará el scheduler automático.
     */
    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse<Void>>
    registerDailyCut(
            @RequestParam LocalDate fecha
    ) {

        bankAccountDailyCutService.registerDailyCut(fecha);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Corte diario de cuentas bancarias registrado exitosamente",
                        null,
                        null
                )
        );
    }
}