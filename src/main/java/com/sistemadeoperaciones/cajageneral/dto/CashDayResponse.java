package com.sistemadeoperaciones.cajageneral.dto;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import jakarta.validation.constraints.*;
import com.sistemadeoperaciones.cajageneral.enums.*;
public record CashDayResponse(Long id, LocalDate fecha, Long version, BigDecimal saldoInicial, BigDecimal saldoActual,
    BigDecimal saldoContado, BigDecimal diferencia, Map<CashDenomination, Integer> apertura,
    Map<CashDenomination, Integer> cierre,
    /** Desglose que debería haber: apertura + entradas − salidas. Null en cajas ya cerradas. */
    Map<CashDenomination, Integer> denominacionesEsperadas,
    String observacionesCierre, LocalDateTime createdAt,
    LocalDateTime closedAt, Long abiertoPor, String abiertoPorNombre, Long cerradoPor) {}
