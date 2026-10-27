package com.sistemadeoperaciones.pagos.service;
import com.sistemadeoperaciones.corte.service.BankAccountDailyCutService;
import com.sistemadeoperaciones.cuentasbancarias.repository.BankAccountRepository;
import com.sistemadeoperaciones.notifications.service.NotificationService;
import com.sistemadeoperaciones.notifications.enums.*;
import com.sistemadeoperaciones.pagos.model.*;
import com.sistemadeoperaciones.pagos.repository.*;
import com.sistemadeoperaciones.pagos.enums.*;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
import com.sistemadeoperaciones.shared.enums.RoleName;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class CardlessReturnFlow {
    private final OperationReturnPaymentRepository requests;
    private final PaymentOperationRepository operations;
    private final BankAccountRepository accounts;
    private final BankAccountDailyCutService balances;
    private final AuthenticatedUserService auth;
    private final BusinessAuditService audit;
    private final NotificationService notifications;
    private final ReturnPaymentDtoMapper mapper;
    @org.springframework.beans.factory.annotation.Autowired
    private com.sistemadeoperaciones.retiroscaja.BankAvailability availability;
    @org.springframework.beans.factory.annotation.Autowired
    private OperationReturnInstallmentRepository codeRepository;
    @org.springframework.beans.factory.annotation.Autowired
    private com.sistemadeoperaciones.cajageneral.repository.CashGeneralRegisterRepository periodLock;
    private void lockMoney() { periodLock.ensureRegister(); periodLock.lockRegister(); }
    @org.springframework.beans.factory.annotation.Autowired
    private com.sistemadeoperaciones.retiroscaja.BankReservationGuard reservationGuard;
    public void protectOtherPayment(Long accountId, java.math.BigDecimal amount) { reservationGuard.protect(accountId, amount); }
    public void reserveCode(OperationReturnInstallment installment, Long accountId) {
        if (accountId == null) throw new BadRequestException("Selecciona la cuenta origen");
        lockMoney();
        var account = accounts.findByIdForUpdate(accountId).orElseThrow(() -> new BadRequestException("Cuenta no encontrada"));
        if (!Boolean.TRUE.equals(account.getActivo())) throw new BadRequestException("La cuenta está inactiva");
        if (codeRepository.duplicateClientCode(accountId, installment.getCodigoRetiroSinTarjeta(), installment.getSolicitud().getId()))
            throw new BadRequestException("Este código ya está registrado para esta cuenta y solicitud o sigue pendiente de cobro");
        if (availability.available(accountId).compareTo(remaining(installment.getSolicitud())) < 0)
            throw new BadRequestException("Saldo disponible insuficiente: considera los códigos y retiros pendientes");
        installment.setCuentaOrigen(account);
        installment.setCodigoParaCliente(true);
    }
    public java.math.BigDecimal remaining(OperationReturnPayment request) {
        return request.getMonto().subtract(Objects.requireNonNullElse(codeRepository.sumCompletedBySolicitud(request.getId()), java.math.BigDecimal.ZERO))
            .subtract(Objects.requireNonNullElse(codeRepository.sumInFlightBySolicitud(request.getId()), java.math.BigDecimal.ZERO));
    }
    public void validateEdit(OperationReturnInstallment i, Long accountId, java.math.BigDecimal amount, String code) {
        if (accountId == null) throw new BadRequestException("Selecciona la cuenta origen");
        lockMoney();
        var account = accounts.findByIdForUpdate(accountId).orElseThrow(() -> new BadRequestException("Cuenta no encontrada"));
        if (!Boolean.TRUE.equals(account.getActivo())) throw new BadRequestException("La cuenta está inactiva");
        var usable = availability.available(accountId);
        if (i.getCuentaOrigen().getId().equals(accountId)) usable = usable.add(i.getMonto());
        if (usable.compareTo(amount) < 0) throw new BadRequestException("Saldo disponible insuficiente para este código");
        if (codeRepository.duplicateOtherClientCode(accountId, code, i.getSolicitud().getId(), i.getId()))
            throw new BadRequestException("Este código ya está registrado para esta cuenta");
        i.setCuentaOrigen(account);
    }
    public java.util.List<com.sistemadeoperaciones.cuentasbancarias.dto.BankAccountResponseDto> eligible(OperationReturnPayment request, OperationReturnInstallment editing) {
        var required = editing == null ? remaining(request) : editing.getMonto();
        if (required.signum() <= 0) return java.util.List.of();
        return accounts.findAll().stream().filter(a -> Boolean.TRUE.equals(a.getActivo()))
            .filter(a -> availability.available(a.getId()).add(editing != null && editing.getCuentaOrigen().getId().equals(a.getId()) ? editing.getMonto() : java.math.BigDecimal.ZERO).compareTo(required) >= 0)
            .map(a -> new com.sistemadeoperaciones.cuentasbancarias.dto.BankAccountResponseDto(a.getId(), a.getBanco(), a.getTitular(), a.getNumeroCuenta(), a.getClabe(), a.getActivo(), a.getCreatedAt(), a.getUpdatedAt())).toList();
    }
    public void lockCodeAccount(OperationReturnInstallment installment) {
        lockMoney();
        accounts.findByIdForUpdate(installment.getCuentaOrigen().getId()).orElseThrow(() -> new BadRequestException("Cuenta no encontrada"));
    }
    public CardlessReturnFlow(OperationReturnPaymentRepository requests, PaymentOperationRepository operations,
            BankAccountRepository accounts, BankAccountDailyCutService balances, AuthenticatedUserService auth,
            BusinessAuditService audit, NotificationService notifications, ReturnPaymentDtoMapper mapper) {
        this.requests = requests; this.operations = operations; this.accounts = accounts; this.balances = balances;
        this.auth = auth; this.audit = audit; this.notifications = notifications; this.mapper = mapper;
    }
    @Transactional
    public com.sistemadeoperaciones.pagos.dto.retornos.ReturnPaymentResponseDto assign(Long id, Long accountId) {
        throw new BadRequestException("Registra la cuenta y el código de cada parcialidad desde Retornos");
    }
    /** Dentro de la transacción de creación, después de bloquear operación y solicitud. */
    public void prepare(OperationReturnPayment request, OperationReturnInstallment installment) {
        if (request.getTipoPago() != PaymentType.RETIRO_SIN_TARJETA || installment.getTipoPago() != PaymentType.RETIRO_SIN_TARJETA)
            throw new BadRequestException("La preparación bancaria solo aplica a retiro sin tarjeta");
        if (installment.getFechaPreparacionBancaria() != null || installment.getId() != null)
            throw new BadRequestException("La parcialidad ya fue preparada");
        if (request.getCuentaOrigen() == null || request.getCuentaAsignadaEn() == null)
            throw new BadRequestException("Jefa de Cuentas debe asignar la cuenta antes de preparar el retiro");
        var account = accounts.findByIdForUpdate(request.getCuentaOrigen().getId())
                .orElseThrow(() -> new BadRequestException("Cuenta no encontrada"));
        if (!Boolean.TRUE.equals(account.getActivo())) throw new BadRequestException("La cuenta está inactiva");
        var balance = balances.calculateBalance(account.getId(), LocalDate.now()).getSaldoFinal();
        if (balance == null || balance.compareTo(installment.getMonto()) < 0)
            throw new BadRequestException("Saldo bancario insuficiente para preparar el retiro");
        installment.setCuentaOrigen(account);
        installment.setFechaPreparacionBancaria(LocalDateTime.now());
    }
}
