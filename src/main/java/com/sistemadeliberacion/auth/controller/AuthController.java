package com.sistemadeliberacion.auth.controller;

import com.sistemadeliberacion.auth.dto.AuthResponse;
import com.sistemadeliberacion.auth.dto.LoginRequest;
import com.sistemadeliberacion.auth.service.AuthService;
import com.sistemadeliberacion.shared.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Login exitoso", response, null)
        );
    }

}