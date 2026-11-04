package com.sistemadeoperaciones.usuarios.service;

import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.enums.RoleName;
import com.sistemadeoperaciones.usuarios.dto.request.CreateUserRequestDto;
import com.sistemadeoperaciones.usuarios.dto.request.UpdateUserRequestDto;
import com.sistemadeoperaciones.usuarios.model.Role;
import com.sistemadeoperaciones.usuarios.model.User;
import com.sistemadeoperaciones.usuarios.repository.CommercialPartnerSettingsRepository;
import com.sistemadeoperaciones.usuarios.repository.RoleRepository;
import com.sistemadeoperaciones.usuarios.repository.UserRepository;
import com.sistemadeoperaciones.usuarios.service.activation.UserActivationService;
import com.sistemadeoperaciones.shared.audit.service.DeletionAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminRoleAssignmentTest {
    private final UserRepository users = mock(UserRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final AuthenticatedUserService auth = mock(AuthenticatedUserService.class);
    private UserManagementServiceImpl service;

    private static Role role(long id, RoleName name) {
        Role role = new Role(); role.setId(id); role.setName(name); return role;
    }

    private static User userWith(Role role) {
        User user = new User(); user.setId(9L); user.setNombre("Persona"); user.setCorreo("p@tutorial.test");
        user.getRoles().add(role); return user;
    }

    @BeforeEach
    void setUp() {
        service = new UserManagementServiceImpl(users, roles, mock(UserActivationService.class),
                mock(CommercialPartnerSettingsRepository.class), auth, mock(UserDeletionGuard.class),
                mock(UserDeletionDependencyCleaner.class), mock(DeletionAuditService.class), mock(BusinessAuditService.class));
        when(roles.findById(1L)).thenReturn(Optional.of(role(1L, RoleName.ADMIN)));
        when(roles.findById(6L)).thenReturn(Optional.of(role(6L, RoleName.AUXILIAR_CUENTAS)));
        when(users.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
    }

    private CreateUserRequestDto create(long roleId) {
        CreateUserRequestDto request = new CreateUserRequestDto();
        request.setNombre("Andrea Ríos"); request.setCorreo("auxiliar2@tutorial.test");
        request.setTelefono("5500000107"); request.setRoleId(roleId);
        return request;
    }

    @Test
    void directorCannotCreateAnAdministrator() {
        when(auth.getCurrentUser()).thenReturn(userWith(role(8L, RoleName.DIRECCION)));
        assertThatThrownBy(() -> service.create(create(1L))).isInstanceOf(AccessDeniedException.class);
        verify(users, never()).save(any());
    }

    @Test
    void directorCreatesOtherRoles() {
        when(auth.getCurrentUser()).thenReturn(userWith(role(8L, RoleName.DIRECCION)));
        try { service.create(create(6L)); } catch (RuntimeException ignored) { /* el correo de activación no se simula */ }
        verify(users).save(any(User.class));
    }

    @Test
    void directorCannotChangeTheRoleOfAnAdministrator() {
        when(auth.getCurrentUser()).thenReturn(userWith(role(8L, RoleName.DIRECCION)));
        when(users.findById(9L)).thenReturn(Optional.of(userWith(role(1L, RoleName.ADMIN))));
        UpdateUserRequestDto request = new UpdateUserRequestDto();
        request.setNombre("Persona"); request.setTelefono("5500000109"); request.setRoleId(6L);
        assertThatThrownBy(() -> service.update(9L, request)).isInstanceOf(AccessDeniedException.class);
        verify(users, never()).save(any());
    }

    @Test
    void administratorCanAssignTheAdministratorRole() {
        when(auth.getCurrentUser()).thenReturn(userWith(role(1L, RoleName.ADMIN)));
        try { service.create(create(1L)); } catch (RuntimeException ignored) { /* el correo de activación no se simula */ }
        verify(users).save(any(User.class));
    }
}
