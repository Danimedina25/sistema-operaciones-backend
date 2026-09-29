package com.sistemadeoperaciones.retiroscaja;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
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

/** Cuentas registra y cancela; Cajas confirma y rechaza; ambos lados y Dirección consultan. */
class BankCashWithdrawalSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean BankCashWithdrawalService service() { return mock(BankCashWithdrawalService.class); }
        @Bean BankCashWithdrawalController controller(BankCashWithdrawalService service) {
            return new BankCashWithdrawalController(service);
        }
    }

    static final List<String> ROLES = List.of("ADMIN", "JEFA_CUENTAS", "AUXILIAR_CUENTAS", "JEFA_CAJAS",
            "GERENTE", "DIRECCION", "SOCIO_COMERCIAL");

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    static void check(boolean allowed, Executable call) {
        if (allowed) assertThatCode(call::execute).doesNotThrowAnyException();
        else assertThatThrownBy(call::execute).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void eachSideOnlyDoesItsPart() {
        LocalDate hoy = LocalDate.now();
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var controller = context.getBean(BankCashWithdrawalController.class);
            for (String role : ROLES) {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        "user", "", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
                boolean reader = !role.equals("SOCIO_COMERCIAL");
                boolean cuentas = role.equals("ADMIN") || role.equals("JEFA_CUENTAS");
                boolean cajas = role.equals("ADMIN") || role.equals("JEFA_CAJAS");
                check(reader, controller::pending);
                check(reader, () -> controller.list(hoy, hoy));
                check(cuentas, () -> controller.register(null));
                check(cuentas, () -> controller.cancel(1L, null));
                check(cajas, () -> controller.confirm(1L, null));
                check(cajas, () -> controller.reject(1L, null));
            }
        }
    }
}
