package com.sistemadeoperaciones.shared.config;

import com.sistemadeoperaciones.usuarios.controller.UserManagementController;
import com.sistemadeoperaciones.usuarios.service.UserManagementService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class UserManagementPermissionsTest {
    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean RolePermissionPolicy rolePermissionPolicy() { return new RolePermissionPolicy(); }
        @Bean UserManagementService service() { return mock(UserManagementService.class); }
        @Bean UserManagementController controller(UserManagementService service) {
            return new UserManagementController(service);
        }
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    private void as(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "staff", "", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test void createIsDirectorOnlyAmongBusinessRolesAndDeniedCallsDoNotReachService() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var controller = context.getBean(UserManagementController.class);
            var service = context.getBean(UserManagementService.class);
            for (String role : List.of("GERENTE", "JEFA_CAJAS", "JEFA_CUENTAS", "AUXILIAR_CUENTAS", "SOCIO_COMERCIAL")) {
                as(role);
                assertThatThrownBy(() -> controller.create(null)).as(role).isInstanceOf(AccessDeniedException.class);
            }
            verifyNoInteractions(service);
            for (String role : List.of("DIRECCION", "ADMIN")) {
                as(role);
                assertThatCode(() -> controller.create(null)).as(role).doesNotThrowAnyException();
            }
            verify(service, times(2)).create(null);
        }
    }

    @Test void gerenteCanEditButCannotDelete() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var controller = context.getBean(UserManagementController.class);
            var service = context.getBean(UserManagementService.class);
            as("GERENTE");
            assertThatCode(() -> controller.update(7L, null)).doesNotThrowAnyException();
            assertThatCode(() -> controller.deactivate(7L)).doesNotThrowAnyException();
            verify(service).update(7L, null);
            verify(service).deactivate(7L);
            clearInvocations(service);
            assertThatThrownBy(() -> controller.delete(7L)).isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(service);
            for (String role : List.of("DIRECCION", "ADMIN")) {
                as(role);
                assertThatCode(() -> controller.delete(7L)).as(role).doesNotThrowAnyException();
            }
            verify(service, times(2)).delete(7L);
        }
    }
}
