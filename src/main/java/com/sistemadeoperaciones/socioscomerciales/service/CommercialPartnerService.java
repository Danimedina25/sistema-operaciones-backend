package com.sistemadeoperaciones.socioscomerciales.service;

import com.sistemadeoperaciones.socioscomerciales.dto.CommercialPartnerRequestDTO;
import com.sistemadeoperaciones.socioscomerciales.dto.CommercialPartnerResponseDto;
import org.springframework.data.domain.Page;

public interface CommercialPartnerService {

    CommercialPartnerResponseDto create(
            CommercialPartnerRequestDTO request
    );

    Page<CommercialPartnerResponseDto> findAll(
            int page,
            int size,
            String sortBy,
            String sortDir,
            Boolean activo,
            String nombre,
            String cuentaBancaria
    );

    CommercialPartnerResponseDto findById(Long id);

    CommercialPartnerResponseDto update(
            Long id,
            CommercialPartnerRequestDTO request
    );

    CommercialPartnerResponseDto activate(Long id);

    CommercialPartnerResponseDto deactivate(Long id);

    void delete(Long id);
}