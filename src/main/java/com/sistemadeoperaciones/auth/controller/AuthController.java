package com.sistemadeoperaciones.auth.controller;

import com.sistemadeoperaciones.auth.dto.AuthResponse;
import com.sistemadeoperaciones.auth.dto.LoginRequest;
import com.sistemadeoperaciones.auth.service.AuthService;
import com.sistemadeoperaciones.shared.dto.ApiResponse;
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