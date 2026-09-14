package com.sistemadeoperaciones.cajageneral.dto;
import java.math.BigDecimal;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.*;
import java.util.*;
import jakarta.validation.constraints.*;
import com.sistemadeoperaciones.cajageneral.enums.*;
public record CreateCashMovementRequest(@NotNull UUID requestId, @NotNull CashMovementDirection direccion, @NotNull CashMovementConcept tipo,
    @NotBlank @Size(max=300) String concepto, @Size(max=50) String banco,
    @DecimalMin("0.01") @Digits(integer=13, fraction=2) BigDecimal monto, Long parcialidadId,
    @NotNull @JsonDeserialize(contentUsing = CashQuantityDeserializer.class) Map<CashDenomination, @NotNull @Min(0) Integer> denominaciones,
    @Size(max=500) String comprobanteUrl) {}
