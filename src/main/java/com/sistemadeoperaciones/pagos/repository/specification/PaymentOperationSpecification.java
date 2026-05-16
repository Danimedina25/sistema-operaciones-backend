package com.sistemadeoperaciones.pagos.repository.specification;

import com.sistemadeoperaciones.pagos.enums.OperationStatus;
import com.sistemadeoperaciones.pagos.enums.ReturnPaymentStatus;
import com.sistemadeoperaciones.pagos.model.PaymentOperation;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

public final class PaymentOperationSpecification {

    private PaymentOperationSpecification() {
    }
    public static Specification<PaymentOperation> clienteONombreSocioOIdContains(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }

            String searchTrimmed = search.trim();

            // Intentar búsqueda por ID exacto primero
            try {
                Long id = Long.valueOf(searchTrimmed);
                return cb.equal(root.get("id"), id);
            } catch (NumberFormatException e) {
                // Si no es un número válido, buscar por nombre
            }

            // Búsqueda por nombre de cliente o socio comercial
            String value = "%" + searchTrimmed.toLowerCase() + "%";

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

    public static Specification<PaymentOperation> hasReturnWithStatus(ReturnPaymentStatus status) {
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);

            Join<Object, Object> retornos = root.join("retornos");

            return criteriaBuilder.equal(
                    retornos.get("estatus"),
                    status
            );
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

    public static Specification<PaymentOperation> hasStatusIn(List<OperationStatus> statuses) {
        return (root, query, cb) -> {
            if (statuses == null || statuses.isEmpty()) {
                return cb.conjunction();
            }

            return root.get("estatus").in(statuses);
        };
    }
}