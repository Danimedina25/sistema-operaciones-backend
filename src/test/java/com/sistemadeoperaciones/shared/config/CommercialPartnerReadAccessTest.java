package com.sistemadeoperaciones.shared.config;

import com.sistemadeoperaciones.comisionessocioscomerciales.repository.CommercialPartnerCommissionRepository;
import com.sistemadeoperaciones.pagos.repository.PaymentOperationRepository;
import com.sistemadeoperaciones.shared.audit.service.DeletionAuditService;
import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.enums.RoleName;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
import com.sistemadeoperaciones.socioscomerciales.models.CommercialPartner;
import com.sistemadeoperaciones.socioscomerciales.dto.CommercialPartnerRequestDTO;
import com.sistemadeoperaciones.socioscomerciales.repository.CommercialPartnerRepository;
import com.sistemadeoperaciones.socioscomerciales.service.CommercialPartnerServiceImpl;
import com.sistemadeoperaciones.usuarios.model.Role;
import com.sistemadeoperaciones.usuarios.model.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CommercialPartnerReadAccessTest {
    private User user(long id, RoleName roleName) {
        var user = new User();
        user.setId(id);
        var role = new Role();
        role.setName(roleName);
        user.setRoles(Set.of(role));
        return user;
    }

    @Test void directorAndUnrelatedSocioCanReadSharedPartner() {
        var repository = mock(CommercialPartnerRepository.class);
        var authenticatedUserService = mock(AuthenticatedUserService.class);
        var service = new CommercialPartnerServiceImpl(repository, authenticatedUserService,
                mock(PaymentOperationRepository.class), mock(CommercialPartnerCommissionRepository.class),
                mock(DeletionAuditService.class), mock(BusinessAuditService.class));
        var partner = new CommercialPartner();
        partner.setId(10L);
        partner.setNombre("Red ajena");
        partner.setSocioComercial(user(3L, RoleName.SOCIO_COMERCIAL));
        when(repository.findById(10L)).thenReturn(Optional.of(partner));

        when(authenticatedUserService.getCurrentUser()).thenReturn(user(8L, RoleName.DIRECCION));
        assertThatCode(() -> service.findById(10L)).doesNotThrowAnyException();

        when(authenticatedUserService.getCurrentUser()).thenReturn(user(9L, RoleName.SOCIO_COMERCIAL));
        assertThatCode(() -> service.findById(10L)).doesNotThrowAnyException();
    }

    @Test void deniedUpdateDoesNotAuditAndOwnerUpdateHidesAccountNumber() {
        var repository = mock(CommercialPartnerRepository.class);
        var auth = mock(AuthenticatedUserService.class);
        var audit = mock(BusinessAuditService.class);
        var service = new CommercialPartnerServiceImpl(repository, auth,
                mock(PaymentOperationRepository.class), mock(CommercialPartnerCommissionRepository.class),
                mock(DeletionAuditService.class), audit);
        var partner = new CommercialPartner(); partner.setId(10L);
        partner.setNombre("Socio N2"); partner.setCuentaBancaria("123456789012345678");
        partner.setPorcentajeComision(new BigDecimal("2.50"));
        partner.setSocioComercial(user(3L, RoleName.SOCIO_COMERCIAL));
        when(repository.findById(10L)).thenReturn(Optional.of(partner));
        when(repository.save(partner)).thenReturn(partner);
        var request = new CommercialPartnerRequestDTO();
        request.setNombre("Socio editado"); request.setCuentaBancaria("987654321098765432");
        request.setBanco("BBVA"); request.setTitularCuenta("Titular");
        request.setPorcentajeComision(new BigDecimal("1.25"));

        when(auth.getCurrentUser()).thenReturn(user(9L, RoleName.SOCIO_COMERCIAL));
        assertThatThrownBy(() -> service.update(10L, request)).isInstanceOf(BadRequestException.class);
        verify(repository, never()).save(any());
        verifyNoInteractions(audit);

        when(auth.getCurrentUser()).thenReturn(user(3L, RoleName.SOCIO_COMERCIAL));
        service.update(10L, request);
        assertThat(partner.getPorcentajeComision()).isEqualByComparingTo("2.50");
        @SuppressWarnings("unchecked") ArgumentCaptor<Map<String, Object>> before =
                (ArgumentCaptor<Map<String, Object>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Map.class);
        @SuppressWarnings("unchecked") ArgumentCaptor<Map<String, Object>> after =
                (ArgumentCaptor<Map<String, Object>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Map.class);
        verify(audit).record(eq(3L), eq("COMMERCIAL_PARTNER_UPDATE"), eq("COMMERCIAL_PARTNER"),
                eq(10L), before.capture(), after.capture());
        assertThat(before.getValue()).containsEntry("accountLast4", "5678");
        assertThat(after.getValue()).containsEntry("accountLast4", "5432");
        assertThat(after.getValue().toString()).doesNotContain("987654321098765432");
    }

    @Test void newPartnerIgnoresSubmittedCommissionUntilManagerConfiguresIt() {
        var repository = mock(CommercialPartnerRepository.class);
        var auth = mock(AuthenticatedUserService.class);
        var audit = mock(BusinessAuditService.class);
        var service = new CommercialPartnerServiceImpl(repository, auth,
                mock(PaymentOperationRepository.class), mock(CommercialPartnerCommissionRepository.class),
                mock(DeletionAuditService.class), audit);
        var savedPartner = new java.util.concurrent.atomic.AtomicReference<CommercialPartner>();
        when(auth.getCurrentUser()).thenReturn(user(3L, RoleName.SOCIO_COMERCIAL));
        when(repository.save(any(CommercialPartner.class))).thenAnswer(invocation -> {
            CommercialPartner partner = invocation.getArgument(0);
            partner.setId(10L);
            savedPartner.set(partner);
            return partner;
        });
        var request = new CommercialPartnerRequestDTO();
        request.setNombre("Nuevo nivel 2"); request.setCuentaBancaria("123456789012345678");
        request.setBanco("BANORTE"); request.setTitularCuenta("Nuevo nivel 2");
        request.setPorcentajeComision(new BigDecimal("90"));
        var created = service.create(request);
        assertThat(created.getPorcentajeComision()).isEqualByComparingTo("0");
        assertThat(created.getCommissionConfigured()).isFalse();
        when(repository.findById(10L)).thenAnswer(invocation -> Optional.of(savedPartner.get()));
        when(auth.getCurrentUser()).thenReturn(user(8L, RoleName.GERENTE));
        var configured = service.configureCommission(10L, new BigDecimal("1.25"));
        assertThat(configured.getCommissionConfigured()).isTrue();
        assertThat(configured.getPorcentajeComision()).isEqualByComparingTo("1.25");
        verify(audit).record(eq(8L), eq("COMMERCIAL_PARTNER_COMMISSION_CONFIGURE"),
                eq("COMMERCIAL_PARTNER"), eq(10L), any(), any());
    }
}
