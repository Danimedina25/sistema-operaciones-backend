package com.sistemadeoperaciones.pagos.controller;

import com.sistemadeoperaciones.pagos.service.PaymentOperationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * La eliminación física de operaciones es exclusiva de ADMIN y GERENTE.
 * DIRECCION sí puede activar/desactivar, pero queda fuera de este endpoint.
 */
class DeleteOperationSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean PaymentOperationService service() { return mock(PaymentOperationService.class); }
        @Bean PaymentOperationController controller(PaymentOperationService service) {
            return new PaymentOperationController(service);
        }
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "staff", "", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test
    void adminAndGerenteCanDeleteOperations() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var controller = context.getBean(PaymentOperationController.class);
            var service = context.getBean(PaymentOperationService.class);

            for (var role : List.of("ADMIN", "GERENTE")) {
                authenticateAs(role);
                assertThat(controller.delete(7L).getStatusCode().is2xxSuccessful()).isTrue();
            }

            verify(service, times(2)).delete(7L);
        }
    }

    @Test
    void everyOtherProfileIsForbidden() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var controller = context.getBean(PaymentOperationController.class);
            var service = context.getBean(PaymentOperationService.class);

            var forbidden = List.of(
                    "DIRECCION", "SOCIO_COMERCIAL", "JEFA_CAJAS", "JEFA_CUENTAS", "AUXILIAR_CUENTAS");

            for (var role : forbidden) {
                authenticateAs(role);
                assertThatThrownBy(() -> controller.delete(7L))
                        .as("el rol %s no debe poder eliminar operaciones", role)
                        .isInstanceOf(AccessDeniedException.class);
            }

            verify(service, times(0)).delete(7L);
        }
    }

    @Test
    void deleteEndpointKeepsItsRoles() throws NoSuchMethodException {
        Method delete = PaymentOperationController.class.getDeclaredMethod("delete", Long.class);
        PreAuthorize preAuthorize = delete.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasAnyRole('ADMIN', 'GERENTE')");
    }
}
