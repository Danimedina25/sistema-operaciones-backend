package com.sistemadeoperaciones.pagos.controller;
import com.sistemadeoperaciones.pagos.service.ReturnInstallmentService;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class StaffPendingSecurityTest {
    @Configuration @EnableMethodSecurity static class Config {
        @Bean ReturnInstallmentService service() { return mock(ReturnInstallmentService.class); }
        @Bean ReturnInstallmentController controller(ReturnInstallmentService service) { return new ReturnInstallmentController(service); }
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void pendingDeliveriesRequireCashRoleEvenWhenOtherRolesCanReadGeneralDeliveries() {
        try(var context=new AnnotationConfigApplicationContext(Config.class)) {
            var controller=context.getBean(ReturnInstallmentController.class);
            var service=context.getBean(ReturnInstallmentService.class);
            when(service.findPendingPickups(anyString(), any(), any(), any())).thenReturn(Page.empty());
            for(var role:List.of("SOCIO_COMERCIAL","JEFA_CUENTAS","AUXILIAR_CUENTAS","ADMIN")) {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("staff","",List.of(new SimpleGrantedAuthority("ROLE_"+role))));
                assertThatThrownBy(() -> controller.pending("CONFIRMATION",null,null,PageRequest.of(0,10))).isInstanceOf(AccessDeniedException.class);
            }
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("staff","",List.of(new SimpleGrantedAuthority("ROLE_JEFA_CAJAS"),new SimpleGrantedAuthority("ROLE_JEFA_CUENTAS"))));
            assertThat(controller.pending("CONFIRMATION",null,null,PageRequest.of(0,10)).getStatusCode().is2xxSuccessful()).isTrue();
            verify(service,times(1)).findPendingPickups("CONFIRMATION",null,null,PageRequest.of(0,10));
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("manager","",List.of(new SimpleGrantedAuthority("ROLE_GERENTE"))));
            assertThat(controller.pending("TODAY",null,com.sistemadeoperaciones.shared.enums.RoleName.JEFA_CAJAS,PageRequest.of(0,10)).getStatusCode().is2xxSuccessful()).isTrue();
            verify(service).findPendingPickups("TODAY",null,com.sistemadeoperaciones.shared.enums.RoleName.JEFA_CAJAS,PageRequest.of(0,10));
        }
    }
}
