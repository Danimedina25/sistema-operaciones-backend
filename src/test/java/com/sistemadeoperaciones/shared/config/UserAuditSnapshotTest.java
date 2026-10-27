package com.sistemadeoperaciones.shared.config;

import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
import com.sistemadeoperaciones.shared.audit.service.DeletionAuditService;
import com.sistemadeoperaciones.shared.enums.RoleName;
import com.sistemadeoperaciones.usuarios.model.CommercialPartnerSettings;
import com.sistemadeoperaciones.usuarios.model.Role;
import com.sistemadeoperaciones.usuarios.model.User;
import com.sistemadeoperaciones.usuarios.repository.CommercialPartnerSettingsRepository;
import com.sistemadeoperaciones.usuarios.repository.RoleRepository;
import com.sistemadeoperaciones.usuarios.repository.UserRepository;
import com.sistemadeoperaciones.usuarios.service.UserDeletionDependencyCleaner;
import com.sistemadeoperaciones.usuarios.service.UserDeletionGuard;
import com.sistemadeoperaciones.usuarios.service.UserManagementServiceImpl;
import com.sistemadeoperaciones.usuarios.service.activation.UserActivationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserAuditSnapshotTest {
    @Test void excludesCredentialsAndMasksTheCommissionAccount() {
        var settingsRepository = mock(CommercialPartnerSettingsRepository.class);
        var service = new UserManagementServiceImpl(mock(UserRepository.class), mock(RoleRepository.class),
                mock(UserActivationService.class), settingsRepository, mock(AuthenticatedUserService.class),
                mock(UserDeletionGuard.class), mock(UserDeletionDependencyCleaner.class),
                mock(DeletionAuditService.class), mock(BusinessAuditService.class));
        User user = new User();
        user.setId(42L);
        user.setNombre("Socio");
        user.setCorreo("socio@prueba.test");
        user.setPassword("$2a$10$private-hash");
        Role role = new Role();
        role.setName(RoleName.SOCIO_COMERCIAL);
        user.setRoles(Set.of(role));
        var settings = new CommercialPartnerSettings();
        settings.setBanco("BANORTE");
        settings.setCuentaBancaria("1234567890128998");
        settings.setPorcentajeComision(new BigDecimal("1.50"));
        when(settingsRepository.findByUsuarioId(42L)).thenReturn(Optional.of(settings));

        @SuppressWarnings("unchecked")
        Map<String, Object> snapshot = ReflectionTestUtils.invokeMethod(service, "auditSnapshot", user);
        assertThat(snapshot).containsEntry("cuentaComision", "••8998");
        assertThat(snapshot.toString()).doesNotContain("private-hash", "1234567890128998");
        assertThat(snapshot).doesNotContainKeys("password", "token");
    }
}
