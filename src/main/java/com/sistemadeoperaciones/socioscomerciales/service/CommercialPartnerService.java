package com.sistemadeoperaciones.socioscomerciales.service;

import com.sistemadeoperaciones.cuentasbancarias.dto.BankAccountResponseDto;
import com.sistemadeoperaciones.socioscomerciales.dto.CommercialPartnerNetworkDto;
import com.sistemadeoperaciones.socioscomerciales.dto.CommercialPartnerRequestDto;
import com.sistemadeoperaciones.socioscomerciales.dto.CommercialPartnerResponseDto;
import org.springframework.data.domain.Page;

public interface CommercialPartnerService {

    CommercialPartnerResponseDto create(CommercialPartnerRequestDto request);

    Page<CommercialPartnerResponseDto> findAll(
            int page,
            int size,
            String sortBy,
            String sortDir,
            Boolean activo,
            String nombre,
            String correo,
            Long socioPadreId
    );

    CommercialPartnerResponseDto findById(Long id);

    CommercialPartnerResponseDto update(Long id, CommercialPartnerRequestDto request);

    CommercialPartnerNetworkDto getNetworkById(Long id);

    CommercialPartnerResponseDto deactivate(Long id);

    CommercialPartnerResponseDto activate(Long id);
}