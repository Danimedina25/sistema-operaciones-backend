package com.sistemadeoperaciones.configuraciones.service;

import com.sistemadeoperaciones.configuraciones.dto.ConfiguracionGeneralRequestDTO;
import com.sistemadeoperaciones.configuraciones.dto.ConfiguracionGeneralResponseDto;
import com.sistemadeoperaciones.configuraciones.dto.PendingUrgencyThresholdsDto;
import com.sistemadeoperaciones.shared.exception.BusinessException;
import com.sistemadeoperaciones.configuraciones.model.ConfiguracionGeneral;
import com.sistemadeoperaciones.configuraciones.repository.ConfiguracionGeneralRepository;
import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

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
    public PendingUrgencyThresholdsDto umbralesPendientes() {
        ConfiguracionGeneral configuracion = obtener();
        return new PendingUrgencyThresholdsDto(
                configuracion.getHorasPendienteAtencion(),
                configuracion.getHorasPendienteUrgente()
        );
    }

    @Override
    @Transactional
    public ConfiguracionGeneralResponseDto actualizar(ConfiguracionGeneralRequestDTO request) {
        ConfiguracionGeneral configuracion = obtener();
        Map<String, Object> anterior = snapshot(configuracion);

        Integer atencion = request.getHorasPendienteAtencion() != null
                ? request.getHorasPendienteAtencion() : configuracion.getHorasPendienteAtencion();
        Integer urgente = request.getHorasPendienteUrgente() != null
                ? request.getHorasPendienteUrgente() : configuracion.getHorasPendienteUrgente();
        if (atencion >= urgente) {
            throw new BusinessException("Las horas para urgente deben ser mayores que las de atender pronto");
        }

        configuracion.setPorcentajeComisionOficina(request.getPorcentajeComisionOficina());
        configuracion.setHorasPendienteAtencion(atencion);
        configuracion.setHorasPendienteUrgente(urgente);

        ConfiguracionGeneral actualizada = configuracionGeneralRepository.save(configuracion);
        businessAuditService.record(authenticatedUserService.getCurrentUser().getId(),
                "CONFIGURATION_UPDATE", "CONFIGURATION", actualizada.getId(),
                anterior, snapshot(actualizada));

        return mapToResponse(actualizada);
    }

    private static Map<String, Object> snapshot(ConfiguracionGeneral configuracion) {
        return Map.of(
                "porcentajeComisionOficina", configuracion.getPorcentajeComisionOficina(),
                "horasPendienteAtencion", configuracion.getHorasPendienteAtencion(),
                "horasPendienteUrgente", configuracion.getHorasPendienteUrgente()
        );
    }

    private ConfiguracionGeneralResponseDto mapToResponse(ConfiguracionGeneral configuracion) {
        return new ConfiguracionGeneralResponseDto(
                configuracion.getId(),
                configuracion.getPorcentajeComisionOficina(),
                configuracion.getHorasPendienteAtencion(),
                configuracion.getHorasPendienteUrgente(),
                configuracion.getUpdatedAt()
        );
    }
}
