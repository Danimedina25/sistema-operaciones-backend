package com.sistemadeoperaciones.cajageneral.service;
import com.sistemadeoperaciones.cajageneral.enums.CashDenomination;
import com.sistemadeoperaciones.cajageneral.exceptions.InvalidCashGeneralException;
import java.math.*;
import java.util.*;
public final class CashGeneralAmounts {
    private CashGeneralAmounts() {}
    public static BigDecimal money(BigDecimal amount, boolean positive) {
        if (amount == null || amount.signum() < 0 || (positive && amount.signum() == 0)
                || amount.compareTo(new BigDecimal("9999999999999.99")) > 0)
            throw new InvalidCashGeneralException("Importe inválido");
        try { return amount.setScale(2, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException ex) { throw new InvalidCashGeneralException("El importe admite máximo dos decimales"); }
    }
    public static BigDecimal total(Map<CashDenomination, Integer> counts) {
        if (counts == null || counts.size() != CashDenomination.values().length)
            throw new InvalidCashGeneralException("Captura las 11 denominaciones, usando cero donde no aplique");
        BigDecimal total = BigDecimal.ZERO;
        for (CashDenomination denomination : CashDenomination.values()) {
            Integer quantity = counts.get(denomination);
            if (quantity == null || quantity < 0)
                throw new InvalidCashGeneralException("Las cantidades deben ser enteros no negativos");
            total = total.add(denomination.value().multiply(BigDecimal.valueOf(quantity)));
        }
        return money(total, false);
    }
    public static void requireTotal(Map<CashDenomination, Integer> counts, BigDecimal amount) {
        if (total(counts).compareTo(money(amount, false)) != 0)
            throw new InvalidCashGeneralException("El desglose debe sumar exactamente el importe");
    }
    public static BigDecimal balance(BigDecimal previous, BigDecimal incoming, BigDecimal outgoing) {
        BigDecimal next = money(previous, false).add(money(incoming, false)).subtract(money(outgoing, false));
        if (next.signum() < 0) throw new InvalidCashGeneralException("Saldo insuficiente: la caja no puede quedar negativa");
        return money(next, false);
    }
}
