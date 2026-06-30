package com.sistemadeoperaciones.cuentasbancarias.controller;

import com.sistemadeoperaciones.cuentasbancarias.dto.BankAccountRequestDto;
import com.sistemadeoperaciones.cuentasbancarias.dto.BankAccountResponseDto;
import com.sistemadeoperaciones.cuentasbancarias.service.BankAccountService;
import com.sistemadeoperaciones.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bank-accounts")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    public BankAccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'AUXILIAR_CUENTAS')")
    public ResponseEntity<ApiResponse<BankAccountResponseDto>> createBankAccount(
            @Valid @RequestBody BankAccountRequestDto request
    ) {
        BankAccountResponseDto response = bankAccountService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(true, "Cuenta bancaria registrada exitosamente", response, null)
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'AUXILIAR_CUENTAS', 'JEFA_CAJAS', 'JEFA_CUENTAS', 'SOCIO_COMERCIAL')")
    public ResponseEntity<ApiResponse<List<BankAccountResponseDto>>> getAllBankAccounts() {
        List<BankAccountResponseDto> response = bankAccountService.findAll();

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Listado de cuentas bancarias obtenido exitosamente", response, null)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'AUXILIAR_CUENTAS', 'JEFA_CAJAS', 'JEFA_CUENTAS')")
    public ResponseEntity<ApiResponse<BankAccountResponseDto>> getBankAccountById(@PathVariable Long id) {
        BankAccountResponseDto response = bankAccountService.findById(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Cuenta bancaria obtenida exitosamente", response, null)
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'AUXILIAR_CUENTAS')")
    public ResponseEntity<ApiResponse<BankAccountResponseDto>> updateBankAccount(
            @PathVariable Long id,
            @Valid @RequestBody BankAccountRequestDto request
    ) {
        BankAccountResponseDto response = bankAccountService.update(id, request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Cuenta bancaria actualizada exitosamente", response, null)
        );
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse<BankAccountResponseDto>> deactivateBankAccount(@PathVariable Long id) {
        BankAccountResponseDto response = bankAccountService.deactivate(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Cuenta bancaria desactivada exitosamente", response, null)
        );
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse<BankAccountResponseDto>> activateBankAccount(@PathVariable Long id) {
        BankAccountResponseDto response = bankAccountService.activate(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Cuenta bancaria activada exitosamente", response, null)
        );
    }
}