package com.sistemadeoperaciones.usuarios.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserActivationService userActivationService;
    private final CommercialPartnerSettingsRepository commercialPartnerSettingsRepository;

    public UserManagementServiceImpl(UserRepository userRepository,
                                     RoleRepository roleRepository,
                                     UserActivationService userActivationService,
                                     CommercialPartnerSettingsRepository commercialPartnerSettingsRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userActivationService = userActivationService;
        this.commercialPartnerSettingsRepository = commercialPartnerSettingsRepository;
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

        User user = new User();
        user.setNombre(request.getNombre().trim());
        user.setCorreo(correoNormalizado);
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

            CommercialPartnerSettings settings = new CommercialPartnerSettings();
            settings.setUsuario(savedUser);
            settings.setBanco(request.getBanco());
            settings.setCuentaBancaria(request.getCuentaBancaria());
            settings.setTitularCuenta(request.getTitularCuenta());
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
    public void resendActivationEmail(Long userId) {
        userActivationService.resendActivationEmail(userId);
    }

    @Override
    public void updateUserEmail(Long userId, UpdateUserEmailRequestDto request) {
        userActivationService.updateUserEmailAndResendActivation(userId, request);
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

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Rol no encontrado con id: "
                                        + request.getRoleId()
                        ));

        user.setNombre(request.getNombre().trim());

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

        return mapToResponse(updatedUser);
    }

    @Override
    public UserResponseDto deactivate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        if (!Boolean.TRUE.equals(user.getActivo())) {
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

        if (Boolean.TRUE.equals(user.getActivo())) {
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
                            settings.getUpdatedAt()
                    ))
                    .orElse(null);
        }

        return new UserResponseDto(
                user.getId(),
                user.getNombre(),
                user.getCorreo(),
                user.getActivo(),
                roleId,
                roleName,
                user.getCorreoVerificado(),
                user.getDebeCambiarPassword(),
                commercialSettings
        );
    }
}