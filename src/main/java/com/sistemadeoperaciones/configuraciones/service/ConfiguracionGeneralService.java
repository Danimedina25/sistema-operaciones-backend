package com.sistemadeoperaciones.configuraciones.service;

import com.sistemadeoperaciones.configuraciones.dto.ConfiguracionGeneralRequestDTO;
import com.sistemadeoperaciones.configuraciones.dto.ConfiguracionGeneralResponseDto;
import com.sistemadeoperaciones.configuraciones.model.ConfiguracionGeneral;

public interface ConfiguracionGeneralService {

    ConfiguracionGeneral obtener();

    ConfiguracionGeneralResponseDto obtenerResponse();

    ConfiguracionGeneralResponseDto actualizar(ConfiguracionGeneralRequestDTO request);
}
