package com.sistemadeoperaciones.retiroscaja;
import com.sistemadeoperaciones.pagos.repository.OperationReturnInstallmentRepository;
import com.sistemadeoperaciones.cajageneral.repository.CashGeneralRegisterRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class BankReservationGuardTest {
 @Test void reservedCodesReduceAvailableFundsAndProtectOtherPayments() {
  var balances=mock(com.sistemadeoperaciones.corte.service.BankAccountDailyCutService.class);
  var withdrawals=mock(BankCashWithdrawalRepository.class);
  var codes=mock(OperationReturnInstallmentRepository.class);
  var lock=mock(CashGeneralRegisterRepository.class);
  var balance=new com.sistemadeoperaciones.corte.dto.BankAccountBalanceDetailResponseDto(); balance.setSaldoFinal(new BigDecimal("1000"));
  when(balances.calculateBalance(eq(1L),any())).thenReturn(balance);
  when(withdrawals.sumPendingByAccount(1L)).thenReturn(new BigDecimal("100"));
  when(codes.sumReservedCodes(1L)).thenReturn(new BigDecimal("300"));
  var availability=new BankAvailability(balances,withdrawals,codes);
  assertThat(availability.available(1L)).isEqualByComparingTo("600");
  var guard=new BankReservationGuard(availability,codes,lock);
  guard.protect(1L,new BigDecimal("600"));
  assertThatThrownBy(()->guard.protect(1L,new BigDecimal("601"))).hasMessageContaining("reservados");
  verify(lock,times(2)).lockRegister();
 }
}
