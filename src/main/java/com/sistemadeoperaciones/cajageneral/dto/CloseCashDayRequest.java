package com.sistemadeoperaciones.cajageneral.dto;
import java.math.BigDecimal;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.*;
import java.util.*;
import jakarta.validation.constraints.*;
import com.sistemadeoperaciones.cajageneral.enums.*;
public record CloseCashDayRequest(@NotNull @DecimalMin("0") @Digits(integer=13, fraction=2) BigDecimal saldoContado,
    @NotNull Long version, @NotNull @JsonDeserialize(contentUsing = CashQuantityDeserializer.class) Map<CashDenomination, @NotNull @Min(0) Integer> denominaciones,
    @Size(max=500) String observaciones) {}
