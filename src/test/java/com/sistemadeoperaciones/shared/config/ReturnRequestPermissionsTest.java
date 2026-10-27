package com.sistemadeoperaciones.shared.config;

import com.sistemadeoperaciones.cuentasbancarias.repository.BankAccountRepository;
import com.sistemadeoperaciones.notifications.service.NotificationService;
import com.sistemadeoperaciones.pagos.controller.ReturnsOperationController;
import com.sistemadeoperaciones.pagos.model.OperationReturnPayment;
import com.sistemadeoperaciones.pagos.model.PaymentOperation;
import com.sistemadeoperaciones.pagos.repository.OperationReturnInstallmentRepository;
import com.sistemadeoperaciones.pagos.repository.OperationReturnPaymentRepository;
import com.sistemadeoperaciones.pagos.repository.PaymentOperationRepository;
import com.sistemadeoperaciones.pagos.service.ReturnInstallmentService;
import com.sistemadeoperaciones.pagos.service.ReturnPaymentDtoMapper;
import com.sistemadeoperaciones.pagos.service.ReturnsOperationService;
import com.sistemadeoperaciones.pagos.service.ReturnsOperationServiceImpl;
import com.sistemadeoperaciones.shared.enums.RoleName;
import com.sistemadeoperaciones.usuarios.model.Role;
import com.sistemadeoperaciones.usuarios.model.User;
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
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ReturnRequestPermissionsTest {
    @Configuration @EnableMethodSecurity
    static class Config {
        @Bean RolePermissionPolicy rolePermissionPolicy() { return new RolePermissionPolicy(); }
        @Bean ReturnsOperationService service() { return mock(ReturnsOperationService.class); }
        @Bean ReturnsOperationController controller(ReturnsOperationService service) { return new ReturnsOperationController(service); }
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    private User user(long id, RoleName roleName) {
        var user = new User(); user.setId(id);
        var role = new Role(); role.setName(roleName); user.setRoles(Set.of(role));
        return user;
    }

    @Test void managerAndDirectorCannotRequestOrEditReturnsThroughController() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var controller = context.getBean(ReturnsOperationController.class);
            var service = context.getBean(ReturnsOperationService.class);
            for (String role : List.of("GERENTE", "DIRECCION", "JEFA_CAJAS", "JEFA_CUENTAS", "AUXILIAR_CUENTAS")) {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        "staff", "", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
                assertThatThrownBy(() -> controller.requestReturnPayment(1L, null)).as(role).isInstanceOf(AccessDeniedException.class);
                assertThatThrownBy(() -> controller.updateRequestReturnPayment(1L, null)).as(role).isInstanceOf(AccessDeniedException.class);
            }
            verifyNoInteractions(service);
        }
    }

    @Test void socioCannotRequestOrEditAnotherSociosReturn() {
        var operations = mock(PaymentOperationRepository.class);
        var returns = mock(OperationReturnPaymentRepository.class);
        var auth = mock(AuthenticatedUserService.class);
        var service = new ReturnsOperationServiceImpl(operations, returns,
                mock(OperationReturnInstallmentRepository.class), auth,
                mock(BankAccountRepository.class), mock(NotificationService.class),
                mock(ReturnInstallmentService.class), mock(ReturnPaymentDtoMapper.class),
                mock(com.sistemadeoperaciones.shared.audit.service.BusinessAuditService.class));
        var operation = new PaymentOperation(); operation.setId(7L);
        operation.setSocioComercial(user(10L, RoleName.SOCIO_COMERCIAL));
        var returnPayment = new OperationReturnPayment(); returnPayment.setOperacion(operation);
        when(operations.findById(7L)).thenReturn(Optional.of(operation));
        when(returns.findById(8L)).thenReturn(Optional.of(returnPayment));
        when(auth.getCurrentUser()).thenReturn(user(11L, RoleName.SOCIO_COMERCIAL));

        assertThatThrownBy(() -> service.requestReturnPayment(7L, null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.updateRequestReturnPayment(8L, null)).isInstanceOf(AccessDeniedException.class);
        verify(returns, never()).save(any());
    }
}
