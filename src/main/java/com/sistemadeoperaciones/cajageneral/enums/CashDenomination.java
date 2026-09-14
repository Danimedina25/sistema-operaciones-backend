package com.sistemadeoperaciones.cajageneral.enums;
import java.math.BigDecimal;
public enum CashDenomination {
    D1000("1000"), D500("500"), D200("200"), D100("100"), D50("50"), D20("20"),
    D10("10"), D5("5"), D2("2"), D1("1"), D050("0.50");
    private final BigDecimal value;
    CashDenomination(String value) { this.value = new BigDecimal(value); }
    public BigDecimal value() { return value; }
}
