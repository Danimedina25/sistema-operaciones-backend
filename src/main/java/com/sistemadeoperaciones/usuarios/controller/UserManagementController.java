package com.sistemadeoperaciones.usuarios.controller;

import com.sistemadeoperaciones.shared.dto.ApiResponse;
import com.sistemadeoperaciones.usuarios.dto.request.*;
import com.sistemadeoperaciones.usuarios.dto.response.ActivateAccountResponseDto;
import com.sistemadeoperaciones.usuarios.dto.response.UserCreatedResponseDto;
import com.sistemadeoperaciones.usuarios.dto.response.UserResponseDto;
import com.sistemadeoperaciones.usuarios.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class UserManagementController {

    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION')")
    public ResponseEntity<ApiResponse<UserCreatedResponseDto>> create(
            @Valid @RequestBody CreateUserRequestDto request
    ) {
        UserCreatedResponseDto response = userManagementService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(true, "Usuario creado exitosamente. Se envió correo de activación.", response, null)
        );
    }

    @PostMapping("/complete-activation")
    public ResponseEntity<ApiResponse<ActivateAccountResponseDto>> completeUserActivation(
            @Valid @RequestBody CompleteUserActivationRequestDto request
    ) {
        userManagementService.completeUserActivation(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Cuenta activada exitosamente",
                        new ActivateAccountResponseDto(true),
                        null
                )
        );
    }

    @PostMapping("/{id}/resend-activation-email")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION')")
    public ResponseEntity<ApiResponse<Void>> resendActivationEmail(@PathVariable Long id) {
        userManagementService.resendActivationEmail(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Correo de activación reenviado exitosamente", null, null)
        );
    }

    @PatchMapping("/{id}/email")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION')")
    public ResponseEntity<ApiResponse<Void>> updateUserEmail(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserEmailRequestDto request
    ) {
        userManagementService.updateUserEmail(id, request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Correo actualizado y activación reenviada exitosamente", null, null)
        );
    }

    @PostMapping("/complete-email-verification")
    public ResponseEntity<ApiResponse<Void>> completeEmailVerification(@RequestBody CompleteEmailVerificationRequestDto request) {
        userManagementService.completeEmailVerification(request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Correo verificado exitosamente", null, null)
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION')")
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> findAll() {
        List<UserResponseDto> response = userManagementService.findAll();

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Listado de usuarios obtenido exitosamente", response, null)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION')")
    public ResponseEntity<ApiResponse<UserResponseDto>> findById(@PathVariable Long id) {
        UserResponseDto response = userManagementService.findById(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Usuario obtenido exitosamente", response, null)
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION')")
    public ResponseEntity<ApiResponse<UserResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequestDto request
    ) {
        UserResponseDto response = userManagementService.update(id, request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Usuario actualizado exitosamente", response, null)
        );
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION')")
    public ResponseEntity<ApiResponse<UserResponseDto>> deactivate(@PathVariable Long id) {
        UserResponseDto response = userManagementService.deactivate(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Usuario desactivado exitosamente", response, null)
        );
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION')")
    public ResponseEntity<ApiResponse<UserResponseDto>> activate(@PathVariable Long id) {
        UserResponseDto response = userManagementService.activate(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Usuario activado exitosamente", response, null)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECCION')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        userManagementService.delete(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Usuario eliminado permanentemente", null, null)
        );
    }
}