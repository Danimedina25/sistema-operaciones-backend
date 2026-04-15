package com.sistemadeoperaciones.auth.service;

import com.sistemadeoperaciones.auth.dto.AuthResponse;
import com.sistemadeoperaciones.auth.dto.LoginRequest;
import com.sistemadeoperaciones.usuarios.model.User;
import com.sistemadeoperaciones.usuarios.dto.request.CompletePasswordResetRequestDto;
import com.sistemadeoperaciones.usuarios.dto.request.RequestPasswordResetDto;

public interface AuthService {

    AuthResponse login (LoginRequest loginRequest);

    void requestPasswordReset(RequestPasswordResetDto request);

    void completePasswordReset(CompletePasswordResetRequestDto request);

    void invalidatePreviousPasswordResetTokens(User user);
    String createAndSavePasswordResetToken(User user);

    String buildPasswordResetUrl(String token);

    void sendPasswordResetEmail(User user, String resetUrl);
}