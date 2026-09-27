package com.sistemadeoperaciones.transferencias;

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

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class BankTransferSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean BankTransferService service() { return mock(BankTransferService.class); }
        @Bean BankTransferController controller(BankTransferService service) { return new BankTransferController(service); }
    }

    static final List<String> ROLES =
            List.of("ADMIN", "JEFA_CUENTAS", "GERENTE", "DIRECCION", "AUXILIAR_CUENTAS", "JEFA_CAJAS", "SOCIO_COMERCIAL");
    static final List<String> READERS = List.of("ADMIN", "JEFA_CUENTAS", "GERENTE", "DIRECCION", "AUXILIAR_CUENTAS");
    static final List<String> WRITERS = List.of("ADMIN", "JEFA_CUENTAS");

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    void as(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "user", "", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test
    void onlyAdminAndJefaDeCuentasRegisterTransfers() {
        LocalDate hoy = LocalDate.now();
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var controller = context.getBean(BankTransferController.class);
            for (String role : ROLES) {
                as(role);
                if (WRITERS.contains(role))
                    assertThatCode(() -> controller.create(null)).doesNotThrowAnyException();
                else
                    assertThatThrownBy(() -> controller.create(null)).isInstanceOf(AccessDeniedException.class);

                if (READERS.contains(role))
                    assertThatCode(() -> controller.list(hoy, hoy)).doesNotThrowAnyException();
                else
                    assertThatThrownBy(() -> controller.list(hoy, hoy)).isInstanceOf(AccessDeniedException.class);
            }
        }
    }
}
