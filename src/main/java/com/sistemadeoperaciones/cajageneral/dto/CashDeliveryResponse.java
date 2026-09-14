package com.sistemadeoperaciones.cajageneral.dto;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import jakarta.validation.constraints.*;
import com.sistemadeoperaciones.cajageneral.enums.*;
public record CashDeliveryResponse(Long id, Long operacionId, BigDecimal monto, LocalDateTime fechaRealizacion, String personaQueRecibioEfectivo) {}
