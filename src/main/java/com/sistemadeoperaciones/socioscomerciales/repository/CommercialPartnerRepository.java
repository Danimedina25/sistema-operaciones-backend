package com.sistemadeoperaciones.socioscomerciales.repository;

import com.sistemadeoperaciones.socioscomerciales.models.CommercialPartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CommercialPartnerRepository
        extends JpaRepository<CommercialPartner, Long>,
        JpaSpecificationExecutor<CommercialPartner> {

}