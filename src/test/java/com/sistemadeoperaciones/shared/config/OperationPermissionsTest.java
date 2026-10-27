package com.sistemadeoperaciones.shared.config;

import com.sistemadeoperaciones.pagos.controller.PaymentOperationController;
import com.sistemadeoperaciones.pagos.service.PaymentOperationService;
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
import static org.mockito.Mockito.*;

class OperationPermissionsTest {
    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean RolePermissionPolicy rolePermissionPolicy() { return new RolePermissionPolicy(); }
        @Bean PaymentOperationService service() { return mock(PaymentOperationService.class); }
        @Bean PaymentOperationController controller(PaymentOperationService service) {
            return new PaymentOperationController(service);
        }
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    private void as(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "staff", "", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test void managerAndDirectorCannotCreateEditRegisterOrValidateAndNoDeniedCallMutates() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var controller = context.getBean(PaymentOperationController.class);
            var service = context.getBean(PaymentOperationService.class);
            for (String role : List.of("GERENTE", "DIRECCION")) {
                as(role);
                assertThatThrownBy(() -> controller.createOperation(null)).as(role).isInstanceOf(AccessDeniedException.class);
                assertThatThrownBy(() -> controller.updateOperation(1L, null)).as(role).isInstanceOf(AccessDeniedException.class);
                assertThatThrownBy(() -> controller.addPayment(null)).as(role).isInstanceOf(AccessDeniedException.class);
                assertThatThrownBy(() -> controller.updatePayment(1L, null)).as(role).isInstanceOf(AccessDeniedException.class);
                assertThatThrownBy(() -> controller.validatePayment(1L, null)).as(role).isInstanceOf(AccessDeniedException.class);
                assertThatThrownBy(() -> controller.rejectPayment(1L, null)).as(role).isInstanceOf(AccessDeniedException.class);
                assertThatThrownBy(() -> controller.markPaymentInProgress(1L, null)).as(role).isInstanceOf(AccessDeniedException.class);
                assertThatThrownBy(() -> controller.releasePaymentInProgress(1L)).as(role).isInstanceOf(AccessDeniedException.class);
                assertThatThrownBy(() -> controller.updateValidationReceipt(1L, null)).as(role).isInstanceOf(AccessDeniedException.class);
            }
            verifyNoInteractions(service);
            as("GERENTE");
            assertThatCode(() -> controller.delete(1L)).doesNotThrowAnyException();
            verify(service).delete(1L);
            clearInvocations(service);
            as("DIRECCION");
            assertThatThrownBy(() -> controller.delete(1L)).isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(service);
        }
    }

    @Test void socioRegistersAndJefasValidateWhileOtherChangesStayDenied() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var controller = context.getBean(PaymentOperationController.class);
            var service = context.getBean(PaymentOperationService.class);
            as("SOCIO_COMERCIAL");
            assertThatCode(() -> controller.createOperation(null)).doesNotThrowAnyException();
            assertThatCode(() -> controller.addPayment(null)).doesNotThrowAnyException();
            assertThatThrownBy(() -> controller.validatePayment(1L, null)).isInstanceOf(AccessDeniedException.class);
            for (String role : List.of("JEFA_CAJAS", "JEFA_CUENTAS", "AUXILIAR_CUENTAS")) {
                as(role);
                assertThatCode(() -> controller.validatePayment(1L, null)).as(role).doesNotThrowAnyException();
                assertThatThrownBy(() -> controller.addPayment(null)).as(role).isInstanceOf(AccessDeniedException.class);
            }
            verify(service, times(3)).validatePayment(1L, null);
            verify(service).createOperation(null);
            verify(service).addPayment(null);
        }
    }
}
