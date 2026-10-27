package com.sistemadeoperaciones.shared.config;

import com.sistemadeoperaciones.clientes.controller.ClientesController;
import com.sistemadeoperaciones.clientes.service.ClientesService;
import com.sistemadeoperaciones.cuentasbancarias.controller.BankAccountController;
import com.sistemadeoperaciones.cuentasbancarias.service.BankAccountService;
import com.sistemadeoperaciones.socioscomerciales.controller.CommercialPartnerController;
import com.sistemadeoperaciones.socioscomerciales.dto.ConfigurePartnerCommissionRequest;
import com.sistemadeoperaciones.socioscomerciales.service.CommercialPartnerService;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verifyNoInteractions;

class CatalogPermissionsTest {
    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean RolePermissionPolicy rolePermissionPolicy() { return new RolePermissionPolicy(); }
        @Bean ClientesService clientesService() { return mock(ClientesService.class); }
        @Bean BankAccountService bankAccountService() { return mock(BankAccountService.class); }
        @Bean CommercialPartnerService commercialPartnerService() { return mock(CommercialPartnerService.class); }
        @Bean ClientesController clientesController(ClientesService service) { return new ClientesController(service); }
        @Bean BankAccountController bankAccountController(BankAccountService service) { return new BankAccountController(service); }
        @Bean CommercialPartnerController commercialPartnerController(CommercialPartnerService service) { return new CommercialPartnerController(service); }
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    private void as(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "staff", "", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test void directorCanReadButCannotMutateCatalogs() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var clients = context.getBean(ClientesController.class);
            var partners = context.getBean(CommercialPartnerController.class);
            var accounts = context.getBean(BankAccountController.class);
            as("DIRECCION");
            assertThatCode(clients::findAll).doesNotThrowAnyException();
            assertThatCode(() -> partners.findById(1L)).doesNotThrowAnyException();
            assertThatCode(accounts::getAllBankAccounts).doesNotThrowAnyException();
            clearInvocations(context.getBean(ClientesService.class), context.getBean(CommercialPartnerService.class), context.getBean(BankAccountService.class));
            assertThatThrownBy(() -> clients.create(null)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> clients.update(1L, null)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> clients.deactivate(1L)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> clients.activate(1L)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> clients.delete(1L)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> partners.create(null)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> partners.update(1L, null)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> partners.configureCommission(1L, new ConfigurePartnerCommissionRequest(java.math.BigDecimal.ONE))).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> partners.deactivateCommercialPartner(1L)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> partners.activateCommercialPartner(1L)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> partners.delete(1L)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> accounts.createBankAccount(null)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> accounts.updateBankAccount(1L, null)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> accounts.deactivateBankAccount(1L)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> accounts.activateBankAccount(1L)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> accounts.deleteBankAccount(1L)).isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(context.getBean(ClientesService.class), context.getBean(CommercialPartnerService.class), context.getBean(BankAccountService.class));
        }
    }

    @Test void businessEditorsRetainTheirWriteAccess() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var clients = context.getBean(ClientesController.class);
            var partners = context.getBean(CommercialPartnerController.class);
            var accounts = context.getBean(BankAccountController.class);
            as("GERENTE");
            assertThatCode(() -> clients.update(1L, null)).doesNotThrowAnyException();
            assertThatCode(() -> partners.update(1L, null)).doesNotThrowAnyException();
            assertThatCode(() -> partners.configureCommission(1L, new ConfigurePartnerCommissionRequest(java.math.BigDecimal.ONE))).doesNotThrowAnyException();
            assertThatCode(() -> accounts.updateBankAccount(1L, null)).doesNotThrowAnyException();
            as("JEFA_CUENTAS");
            assertThatCode(() -> accounts.createBankAccount(null)).doesNotThrowAnyException();
            assertThatThrownBy(() -> accounts.updateBankAccount(1L, null)).isInstanceOf(AccessDeniedException.class);
            as("SOCIO_COMERCIAL");
            assertThatCode(() -> clients.create(null)).doesNotThrowAnyException();
            assertThatCode(() -> partners.create(null)).doesNotThrowAnyException();
            assertThatThrownBy(() -> partners.configureCommission(1L, new ConfigurePartnerCommissionRequest(java.math.BigDecimal.ONE))).isInstanceOf(AccessDeniedException.class);
            assertThatCode(() -> clients.deactivate(1L)).doesNotThrowAnyException();
            as("ADMIN");
            assertThatCode(() -> accounts.deleteBankAccount(1L)).doesNotThrowAnyException();
        }
    }
}
