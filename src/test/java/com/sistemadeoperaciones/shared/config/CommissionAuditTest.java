package com.sistemadeoperaciones.shared.config;

import com.sistemadeoperaciones.comisionessocioscomerciales.dto.request.PayCommissionBatchRequestDto;
import com.sistemadeoperaciones.comisionessocioscomerciales.dto.request.PayCommissionRequestDto;
import com.sistemadeoperaciones.comisionessocioscomerciales.models.CommercialPartnerCommission;
import com.sistemadeoperaciones.comisionessocioscomerciales.repository.CommercialPartnerCommissionRepository;
import com.sistemadeoperaciones.comisionessocioscomerciales.service.CommercialPartnerCommissionServiceImpl;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.cuentasbancarias.repository.BankAccountRepository;
import com.sistemadeoperaciones.notifications.service.NotificationService;
import com.sistemadeoperaciones.pagos.enums.CommissionStatus;
import com.sistemadeoperaciones.pagos.model.PaymentOperation;
import com.sistemadeoperaciones.pagos.repository.PaymentOperationRepository;
import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
import com.sistemadeoperaciones.usuarios.model.User;
import com.sistemadeoperaciones.usuarios.repository.CommercialPartnerSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommissionAuditTest {
    private final CommercialPartnerCommissionRepository commissions = mock(CommercialPartnerCommissionRepository.class);
    private final PaymentOperationRepository operations = mock(PaymentOperationRepository.class);
    private final CommercialPartnerSettingsRepository settings = mock(CommercialPartnerSettingsRepository.class);
    private final AuthenticatedUserService auth = mock(AuthenticatedUserService.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final BankAccountRepository accounts = mock(BankAccountRepository.class);
    private final BusinessAuditService audit = mock(BusinessAuditService.class);
    private CommercialPartnerCommissionServiceImpl service;

    @BeforeEach void setup() {
        service = new CommercialPartnerCommissionServiceImpl(commissions, operations, settings, auth,
                notifications, accounts, audit, mock(com.sistemadeoperaciones.retiroscaja.BankReservationGuard.class));
        var actor = new User(); actor.setId(8L);
        lenient().when(auth.getCurrentUser()).thenReturn(actor);
        var bank = new BankAccount(); bank.setId(4L); bank.setActivo(true);
        lenient().when(accounts.findById(4L)).thenReturn(Optional.of(bank));
    }

    private CommercialPartnerCommission commission(long id, CommissionStatus status) {
        var row = new CommercialPartnerCommission(); row.setId(id); row.setStatus(status);
        row.setNivel(1); row.setCommissionAmount(new BigDecimal("125.00"));
        row.setCommissionPercentage(new BigDecimal("1.00"));
        var operation = new PaymentOperation(); operation.setId(20L); row.setOperation(operation);
        var beneficiary = new User(); beneficiary.setId(30L); beneficiary.setNombre("Socio"); row.setUser(beneficiary);
        return row;
    }

    @Test void singlePaymentAuditsStatusAndAccountWithoutProofUrl() {
        var row = commission(1L, CommissionStatus.GENERADA);
        when(commissions.findById(1L)).thenReturn(Optional.of(row));
        when(commissions.save(row)).thenReturn(row);
        var request = new PayCommissionRequestDto();
        request.setCuentaOrigenId(4L); request.setPaymentProofUrl("https://secret.example/proof");

        service.markAsPaid(1L, request);

        @SuppressWarnings("unchecked") var before = (org.mockito.ArgumentCaptor<Map<String, Object>>)
                (org.mockito.ArgumentCaptor<?>) org.mockito.ArgumentCaptor.forClass(Map.class);
        @SuppressWarnings("unchecked") var after = (org.mockito.ArgumentCaptor<Map<String, Object>>)
                (org.mockito.ArgumentCaptor<?>) org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(audit).record(eq(8L), eq("COMMISSION_PAY"), eq("COMMISSION"), eq(1L), before.capture(), after.capture());
        assertThat(before.getValue()).containsEntry("status", CommissionStatus.GENERADA)
                .containsEntry("originAccountId", null).doesNotContainKey("paymentProofUrl");
        assertThat(after.getValue()).containsEntry("status", CommissionStatus.PAGADA)
                .containsEntry("originAccountId", 4L).doesNotContainKey("paymentProofUrl");
    }

    @Test void batchAuditsOnlyNewlyPaidRows() {
        var pending = commission(1L, CommissionStatus.GENERADA);
        var paid = commission(2L, CommissionStatus.PAGADA);
        when(commissions.findAllById(List.of(1L, 2L))).thenReturn(List.of(pending, paid));
        var request = new PayCommissionBatchRequestDto();
        request.setCommissionIds(List.of(1L, 2L)); request.setCuentaOrigenId(4L);
        request.setPaymentProofUrl("https://secret.example/proof");

        service.markBatchAsPaid(request);

        verify(audit).record(eq(8L), eq("COMMISSION_PAY"), eq("COMMISSION"), eq(1L), any(), any());
        verify(audit, never()).record(any(), any(), any(), eq(2L), any(), any());
        verify(audit, times(1)).record(any(), any(), any(), any(), any(), any());
    }

    @Test void regenerationAuditsRemovedAndNewCommissions() {
        var operation = new PaymentOperation(); operation.setId(20L);
        operation.setNivelesRedComercial(1);
        operation.setMontoTotal(new BigDecimal("10000.00"));
        operation.setPorcentajeComisionSocio(new BigDecimal("1.00"));
        var beneficiary = new User(); beneficiary.setId(30L); operation.setSocioComercial(beneficiary);
        when(operations.findById(20L)).thenReturn(Optional.of(operation));
        var old = commission(1L, CommissionStatus.GENERADA);
        when(commissions.findByOperationIdAndStatus(20L, CommissionStatus.GENERADA)).thenReturn(List.of(old));
        var nextId = new AtomicLong(2L);
        when(commissions.save(any(CommercialPartnerCommission.class))).thenAnswer(invocation -> {
            CommercialPartnerCommission row = invocation.getArgument(0);
            row.setId(nextId.getAndIncrement()); row.setStatus(CommissionStatus.GENERADA);
            return row;
        });

        service.regenerateOperationCommissions(20L);

        verify(audit).record(eq(8L), eq("COMMISSION_REGENERATE_REMOVE"), eq("COMMISSION"), eq(1L), any(), isNull());
        verify(audit).record(eq(8L), eq("COMMISSION_GENERATE"), eq("COMMISSION"), eq(2L), isNull(), any());
        verify(commissions).deleteAll(List.of(old));
    }
}
