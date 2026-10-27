package com.sistemadeoperaciones.pagos.controller;
import com.sistemadeoperaciones.pagos.repository.*;
import com.sistemadeoperaciones.pagos.service.ReturnPaymentDtoMapper;
import com.sistemadeoperaciones.pagos.dto.retornos.ReturnPaymentResponseDto;
import com.sistemadeoperaciones.pagos.model.*;
import com.sistemadeoperaciones.pagos.enums.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import java.util.*;
import java.math.BigDecimal;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.*;
class CardlessWorkControllerTest {
 @SuppressWarnings("unchecked")
 @Test void countsRequestOnceAndKeepsFullyRegisteredCodesPendingUntilBankConfirms() {
  var requests=mock(OperationReturnPaymentRepository.class); var codes=mock(OperationReturnInstallmentRepository.class); var mapper=mock(ReturnPaymentDtoMapper.class);
  var operation=new PaymentOperation();operation.setId(47L);
  var request=new OperationReturnPayment();request.setId(39L);request.setOperacion(operation);
  var dto=new ReturnPaymentResponseDto();dto.setMontoDisponible(new BigDecimal("200"));dto.setClienteNombre("Cliente");
  var code=new OperationReturnInstallment();code.setCodigoParaCliente(true);code.setMonto(new BigDecimal("100"));code.setEstatus(ReturnInstallmentStatus.PROGRAMADA);
  when(requests.findAll(any(Specification.class))).thenReturn(List.of(request));when(mapper.toDto(request)).thenReturn(dto);when(codes.findBySolicitudIdOrderByCreatedAtAsc(39L)).thenReturn(List.of(code));
  var controller=new CardlessWorkController(requests,codes,mapper);
  var task=controller.list().getData().get(0);
  assertThat(task.operationId()).isEqualTo(47L); assertThat(task.porRegistrar()).isEqualByComparingTo("200");assertThat(task.porConfirmar()).isEqualByComparingTo("100");
  dto.setMontoDisponible(BigDecimal.ZERO);assertThat(controller.list().getData()).hasSize(1);
  code.setFechaCobroBancario(java.time.LocalDateTime.now());assertThat(controller.list().getData()).isEmpty();
 }
}
