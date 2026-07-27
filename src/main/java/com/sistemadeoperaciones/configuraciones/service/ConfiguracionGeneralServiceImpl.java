package com.sistemadeoperaciones.configuraciones.service;

import com.sistemadeoperaciones.configuraciones.dto.ConfiguracionGeneralRequestDTO;
import com.sistemadeoperaciones.configuraciones.dto.ConfiguracionGeneralResponseDto;
import com.sistemadeoperaciones.configuraciones.model.ConfiguracionGeneral;
import com.sistemadeoperaciones.configuraciones.repository.ConfiguracionGeneralRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfiguracionGeneralServiceImpl implements ConfiguracionGeneralService {

    private final ConfiguracionGeneralRepository configuracionGeneralRepository;

    public ConfiguracionGeneralServiceImpl(
            ConfiguracionGeneralRepository configuracionGeneralRepository
    ) {
        this.configuracionGeneralRepository = configuracionGeneralRepository;
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

        configuracion.setPorcentajeComisionOficina(request.getPorcentajeComisionOficina());

        ConfiguracionGeneral actualizada = configuracionGeneralRepository.save(configuracion);

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
