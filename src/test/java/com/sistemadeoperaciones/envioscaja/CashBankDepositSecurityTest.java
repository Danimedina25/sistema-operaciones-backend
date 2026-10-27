package com.sistemadeoperaciones.envioscaja;

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
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CashBankDepositSecurityTest {
    @Configuration @EnableMethodSecurity
    static class Config {
        @Bean CashBankDepositService service() { return mock(CashBankDepositService.class); }
        @Bean CashBankDepositController controller(CashBankDepositService service) {
            return new CashBankDepositController(service);
        }
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    static void check(boolean allowed, Executable call) {
        if (allowed) assertThatCode(call::execute).doesNotThrowAnyException();
        else assertThatThrownBy(call::execute).isInstanceOf(AccessDeniedException.class);
    }
    @Test void eachRoleHasOnlyItsActions() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var controller = context.getBean(CashBankDepositController.class);
            for (String role : List.of("ADMIN", "JEFA_CAJAS", "JEFA_CUENTAS", "AUXILIAR_CUENTAS",
                    "GERENTE", "DIRECCION", "SOCIO_COMERCIAL")) {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        "user", "", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
                boolean reader = !role.equals("SOCIO_COMERCIAL") && !role.equals("AUXILIAR_CUENTAS");
                boolean cajas = role.equals("ADMIN") || role.equals("JEFA_CAJAS");
                boolean cuentas = role.equals("ADMIN") || role.equals("JEFA_CUENTAS");
                check(reader, controller::pending);
                check(reader, controller::summary);
                check(reader, controller::returns);
                check(reader, () -> controller.preview(null, null));
                check(reader, () -> controller.list(LocalDate.now(), LocalDate.now()));
                check(cajas, () -> controller.register(null));
                check(cajas, () -> controller.edit(1L, null));
                check(cajas, () -> controller.confirmReturn(1L, null));
                check(cuentas, () -> controller.confirm(1L, null));
                check(cuentas, () -> controller.reject(1L, null));
            }
        }
    }
}
