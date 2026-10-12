package com.sistemadeoperaciones.pagos.controller;

import com.sistemadeoperaciones.cajageneral.service.CashGeneralService;
import com.sistemadeoperaciones.cajageneral.dto.CashLedgerResponse;
import com.sistemadeoperaciones.cajageneral.dto.CashMovementResponse;
import com.sistemadeoperaciones.cajageneral.enums.CashMovementConcept;
import com.sistemadeoperaciones.cajageneral.enums.CashMovementDirection;
import com.sistemadeoperaciones.clientes.model.Clientes;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashWorkControllerTest {
    @Mock OperationPaymentRepository payments;
    @Mock OperationReturnPaymentRepository returns;
    @Mock BankCashWithdrawalRepository withdrawals;
    @Mock ReturnPaymentDtoMapper mapper;
    @Mock CashGeneralService cash;
    CashWorkController controller;

    @BeforeEach void setup() { controller = new CashWorkController(payments, returns, withdrawals, mapper, cash); }

    @SuppressWarnings("unchecked")
    @Test void listsPaymentsAndRequestsAsIndividualItemsWithServerTotals() {
        var operation = new PaymentOperation(); operation.setId(42L);
        var client = new Clientes(); client.setNombre("Cliente Norte"); operation.setCliente(client);
        var partner = new User(); partner.setNombre("Socio Uno"); operation.setSocioComercial(partner);
        var cashPayment = payment(1L, operation, PaymentType.EFECTIVO, "100.00");
        var cheque = payment(2L, operation, PaymentType.CHEQUE, "200.00");
        cheque.setChequeEstado("POR_COBRAR");
        when(payments.findAll(any(Specification.class))).thenReturn(List.of(cashPayment, cheque));
        var request = new OperationReturnPayment();
        var anotherRequest = new OperationReturnPayment();
        var dto = new ReturnPaymentResponseDto(); dto.setId(8L); dto.setOperationId(42L);
        dto.setClienteNombre("Cliente Norte"); dto.setTipoPago(PaymentType.EFECTIVO);
        dto.setFechaSolicitud(LocalDateTime.of(2026, 10, 1, 10, 0));
        dto.setMontoPendiente(new BigDecimal("65.00"));
        var anotherDto = new ReturnPaymentResponseDto(); anotherDto.setId(9L); anotherDto.setOperationId(42L);
        anotherDto.setClienteNombre("Cliente Norte"); anotherDto.setTipoPago(PaymentType.RETIRO_SIN_TARJETA);
        anotherDto.setFechaSolicitud(LocalDateTime.of(2026, 10, 2, 10, 0));
        anotherDto.setMontoPendiente(new BigDecimal("35.00"));
        when(returns.findAll(any(Specification.class))).thenReturn(List.of(request, anotherRequest));
        when(mapper.toDto(request)).thenReturn(dto);
        when(mapper.toDto(anotherRequest)).thenReturn(anotherDto);
        when(withdrawals.findByEstatusOrderByRegistradoEnAscIdAsc(WithdrawalStatus.PENDIENTE)).thenReturn(List.of());

        var summary = controller.summary().getData();
        assertEquals(2, summary.income().count());
        assertEquals(new BigDecimal("300.00"), summary.income().amount());
        assertEquals(2, summary.outflow().count());
        assertEquals(new BigDecimal("100.00"), summary.outflow().amount());
        assertEquals(2, controller.income(0, 20, "", "").getData().totalElements());
        assertEquals(1, controller.income(0, 20, "", "CHEQUE").getData().totalElements());
        assertEquals(2, controller.outflow(0, 20, "Cliente", "").getData().totalElements());
        assertEquals(0, controller.outflow(0, 20, "desconocido", "").getData().totalElements());
    }

    private OperationPayment payment(Long id, PaymentOperation operation, PaymentType type, String amount) {
        var payment = new OperationPayment(); payment.setId(id); payment.setOperacion(operation);
        payment.setTipoPago(type); payment.setMonto(new BigDecimal(amount));
        payment.setEstatus(PaymentStatus.PENDIENTE_VALIDACION);
        payment.setFechaPago(LocalDateTime.of(2026, 10, 1, 10, 0));
        return payment;
    }

    @Test void cutTotalsComeFromReadOnlyLedgerAndKeepDirectionsSeparate() {
        var date = LocalDate.of(2020, 1, 1);
        when(cash.ledger(date, date)).thenReturn(new CashLedgerResponse(List.of(), List.of(
                movement(1L, date, CashMovementDirection.ENTRADA, "100.00"),
                movement(2L, date, CashMovementDirection.SALIDA, "25.00")
        ), List.of()));
        var result = controller.cutSummary(date, date).getData();
        assertEquals(new BigDecimal("100.00"), result.income());
        assertEquals(new BigDecimal("25.00"), result.outflow());
        assertEquals(2, result.movements());
        verify(cash).ledger(date, date);
    }

    private CashMovementResponse movement(Long id, LocalDate date, CashMovementDirection direction, String amount) {
        return new CashMovementResponse(id, 1L, date, date.atStartOfDay(), direction, CashMovementConcept.EFECTIVO,
                "Efectivo", null, null, null, null, null, null,
                new BigDecimal(amount), BigDecimal.ZERO, null, null, Map.of(), null, 1L);
    }
}
