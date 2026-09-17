package com.sistemadeoperaciones.corte.controller;

import com.sistemadeoperaciones.corte.dto.DailyCashCutRequest;
import com.sistemadeoperaciones.corte.service.BankAccountDailyCutService;
import com.sistemadeoperaciones.corte.service.DailyCashCutService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Quién puede ver y quién puede operar "Cortes y saldos".
 *
 * <p>Jefa de Cuentas opera el módulo junto con Administración. Gerencia y Dirección sólo
 * consultan: antes podían registrar cortes y ya no. Las operaciones destructivas —recalcular
 * y reconstruir la serie, que reescriben cortes cerrados— siguen siendo exclusivas de
 * Administración.
 */
class CorteSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean DailyCashCutService dailyCashCutService() { return mock(DailyCashCutService.class); }
        @Bean BankAccountDailyCutService bankAccountDailyCutService() { return mock(BankAccountDailyCutService.class); }
        @Bean DailyCashCutController dailyCashCutController(DailyCashCutService s) { return new DailyCashCutController(s); }
        @Bean BankAccountDailyCutController bankAccountDailyCutController(BankAccountDailyCutService s) {
            return new BankAccountDailyCutController(s);
        }
    }

    static final List<String> TODOS = List.of(
            "ADMIN", "JEFA_CUENTAS", "GERENTE", "DIRECCION", "AUXILIAR_CUENTAS", "JEFA_CAJAS", "SOCIO_COMERCIAL");
    static final List<String> CONSULTAN = List.of("ADMIN", "JEFA_CUENTAS", "GERENTE", "DIRECCION", "AUXILIAR_CUENTAS");
    static final List<String> OPERAN = List.of("ADMIN", "JEFA_CUENTAS");
    static final List<String> ADMINISTRAN = List.of("ADMIN");

    void as(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "user", "", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    static void allowedIf(boolean allowed, org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        if (allowed) assertThatCode(call).doesNotThrowAnyException();
        else assertThatThrownBy(call).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void jefaDeCuentasOperatesWhileManagementOnlyReads() {
        LocalDate hoy = LocalDate.now();
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var cortes = context.getBean(DailyCashCutController.class);
            var bancos = context.getBean(BankAccountDailyCutController.class);

            for (String role : TODOS) {
                as(role);
                boolean consulta = CONSULTAN.contains(role);
                boolean opera = OPERAN.contains(role);
                boolean administra = ADMINISTRAN.contains(role);

                // Consulta
                allowedIf(consulta, () -> cortes.calculateDailyCut(hoy));
                allowedIf(consulta, () -> cortes.calculateRangeCut(hoy, hoy));
                allowedIf(consulta, () -> bancos.calculateBalances(hoy));
                allowedIf(consulta, () -> bancos.calculateBalancesGrouped(hoy));
                allowedIf(consulta, () -> bancos.calculateBalance(1L, hoy));

                // Operación: registrar cortes
                allowedIf(opera, () -> cortes.registerDailyCut(hoy));
                allowedIf(opera, () -> cortes.registerDailyCutWithRequest(new DailyCashCutRequest()));
                allowedIf(opera, () -> bancos.registerDailyCut(hoy));

                // Administración: reescribir cortes ya cerrados
                allowedIf(administra, () -> cortes.recalculateFrom(hoy));
                allowedIf(administra, () -> cortes.rebuildRange(hoy, hoy, BigDecimal.ZERO));
            }
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
