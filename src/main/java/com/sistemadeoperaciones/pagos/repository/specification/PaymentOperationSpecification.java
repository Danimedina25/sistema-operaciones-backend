package com.sistemadeoperaciones.pagos.repository.specification;

import com.sistemadeoperaciones.pagos.enums.OperationStatus;
import com.sistemadeoperaciones.pagos.model.PaymentOperation;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class PaymentOperationSpecification {

    private PaymentOperationSpecification() {
    }
    public static Specification<PaymentOperation> clienteONombreSocioContains(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }

            String value = "%" + search.trim().toLowerCase() + "%";

            var clientePredicate =
                    cb.like(cb.lower(root.get("cliente").get("nombre")), value);

            var socioPredicate =
                    cb.like(cb.lower(root.get("socioComercial").get("nombre")), value);

            return cb.or(clientePredicate, socioPredicate);
        };
    }

    public static Specification<PaymentOperation> hasStatus(OperationStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("estatus"), status);
        };
    }

    public static Specification<PaymentOperation> hasSocioComercialId(Long socioComercialId) {
        return (root, query, cb) -> {
            if (socioComercialId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("socioComercial").get("id"), socioComercialId);
        };
    }

    public static Specification<PaymentOperation> createdAtBetween(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {
        return (root, query, cb) -> {
            if (startDateTime == null && endDateTime == null) {
                return cb.conjunction();
            }

            if (startDateTime != null && endDateTime != null) {
                return cb.between(root.get("createdAt"), startDateTime, endDateTime);
            }

            if (startDateTime != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), startDateTime);
            }

            return cb.lessThanOrEqualTo(root.get("createdAt"), endDateTime);
        };
    }
}