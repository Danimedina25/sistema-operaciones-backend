package com.sistemadeoperaciones.usuarios.service.comercialpartners;

import com.sistemadeoperaciones.shared.enums.RoleName;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;
import com.sistemadeoperaciones.usuarios.dto.request.UpdateCommercialPartnerSettingsRequestDto;
import com.sistemadeoperaciones.usuarios.dto.response.CommercialPartnerSettingsResponseDto;
import com.sistemadeoperaciones.usuarios.model.CommercialPartnerSettings;
import com.sistemadeoperaciones.usuarios.model.Role;
import com.sistemadeoperaciones.usuarios.model.User;
import com.sistemadeoperaciones.usuarios.repository.CommercialPartnerSettingsRepository;
import com.sistemadeoperaciones.usuarios.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommercialPartnerSettingsServiceImpl implements CommercialPartnerSettingsService {

    private final UserRepository userRepository;
    private final CommercialPartnerSettingsRepository commercialPartnerSettingsRepository;

    public CommercialPartnerSettingsServiceImpl(UserRepository userRepository,
                                                CommercialPartnerSettingsRepository commercialPartnerSettingsRepository) {
        this.userRepository = userRepository;
        this.commercialPartnerSettingsRepository = commercialPartnerSettingsRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CommercialPartnerSettingsResponseDto findByUserId(Long userId) {
        CommercialPartnerSettings settings = commercialPartnerSettingsRepository.findByUsuarioId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Configuración comercial no encontrada para el usuario con id: " + userId));

        return mapToResponse(settings);
    }

    @Override
    @Transactional
    public CommercialPartnerSettingsResponseDto updateByUserId(Long userId, UpdateCommercialPartnerSettingsRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + userId));

        boolean isSocioComercial = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.SOCIO_COMERCIAL);

        if (!isSocioComercial) {
            throw new BadRequestException("El usuario no tiene el rol SOCIO_COMERCIAL");
        }

        CommercialPartnerSettings settings = commercialPartnerSettingsRepository.findByUsuarioId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Configuración comercial no encontrada para el usuario con id: " + userId));

        settings.setAppliesToNetwork(request.getAppliesToNetwork());
        settings.setUpdatedBy(null); // luego puedes poner aquí el admin autenticado

        CommercialPartnerSettings updated = commercialPartnerSettingsRepository.save(settings);

        return mapToResponse(updated);
    }

    private CommercialPartnerSettingsResponseDto mapToResponse(CommercialPartnerSettings settings) {
        return new CommercialPartnerSettingsResponseDto(
                settings.getId(),
                settings.getUsuario().getId(),
                settings.getAppliesToNetwork(),
                settings.getCuentaBancaria(),
                settings.getBanco(),
                settings.getTitularCuenta(),
                settings.getCreatedAt(),
                settings.getUpdatedAt()
        );
    }
}