package com.sistemadeoperaciones.socioscomerciales.service;

import com.sistemadeoperaciones.usuarios.model.User;
import com.sistemadeoperaciones.shared.email.EmailService;
import com.sistemadeoperaciones.shared.enums.RoleName;
import com.sistemadeoperaciones.socioscomerciales.dto.*;
import com.sistemadeoperaciones.socioscomerciales.models.CommercialPartner;
import com.sistemadeoperaciones.socioscomerciales.repository.CommercialPartnerRepository;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;
import com.sistemadeoperaciones.usuarios.model.UserActivationToken;
import com.sistemadeoperaciones.usuarios.repository.UserActivationTokenRepository;
import com.sistemadeoperaciones.usuarios.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import com.sistemadeoperaciones.socioscomerciales.specification.CommercialPartnerSpecification;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

@Service
public class CommercialPartnerServiceImpl implements CommercialPartnerService {

    private final CommercialPartnerRepository commercialPartnerRepository;
    private final UserRepository userRepository;
    private final UserActivationTokenRepository userActivationTokenRepository;
    private final EmailService emailService;
    private final String frontendActivationUrl;

    public CommercialPartnerServiceImpl(CommercialPartnerRepository commercialPartnerRepository,
                                        UserRepository userRepository, UserActivationTokenRepository userActivationTokenRepository,
                                        EmailService emailService,
                                        @Value("${app.frontend.activation-url}") String frontendActivationUrl) {
        this.commercialPartnerRepository = commercialPartnerRepository;
        this.userRepository = userRepository;
        this.userActivationTokenRepository = userActivationTokenRepository;
        this.emailService = emailService;
        this.frontendActivationUrl = frontendActivationUrl;
    }

    @Override
    public CommercialPartnerResponseDto create(CommercialPartnerRequestDto request) {
        if (commercialPartnerRepository.existsByCorreo(request.getCorreo())) {
            throw new BadRequestException("Ya existe un socio comercial con ese correo");
        }

        CommercialPartner socioPadre = null;
        if (request.getSocioPadreId() != null) {
            socioPadre = commercialPartnerRepository.findById(request.getSocioPadreId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Socio padre no encontrado con id: " + request.getSocioPadreId()));
        }

        CommercialPartner partner = new CommercialPartner();
        partner.setNombre(request.getNombre());
        partner.setCorreo(request.getCorreo());
        partner.setTelefono(request.getTelefono());
        partner.setActivo(request.getActivo() != null ? request.getActivo() : true);
        partner.setSocioPadre(socioPadre);

        CommercialPartner saved = commercialPartnerRepository.save(partner);
        return mapToResponse(saved);
    }

    @Override
    public Page<CommercialPartnerResponseDto> findAll(
            int page,
            int size,
            String sortBy,
            String sortDir,
            Boolean activo,
            String nombre,
            String correo,
            Long socioPadreId
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<CommercialPartner> specification =
                Specification.where(CommercialPartnerSpecification.hasActivo(activo))
                        .and(CommercialPartnerSpecification.hasNombre(nombre))
                        .and(CommercialPartnerSpecification.hasCorreo(correo))
                        .and(CommercialPartnerSpecification.hasSocioPadreId(socioPadreId));

        return commercialPartnerRepository.findAll(specification, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public CommercialPartnerResponseDto findById(Long id) {
        CommercialPartner partner = commercialPartnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Socio comercial no encontrado con id: " + id));

        return mapToResponse(partner);
    }

    @Override
    public CommercialPartnerResponseDto update(Long id, CommercialPartnerRequestDto request) {
        CommercialPartner partner = commercialPartnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Socio comercial no encontrado con id: " + id));

        if (commercialPartnerRepository.existsByCorreoAndIdNot(request.getCorreo(), id)) {
            throw new BadRequestException("Ya existe otro socio comercial con ese correo");
        }

        CommercialPartner socioPadre = null;
        if (request.getSocioPadreId() != null) {
            if (request.getSocioPadreId().equals(id)) {
                throw new BadRequestException("Un socio comercial no puede ser su propio padre");
            }

            socioPadre = commercialPartnerRepository.findById(request.getSocioPadreId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Socio padre no encontrado con id: " + request.getSocioPadreId()));

            validateHierarchyCycle(partner, socioPadre);
        }

        partner.setNombre(request.getNombre());
        partner.setCorreo(request.getCorreo());
        partner.setTelefono(request.getTelefono());
        partner.setActivo(request.getActivo() != null ? request.getActivo() : partner.getActivo());
        partner.setSocioPadre(socioPadre);

        CommercialPartner updated = commercialPartnerRepository.save(partner);
        return mapToResponse(updated);
    }

    @Override
    public CommercialPartnerNetworkDto getNetworkById(Long id) {
        CommercialPartner partner = commercialPartnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Socio comercial no encontrado con id: " + id));

        return mapToNetwork(partner);
    }

    @Override
    public CommercialPartnerResponseDto deactivate(Long id) {
        CommercialPartner commercialPartner = commercialPartnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Socio comerial no encontrado con id: " + id));

        if (!commercialPartner.getActivo()) {
            throw new BadRequestException("El socio comercial ya se encuentra inactivo");
        }

        commercialPartner.setActivo(false);

        CommercialPartner updated = commercialPartnerRepository.save(commercialPartner);
        return mapToResponse(updated);
    }

    @Override
    public CommercialPartnerResponseDto activate(Long id) {
        CommercialPartner commercialPartner = commercialPartnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Socio comerial no encontrado con id: " + id));

        if (commercialPartner.getActivo()) {
            throw new BadRequestException("El socio comercial ya se encuentra activo");
        }

        commercialPartner.setActivo(true);

        CommercialPartner updated = commercialPartnerRepository.save(commercialPartner);
        return mapToResponse(updated);
    }

    private CommercialPartnerResponseDto mapToResponse(CommercialPartner partner) {
        Long socioPadreId = null;
        String socioPadreNombre = null;

        if (partner.getSocioPadre() != null) {
            socioPadreId = partner.getSocioPadre().getId();
            socioPadreNombre = partner.getSocioPadre().getNombre();
        }

        return new CommercialPartnerResponseDto(
                partner.getId(),
                partner.getNombre(),
                partner.getCorreo(),
                partner.getTelefono(),
                partner.getActivo(),
                socioPadreId,
                socioPadreNombre
        );
    }

    private CommercialPartnerNetworkDto mapToNetwork(CommercialPartner partner) {
        CommercialPartnerNetworkDto dto = new CommercialPartnerNetworkDto(
                partner.getId(),
                partner.getNombre(),
                partner.getCorreo(),
                partner.getTelefono(),
                partner.getActivo()
        );

        List<CommercialPartnerNetworkDto> hijosDto = partner.getHijos()
                .stream()
                .map(this::mapToNetwork)
                .toList();

        dto.setHijos(hijosDto);
        return dto;
    }

    private void validateHierarchyCycle(CommercialPartner currentPartner, CommercialPartner proposedParent) {
        CommercialPartner current = proposedParent;

        while (current != null) {
            if (current.getId().equals(currentPartner.getId())) {
                throw new BadRequestException("No se puede asignar ese socio padre porque genera un ciclo jerárquico en la red");
            }
            current = current.getSocioPadre();
        }
    }

    @Override
    public SocioComercialEmailUpdatedResponseDto updateSocioComercialEmail(Long id, UpdateSocioComercialEmailRequestDto request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        validateSocioComercial(user);

        String nuevoCorreo = request.getCorreo().trim().toLowerCase();
        String correoActual = user.getCorreo() != null ? user.getCorreo().trim().toLowerCase() : null;

        if (nuevoCorreo.equals(correoActual)) {
            throw new BadRequestException("El nuevo correo es igual al correo actual");
        }

        if (userRepository.existsByCorreoAndIdNot(nuevoCorreo, id)) {
            throw new BadRequestException("Ya existe otro usuario con ese correo");
        }

        user.setCorreo(nuevoCorreo);
        user.setCorreoVerificado(false);

        // Si todavía no ha terminado activación, mantenemos el flujo pendiente
        if (user.getPassword() == null || Boolean.TRUE.equals(user.getDebeCambiarPassword())) {
            user.setActivo(false);
            user.setDebeCambiarPassword(true);
        }

        User updatedUser = userRepository.save(user);

        invalidatePreviousActivationTokens(updatedUser);

        String token = createAndSaveActivationToken(updatedUser);
        String activationUrl = frontendActivationUrl + "?token=" + token;

        sendActivationEmail(updatedUser.getCorreo(), updatedUser.getNombre(), activationUrl);

        return new SocioComercialEmailUpdatedResponseDto(
                updatedUser.getId(),
                updatedUser.getNombre(),
                updatedUser.getCorreo(),
                updatedUser.getActivo(),
                updatedUser.getCorreoVerificado(),
                updatedUser.getDebeCambiarPassword(),
                activationUrl,
                "Correo actualizado correctamente y email de activación reenviado"
        );
    }
    @Override
    public void resendSocioComercialActivationEmail(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        validateSocioComercial(user);

        if (Boolean.TRUE.equals(user.getCorreoVerificado()) && user.getPassword() != null) {
            throw new BadRequestException("El socio comercial ya activó su cuenta");
        }

        if (user.getCorreo() == null || user.getCorreo().isBlank()) {
            throw new BadRequestException("El usuario no tiene un correo válido registrado");
        }

        invalidatePreviousActivationTokens(user);

        String token = createAndSaveActivationToken(user);
        String activationUrl = frontendActivationUrl + "?token=" + token;

        sendActivationEmail(user.getCorreo(), user.getNombre(), activationUrl);
    }

    private void validateSocioComercial(User user) {
        boolean isSocioComercial = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.SOCIO_COMERCIAL);

        if (!isSocioComercial) {
            throw new BadRequestException("El usuario no corresponde a un socio comercial");
        }
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

    private void invalidatePreviousActivationTokens(User user) {
        List<UserActivationToken> activeTokens = userActivationTokenRepository.findByUserAndUsedFalse(user);

        for (UserActivationToken token : activeTokens) {
            token.setUsed(true);
        }

        userActivationTokenRepository.saveAll(activeTokens);
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

    private String generateActivationToken() {
        return java.util.UUID.randomUUID().toString();
    }

}