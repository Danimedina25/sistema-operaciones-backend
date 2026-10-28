package com.sistemadeoperaciones.configuraciones.service;

import com.sistemadeoperaciones.configuraciones.dto.ConfiguracionGeneralRequestDTO;
import com.sistemadeoperaciones.configuraciones.model.ConfiguracionGeneral;
import com.sistemadeoperaciones.configuraciones.repository.ConfiguracionGeneralRepository;
import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.exception.BusinessException;
import com.sistemadeoperaciones.usuarios.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** El semáforo de pendientes se configura en la tabla única, nunca con horas fijas en el código. */
class PendingUrgencyThresholdsTest {
    ConfiguracionGeneralRepository repository = mock(ConfiguracionGeneralRepository.class);
    AuthenticatedUserService users = mock(AuthenticatedUserService.class);
    BusinessAuditService audit = mock(BusinessAuditService.class);
    ConfiguracionGeneralServiceImpl service = new ConfiguracionGeneralServiceImpl(repository, users, audit);
    ConfiguracionGeneral config = new ConfiguracionGeneral();

    @BeforeEach
    void setUp() {
        config.setId(1L);
        when(repository.findAll()).thenReturn(List.of(config));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        User gerente = mock(User.class);
        when(gerente.getId()).thenReturn(7L);
        when(users.getCurrentUser()).thenReturn(gerente);
    }

    @Test
    void startsAtFourAndTwentyFourHours() {
        var thresholds = service.umbralesPendientes();
        assertThat(thresholds.horasPendienteAtencion()).isEqualTo(4);
        assertThat(thresholds.horasPendienteUrgente()).isEqualTo(24);
    }

    @Test
    void updatesTheThresholdsAndRecordsBeforeAndAfter() {
        var response = service.actualizar(request(6, 12));

        assertThat(response.getHorasPendienteAtencion()).isEqualTo(6);
        assertThat(response.getHorasPendienteUrgente()).isEqualTo(12);
        verify(audit).record(eq(7L), eq("CONFIGURATION_UPDATE"), eq("CONFIGURATION"), eq(1L),
                argThat((Map<String, Object> before) -> Integer.valueOf(24).equals(before.get("horasPendienteUrgente"))),
                argThat((Map<String, Object> after) -> Integer.valueOf(12).equals(after.get("horasPendienteUrgente"))));
    }

    @Test
    void keepsTheThresholdsWhenTheRequestOmitsThem() {
        var response = service.actualizar(request(null, null));
        assertThat(response.getHorasPendienteAtencion()).isEqualTo(4);
        assertThat(response.getHorasPendienteUrgente()).isEqualTo(24);
    }

    @Test
    void rejectsAnUrgentThresholdThatIsNotAfterTheAttentionOne() {
        assertThatThrownBy(() -> service.actualizar(request(24, 24)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("mayores");
        verify(repository, never()).save(any());
        verify(audit, never()).record(any(), any(), any(), any(), anyMap(), anyMap());
    }

    private static ConfiguracionGeneralRequestDTO request(Integer atencion, Integer urgente) {
        var request = new ConfiguracionGeneralRequestDTO();
        request.setPorcentajeComisionOficina(new BigDecimal("1.50"));
        request.setHorasPendienteAtencion(atencion);
        request.setHorasPendienteUrgente(urgente);
        return request;
    }
}
