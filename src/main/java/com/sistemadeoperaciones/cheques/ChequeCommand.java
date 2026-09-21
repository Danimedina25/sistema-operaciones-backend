package com.sistemadeoperaciones.cheques;
import com.sistemadeoperaciones.cajageneral.enums.CashDenomination;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
public record ChequeCommand(@NotNull UUID requestId, @NotNull @PositiveOrZero Long version,
        @NotNull ChequeAction accion, @NotNull @PastOrPresent LocalDate fecha,
        @Positive Long cuentaDestinoId, @Size(max=500) String comprobanteUrl,
        @Size(max=500) String motivo, @Positive Long diaCajaId,
        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(contentUsing=com.sistemadeoperaciones.cajageneral.dto.CashQuantityDeserializer.class)
        Map<CashDenomination, Integer> denominaciones) {}
