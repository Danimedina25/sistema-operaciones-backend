package com.sistemadeoperaciones.configuraciones.service;

import com.sistemadeoperaciones.configuraciones.dto.ConfiguracionGeneralRequestDTO;
import com.sistemadeoperaciones.configuraciones.dto.ConfiguracionGeneralResponseDto;
import com.sistemadeoperaciones.configuraciones.model.ConfiguracionGeneral;
import com.sistemadeoperaciones.configuraciones.repository.ConfiguracionGeneralRepository;
import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ConfiguracionGeneralServiceImpl implements ConfiguracionGeneralService {

    private final ConfiguracionGeneralRepository configuracionGeneralRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final BusinessAuditService businessAuditService;

    public ConfiguracionGeneralServiceImpl(
            ConfiguracionGeneralRepository configuracionGeneralRepository,
            AuthenticatedUserService authenticatedUserService,
            BusinessAuditService businessAuditService
    ) {
        this.configuracionGeneralRepository = configuracionGeneralRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.businessAuditService = businessAuditService;
    }

    @Override
    @Transactional
    public ConfiguracionGeneral obtener() {
        return configuracionGeneralRepository.findAll()
                .stream()
                .findFirst()
                .orElseGet(() -> configuracionGeneralRepository.save(new ConfiguracionGeneral()));
    }

    @Override
    @Transactional(readOnly = true)
    public ConfiguracionGeneralResponseDto obtenerResponse() {
        return mapToResponse(obtener());
    }

    @Override
    @Transactional
    public ConfiguracionGeneralResponseDto actualizar(ConfiguracionGeneralRequestDTO request) {
        ConfiguracionGeneral configuracion = obtener();
        BigDecimal anterior = configuracion.getPorcentajeComisionOficina();

        configuracion.setPorcentajeComisionOficina(request.getPorcentajeComisionOficina());

        ConfiguracionGeneral actualizada = configuracionGeneralRepository.save(configuracion);
        businessAuditService.record(authenticatedUserService.getCurrentUser().getId(),
                "CONFIGURATION_UPDATE", "CONFIGURATION", actualizada.getId(),
                java.util.Map.of("porcentajeComisionOficina", anterior),
                java.util.Map.of("porcentajeComisionOficina", actualizada.getPorcentajeComisionOficina()));

        return mapToResponse(actualizada);
    }

    private ConfiguracionGeneralResponseDto mapToResponse(ConfiguracionGeneral configuracion) {
        return new ConfiguracionGeneralResponseDto(
                configuracion.getId(),
                configuracion.getPorcentajeComisionOficina(),
                configuracion.getUpdatedAt()
        );
    }
}
