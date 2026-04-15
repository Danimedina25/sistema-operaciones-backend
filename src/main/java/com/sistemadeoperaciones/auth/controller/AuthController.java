package com.sistemadeoperaciones.auth.controller;

import com.sistemadeoperaciones.auth.dto.AuthResponse;
import com.sistemadeoperaciones.auth.dto.LoginRequest;
import com.sistemadeoperaciones.auth.service.AuthServiceImpl;
import com.sistemadeoperaciones.shared.dto.ApiResponse;
import com.sistemadeoperaciones.usuarios.dto.request.CompletePasswordResetRequestDto;
import com.sistemadeoperaciones.usuarios.dto.request.RequestPasswordResetDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthServiceImpl authService;

    public AuthController(AuthServiceImpl authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Login exitoso", response, null)
        );
    }
    @PostMapping("/request-password-reset")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody RequestPasswordResetDto request
    ) {
        authService.requestPasswordReset(request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Si el correo existe, se enviaron instrucciones para restablecer la contraseña", null, null)
        );
    }

    @PostMapping("/complete-password-reset")
    public ResponseEntity<ApiResponse<Void>> completePasswordReset(
            @Valid @RequestBody CompletePasswordResetRequestDto request
    ) {
        authService.completePasswordReset(request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Contraseña actualizada exitosamente", null, null)
        );
    }


}