package com.sistemadeoperaciones.configuraciones.service;

import com.sistemadeoperaciones.configuraciones.dto.ConfiguracionGeneralRequestDTO;
import com.sistemadeoperaciones.configuraciones.model.ConfiguracionGeneral;
import com.sistemadeoperaciones.configuraciones.repository.ConfiguracionGeneralRepository;
import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.usuarios.model.User;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Horas para operación detenida y día de pago de comisiones viven en la tabla única, no en el código. */
class StalledHoursAndPaydayTest {
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
        User director = mock(User.class);
        when(director.getId()).thenReturn(8L);
        when(users.getCurrentUser()).thenReturn(director);
    }

    @Test
    void startsAtFortyEightHoursAndSaturday() {
        var response = service.obtenerResponse();
        assertThat(response.getHorasOperacionDetenida()).isEqualTo(48);
        assertThat(response.getDiaPagoComisiones()).isEqualTo(6);
    }

    @Test
    void updatesBothValuesAndRecordsBeforeAndAfter() {
        var response = service.actualizar(request(72, 5));

        assertThat(response.getHorasOperacionDetenida()).isEqualTo(72);
        assertThat(response.getDiaPagoComisiones()).isEqualTo(5);
        verify(audit).record(eq(8L), eq("CONFIGURATION_UPDATE"), eq("CONFIGURATION"), eq(1L),
                argThat((Map<String, Object> before) -> Integer.valueOf(48).equals(before.get("horasOperacionDetenida"))
                        && Integer.valueOf(6).equals(before.get("diaPagoComisiones"))),
                argThat((Map<String, Object> after) -> Integer.valueOf(72).equals(after.get("horasOperacionDetenida"))
                        && Integer.valueOf(5).equals(after.get("diaPagoComisiones"))));
    }

    @Test
    void keepsBothValuesWhenTheRequestOmitsThem() {
        var response = service.actualizar(request(null, null));
        assertThat(response.getHorasOperacionDetenida()).isEqualTo(48);
        assertThat(response.getDiaPagoComisiones()).isEqualTo(6);
    }

    @Test
    void rejectsOutOfRangeValues() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(request(0, 6))).isNotEmpty();
            assertThat(validator.validate(request(721, 6))).isNotEmpty();
            assertThat(validator.validate(request(48, 0))).isNotEmpty();
            assertThat(validator.validate(request(48, 8))).isNotEmpty();
            assertThat(validator.validate(request(48, 7))).isEmpty();
        }
    }

    private static ConfiguracionGeneralRequestDTO request(Integer horasDetenida, Integer diaPago) {
        var request = new ConfiguracionGeneralRequestDTO();
        request.setPorcentajeComisionOficina(new BigDecimal("1.50"));
        request.setHorasOperacionDetenida(horasDetenida);
        request.setDiaPagoComisiones(diaPago);
        return request;
    }
}
