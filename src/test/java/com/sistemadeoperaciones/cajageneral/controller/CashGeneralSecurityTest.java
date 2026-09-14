package com.sistemadeoperaciones.cajageneral.controller;
import com.sistemadeoperaciones.cajageneral.dto.*;
import com.sistemadeoperaciones.cajageneral.service.CashGeneralService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CashGeneralSecurityTest {
    @Configuration @EnableMethodSecurity
    static class Config {
        @Bean CashGeneralService service() { return mock(CashGeneralService.class); }
        @Bean CashGeneralController controller(CashGeneralService service) { return new CashGeneralController(service); }
    }
    @Test void roleGuardAllowsCashierWritesAndManagementReadsOnly() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var controller = context.getBean(CashGeneralController.class);
            for (String role : List.of("ADMIN", "JEFA_CAJAS", "GERENTE", "DIRECCION", "SOCIO_COMERCIAL", "AUXILIAR_CUENTAS", "JEFA_CUENTAS")) {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", "", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
                if (List.of("ADMIN", "JEFA_CAJAS", "GERENTE", "DIRECCION").contains(role)) assertThatCode(controller::latest).doesNotThrowAnyException();
                else assertThatThrownBy(controller::latest).isInstanceOf(AccessDeniedException.class);
                if (List.of("ADMIN", "JEFA_CAJAS").contains(role)) {
                    assertThatCode(() -> controller.open(null)).doesNotThrowAnyException();
                    assertThatCode(() -> controller.movement(1L,null)).doesNotThrowAnyException();
                    assertThatCode(() -> controller.close(1L,null)).doesNotThrowAnyException();
                } else {
                    assertThatThrownBy(() -> controller.open(null)).isInstanceOf(AccessDeniedException.class);
                    assertThatThrownBy(() -> controller.movement(1L,null)).isInstanceOf(AccessDeniedException.class);
                    assertThatThrownBy(() -> controller.close(1L,null)).isInstanceOf(AccessDeniedException.class);
                }
            }
        } finally { SecurityContextHolder.clearContext(); }
    }
}
