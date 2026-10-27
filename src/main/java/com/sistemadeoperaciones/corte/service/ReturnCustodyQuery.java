package com.sistemadeoperaciones.corte.service;
import com.sistemadeoperaciones.pagos.repository.OperationReturnInstallmentRepository;
import com.sistemadeoperaciones.pagos.model.OperationReturnInstallment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ReturnCustodyQuery {
    private final OperationReturnInstallmentRepository installments;
    public ReturnCustodyQuery(OperationReturnInstallmentRepository installments) { this.installments = installments; }
    public record Custody(BigDecimal initial, BigDecimal prepared, BigDecimal delivered, BigDecimal balance,
            List<OperationReturnInstallment> rows) {}
    public Custody read(LocalDate start, LocalDate end) {
        var rows = installments.findByFechaPreparacionBancariaIsNotNull();
        var first = start.atStartOfDay(); var last = end.plusDays(1).atStartOfDay();
        BigDecimal initial = BigDecimal.ZERO, prepared = BigDecimal.ZERO, delivered = BigDecimal.ZERO, balance = BigDecimal.ZERO;
        for (var row : rows) {
            var date = row.getFechaPreparacionBancaria(); var delivery = row.getFechaEntrega(); var amount = row.getMonto();
            if (date.isBefore(first) && (delivery == null || !delivery.isBefore(first))) initial = initial.add(amount);
            if (!date.isBefore(first) && date.isBefore(last)) prepared = prepared.add(amount);
            if (date.isBefore(last) && delivery != null && !delivery.isBefore(first) && delivery.isBefore(last)) delivered = delivered.add(amount);
            if (date.isBefore(last) && (delivery == null || !delivery.isBefore(last))) balance = balance.add(amount);
        }
        return new Custody(initial, prepared, delivered, balance, rows);
    }
}
