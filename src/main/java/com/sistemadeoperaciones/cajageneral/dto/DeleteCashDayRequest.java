package com.sistemadeoperaciones.cajageneral.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeleteCashDayRequest(
        @NotBlank @Size(max = 20) String confirmacion,
        @NotBlank @Size(max = 500) String motivo,
        @NotNull Long version
) {}
