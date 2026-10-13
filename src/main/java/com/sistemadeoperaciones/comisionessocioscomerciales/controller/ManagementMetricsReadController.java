package com.sistemadeoperaciones.comisionessocioscomerciales.controller;

import com.sistemadeoperaciones.comisionessocioscomerciales.dto.response.CommissionOperationSummaryResponseDto;
import com.sistemadeoperaciones.comisionessocioscomerciales.service.CommercialPartnerCommissionService;
import com.sistemadeoperaciones.shared.dto.ApiResponse;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

/** Backend amount for dashboard cards; each operation contributes once. */
@RestController
@RequestMapping("/api/management-metrics")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION')")
@Transactional(readOnly = true)
public class ManagementMetricsReadController {
    private final CommercialPartnerCommissionService commissions;
    public ManagementMetricsReadController(CommercialPartnerCommissionService commissions) { this.commissions = commissions; }
    public record Metrics(BigDecimal operatedAmount, int operatedOperations) {}

    @GetMapping("/operated")
    public ApiResponse<Metrics> operated(@RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
        if (endDate.isBefore(startDate) || endDate.isAfter(LocalDate.now()) || endDate.isAfter(startDate.plusYears(1)))
            throw new BadRequestException("Periodo inválido");
        List<CommissionOperationSummaryResponseDto> operations = commissions.getSummary(startDate, endDate).getOperaciones();
        var seen = new HashSet<Long>();
        BigDecimal amount = BigDecimal.ZERO;
        if (operations != null) for (var operation : operations) {
            if (operation.getOperationId() != null && seen.add(operation.getOperationId()) && operation.getMontoOperacion() != null)
                amount = amount.add(operation.getMontoOperacion());
        }
        return new ApiResponse<>(true, "Monto operado", new Metrics(amount, seen.size()), null);
    }
}
