package com.sistemadeoperaciones.cuentasbancarias.service;

import com.sistemadeoperaciones.cajageneral.repository.CashGeneralMovementRepository;
import com.sistemadeoperaciones.corte.repository.BankAccountDailyCutRepository;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.cuentasbancarias.dto.BankAccountRequestDto;
import com.sistemadeoperaciones.cuentasbancarias.repository.BankAccountRepository;
import com.sistemadeoperaciones.pagos.repository.OperationPaymentRepository;
import com.sistemadeoperaciones.pagos.repository.OperationReturnInstallmentRepository;
import com.sistemadeoperaciones.pagos.repository.OperationReturnPaymentRepository;
import com.sistemadeoperaciones.shared.audit.service.DeletionAuditService;
import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.crypto.CryptoService;
import com.sistemadeoperaciones.shared.exception.EntityHasDependenciesException;
import com.sistemadeoperaciones.usuarios.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import org.mockito.ArgumentCaptor;

/**
 * Una cuenta con movimientos históricos no se puede borrar. El cheque cobrado de Caja
 * General es ahora una de esas dependencias.
 */
@ExtendWith(MockitoExtension.class)
class BankAccountDeleteGuardTest {

    @Mock BankAccountRepository bankAccountRepository;
    @Mock CryptoService cryptoService;
    @Mock OperationPaymentRepository operationPaymentRepository;
    @Mock OperationReturnPaymentRepository operationReturnPaymentRepository;
    @Mock OperationReturnInstallmentRepository operationReturnInstallmentRepository;
    @Mock BankAccountDailyCutRepository bankAccountDailyCutRepository;
    @Mock AuthenticatedUserService authenticatedUserService;
    @Mock DeletionAuditService deletionAuditService;
    @Mock BusinessAuditService businessAuditService;
    @Mock CashGeneralMovementRepository cashGeneralMovementRepository;
    @InjectMocks BankAccountServiceImpl service;

    BankAccount account;

    @BeforeEach
    void setUp() {
        account = new BankAccount();
        account.setId(4L);
        account.setBanco("BBVA");
        account.setTitular("Operaciones SA");
        account.setActivo(true);
        lenient().when(bankAccountRepository.findById(4L)).thenReturn(Optional.of(account));
        lenient().when(authenticatedUserService.getCurrentUser()).thenReturn(new User());
    }

    void noDependencies() {
        when(operationPaymentRepository.countByCuentaDestinoId(4L)).thenReturn(0L);
        when(operationReturnPaymentRepository.countByCuentaOrigenId(4L)).thenReturn(0L);
        when(operationReturnInstallmentRepository.countByCuentaOrigenId(4L)).thenReturn(0L);
        when(bankAccountDailyCutRepository.countByBankAccountId(4L)).thenReturn(0L);
    }

    @Test
    void aCashedChequeBlocksDeletion() {
        noDependencies();
        when(cashGeneralMovementRepository.countByCuentaBancariaId(4L)).thenReturn(3L);

        assertThatThrownBy(() -> service.delete(4L))
                .isInstanceOf(EntityHasDependenciesException.class)
                .satisfies(error -> assertThat(((EntityHasDependenciesException) error).getDependencies())
                        .containsEntry("chequesCobradosCajaGeneral", 3L));
    }

    @Test
    void anAccountWithoutHistoryCanStillBeDeleted() {
        noDependencies();
        when(cashGeneralMovementRepository.countByCuentaBancariaId(4L)).thenReturn(0L);

        assertThatCode(() -> service.delete(4L)).doesNotThrowAnyException();
    }

    @Test
    void updatingAccountAuditsOnlyLastFourDigits() {
        account.setNumeroCuenta("123456789012345678");
        account.setClabe("012345678901234567");
        User actor = new User(); actor.setId(9L);
        when(authenticatedUserService.getCurrentUser()).thenReturn(actor);
        when(bankAccountRepository.save(account)).thenReturn(account);
        BankAccountRequestDto request = new BankAccountRequestDto();
        request.setBanco("Banorte"); request.setTitular("Operaciones SA");
        request.setNumeroCuenta("987654321098765432"); request.setClabe("765432109876543210");

        service.update(4L, request);

        @SuppressWarnings("unchecked") ArgumentCaptor<Map<String, Object>> before =
                (ArgumentCaptor<Map<String, Object>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Map.class);
        @SuppressWarnings("unchecked") ArgumentCaptor<Map<String, Object>> after =
                (ArgumentCaptor<Map<String, Object>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Map.class);
        verify(businessAuditService).record(eq(9L), eq("BANK_ACCOUNT_UPDATE"), eq("BANK_ACCOUNT"),
                eq(4L), before.capture(), after.capture());
        assertThat(before.getValue()).containsEntry("accountLast4", "5678")
                .containsEntry("clabeLast4", "4567");
        assertThat(after.getValue()).containsEntry("accountLast4", "5432")
                .containsEntry("clabeLast4", "3210");
        assertThat(after.getValue().toString()).doesNotContain("987654321098765432", "765432109876543210");
    }

    @Test
    void accountListAndReadOnlyDetailHideCompleteNumbers() {
        account.setNumeroCuenta("123456789012345678");
        account.setClabe("012345678901234567");
        when(bankAccountRepository.findAll()).thenReturn(List.of(account));

        var listed = service.findAll().get(0);
        var detail = service.findById(4L);

        assertThat(listed.getNumeroCuenta()).endsWith("5678").doesNotContain("123456789012345678");
        assertThat(listed.getClabe()).endsWith("4567").doesNotContain("012345678901234567");
        assertThat(detail.getNumeroCuenta()).isEqualTo(listed.getNumeroCuenta());
        assertThat(detail.getClabe()).isEqualTo(listed.getClabe());
    }
}
