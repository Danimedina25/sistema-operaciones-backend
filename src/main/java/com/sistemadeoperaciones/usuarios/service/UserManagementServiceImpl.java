package com.sistemadeoperaciones.usuarios.service;

import com.sistemadeoperaciones.shared.audit.service.DeletionAuditService;
import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.enums.RoleName;
import com.sistemadeoperaciones.usuarios.dto.response.CommercialPartnerSettingsResponseDto;
import com.sistemadeoperaciones.usuarios.exceptions.*;
import com.sistemadeoperaciones.usuarios.model.CommercialPartnerSettings;
import com.sistemadeoperaciones.usuarios.model.Role;
import com.sistemadeoperaciones.usuarios.model.User;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;
import com.sistemadeoperaciones.usuarios.dto.request.*;
import com.sistemadeoperaciones.usuarios.dto.response.UserCreatedResponseDto;
import com.sistemadeoperaciones.usuarios.dto.response.UserResponseDto;
import com.sistemadeoperaciones.usuarios.repository.CommercialPartnerSettingsRepository;
import com.sistemadeoperaciones.usuarios.repository.RoleRepository;
import com.sistemadeoperaciones.usuarios.repository.UserRepository;
import com.sistemadeoperaciones.usuarios.service.activation.UserActivationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserActivationService userActivationService;
    private final CommercialPartnerSettingsRepository commercialPartnerSettingsRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final UserDeletionGuard userDeletionGuard;
    private final UserDeletionDependencyCleaner userDeletionDependencyCleaner;
    private final DeletionAuditService deletionAuditService;
    private final BusinessAuditService businessAuditService;

    public UserManagementServiceImpl(UserRepository userRepository,
                                     RoleRepository roleRepository,
                                     UserActivationService userActivationService,
                                     CommercialPartnerSettingsRepository commercialPartnerSettingsRepository,
                                     AuthenticatedUserService authenticatedUserService,
                                     UserDeletionGuard userDeletionGuard,
                                     UserDeletionDependencyCleaner userDeletionDependencyCleaner,
                                     DeletionAuditService deletionAuditService,
                                     BusinessAuditService businessAuditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userActivationService = userActivationService;
        this.commercialPartnerSettingsRepository = commercialPartnerSettingsRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.userDeletionGuard = userDeletionGuard;
        this.userDeletionDependencyCleaner = userDeletionDependencyCleaner;
        this.deletionAuditService = deletionAuditService;
        this.businessAuditService = businessAuditService;
    }

    @Override
    @Transactional
    public UserCreatedResponseDto create(CreateUserRequestDto request) {
        String correoNormalizado = request.getCorreo().trim().toLowerCase();

        if (userRepository.existsByCorreo(correoNormalizado)) {
            throw new BadRequestException("Ya existe un usuario con ese correo");
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con id: " + request.getRoleId()));

        if (requiresTelefono(role.getName())
                && (request.getTelefono() == null || request.getTelefono().isBlank())) {
            throw new TelefonoRequiredException();
        }

        User user = new User();
        user.setNombre(request.getNombre().trim());
        user.setCorreo(correoNormalizado);
        user.setTelefono(request.getTelefono() != null ? request.getTelefono().trim() : null);
        user.setPassword(null);
        user.setActivo(false);
        user.setDebeCambiarPassword(true);
        user.setCorreoVerificado(false);

        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);

        // Si es socio comercial, crear su configuración de comisión
        if (role.getName() == RoleName.SOCIO_COMERCIAL) {
            if (commercialPartnerSettingsRepository.existsByUsuarioId(savedUser.getId())) {
                throw new CommercialPartnerSettingsAlreadyExistsException();
            }

            if (request.getCuentaBancaria() == null
                    || request.getCuentaBancaria().isBlank()) {
                throw new CuentaBancariaRequiredException();
            }

            if (request.getBanco() == null
                    || request.getBanco().isBlank()) {
                throw new BancoRequiredException();
            }

            if (request.getTitularCuenta() == null
                    || request.getTitularCuenta().isBlank()) {
                throw new TitularCuentaRequiredException();
            }

            if (request.getPorcentajeComision() == null) {
                throw new BadRequestException(
                        "El porcentaje de comisión es obligatorio"
                );
            }

            CommercialPartnerSettings settings = new CommercialPartnerSettings();
            settings.setUsuario(savedUser);
            settings.setBanco(request.getBanco());
            settings.setCuentaBancaria(request.getCuentaBancaria());
            settings.setTitularCuenta(request.getTitularCuenta());
            settings.setPorcentajeComision(request.getPorcentajeComision());
            settings.setAppliesToNetwork(
                    request.getAppliesToNetwork() != null ? request.getAppliesToNetwork() : true
            );

            // Aquí puedes poner el usuario admin autenticado si luego lo tienes disponible
            settings.setUpdatedBy(null);

            commercialPartnerSettingsRepository.save(settings);
        }

        String token = userActivationService.createAndSaveActivationToken(savedUser);
        String activationUrl = userActivationService.buildActivationUrl(token);

        try {
            userActivationService.sendActivationEmail(savedUser, activationUrl);
        } catch (Exception e) {
            throw new EmailSendException("El usuario fue creado correctamente, pero no se pudo enviar el correo de activación");
        }

        auditUserChange("USER_CREATE", savedUser.getId(), null, auditSnapshot(savedUser));

        return new UserCreatedResponseDto(
                savedUser.getId(),
                savedUser.getNombre(),
                savedUser.getCorreo(),
                savedUser.getActivo(),
                role.getName().name(),
                savedUser.getDebeCambiarPassword(),
                savedUser.getCorreoVerificado(),
                activationUrl
        );
    }

    @Override
    public void completeUserActivation(CompleteUserActivationRequestDto request) {
        userActivationService.completeActivation(request);
    }

    @Override
    public void completeEmailVerification(CompleteEmailVerificationRequestDto request) {
        userActivationService.completeEmailVerification(request.getToken());
    }

    @Override
    @Transactional
    public void resendActivationEmail(Long userId) {
        userActivationService.resendActivationEmail(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + userId));
        auditUserChange("USER_RESEND_ACTIVATION", userId, auditSnapshot(user), auditSnapshot(user));
    }

    @Override
    @Transactional
    public void updateUserEmail(Long userId, UpdateUserEmailRequestDto request) {
        User beforeUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + userId));
        Map<String, Object> before = auditSnapshot(beforeUser);
        userActivationService.updateUserEmailAndResendActivation(userId, request);
        User afterUser = userRepository.findById(userId).orElseThrow();
        auditUserChange("USER_EMAIL_UPDATE", userId, before, auditSnapshot(afterUser));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> findAll() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        return mapToResponse(user);
    }

    @Override
    @Transactional
    public UserResponseDto update(Long id, UpdateUserRequestDto request) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Usuario no encontrado con id: " + id
                        ));
        Map<String, Object> before = auditSnapshot(user);

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Rol no encontrado con id: "
                                        + request.getRoleId()
                        ));

        if (requiresTelefono(role.getName())
                && (request.getTelefono() == null || request.getTelefono().isBlank())) {
            throw new TelefonoRequiredException();
        }

        user.setNombre(request.getNombre().trim());
        user.setTelefono(request.getTelefono() != null ? request.getTelefono().trim() : null);

        if (request.getActivo() != null) {
            user.setActivo(request.getActivo());
        }

        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);

        User updatedUser = userRepository.save(user);

        boolean isSocioComercial =
                role.getName() == RoleName.SOCIO_COMERCIAL;

        Optional<CommercialPartnerSettings> existingSettingsOpt =
                commercialPartnerSettingsRepository.findByUsuarioId(
                        updatedUser.getId()
                );

        if (isSocioComercial) {
            if (request.getCuentaBancaria() == null
                    || request.getCuentaBancaria().isBlank()) {
                throw new CuentaBancariaRequiredException();
            }

            if (request.getBanco() == null
                    || request.getBanco().isBlank()) {
                throw new BancoRequiredException();
            }

            if (request.getTitularCuenta() == null
                    || request.getTitularCuenta().isBlank()) {
                throw new TitularCuentaRequiredException();
            }

            if (request.getPorcentajeComision() == null) {
                throw new BadRequestException(
                        "El porcentaje de comisión es obligatorio"
                );
            }

            if (existingSettingsOpt.isPresent()) {

                CommercialPartnerSettings settings =
                        existingSettingsOpt.get();

                settings.setAppliesToNetwork(
                        request.getAppliesToNetwork() != null
                                ? request.getAppliesToNetwork()
                                : true
                );

                settings.setCuentaBancaria(
                        request.getCuentaBancaria().trim()
                );

                settings.setBanco(
                        request.getBanco().trim()
                );

                settings.setTitularCuenta(
                        request.getTitularCuenta().trim()
                );

                settings.setPorcentajeComision(
                        request.getPorcentajeComision()
                );

                settings.setUpdatedBy(null);

                commercialPartnerSettingsRepository.save(
                        settings
                );

            } else {

                CommercialPartnerSettings settings =
                        new CommercialPartnerSettings();

                settings.setUsuario(updatedUser);

                settings.setAppliesToNetwork(
                        request.getAppliesToNetwork() != null
                                ? request.getAppliesToNetwork()
                                : true
                );

                settings.setCuentaBancaria(
                        request.getCuentaBancaria().trim()
                );

                settings.setBanco(
                        request.getBanco().trim()
                );

                settings.setTitularCuenta(
                        request.getTitularCuenta().trim()
                );

                settings.setPorcentajeComision(
                        request.getPorcentajeComision()
                );

                settings.setUpdatedBy(null);

                commercialPartnerSettingsRepository.save(
                        settings
                );
            }

        } else {

            existingSettingsOpt.ifPresent(
                    commercialPartnerSettingsRepository::delete
            );
        }

        auditUserChange("USER_UPDATE", id, before, auditSnapshot(updatedUser));
        return mapToResponse(updatedUser);
    }

    @Override
    @Transactional
    public UserResponseDto deactivate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        Map<String, Object> before = auditSnapshot(user);

        if (!Boolean.TRUE.equals(user.getActivo())) {
            throw new BadRequestException("El usuario ya se encuentra inactivo");
        }

        user.setActivo(false);

        User updated = userRepository.save(user);
        auditUserChange("USER_DEACTIVATE", id, before, auditSnapshot(updated));
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public UserResponseDto activate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        Map<String, Object> before = auditSnapshot(user);

        if (Boolean.TRUE.equals(user.getActivo())) {
            throw new BadRequestException("El usuario ya se encuentra activo");
        }

        user.setActivo(true);

        User updated = userRepository.save(user);
        auditUserChange("USER_ACTIVATE", id, before, auditSnapshot(updated));
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User currentUser = authenticatedUserService.getCurrentUser();

        User target = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        Map<String, Object> before = auditSnapshot(target);

        userDeletionGuard.assertCanDelete(currentUser, target);

        deletionAuditService.record("USER", target.getId(), target.getNombre() + " (" + target.getCorreo() + ")", currentUser);
        userDeletionDependencyCleaner.clean(target.getId(), currentUser.getId());

        try {
            userRepository.delete(target);
            userRepository.flush();
            auditUserChange("USER_DELETE", id, before, null);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("No se puede eliminar el usuario porque tiene información relacionada");
        }
    }

    private void auditUserChange(String action, Long id, Object before, Object after) {
        businessAuditService.record(authenticatedUserService.getCurrentUser().getId(), action, "USER", id, before, after);
    }

    /** Explicit allowlist: never serialize User, passwords, activation tokens or full accounts. */
    private Map<String, Object> auditSnapshot(User user) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("nombre", user.getNombre());
        value.put("correo", user.getCorreo());
        value.put("telefono", user.getTelefono());
        value.put("activo", user.getActivo());
        value.put("correoVerificado", user.getCorreoVerificado());
        value.put("roles", user.getRoles().stream().map(role -> role.getName().name()).sorted().toList());
        commercialPartnerSettingsRepository.findByUsuarioId(user.getId()).ifPresent(settings -> {
            value.put("bancoComision", settings.getBanco());
            String account = settings.getCuentaBancaria();
            value.put("cuentaComision", account == null ? null : "••" + account.substring(Math.max(0, account.length() - 4)));
            value.put("porcentajeComision", settings.getPorcentajeComision());
        });
        return value;
    }

    private UserResponseDto mapToResponse(User user) {
        Role role = user.getRoles().stream().findFirst().orElse(null);

        Long roleId = role != null ? role.getId() : null;
        String roleName = role != null ? role.getName().name() : null;

        CommercialPartnerSettingsResponseDto commercialSettings = null;

        boolean isSocioComercial = role != null && role.getName() == RoleName.SOCIO_COMERCIAL;

        if (isSocioComercial) {
            commercialSettings = commercialPartnerSettingsRepository.findByUsuarioId(user.getId())
                    .map(settings -> new CommercialPartnerSettingsResponseDto(
                            settings.getId(),
                            settings.getUsuario().getId(),
                            settings.getAppliesToNetwork(),
                            settings.getCuentaBancaria(),
                            settings.getBanco(),
                            settings.getTitularCuenta(),
                            settings.getCreatedAt(),
                            settings.getUpdatedAt(),
                            settings.getPorcentajeComision()
                    ))
                    .orElse(null);
        }

        return new UserResponseDto(
                user.getId(),
                user.getNombre(),
                user.getCorreo(),
                user.getTelefono(),
                user.getActivo(),
                roleId,
                roleName,
                user.getCorreoVerificado(),
                user.getDebeCambiarPassword(),
                commercialSettings
        );
    }

    /**
     * Roles que necesitan teléfono registrado para poder enviarles avisos
     * por WhatsApp (retornos en efectivo programados, comisiones pagadas, etc.).
     */
    private boolean requiresTelefono(RoleName roleName) {
        return roleName == RoleName.SOCIO_COMERCIAL
                || roleName == RoleName.JEFA_CAJAS
                || roleName == RoleName.JEFA_CUENTAS
                || roleName == RoleName.AUXILIAR_CUENTAS;
    }
}
