package com.sistemadeoperaciones.usuarios.service.activation;

import com.sistemadeoperaciones.usuarios.model.User;
import com.sistemadeoperaciones.shared.email.EmailService;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;
import com.sistemadeoperaciones.usuarios.dto.request.CompleteUserActivationRequestDto;
import com.sistemadeoperaciones.usuarios.dto.request.UpdateUserEmailRequestDto;
import com.sistemadeoperaciones.usuarios.exceptions.EmailSendException;
import com.sistemadeoperaciones.usuarios.model.UserActivationToken;
import com.sistemadeoperaciones.usuarios.enums.UserTokenType;
import com.sistemadeoperaciones.usuarios.repository.UserActivationTokenRepository;
import com.sistemadeoperaciones.usuarios.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserActivationServiceImpl implements UserActivationService {

    private final UserRepository userRepository;
    private final UserActivationTokenRepository userActivationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final String frontendActivationUrl;
    private final String frontendEmailVerificationUrl;
    private final String frontendPasswordResetUrl;

    public UserActivationServiceImpl(UserRepository userRepository,
                                     UserActivationTokenRepository userActivationTokenRepository,
                                     PasswordEncoder passwordEncoder,
                                     EmailService emailService,
                                     @Value("${app.frontend.activation-url}") String frontendActivationUrl,
                                     @Value("${app.frontend.email-verification-url}") String frontendEmailVerificationUrl,
                                     @Value("${app.frontend.password-reset-url}") String frontendPasswordResetUrl) {
        this.userRepository = userRepository;
        this.userActivationTokenRepository = userActivationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.frontendActivationUrl = frontendActivationUrl;
        this.frontendEmailVerificationUrl = frontendEmailVerificationUrl;
        this.frontendPasswordResetUrl = frontendPasswordResetUrl;
    }

    @Override
    public String createAndSaveActivationToken(User user) {
        return createAndSaveToken(user, UserTokenType.INITIAL_ACTIVATION);
    }

    @Override
    public String createAndSaveEmailVerificationToken(User user) {
        return createAndSaveToken(user, UserTokenType.EMAIL_VERIFICATION);
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

    private String formatRoleName(User user) {
        return user.getRoles().stream()
                .findFirst()
                .map(role -> switch (role.getName()) {
                    case ADMIN -> "Administrador";
                    case GERENTE -> "Gerente";
                    case DIRECCION -> "Director";
                    case SOCIO_COMERCIAL -> "Socio comercial";
                    case JEFA_CUENTAS -> "Jefa de cuentas";
                    case JEFA_CAJAS -> "Jefa de cajas";
                    case AUXILIAR_CUENTAS -> "Auxiliar de cuentas";
                })
                .orElse("Usuario");
    }

    @Override
    public void sendActivationEmail(User user, String activationUrl) {
        String subject = "Activación de cuenta";
        String roleName = formatRoleName(user);

        String body = "Hola " + user.getNombre() + ",\n\n"
                + "Se ha creado una cuenta para ti en el sistema.\n"
                + "Rol asignado: " + roleName + ".\n\n"
                + "Para activar tu cuenta y definir tu contraseña, ingresa al siguiente enlace:\n\n"
                + activationUrl + "\n\n"
                + "Este enlace expirará en 24 horas.\n\n"
                + "Si no reconoces este proceso, puedes ignorar este mensaje.\n\n"
                + "Atentamente,\n"
                + "Equipo de soporte";

        try {
            emailService.sendEmail(user.getCorreo(), subject, body);
        } catch (Exception e) {
            throw new EmailSendException("No se pudo enviar el correo de activación");
        }
    }

    @Override
    public void sendEmailVerificationEmail(User user, String verificationUrl) {
        String subject = "Verificación de nuevo correo";

        String body = "Hola " + user.getNombre() + ",\n\n"
                + "Se solicitó la actualización de tu correo electrónico en el sistema.\n\n"
                + "Para verificar tu nuevo correo, ingresa al siguiente enlace:\n\n"
                + verificationUrl + "\n\n"
                + "Este enlace expirará en 24 horas.\n\n"
                + "Si no reconoces este cambio, comunícate con soporte.\n\n"
                + "Atentamente,\n"
                + "Equipo de soporte";

        try {
            emailService.sendEmail(user.getCorreo(), subject, body);
        } catch (Exception e) {
            throw new EmailSendException("No se pudo enviar el correo de verificación");
        }
    }

    @Override
    public void completeActivation(CompleteUserActivationRequestDto request) {
        UserActivationToken activationToken = userActivationTokenRepository
                .findByTokenAndType(request.getToken(), UserTokenType.INITIAL_ACTIVATION)
                .orElseThrow(() -> new ResourceNotFoundException("Token de activación no encontrado"));

        if (Boolean.TRUE.equals(activationToken.getUsed())) {
            throw new BadRequestException("El token de activación ya fue utilizado");
        }

        if (activationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("El token de activación ha expirado");
        }

        User user = activationToken.getUser();

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setActivo(true);
        user.setDebeCambiarPassword(false);
        user.setCorreoVerificado(true);

        userRepository.save(user);

        activationToken.setUsed(true);
        userActivationTokenRepository.save(activationToken);
    }

    @Override
    public void completeEmailVerification(String token) {
        UserActivationToken verificationToken = userActivationTokenRepository
                .findByTokenAndType(token, UserTokenType.EMAIL_VERIFICATION)
                .orElseThrow(() -> new ResourceNotFoundException("Token de verificación de correo no encontrado"));

        if (Boolean.TRUE.equals(verificationToken.getUsed())) {
            throw new BadRequestException("El token de verificación ya fue utilizado");
        }

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("El token de verificación ha expirado");
        }

        User user = verificationToken.getUser();
        user.setCorreoVerificado(true);

        userRepository.save(user);

        verificationToken.setUsed(true);
        userActivationTokenRepository.save(verificationToken);
    }

    @Override
    public void resendActivationEmail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + userId));

        if (Boolean.TRUE.equals(user.getCorreoVerificado()) && user.getPassword() != null) {
            throw new BadRequestException("El usuario ya activó su cuenta");
        }

        if (user.getCorreo() == null || user.getCorreo().isBlank()) {
            throw new BadRequestException("El usuario no tiene un correo válido registrado");
        }

        invalidatePreviousActivationTokens(user);

        String token = createAndSaveActivationToken(user);
        String activationUrl = buildActivationUrl(token);

        sendActivationEmail(user, activationUrl);
    }

    @Override
    public void updateUserEmailAndResendActivation(Long userId, UpdateUserEmailRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + userId));

        String nuevoCorreo = request.getCorreo().trim().toLowerCase();
        String correoActual = user.getCorreo() != null ? user.getCorreo().trim().toLowerCase() : "";

        if (nuevoCorreo.equals(correoActual)) {
            throw new BadRequestException("El nuevo correo es igual al correo actual");
        }

        if (userRepository.existsByCorreoAndIdNot(nuevoCorreo, userId)) {
            throw new BadRequestException("Ya existe otro usuario con ese correo");
        }

        boolean requiereActivacionInicial =
                user.getPassword() == null || Boolean.TRUE.equals(user.getDebeCambiarPassword());

        user.setCorreo(nuevoCorreo);
        user.setCorreoVerificado(false);

        if (requiereActivacionInicial) {
            user.setActivo(false);
            user.setDebeCambiarPassword(true);

            userRepository.save(user);

            invalidatePreviousActivationTokens(user);

            String token = createAndSaveActivationToken(user);
            String activationUrl = buildActivationUrl(token);

            sendActivationEmail(user, activationUrl);
            return;
        }

        // Usuario ya activo: solo debe verificar el nuevo correo
        userRepository.save(user);

        invalidatePreviousEmailVerificationTokens(user);

        String token = createAndSaveEmailVerificationToken(user);
        String verificationUrl = buildEmailVerificationUrl(token);

        sendEmailVerificationEmail(user, verificationUrl);
    }

    private void invalidatePreviousActivationTokens(User user) {
        List<UserActivationToken> activeTokens = userActivationTokenRepository
                .findByUserAndUsedFalseAndType(user, UserTokenType.INITIAL_ACTIVATION);

        for (UserActivationToken token : activeTokens) {
            token.setUsed(true);
        }

        userActivationTokenRepository.saveAll(activeTokens);
    }

    @Override
    public void invalidatePreviousEmailVerificationTokens(User user) {
        List<UserActivationToken> activeTokens = userActivationTokenRepository
                .findByUserAndUsedFalseAndType(user, UserTokenType.EMAIL_VERIFICATION);

        for (UserActivationToken token : activeTokens) {
            token.setUsed(true);
        }

        userActivationTokenRepository.saveAll(activeTokens);
    }

    @Override
    public String buildActivationUrl(String token) {
        return frontendActivationUrl + "?token=" + token;
    }

    @Override
    public String buildEmailVerificationUrl(String token) {
        return frontendEmailVerificationUrl + "?token=" + token;
    }
}