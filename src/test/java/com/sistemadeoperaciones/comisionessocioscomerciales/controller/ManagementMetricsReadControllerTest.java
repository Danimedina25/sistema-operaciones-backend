package com.sistemadeoperaciones.comisionessocioscomerciales.controller;

import com.sistemadeoperaciones.comisionessocioscomerciales.dto.response.CommissionOperationSummaryResponseDto;
import com.sistemadeoperaciones.comisionessocioscomerciales.dto.response.CommissionSummaryResponseDto;
import com.sistemadeoperaciones.comisionessocioscomerciales.service.CommercialPartnerCommissionService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ManagementMetricsReadControllerTest {
    @Test void countsEachOperationOnlyOnce() {
        var service = mock(CommercialPartnerCommissionService.class);
        var first = new CommissionOperationSummaryResponseDto(); first.setOperationId(1L); first.setMontoOperacion(new BigDecimal("100.00"));
        var duplicate = new CommissionOperationSummaryResponseDto(); duplicate.setOperationId(1L); duplicate.setMontoOperacion(new BigDecimal("100.00"));
        var second = new CommissionOperationSummaryResponseDto(); second.setOperationId(2L); second.setMontoOperacion(new BigDecimal("250.00"));
        var summary = new CommissionSummaryResponseDto(); summary.setOperaciones(List.of(first, duplicate, second));
        var start = LocalDate.now().minusDays(1); var end = LocalDate.now();
        when(service.getSummary(start, end)).thenReturn(summary);
        var actual = new ManagementMetricsReadController(service).operated(start, end).getData();
        assertEquals(new BigDecimal("350.00"), actual.operatedAmount());
        assertEquals(2, actual.operatedOperations());
    }
}
