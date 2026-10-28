package com.sistemadeoperaciones.configuraciones.service;

import com.sistemadeoperaciones.configuraciones.dto.ConfiguracionGeneralRequestDTO;
import com.sistemadeoperaciones.configuraciones.dto.ConfiguracionGeneralResponseDto;
import com.sistemadeoperaciones.configuraciones.dto.PendingUrgencyThresholdsDto;
import com.sistemadeoperaciones.configuraciones.model.ConfiguracionGeneral;

public interface ConfiguracionGeneralService {

    ConfiguracionGeneral obtener();

    ConfiguracionGeneralResponseDto obtenerResponse();

    PendingUrgencyThresholdsDto umbralesPendientes();

    ConfiguracionGeneralResponseDto actualizar(ConfiguracionGeneralRequestDTO request);
}
