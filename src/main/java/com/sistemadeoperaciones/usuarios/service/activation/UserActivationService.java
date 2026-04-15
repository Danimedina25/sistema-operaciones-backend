package com.sistemadeoperaciones.usuarios.service.activation;

import com.sistemadeoperaciones.usuarios.model.User;
import com.sistemadeoperaciones.usuarios.dto.request.CompleteUserActivationRequestDto;
import com.sistemadeoperaciones.usuarios.dto.request.UpdateUserEmailRequestDto;

public interface UserActivationService {

    String createAndSaveActivationToken(User user);

    String createAndSaveEmailVerificationToken(User user);

    void sendActivationEmail(User user, String activationUrl);

    void sendEmailVerificationEmail(User user, String verificationUrl);

    void completeActivation(CompleteUserActivationRequestDto request);

    void completeEmailVerification(String token);

    void resendActivationEmail(Long userId);

    void updateUserEmailAndResendActivation(Long userId, UpdateUserEmailRequestDto request);

    void invalidatePreviousEmailVerificationTokens(User user);

    String buildActivationUrl(String token);

    String buildEmailVerificationUrl(String token);
}