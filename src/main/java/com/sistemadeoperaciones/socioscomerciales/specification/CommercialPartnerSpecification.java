package com.sistemadeoperaciones.socioscomerciales.specification;

import com.sistemadeoperaciones.socioscomerciales.models.CommercialPartner;
import org.springframework.data.jpa.domain.Specification;

public class CommercialPartnerSpecification {

    public static Specification<CommercialPartner> hasActivo(Boolean activo) {
        return (root, query, cb) ->
                activo == null ? null : cb.equal(root.get("activo"), activo);
    }

    public static Specification<CommercialPartner> hasNombre(String nombre) {
        return (root, query, cb) ->
                (nombre == null || nombre.isBlank())
                        ? null
                        : cb.like(cb.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%");
    }

    public static Specification<CommercialPartner> hasCorreo(String correo) {
        return (root, query, cb) ->
                (correo == null || correo.isBlank())
                        ? null
                        : cb.like(cb.lower(root.get("correo")), "%" + correo.toLowerCase() + "%");
    }

    public static Specification<CommercialPartner> hasSocioPadreId(Long socioPadreId) {
        return (root, query, cb) ->
                socioPadreId == null
                        ? null
                        : cb.equal(root.get("socioPadre").get("id"), socioPadreId);
    }
}