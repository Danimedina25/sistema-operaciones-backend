package com.sistemadeoperaciones.pagos.service;
import com.sistemadeoperaciones.corte.service.BankAccountDailyCutService;
import com.sistemadeoperaciones.corte.dto.BankAccountBalanceDetailResponseDto;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.cuentasbancarias.repository.BankAccountRepository;
import com.sistemadeoperaciones.pagos.repository.*;
import com.sistemadeoperaciones.pagos.model.*;
import com.sistemadeoperaciones.pagos.enums.*;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
import com.sistemadeoperaciones.shared.enums.RoleName;
import com.sistemadeoperaciones.notifications.service.NotificationService;
import com.sistemadeoperaciones.usuarios.model.*;
import org.junit.jupiter.api.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class CardlessReturnFlowTest {
    final OperationReturnPaymentRepository requests = mock(OperationReturnPaymentRepository.class);
    final PaymentOperationRepository operations = mock(PaymentOperationRepository.class);
    final BankAccountRepository accounts = mock(BankAccountRepository.class);
    final BankAccountDailyCutService balances = mock(BankAccountDailyCutService.class);
    final AuthenticatedUserService auth = mock(AuthenticatedUserService.class);
    final BusinessAuditService audit = mock(BusinessAuditService.class);
    final CardlessReturnFlow flow = new CardlessReturnFlow(requests, operations, accounts, balances, auth, audit, mock(NotificationService.class), mock(ReturnPaymentDtoMapper.class));
    OperationReturnPayment request;
    BankAccount account;
    @BeforeEach void setup() {
        request = new OperationReturnPayment(); request.setId(1L); request.setTipoPago(PaymentType.RETIRO_SIN_TARJETA);
        request.setEstatus(ReturnPaymentStatus.SOLICITADO); request.setMonto(new BigDecimal("300"));
        var operation = new PaymentOperation(); operation.setId(2L); request.setOperacion(operation);
        account = new BankAccount(); account.setId(3L); account.setActivo(true);
    }
    void user(RoleName roleName) { var user = new User(); user.setId(4L); var role = new Role(); role.setName(roleName); user.setRoles(Set.of(role)); when(auth.getCurrentUser()).thenReturn(user); }
    @Test void obsoleteAssignmentCannotActivateOldCashFlow() {
        assertThatThrownBy(() -> flow.assign(1L, 3L)).hasMessageContaining("código");
        verifyNoInteractions(accounts, balances, requests);
    }
    @Test void codeReservesAvailableBankFundsWithoutDebiting() {
        var availability = mock(com.sistemadeoperaciones.retiroscaja.BankAvailability.class);
        var register = mock(com.sistemadeoperaciones.cajageneral.repository.CashGeneralRegisterRepository.class);
        org.springframework.test.util.ReflectionTestUtils.setField(flow, "availability", availability);
        org.springframework.test.util.ReflectionTestUtils.setField(flow, "codeRepository", mock(OperationReturnInstallmentRepository.class));
        org.springframework.test.util.ReflectionTestUtils.setField(flow, "periodLock", register);
        var i = new OperationReturnInstallment(); i.setSolicitud(request); i.setMonto(new BigDecimal("300"));
        when(accounts.findByIdForUpdate(3L)).thenReturn(Optional.of(account));
        when(availability.available(3L)).thenReturn(new BigDecimal("200"));
        assertThatThrownBy(() -> flow.reserveCode(i,3L)).hasMessageContaining("insuficiente");
        when(availability.available(3L)).thenReturn(new BigDecimal("400"));
        flow.reserveCode(i,3L);
        assertThat(i.getCodigoParaCliente()).isTrue(); assertThat(i.getCuentaOrigen()).isSameAs(account);
        assertThat(i.getFechaCobroBancario()).isNull(); assertThat(i.getFechaPreparacionBancaria()).isNull();
        verifyNoInteractions(balances);
    }
    @Test void partialCodeStillRequiresAllPendingFundsAndEditCreditsOnlyItsOwnReservation() {
        var availability = mock(com.sistemadeoperaciones.retiroscaja.BankAvailability.class);
        var repository = mock(OperationReturnInstallmentRepository.class);
        org.springframework.test.util.ReflectionTestUtils.setField(flow, "availability", availability);
        org.springframework.test.util.ReflectionTestUtils.setField(flow, "codeRepository", repository);
        org.springframework.test.util.ReflectionTestUtils.setField(flow, "periodLock", mock(com.sistemadeoperaciones.cajageneral.repository.CashGeneralRegisterRepository.class));
        var i = new OperationReturnInstallment(); i.setId(8L); i.setSolicitud(request); i.setMonto(new BigDecimal("100")); i.setCuentaOrigen(account);
        when(accounts.findByIdForUpdate(3L)).thenReturn(Optional.of(account));
        when(availability.available(3L)).thenReturn(new BigDecimal("200"));
        assertThatThrownBy(() -> flow.reserveCode(i,3L)).hasMessageContaining("insuficiente");
        flow.validateEdit(i,3L,new BigDecimal("300"),"EDIT");
        assertThatThrownBy(() -> flow.validateEdit(i,3L,new BigDecimal("301"),"EDIT")).hasMessageContaining("insuficiente");
        when(repository.duplicateOtherClientCode(3L,"DUP",1L,8L)).thenReturn(true);
        assertThatThrownBy(() -> flow.validateEdit(i,3L,new BigDecimal("100"),"DUP")).hasMessageContaining("registrado");
        when(accounts.findAll()).thenReturn(List.of(account));
        assertThat(flow.eligible(request,null)).isEmpty();
        assertThat(flow.eligible(request,i)).hasSize(1);
        verifyNoInteractions(balances);
    }
    @Test void preparationUsesAssignedAccountWithLockAndRejectsMissingAssignmentInsufficientFundsAndReplay() {
        var installment = new OperationReturnInstallment(); installment.setTipoPago(PaymentType.RETIRO_SIN_TARJETA); installment.setMonto(new BigDecimal("300"));
        assertThatThrownBy(() -> flow.prepare(request, installment)).hasMessageContaining("asignar");
        request.setCuentaOrigen(account); request.setCuentaAsignadaEn(LocalDateTime.now());
        when(accounts.findByIdForUpdate(3L)).thenReturn(Optional.of(account));
        var balance = new BankAccountBalanceDetailResponseDto(); balance.setSaldoFinal(new BigDecimal("200"));
        when(balances.calculateBalance(eq(3L), any())).thenReturn(balance);
        assertThatThrownBy(() -> flow.prepare(request, installment)).hasMessageContaining("insuficiente");
        balance.setSaldoFinal(new BigDecimal("1000")); flow.prepare(request, installment);
        assertThat(installment.getFechaPreparacionBancaria()).isNotNull(); assertThat(installment.getCuentaOrigen()).isSameAs(account);
        assertThatThrownBy(() -> flow.prepare(request, installment)).hasMessageContaining("ya fue preparada");
        var order = inOrder(accounts, balances); order.verify(accounts).findByIdForUpdate(3L); order.verify(balances).calculateBalance(eq(3L), any());
    }
    @Test void preparationCannotAffectOtherMethods() {
        request.setTipoPago(PaymentType.TRANSFERENCIA);
        assertThatThrownBy(() -> flow.prepare(request, new OperationReturnInstallment())).hasMessageContaining("solo aplica");
        verifyNoInteractions(accounts, balances);
    }
}
