package com.sistemadeoperaciones.retiroscaja;

import com.sistemadeoperaciones.cajageneral.enums.CashDenomination;
import java.util.Map;

/** Cajas confirma sobre la caja abierta que tiene a la vista y con el desglose que contó. */
public record ConfirmWithdrawalRequest(Long diaCajaId, Map<CashDenomination, Integer> denominaciones) {}
