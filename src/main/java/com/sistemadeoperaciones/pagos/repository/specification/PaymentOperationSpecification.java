package com.sistemadeoperaciones.pagos.repository.specification;

import com.sistemadeoperaciones.pagos.enums.OperationStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
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
        return hasReturnWithStatusIn(List.of(status));
    }

    public static Specification<PaymentOperation> hasReturnWithStatusIn(List<ReturnPaymentStatus> statuses) {
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);

            if (statuses == null || statuses.isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            Join<Object, Object> retornos = root.join("retornos");

            return retornos.get("estatus").in(statuses);
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

    public static Specification<PaymentOperation> hasStatusNotIn(List<OperationStatus> statuses) {
        return (root, query, cb) -> {
            if (statuses == null || statuses.isEmpty()) {
                return cb.conjunction();
            }

            return cb.not(root.get("estatus").in(statuses));
        };
    }

    public static Specification<PaymentOperation> updatedAtBefore(LocalDateTime threshold) {
        return (root, query, cb) -> {
            if (threshold == null) {
                return cb.conjunction();
            }

            return cb.lessThan(root.get("updatedAt"), threshold);
        };
    }

    /** Type and status must match the SAME payment; distinct keeps operation totals exact. */
    public static Specification<PaymentOperation> hasPaymentMatching(List<PaymentType> types, PaymentStatus status) {
        return (root, query, cb) -> {
            if ((types == null || types.isEmpty()) && status == null) return cb.conjunction();
            query.distinct(true);
            var payment = root.join("pagos");
            return cb.and(types == null || types.isEmpty() ? cb.conjunction() : payment.get("tipoPago").in(types),
                    status == null ? cb.conjunction() : cb.equal(payment.get("estatus"), status));
        };
    }

    /** A request must still have amount available for a new installment (reserved cash is excluded). */
    public static Specification<PaymentOperation> hasReturnToPrepare(List<PaymentType> types) {
        return hasReturnToPrepare(types, null);
    }
    public static Specification<PaymentOperation> hasReturnToPrepare(List<PaymentType> types, List<ReturnPaymentStatus> statuses) {
        return (root, query, cb) -> {
            var requestQuery = query.subquery(Long.class);
            var request = requestQuery.from(com.sistemadeoperaciones.pagos.model.OperationReturnPayment.class);
            var used = requestQuery.subquery(java.math.BigDecimal.class);
            var installment = used.from(com.sistemadeoperaciones.pagos.model.OperationReturnInstallment.class);
            used.select(cb.coalesce(cb.sum(installment.<java.math.BigDecimal>get("monto")), java.math.BigDecimal.ZERO));
            used.where(cb.equal(installment.get("solicitud"), request),
                    cb.notEqual(installment.get("estatus"), com.sistemadeoperaciones.pagos.enums.ReturnInstallmentStatus.CANCELADA));
            requestQuery.select(request.get("id")).where(
                    cb.equal(request.get("operacion"), root), request.get("tipoPago").in(types),
                    statuses == null || statuses.isEmpty() ? cb.conjunction() : request.get("estatus").in(statuses),
                    cb.notEqual(request.get("estatus"), ReturnPaymentStatus.RETORNADO),
                    cb.greaterThan(request.<java.math.BigDecimal>get("monto"), used));
            return cb.exists(requestQuery);
        };
    }

    public static Specification<PaymentOperation> hasPaymentTypeIn(List<PaymentType> paymentTypes) {
        return (root, query, cb) -> {
            if (paymentTypes == null || paymentTypes.isEmpty()) {
                return cb.conjunction();
            }

            query.distinct(true);

            Join<Object, Object> pagos = root.join("pagos");

            return pagos.get("tipoPago").in(paymentTypes);
        };
    }

    public static Specification<PaymentOperation> hasPaymentStatus(PaymentStatus paymentStatus) {
        return (root, query, cb) -> {
            if (paymentStatus == null) {
                return cb.conjunction();
            }

            query.distinct(true);

            Join<Object, Object> pagos = root.join("pagos");

            return cb.equal(pagos.get("estatus"), paymentStatus);
        };
    }

    public static Specification<PaymentOperation> hasPaymentCuentaDestinoId(Long cuentaDestinoId) {
        return (root, query, cb) -> {
            if (cuentaDestinoId == null) {
                return cb.conjunction();
            }

            query.distinct(true);

            Join<Object, Object> pagos = root.join("pagos");

            return cb.equal(pagos.get("cuentaDestino").get("id"), cuentaDestinoId);
        };
    }

    public static Specification<PaymentOperation> hasPaymentBanco(String banco) {
        return (root, query, cb) -> {
            if (banco == null || banco.isBlank()) {
                return cb.conjunction();
            }

            query.distinct(true);

            Join<Object, Object> pagos = root.join("pagos");
            Join<Object, Object> cuentaDestino = pagos.join("cuentaDestino");

            return cb.equal(cuentaDestino.get("banco"), banco);
        };
    }

    public static Specification<PaymentOperation> matchesActivoFilter(String activoFilter) {
        return (root, query, cb) -> {
            if (activoFilter == null || activoFilter.isBlank() || "ACTIVE".equalsIgnoreCase(activoFilter)) {
                return cb.isTrue(root.get("activo"));
            }

            if ("INACTIVE".equalsIgnoreCase(activoFilter)) {
                return cb.isFalse(root.get("activo"));
            }

            return cb.conjunction();
        };
    }
}