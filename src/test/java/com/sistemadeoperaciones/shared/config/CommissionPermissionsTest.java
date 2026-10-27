package com.sistemadeoperaciones.shared.config;

import com.sistemadeoperaciones.comisionessocioscomerciales.controller.CommercialPartnerCommissionController;
import com.sistemadeoperaciones.comisionessocioscomerciales.service.CommercialPartnerCommissionService;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CommissionPermissionsTest {
    @Configuration @EnableMethodSecurity
    static class Config {
        @Bean RolePermissionPolicy rolePermissionPolicy() { return new RolePermissionPolicy(); }
        @Bean CommercialPartnerCommissionService service() { return mock(CommercialPartnerCommissionService.class); }
        @Bean CommercialPartnerCommissionController controller(CommercialPartnerCommissionService service) {
            return new CommercialPartnerCommissionController(service);
        }
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    private void as(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "test", "", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test void directorCannotGenerateOrRegenerateCommissions() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var controller = context.getBean(CommercialPartnerCommissionController.class);
            var service = context.getBean(CommercialPartnerCommissionService.class);
            as("DIRECCION");
            assertThatThrownBy(controller::generatePendingCommissions).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> controller.regenerateOperationCommissions(1L)).isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(service);
            as("GERENTE");
            controller.regenerateOperationCommissions(1L);
            verify(service).regenerateOperationCommissions(1L);
        }
    }
}
