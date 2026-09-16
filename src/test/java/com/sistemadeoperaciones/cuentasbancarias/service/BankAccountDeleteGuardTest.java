package com.sistemadeoperaciones.cuentasbancarias.service;

import com.sistemadeoperaciones.cajageneral.repository.CashGeneralMovementRepository;
import com.sistemadeoperaciones.corte.repository.BankAccountDailyCutRepository;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.cuentasbancarias.repository.BankAccountRepository;
import com.sistemadeoperaciones.pagos.repository.OperationPaymentRepository;
import com.sistemadeoperaciones.pagos.repository.OperationReturnInstallmentRepository;
import com.sistemadeoperaciones.pagos.repository.OperationReturnPaymentRepository;
import com.sistemadeoperaciones.shared.audit.service.DeletionAuditService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

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
}
