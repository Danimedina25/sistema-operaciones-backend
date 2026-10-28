package com.sistemadeoperaciones.shared.config;

import com.sistemadeoperaciones.configuraciones.dto.ConfiguracionGeneralRequestDTO;
import com.sistemadeoperaciones.configuraciones.model.ConfiguracionGeneral;
import com.sistemadeoperaciones.configuraciones.repository.ConfiguracionGeneralRepository;
import com.sistemadeoperaciones.configuraciones.service.ConfiguracionGeneralServiceImpl;
import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
import com.sistemadeoperaciones.usuarios.model.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

class ConfigurationAuditTest {
    @Test void changingOfficeCommissionRecordsActorAndBeforeAfter() {
        var repository = mock(ConfiguracionGeneralRepository.class);
        var auth = mock(AuthenticatedUserService.class);
        var audit = mock(BusinessAuditService.class);
        var service = new ConfiguracionGeneralServiceImpl(repository, auth, audit);
        var configuration = new ConfiguracionGeneral();
        configuration.setId(5L);
        configuration.setPorcentajeComisionOficina(new BigDecimal("1.50"));
        var actor = new User(); actor.setId(9L);
        when(repository.findAll()).thenReturn(List.of(configuration));
        when(repository.save(configuration)).thenReturn(configuration);
        when(auth.getCurrentUser()).thenReturn(actor);
        var request = new ConfiguracionGeneralRequestDTO();
        request.setPorcentajeComisionOficina(new BigDecimal("2.00"));

        service.actualizar(request);

        verify(audit).record(9L, "CONFIGURATION_UPDATE", "CONFIGURATION", 5L,
                Map.of("porcentajeComisionOficina", new BigDecimal("1.50"),
                        "horasPendienteAtencion", 4, "horasPendienteUrgente", 24),
                Map.of("porcentajeComisionOficina", new BigDecimal("2.00"),
                        "horasPendienteAtencion", 4, "horasPendienteUrgente", 24));
    }
}
