package com.sistemadeoperaciones.cajageneral.dto;
import java.time.*;
import java.util.*;
import jakarta.validation.constraints.*;
import com.sistemadeoperaciones.cajageneral.enums.*;
public record CashLedgerResponse(List<CashDayResponse> dias, List<CashMovementResponse> movimientos) {}
