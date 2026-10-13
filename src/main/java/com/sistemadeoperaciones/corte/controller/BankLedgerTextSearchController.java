package com.sistemadeoperaciones.corte.controller;

import com.sistemadeoperaciones.corte.dto.BankLedgerFilter;
import com.sistemadeoperaciones.corte.dto.BankLedgerRowDto;
import com.sistemadeoperaciones.corte.dto.BankLedgerTotalsDto;
import com.sistemadeoperaciones.corte.enums.BankLedgerDirection;
import com.sistemadeoperaciones.corte.service.BankLedgerService;
import com.sistemadeoperaciones.shared.dto.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** New read-only search on the derived bank ledger, applied before pagination and totals. */
@RestController
@RequestMapping("/api/bank-movements/search")
@PreAuthorize("hasAnyRole('ADMIN', 'JEFA_CUENTAS', 'GERENTE', 'DIRECCION', 'AUXILIAR_CUENTAS')")
public class BankLedgerTextSearchController {
    private final BankLedgerService ledger;

    public BankLedgerTextSearchController(BankLedgerService ledger) { this.ledger = ledger; }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BankLedgerRowDto>>> search(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(required = false) String banco,
            @RequestParam(required = false) BankLedgerDirection direccion,
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var filter = new BankLedgerFilter(desde, hasta, bankAccountId, banco, direccion, tipo);
        return ResponseEntity.ok(new ApiResponse<>(true, "Movimientos bancarios obtenidos", ledger.searchText(filter, search, page, size), null));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<BankLedgerTotalsDto>> summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(required = false) String banco,
            @RequestParam(required = false) BankLedgerDirection direccion,
            @RequestParam(required = false) String tipo) {
        var filter = new BankLedgerFilter(desde, hasta, bankAccountId, banco, direccion, tipo);
        return ResponseEntity.ok(new ApiResponse<>(true, "Totales de movimientos bancarios obtenidos", ledger.totalsText(filter, search), null));
    }
}
