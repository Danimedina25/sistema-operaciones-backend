package com.sistemadeoperaciones.retiroscaja;
import com.sistemadeoperaciones.pagos.repository.OperationReturnInstallmentRepository;
import com.sistemadeoperaciones.cajageneral.repository.CashGeneralRegisterRepository;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
@Service
@lombok.RequiredArgsConstructor
public class BankReservationGuard {
 private final BankAvailability availability;
 private final OperationReturnInstallmentRepository codes;
 private final CashGeneralRegisterRepository moneyLock;
 public void protect(Long accountId, BigDecimal outgoing) {
  moneyLock.ensureRegister(); moneyLock.lockRegister();
  var reserved=codes.sumReservedCodes(accountId);
  if (reserved != null && reserved.signum()>0 && availability.available(accountId).compareTo(outgoing)<0)
   throw new BadRequestException("Saldo insuficiente: la cuenta tiene códigos de retiro reservados");
 }
}
