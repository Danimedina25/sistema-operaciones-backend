package com.sistemadeoperaciones.retiroscaja;

import com.sistemadeoperaciones.cajageneral.enums.CashMovementConcept;

/**
 * Cómo salió el efectivo de la cuenta. Negocio agrupa la entrada a Caja General por forma y
 * banco —"RETIRO 16 DE 16 TD BANORTE", "RETIRO 9 DE 9 RST SCOTIABANK", "CHEQUES BAJIO"—, así
 * que la forma sí separa grupos.
 */
public enum WithdrawalMethod {
    RETIRO_CON_TARJETA(CashMovementConcept.RETIRO_CON_TARJETA, "TD", "Retiro con tarjeta"),
    RETIRO_SIN_TARJETA(CashMovementConcept.RETIRO_SIN_TARJETA, "RST", "Retiro sin tarjeta"),
    COBRO_CHEQUE(CashMovementConcept.CHEQUE, "CHEQUE", "Cobro de cheque");

    private final CashMovementConcept cashConcept;
    private final String abbreviation;
    private final String label;

    WithdrawalMethod(CashMovementConcept cashConcept, String abbreviation, String label) {
        this.cashConcept = cashConcept;
        this.abbreviation = abbreviation;
        this.label = label;
    }

    /** Concepto con el que entra a Caja General. */
    public CashMovementConcept cashConcept() { return cashConcept; }
    public String abbreviation() { return abbreviation; }
    public String label() { return label; }
}
