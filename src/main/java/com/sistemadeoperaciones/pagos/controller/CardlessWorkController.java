package com.sistemadeoperaciones.pagos.controller;
import com.sistemadeoperaciones.pagos.repository.*;
import com.sistemadeoperaciones.pagos.service.ReturnPaymentDtoMapper;
import com.sistemadeoperaciones.pagos.enums.*;
import com.sistemadeoperaciones.shared.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.math.BigDecimal;
@RestController
@RequestMapping("/api/operations/returns/cardless-work")
@PreAuthorize("hasAnyRole('JEFA_CUENTAS','AUXILIAR_CUENTAS','ADMIN','GERENTE','DIRECCION')")
@Transactional(readOnly=true)
public class CardlessWorkController {
 private final OperationReturnPaymentRepository requests; private final OperationReturnInstallmentRepository installments; private final ReturnPaymentDtoMapper mapper;
 public CardlessWorkController(OperationReturnPaymentRepository r, OperationReturnInstallmentRepository i, ReturnPaymentDtoMapper m) { requests=r;installments=i;mapper=m; }
 public record Task(Long requestId, Long operationId, String cliente, BigDecimal porRegistrar, BigDecimal porConfirmar, java.time.LocalDateTime fecha) {}
 @GetMapping public ApiResponse<List<Task>> list() {
  var rows=requests.findAll((r,q,c)->c.and(c.equal(r.get("tipoPago"),PaymentType.RETIRO_SIN_TARJETA),c.isTrue(r.get("operacion").get("activo")),c.notEqual(r.get("estatus"),ReturnPaymentStatus.RETORNADO))).stream().map(r->{
   var dto=mapper.toDto(r);
   var history=installments.findBySolicitudIdOrderByCreatedAtAsc(r.getId());
   var pending=history.stream().filter(i->Boolean.TRUE.equals(i.getCodigoParaCliente())&&i.getFechaCobroBancario()==null&&i.getEstatus()!=ReturnInstallmentStatus.CANCELADA).map(i->i.getMonto()).reduce(BigDecimal.ZERO,BigDecimal::add);
   boolean historic=history.stream().anyMatch(i->!Boolean.TRUE.equals(i.getCodigoParaCliente())&&i.getEstatus()!=ReturnInstallmentStatus.CANCELADA);
   return new Task(r.getId(),r.getOperacion().getId(),dto.getClienteNombre(),historic ? BigDecimal.ZERO : dto.getMontoDisponible(),pending,r.getFechaSolicitud());
  }).filter(t->t.porRegistrar().signum()>0||t.porConfirmar().signum()>0).sorted(Comparator.comparing(Task::requestId)).toList();
  return new ApiResponse<>(true,"Retiros sin tarjeta pendientes",rows,null);
 }
}
