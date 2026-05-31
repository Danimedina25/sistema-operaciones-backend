package com.sistemadeoperaciones.socioscomerciales.specification;

import com.sistemadeoperaciones.socioscomerciales.models.CommercialPartner;
import org.springframework.data.jpa.domain.Specification;

public class CommercialPartnerSpecification {

    public static Specification<CommercialPartner> hasActivo(
            Boolean activo
    ) {

        return (root, query, cb) -> {

            if (activo == null) {
                return cb.conjunction();
            }

            return cb.equal(
                    root.get("activo"),
                    activo
            );
        };
    }

    public static Specification<CommercialPartner> hasNombre(
            String nombre
    ) {

        return (root, query, cb) -> {

            if (nombre == null || nombre.isBlank()) {
                return cb.conjunction();
            }

            return cb.like(
                    cb.lower(root.get("nombre")),
                    "%" + nombre.toLowerCase() + "%"
            );
        };
    }

    public static Specification<CommercialPartner> hasCuentaBancaria(
            String cuentaBancaria
    ) {

        return (root, query, cb) -> {

            if (cuentaBancaria == null || cuentaBancaria.isBlank()) {
                return cb.conjunction();
            }

            return cb.equal(
                    root.get("cuentaBancaria"),
                    cuentaBancaria
            );
        };
    }

    public static Specification<CommercialPartner> hasSocioComercialId(
            Long socioComercialId
    ) {

        return (root, query, cb) -> {

            if (socioComercialId == null) {
                return cb.conjunction();
            }

            return cb.equal(
                    root.get("socioComercial").get("id"),
                    socioComercialId
            );
        };
    }
}