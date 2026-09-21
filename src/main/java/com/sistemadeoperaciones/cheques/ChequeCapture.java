package com.sistemadeoperaciones.cheques;

import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.model.OperationPayment;
import com.sistemadeoperaciones.shared.exception.BusinessException;
import com.sistemadeoperaciones.shared.exception.ConflictException;

/** The payment row is the unique cheque record; no duplicate amount or payment identity. */
public final class ChequeCapture {
    private ChequeCapture() {}
    public static void apply(OperationPayment payment, PaymentType nextType, Long account,
                             String number, String bank, String issuer, String beneficiary) {
        if (payment.getTipoPago() == PaymentType.CHEQUE) {
            if (!"POR_COBRAR".equals(payment.getChequeEstado())) throw new ConflictException("El cheque no admite edición ordinaria");
            if (nextType != PaymentType.CHEQUE) throw new ConflictException("Cancela el cheque y registra un pago sustituto para conservar su historial");
        }
        if (nextType != PaymentType.CHEQUE) return;
        if (account != null) throw new BusinessException("La cuenta del cheque se define al gestionar su cobro");
        if (java.util.stream.Stream.of(number, bank, issuer, beneficiary).anyMatch(v -> v == null || v.isBlank() || v.length() > 200))
            throw new BusinessException("Completa la identificación del cheque");
        payment.setNumeroCheque(number.trim()); payment.setBancoEmisor(bank.trim());
        payment.setEmisor(issuer.trim()); payment.setBeneficiario(beneficiary.trim());
        payment.setChequeEstado("POR_COBRAR"); payment.setChequeVersion(payment.getChequeVersion() + 1);
    }
}
