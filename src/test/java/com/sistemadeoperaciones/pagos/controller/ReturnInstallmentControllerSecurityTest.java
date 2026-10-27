package com.sistemadeoperaciones.pagos.controller;

import com.sistemadeoperaciones.pagos.service.ReturnInstallmentService;
import com.sistemadeoperaciones.pagos.service.ReturnsOperationService;
import com.sistemadeoperaciones.shared.config.RolePermissionPolicy;
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

class ReturnInstallmentControllerSecurityTest {
    @Configuration @EnableMethodSecurity
    static class Config {
        @Bean RolePermissionPolicy rolePermissionPolicy() { return new RolePermissionPolicy(); }
        @Bean ReturnInstallmentService installments() { return mock(ReturnInstallmentService.class); }
        @Bean ReturnsOperationService returns() { return mock(ReturnsOperationService.class); }
        @Bean ReturnInstallmentController installmentController(ReturnInstallmentService service) {
            return new ReturnInstallmentController(service);
        }
        @Bean ReturnsOperationController legacyController(ReturnsOperationService service) {
            return new ReturnsOperationController(service);
        }
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    private void as(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "test", "", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test void supervisorsCannotPayDeliverOrCancelReturnsViaEitherApi() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var installments = context.getBean(ReturnInstallmentController.class);
            var legacy = context.getBean(ReturnsOperationController.class);
            for (String role : List.of("GERENTE", "DIRECCION", "SOCIO_COMERCIAL")) {
                as(role);
                assertThatThrownBy(() -> installments.createInstallment(1L, null)).as(role).isInstanceOf(AccessDeniedException.class);
                assertThatThrownBy(() -> installments.deliverInstallment(1L, null)).as(role).isInstanceOf(AccessDeniedException.class);
                assertThatThrownBy(() -> installments.cancelInstallment(1L, null)).as(role).isInstanceOf(AccessDeniedException.class);
                assertThatThrownBy(() -> legacy.realizeReturnPayment(1L, null)).as(role).isInstanceOf(AccessDeniedException.class);
            }
            verifyNoInteractions(context.getBean(ReturnInstallmentService.class), context.getBean(ReturnsOperationService.class));
        }
    }

    @Test void cashierCannotUseBankEndpointAndAccountsCannotDeliverCash() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var installments = context.getBean(ReturnInstallmentController.class);
            var legacy = context.getBean(ReturnsOperationController.class);
            as("JEFA_CAJAS");
            assertThatThrownBy(() -> legacy.realizeReturnPayment(1L, null)).isInstanceOf(AccessDeniedException.class);
            for (String role : List.of("JEFA_CUENTAS", "AUXILIAR_CUENTAS")) {
                as(role);
                assertThatThrownBy(() -> installments.deliverInstallment(1L, null)).isInstanceOf(AccessDeniedException.class);
                assertThatThrownBy(() -> legacy.markCashReturnAsDelivered(1L, null)).isInstanceOf(AccessDeniedException.class);
            }
            verifyNoInteractions(context.getBean(ReturnInstallmentService.class), context.getBean(ReturnsOperationService.class));
        }
    }
    @Test void onlyAccountsHeadAuxiliaryAndSupportCanConfirmCodeBankDebit() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var controller = context.getBean(ReturnInstallmentController.class);
            var proof = new ReturnInstallmentController.BankCodeConfirmation("https://example.com/proof.png");
            for (String role : List.of("JEFA_CAJAS","SOCIO_COMERCIAL","GERENTE","DIRECCION")) {
                as(role); assertThatThrownBy(() -> controller.confirmCodeBank(1L,proof)).isInstanceOf(AccessDeniedException.class);
            }
            for (String role : List.of("JEFA_CUENTAS","AUXILIAR_CUENTAS","ADMIN")) { as(role); controller.confirmCodeBank(1L,proof); }
            verify(context.getBean(ReturnInstallmentService.class),times(3)).confirmCardlessBank(1L,proof.comprobanteUrl());
        }
    }
    @Test void onlyAccountsRolesCanEditCodesOrListEligibleAccounts() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var controller = context.getBean(ReturnInstallmentController.class);
            for (String role : List.of("JEFA_CAJAS","SOCIO_COMERCIAL","GERENTE","DIRECCION")) {
                as(role);
                assertThatThrownBy(() -> controller.editCode(1L,null)).isInstanceOf(AccessDeniedException.class);
                assertThatThrownBy(() -> controller.eligibleAccounts(2L,null)).isInstanceOf(AccessDeniedException.class);
            }
            for (String role : List.of("JEFA_CUENTAS","AUXILIAR_CUENTAS","ADMIN")) { as(role); controller.editCode(1L,null); controller.eligibleAccounts(2L,null); }
            verify(context.getBean(ReturnInstallmentService.class), times(3)).editCardlessCode(1L,null);
            verify(context.getBean(ReturnInstallmentService.class), times(3)).eligibleCardlessAccounts(2L,null);
        }
    }

}
