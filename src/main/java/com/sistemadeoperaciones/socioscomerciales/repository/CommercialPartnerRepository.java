package com.sistemadeoperaciones.socioscomerciales.repository;

import com.sistemadeoperaciones.socioscomerciales.models.CommercialPartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface CommercialPartnerRepository
        extends JpaRepository<CommercialPartner, Long>, JpaSpecificationExecutor<CommercialPartner> {

    boolean existsByCorreo(String correo);

    boolean existsByCorreoAndIdNot(String correo, Long id);

    List<CommercialPartner> findBySocioPadreId(Long socioPadreId);
}