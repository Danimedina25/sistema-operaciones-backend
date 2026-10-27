package com.sistemadeoperaciones.pagos.controller;

import com.sistemadeoperaciones.clientes.model.Clientes;
import com.sistemadeoperaciones.envioscaja.CashBankDepositRepository;
import com.sistemadeoperaciones.envioscaja.CashBankDeposit;
import com.sistemadeoperaciones.pagos.dto.retornos.ReturnPaymentResponseDto;
import com.sistemadeoperaciones.pagos.enums.PaymentStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.model.OperationPayment;
import com.sistemadeoperaciones.pagos.model.OperationReturnPayment;
import com.sistemadeoperaciones.pagos.model.PaymentOperation;
import com.sistemadeoperaciones.pagos.repository.OperationPaymentRepository;
import com.sistemadeoperaciones.pagos.repository.OperationReturnPaymentRepository;
import com.sistemadeoperaciones.pagos.service.ReturnPaymentDtoMapper;
import com.sistemadeoperaciones.retiroscaja.BankCashWithdrawalRepository;
import com.sistemadeoperaciones.retiroscaja.WithdrawalStatus;
import com.sistemadeoperaciones.usuarios.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankWorkControllerTest {
    @Mock OperationPaymentRepository payments;
    @Mock CashBankDepositRepository deposits;
    @Mock OperationReturnPaymentRepository returns;
    @Mock ReturnPaymentDtoMapper mapper;
    @Mock BankCashWithdrawalRepository withdrawals;
    BankWorkController controller;

    @BeforeEach void setup() { controller = new BankWorkController(payments, returns, mapper, withdrawals, deposits); }

    @SuppressWarnings("unchecked")
    @Test void countsPaymentsAndReturnRequestsOnceAndUsesAvailableAmount() {
        var operation = new PaymentOperation(); operation.setId(42L);
        var client = new Clientes(); client.setNombre("Cliente Norte"); operation.setCliente(client);
        var partner = new User(); partner.setNombre("Socio Uno"); operation.setSocioComercial(partner);
        var transfer = payment(1L, operation, PaymentType.TRANSFERENCIA, "100.00");
        var deposit = payment(2L, operation, PaymentType.DEPOSITO, "200.00");
        when(payments.findAll(any(Specification.class))).thenReturn(List.of(transfer, deposit));
        var first = new OperationReturnPayment();
        var second = new OperationReturnPayment();
        var firstDto = request(8L, "65.00");
        var secondDto = request(9L, "35.00");
        when(returns.findAll(any(Specification.class))).thenReturn(List.of(first, second));
        when(mapper.toDto(first)).thenReturn(firstDto);
        when(mapper.toDto(second)).thenReturn(secondDto);
        when(withdrawals.findByEstatusOrderByRegistradoEnAscIdAsc(WithdrawalStatus.PENDIENTE)).thenReturn(List.of());

        var summary = controller.summary().getData();
        assertEquals(2, summary.income().count());
        assertEquals(new BigDecimal("300.00"), summary.income().amount());
        assertEquals(2, summary.outflow().count());
        assertEquals(new BigDecimal("100.00"), summary.outflow().amount());
        assertEquals(0, summary.withdrawalsInTransit().count());
        assertEquals(2, controller.income(0, 20, "Cliente", "").getData().totalElements());
        assertEquals(1, controller.income(0, 20, "", "DEPOSITO").getData().totalElements());
        assertEquals(2, controller.outflow(0, 20, "42").getData().totalElements());
        assertEquals(0, controller.outflow(0, 20, "desconocido").getData().totalElements());
    }

    @SuppressWarnings("unchecked")
    @Test void chequeInSharedDepositQueueIsNotCountedTwice() {
        var operation = new PaymentOperation(); operation.setId(42L);
        var client = new Clientes(); client.setNombre("Cliente Norte"); operation.setCliente(client);
        var partner = new User(); partner.setNombre("Socio Uno"); operation.setSocioComercial(partner);
        var linked = payment(10L, operation, PaymentType.CHEQUE, "100.00"); linked.setChequeEstado("DEPOSITADO");
        var legacy = payment(11L, operation, PaymentType.CHEQUE, "200.00"); legacy.setChequeEstado("DEPOSITADO");
        when(payments.findAll(any(Specification.class))).thenReturn(List.of(linked, legacy));
        when(deposits.findByChequePaymentIdOrderByIdDesc(10L)).thenReturn(List.of(new CashBankDeposit()));
        when(deposits.findByChequePaymentIdOrderByIdDesc(11L)).thenReturn(List.of());
        assertEquals(1, controller.summary().getData().income().count());
        assertEquals(new BigDecimal("200.00"), controller.summary().getData().income().amount());
        assertEquals(1, controller.income(0, 20, "", "CHEQUE").getData().totalElements());
    }

    @SuppressWarnings("unchecked")
    @Test void cardlessAssignmentBelongsOnlyToAccountsHeadQueue() {
        var entity = new OperationReturnPayment();
        var dto = request(39L, "300.00"); dto.setTipoPago(PaymentType.RETIRO_SIN_TARJETA);
        when(returns.findAll(any(Specification.class))).thenReturn(List.of(entity));
        when(mapper.toDto(entity)).thenReturn(dto);
        var context = org.springframework.security.core.context.SecurityContextHolder.getContext();
        try {
            context.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("aux", "", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_AUXILIAR_CUENTAS"))));
            assertEquals(1, controller.outflow(0,20,"").getData().totalElements());
            context.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("head", "", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_JEFA_CUENTAS"))));
            assertEquals(1, controller.outflow(0,20,"").getData().totalElements());
            dto.setNumeroParcialidades(1);
            assertEquals(0, controller.outflow(0,20,"").getData().totalElements());
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    }

    private OperationPayment payment(Long id, PaymentOperation operation, PaymentType type, String amount) {
        var payment = new OperationPayment(); payment.setId(id); payment.setOperacion(operation);
        payment.setTipoPago(type); payment.setMonto(new BigDecimal(amount));
        payment.setEstatus(PaymentStatus.PENDIENTE_VALIDACION);
        payment.setFechaPago(LocalDateTime.of(2026, 10, 1, 10, 0));
        return payment;
    }

    private ReturnPaymentResponseDto request(Long id, String available) {
        var dto = new ReturnPaymentResponseDto(); dto.setId(id); dto.setOperationId(42L);
        dto.setClienteNombre("Cliente Norte"); dto.setTipoPago(PaymentType.TRANSFERENCIA);
        dto.setFechaSolicitud(LocalDateTime.of(2026, 10, 1, 10, 0));
        dto.setMontoDisponible(new BigDecimal(available));
        return dto;
    }
}
