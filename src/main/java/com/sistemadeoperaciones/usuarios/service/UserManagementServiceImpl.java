package com.sistemadeoperaciones.usuarios.service;

import com.sistemadeoperaciones.auth.models.Role;
import com.sistemadeoperaciones.auth.models.User;
import com.sistemadeoperaciones.shared.email.EmailService;
import com.sistemadeoperaciones.shared.enums.RoleName;
import com.sistemadeoperaciones.usuarios.dto.*;
import com.sistemadeoperaciones.usuarios.model.UserActivationToken;
import com.sistemadeoperaciones.usuarios.repository.RoleRepository;
import com.sistemadeoperaciones.usuarios.repository.UserActivationTokenRepository;
import com.sistemadeoperaciones.usuarios.repository.UserRepository;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserActivationTokenRepository userActivationTokenRepository;
    private final EmailService emailService;
    private final String frontendActivationUrl;

    public UserManagementServiceImpl(UserRepository userRepository,
                                     RoleRepository roleRepository,
                                     PasswordEncoder passwordEncoder,
                                     UserActivationTokenRepository userActivationTokenRepository,
                                     EmailService emailService,
                                     @Value("${app.frontend.activation-url}") String frontendActivationUrl) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userActivationTokenRepository = userActivationTokenRepository;
        this.emailService = emailService;
        this.frontendActivationUrl = frontendActivationUrl;
    }

    @Override
    public UserResponseDto create(CreateUserRequestDto request) {
        if (userRepository.existsByCorreo(request.getCorreo())) {
            throw new BadRequestException("Ya existe un usuario con ese correo");
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con id: " + request.getRoleId()));

        User user = new User();
        user.setNombre(request.getNombre());
        user.setCorreo(request.getCorreo());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setActivo(request.getActivo() != null ? request.getActivo() : true);
        user.setRoles(Set.of(role));

        User saved = userRepository.save(user);
        return mapToResponse(saved);
    }

    @Override
    public SocioComercialCreatedResponseDto createSocioComercial(CreateSocioComercialRequestDto request) {
        String correoNormalizado = request.getCorreo().trim().toLowerCase();

        if (userRepository.existsByCorreo(correoNormalizado)) {
            throw new BadRequestException("Ya existe un usuario con ese correo");
        }

        Role role = roleRepository.findByName(RoleName.SOCIO_COMERCIAL)
                .orElseThrow(() -> new ResourceNotFoundException("Rol SOCIO_COMERCIAL no encontrado"));

        User user = new User();
        user.setNombre(request.getNombre().trim());
        user.setCorreo(correoNormalizado);
        user.setPassword(null);
        user.setActivo(false);
        user.setDebeCambiarPassword(true);
        user.setCorreoVerificado(false);
        user.setRoles(Set.of(role));

        User savedUser = userRepository.save(user);

        String token = createAndSaveActivationToken(savedUser);
        String activationUrl = frontendActivationUrl + "?token=" + token;

        sendActivationEmail(savedUser.getCorreo(), savedUser.getNombre(), activationUrl);

        return new SocioComercialCreatedResponseDto(
                savedUser.getId(),
                savedUser.getNombre(),
                savedUser.getCorreo(),
                savedUser.getActivo(),
                role.getName().name(),
                activationUrl,
                savedUser.getDebeCambiarPassword(),
                savedUser.getCorreoVerificado()
        );
    }
    @Override
    public void completeSocioComercialActivation(CompleteSocioComercialActivationRequestDto request) {
        UserActivationToken activationToken = userActivationTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ResourceNotFoundException("Token de activación no encontrado"));

        if (Boolean.TRUE.equals(activationToken.getUsed())) {
            throw new BadRequestException("El token de activación ya fue utilizado");
        }

        if (activationToken.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
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
    public List<UserResponseDto> findAll() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public UserResponseDto findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        return mapToResponse(user);
    }

    @Override
    public UserResponseDto update(Long id, UpdateUserRequestDto request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        if (userRepository.existsByCorreoAndIdNot(request.getCorreo(), id)) {
            throw new BadRequestException("Ya existe otro usuario con ese correo");
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con id: " + request.getRoleId()));

        user.setNombre(request.getNombre());
        user.setCorreo(request.getCorreo());

        if (request.getActivo() != null) {
            user.setActivo(request.getActivo());
        }

        user.setRoles(Set.of(role));

        User updated = userRepository.save(user);
        return mapToResponse(updated);
    }

    @Override
    public UserResponseDto deactivate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        if (!user.getActivo()) {
            throw new BadRequestException("El usuario ya se encuentra inactivo");
        }

        user.setActivo(false);

        User updated = userRepository.save(user);
        return mapToResponse(updated);
    }

    @Override
    public UserResponseDto activate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        if (user.getActivo()) {
            throw new BadRequestException("El usuario ya se encuentra activo");
        }

        user.setActivo(true);

        User updated = userRepository.save(user);
        return mapToResponse(updated);
    }

    private UserResponseDto mapToResponse(User user) {
        Role role = user.getRoles().stream().findFirst().orElse(null);

        Long roleId = role != null ? role.getId() : null;
        String roleName = role != null ? role.getName().name() : null;

        return new UserResponseDto(
                user.getId(),
                user.getNombre(),
                user.getCorreo(),
                user.getActivo(),
                roleId,
                roleName
        );
    }

    private String generateActivationToken() {
        return java.util.UUID.randomUUID().toString();
    }
    private String createAndSaveActivationToken(User user) {
        String token = generateActivationToken();

        UserActivationToken activationToken = new UserActivationToken();
        activationToken.setUser(user);
        activationToken.setToken(token);
        activationToken.setExpiresAt(java.time.LocalDateTime.now().plusHours(24));
        activationToken.setUsed(false);

        userActivationTokenRepository.save(activationToken);

        return token;
    }

    private void sendActivationEmail(String correo, String nombre, String activationUrl) {
        String subject = "Activa tu cuenta de socio comercial";

        String body = "Hola " + nombre + ",\n\n"
                + "Tu cuenta de socio comercial fue creada exitosamente.\n"
                + "Para completar tu registro y definir tu contraseña, ingresa al siguiente enlace:\n\n"
                + activationUrl + "\n\n"
                + "Este enlace expirará en 24 horas.\n\n"
                + "Si no reconoces este proceso, puedes ignorar este mensaje.";

        emailService.sendEmail(correo, subject, body);
    }
}