package com.sistemadeoperaciones.usuarios.service.comercialpartners;


import com.sistemadeoperaciones.usuarios.dto.request.UpdateCommercialPartnerSettingsRequestDto;
import com.sistemadeoperaciones.usuarios.dto.response.CommercialPartnerSettingsResponseDto;

public interface CommercialPartnerSettingsService {

    CommercialPartnerSettingsResponseDto findByUserId(Long userId);

    CommercialPartnerSettingsResponseDto updateByUserId(Long userId, UpdateCommercialPartnerSettingsRequestDto request);
}