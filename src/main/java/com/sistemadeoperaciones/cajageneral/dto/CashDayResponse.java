package com.sistemadeoperaciones.cajageneral.dto;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import jakarta.validation.constraints.*;
import com.sistemadeoperaciones.cajageneral.enums.*;
public record CashDayResponse(Long id, LocalDate fecha, Long version, BigDecimal saldoInicial, BigDecimal saldoActual,
    BigDecimal saldoContado, BigDecimal diferencia, Map<CashDenomination, Integer> apertura,
    Map<CashDenomination, Integer> cierre, String observacionesCierre, LocalDateTime closedAt,
    Long abiertoPor, Long cerradoPor) {}
