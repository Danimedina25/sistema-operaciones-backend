package com.sistemadeoperaciones.shared.config;

import com.sistemadeoperaciones.clientes.model.Clientes;
import com.sistemadeoperaciones.clientes.repository.ClientesRepository;
import com.sistemadeoperaciones.clientes.service.ClientesServiceImpl;
import com.sistemadeoperaciones.pagos.repository.PaymentOperationRepository;
import com.sistemadeoperaciones.shared.audit.service.DeletionAuditService;
import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
import com.sistemadeoperaciones.shared.enums.RoleName;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
import com.sistemadeoperaciones.usuarios.model.Role;
import com.sistemadeoperaciones.usuarios.model.User;
import com.sistemadeoperaciones.usuarios.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ClientStatusOwnershipTest {
    private User user(long id) {
        var user = new User();
        user.setId(id);
        var role = new Role();
        role.setName(RoleName.SOCIO_COMERCIAL);
        user.setRoles(Set.of(role));
        return user;
    }

    @Test void socioCannotActivateOrDeactivateAnotherSociosClient() {
        var repository = mock(ClientesRepository.class);
        var auth = mock(AuthenticatedUserService.class);
        var audit = mock(BusinessAuditService.class);
        var service = new ClientesServiceImpl(repository, mock(UserRepository.class), auth,
                mock(PaymentOperationRepository.class), mock(DeletionAuditService.class), audit);
        var cliente = new Clientes();
        cliente.setId(27L);
        cliente.setNombre("Cliente ajeno");
        cliente.setUser(user(3L));
        when(repository.findById(27L)).thenReturn(Optional.of(cliente));
        when(repository.save(cliente)).thenReturn(cliente);

        when(auth.getCurrentUser()).thenReturn(user(9L));
        assertThatThrownBy(() -> service.activate(27L)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.deactivate(27L)).isInstanceOf(BadRequestException.class);
        verify(repository, never()).save(any());
        verifyNoInteractions(audit);

        when(auth.getCurrentUser()).thenReturn(user(3L));
        assertThatCode(() -> service.deactivate(27L)).doesNotThrowAnyException();
        verify(repository).save(cliente);
        verify(audit).record(eq(3L), eq("CLIENT_DEACTIVATE"), eq("CLIENT"), eq(27L), any(), any());
    }
}
