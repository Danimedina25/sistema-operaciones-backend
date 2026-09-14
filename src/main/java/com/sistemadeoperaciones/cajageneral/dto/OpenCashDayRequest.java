package com.sistemadeoperaciones.cajageneral.dto;
import java.math.BigDecimal;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.*;
import java.util.*;
import jakarta.validation.constraints.*;
import com.sistemadeoperaciones.cajageneral.enums.*;
public record OpenCashDayRequest(@NotNull LocalDate fecha, @NotNull @DecimalMin("0") @Digits(integer=13, fraction=2) BigDecimal saldoInicial,
    @NotNull @JsonDeserialize(contentUsing = CashQuantityDeserializer.class) Map<CashDenomination, @NotNull @Min(0) Integer> denominaciones) {}
