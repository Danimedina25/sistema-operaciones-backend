package com.sistemadeoperaciones.pagos.repository.specification;

import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.enums.ReturnPaymentStatus;
import com.sistemadeoperaciones.pagos.model.OperationReturnPayment;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

public final class OperationReturnPaymentSpecification {

    private OperationReturnPaymentSpecification() {
    }

    public static Specification<OperationReturnPayment> hasTipoPagoIn(List<PaymentType> tipos) {
        return (root, query, cb) -> {
            if (tipos == null || tipos.isEmpty()) {
                return cb.conjunction();
            }

            return root.get("tipoPago").in(tipos);
        };
    }

    public static Specification<OperationReturnPayment> hasEstatus(ReturnPaymentStatus estatus) {
        return (root, query, cb) -> {
            if (estatus == null) {
                return cb.conjunction();
            }

            return cb.equal(root.get("estatus"), estatus);
        };
    }

    public static Specification<OperationReturnPayment> fechaRecoleccionBetween(
            LocalDateTime start,
            LocalDateTime end
    ) {
        return (root, query, cb) -> {
            if (start == null && end == null) {
                return cb.conjunction();
            }

            if (start != null && end != null) {
                return cb.between(root.get("fechaHoraRecoleccionEfectivo"), start, end);
            }

            if (start != null) {
                return cb.greaterThanOrEqualTo(root.get("fechaHoraRecoleccionEfectivo"), start);
            }

            return cb.lessThanOrEqualTo(root.get("fechaHoraRecoleccionEfectivo"), end);
        };
    }
}
