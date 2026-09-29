package com.sistemadeoperaciones.cajageneral.dto;
import java.util.*;
public record CashLedgerResponse(List<CashDayResponse> dias, List<CashMovementResponse> movimientos,
    List<CashWithdrawalGroup> grupos) {}
