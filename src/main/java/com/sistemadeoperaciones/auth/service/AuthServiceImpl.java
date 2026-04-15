package com.sistemadeoperaciones.auth.service;

import com.sistemadeoperaciones.auth.dto.AuthResponse;
import com.sistemadeoperaciones.auth.dto.LoginRequest;
import com.sistemadeoperaciones.auth.exceptions.CredencialesInvalidasException;
import com.sistemadeoperaciones.auth.exceptions.UsuarioInactivoException;
import com.sistemadeoperaciones.usuarios.model.User;
import com.sistemadeoperaciones.auth.repository.AuthRepository;
import com.sistemadeoperaciones.shared.email.EmailService;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;
import com.sistemadeoperaciones.shared.utils.JwtUtil;
import com.sistemadeoperaciones.usuarios.dto.request.CompletePasswordResetRequestDto;
import com.sistemadeoperaciones.usuarios.dto.request.RequestPasswordResetDto;
import com.sistemadeoperaciones.usuarios.enums.UserTokenType;
import com.sistemadeoperaciones.usuarios.exceptions.EmailSendException;
import com.sistemadeoperaciones.usuarios.model.UserActivationToken;
import com.sistemadeoperaciones.usuarios.repository.UserActivationTokenRepository;
import com.sistemadeoperaciones.usuarios.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final AuthRepository authRepository;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final UserActivationTokenRepository userActivationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final String frontendPasswordResetUrl;
    private final EmailService emailService;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           AuthRepository authRepository,
                           JwtUtil jwtUtil,
                           UserRepository userRepository,
                           UserActivationTokenRepository userActivationTokenRepository,
                           PasswordEncoder passwordEncoder,
                           EmailService emailService,
                           @Value("${app.frontend.activation-url}") String frontendActivationUrl,
                           @Value("${app.frontend.email-verification-url}") String frontendEmailVerificationUrl,
                           @Value("${app.frontend.password-reset-url}") String frontendPasswordResetUrl) {
        this.authenticationManager = authenticationManager;
        this.authRepository = authRepository;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.userActivationTokenRepository = userActivationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.frontendPasswordResetUrl = frontendPasswordResetUrl;
    }

    public AuthResponse login(LoginRequest request) {
        try {
            User user = authRepository.findByCorreo(request.getCorreo().trim().toLowerCase())
                    .orElseThrow(CredencialesInvalidasException::new);

            if (user.getPassword() == null || !Boolean.TRUE.equals(user.getActivo())) {
                throw new UsuarioInactivoException("La cuenta aún no ha sido activada");
            }

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getCorreo().trim().toLowerCase(),
                            request.getPassword()
                    )
            );

            String token = jwtUtil.generateToken(user);

            List<String> roles = user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .toList();

            return new AuthResponse(
                    token,
                    user.getId(),
                    user.getCorreo(),
                    user.getNombre(),
                    roles
            );

        } catch (UsuarioInactivoException e) {
            throw e;
        } catch (DisabledException e) {
            throw new UsuarioInactivoException();
        } catch (AuthenticationException e) {
            throw new CredencialesInvalidasException();
        }
    }

    @Override
    public void requestPasswordReset(RequestPasswordResetDto request) {
        String correoNormalizado = request.getCorreo().trim().toLowerCase();

        User user = userRepository.findByCorreo(correoNormalizado).orElse(null);

        if (user == null) {
            return;
        }

        if (user.getCorreo() == null || user.getCorreo().isBlank()) {
            return;
        }

        if (user.getPassword() == null) {
            return;
        }

        if (!Boolean.TRUE.equals(user.getCorreoVerificado())) {
            return;
        }

        invalidatePreviousPasswordResetTokens(user);

        String token = createAndSavePasswordResetToken(user);
        String resetUrl = buildPasswordResetUrl(token);

        sendPasswordResetEmail(user, resetUrl);
    }

    @Override
    public void completePasswordReset(CompletePasswordResetRequestDto request) {
        UserActivationToken resetToken = userActivationTokenRepository
                .findByTokenAndType(request.getToken(), UserTokenType.PASSWORD_RESET)
                .orElseThrow(() -> new ResourceNotFoundException("Token de recuperación no encontrado"));

        if (Boolean.TRUE.equals(resetToken.getUsed())) {
            throw new BadRequestException("El token de recuperación ya fue utilizado");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("El token de recuperación ha expirado");
        }

        User user = resetToken.getUser();

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDebeCambiarPassword(false);

        userRepository.save(user);

        resetToken.setUsed(true);
        userActivationTokenRepository.save(resetToken);
    }

    @Override
    public void invalidatePreviousPasswordResetTokens(User user) {
        List<UserActivationToken> activeTokens = userActivationTokenRepository
                .findByUserAndUsedFalseAndType(user, UserTokenType.PASSWORD_RESET);

        for (UserActivationToken token : activeTokens) {
            token.setUsed(true);
        }

        userActivationTokenRepository.saveAll(activeTokens);
    }
    @Override
    public String createAndSavePasswordResetToken(User user) {
        return createAndSaveToken(user, UserTokenType.PASSWORD_RESET);
    }

    @Override
    public String buildPasswordResetUrl(String token) {
        return frontendPasswordResetUrl + "?token=" + token;
    }

    @Override
    public void sendPasswordResetEmail(User user, String resetUrl) {
        String subject = "Recuperación de contraseña";

        String body = "Hola " + user.getNombre() + ",\n\n"
                + "Recibimos una solicitud para restablecer tu contraseña.\n\n"
                + "Para continuar, ingresa al siguiente enlace:\n\n"
                + resetUrl + "\n\n"
                + "Este enlace expirará en 24 horas.\n\n"
                + "Si no solicitaste este cambio, puedes ignorar este mensaje.\n\n"
                + "Atentamente,\n"
                + "Equipo de soporte";

        try {
            emailService.sendEmail(user.getCorreo(), subject, body);
        } catch (Exception e) {
            throw new EmailSendException("No se pudo enviar el correo de recuperación de contraseña");
        }
    }
    private String createAndSaveToken(User user, UserTokenType type) {
        String token = UUID.randomUUID().toString();

        UserActivationToken activationToken = new UserActivationToken();
        activationToken.setUser(user);
        activationToken.setToken(token);
        activationToken.setExpiresAt(LocalDateTime.now().plusHours(24));
        activationToken.setUsed(false);
        activationToken.setType(type);

        userActivationTokenRepository.save(activationToken);

        return token;
    }
}