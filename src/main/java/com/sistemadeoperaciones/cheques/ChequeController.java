package com.sistemadeoperaciones.cheques;
import com.sistemadeoperaciones.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
@RestController @RequestMapping("/api/operations/cheques")
@PreAuthorize("hasAnyRole('ADMIN','JEFA_CUENTAS','AUXILIAR_CUENTAS','JEFA_CAJAS','GERENTE','DIRECCION')")
@lombok.RequiredArgsConstructor
public class ChequeController {
    private final ChequeService service;
    @GetMapping public ApiResponse<ChequeView.Page> list(
            @RequestParam(required=false) String estados,
            @RequestParam(defaultValue="") String busqueda, @RequestParam(defaultValue="") String cliente,
            @RequestParam(required=false) Long operacionId, @RequestParam(defaultValue="") String banco,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue="0") int page) {
        return new ApiResponse<>(true,"Cheques consultados",service.list(estados == null ? "POR_COBRAR,DEPOSITADO,PENDIENTE_COBRO_EFECTIVO" : estados,busqueda,cliente,operacionId,banco,desde,hasta,page),null);
    }
    @GetMapping("/by-payment/{paymentId}") public ApiResponse<ChequeView> get(@PathVariable Long paymentId) {
        return new ApiResponse<>(true,"Cheque consultado",service.byPayment(paymentId),null);
    }
    @PostMapping("/{id}/actions") public ApiResponse<ChequeView> act(@PathVariable Long id, @Valid @RequestBody ChequeCommand command) {
        return new ApiResponse<>(true,"Acción registrada",service.act(id,command),null);
    }
}
