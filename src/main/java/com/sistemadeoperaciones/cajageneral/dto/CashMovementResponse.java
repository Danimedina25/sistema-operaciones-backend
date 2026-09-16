package com.sistemadeoperaciones.cajageneral.dto;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import jakarta.validation.constraints.*;
import com.sistemadeoperaciones.cajageneral.enums.*;
public record CashMovementResponse(Long id, Long diaId, LocalDate fecha, LocalDateTime createdAt, CashMovementDirection direccion,
    CashMovementConcept tipo, String concepto, String banco, Long bankAccountId, String cuentaBanco, String cuentaTitular,
    String cuentaNumero, Boolean cuentaActiva, BigDecimal monto, BigDecimal saldoAcumulado,
    Long parcialidadId, Long operacionId, Map<CashDenomination, Integer> denominaciones, String comprobanteUrl, Long creadoPor) {}
