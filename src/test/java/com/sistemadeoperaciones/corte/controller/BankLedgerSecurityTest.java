package com.sistemadeoperaciones.corte.controller;

import com.sistemadeoperaciones.corte.service.BankLedgerService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BankLedgerSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean BankLedgerService service() { return mock(BankLedgerService.class); }
        @Bean BankLedgerController controller(BankLedgerService service) { return new BankLedgerController(service); }
    }

    static final List<String> ALLOWED =
            List.of("ADMIN", "JEFA_CUENTAS", "GERENTE", "DIRECCION", "AUXILIAR_CUENTAS");

    @Test
    void onlyAccountingRolesCanQueryTheLedger() {
        LocalDate hoy = LocalDate.now();
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var controller = context.getBean(BankLedgerController.class);
            for (String role : List.of("ADMIN", "JEFA_CUENTAS", "GERENTE", "DIRECCION",
                    "AUXILIAR_CUENTAS", "JEFA_CAJAS", "SOCIO_COMERCIAL")) {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        "user", "", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
                if (ALLOWED.contains(role)) {
                    assertThatCode(() -> controller.search(hoy, hoy, null, null, null, null, 0, 20))
                            .doesNotThrowAnyException();
                    assertThatCode(() -> controller.summary(hoy, hoy, null, null, null, null))
                            .doesNotThrowAnyException();
                } else {
                    assertThatThrownBy(() -> controller.search(hoy, hoy, null, null, null, null, 0, 20))
                            .isInstanceOf(AccessDeniedException.class);
                    assertThatThrownBy(() -> controller.summary(hoy, hoy, null, null, null, null))
                            .isInstanceOf(AccessDeniedException.class);
                }
            }
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /** La vista de movimientos bancarios es de consulta: nada debe poder capturarse por aquí. */
    @Test
    void theLedgerControllerExposesNoWriteOperation() {
        for (Method method : BankLedgerController.class.getDeclaredMethods()) {
            assertThat(method.getAnnotation(PostMapping.class)).as(method.getName()).isNull();
            assertThat(method.getAnnotation(PutMapping.class)).as(method.getName()).isNull();
            assertThat(method.getAnnotation(PatchMapping.class)).as(method.getName()).isNull();
            assertThat(method.getAnnotation(DeleteMapping.class)).as(method.getName()).isNull();
        }
    }
}
