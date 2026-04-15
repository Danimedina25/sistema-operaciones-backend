package com.sistemadeoperaciones.usuarios.controller;

import com.sistemadeoperaciones.shared.dto.ApiResponse;
import com.sistemadeoperaciones.usuarios.dto.request.UpdateCommercialPartnerSettingsRequestDto;
import com.sistemadeoperaciones.usuarios.dto.response.CommercialPartnerSettingsResponseDto;
import com.sistemadeoperaciones.usuarios.service.comercialpartners.CommercialPartnerSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users/{userId}/commercial-settings")
public class CommercialPartnerSettingsController {

    private final CommercialPartnerSettingsService commercialPartnerSettingsService;

    public CommercialPartnerSettingsController(CommercialPartnerSettingsService commercialPartnerSettingsService) {
        this.commercialPartnerSettingsService = commercialPartnerSettingsService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse<CommercialPartnerSettingsResponseDto>> findByUserId(
            @PathVariable Long userId
    ) {
        CommercialPartnerSettingsResponseDto response =
                commercialPartnerSettingsService.findByUserId(userId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Configuración comercial obtenida exitosamente",
                        response,
                        null
                )
        );
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse<CommercialPartnerSettingsResponseDto>> updateByUserId(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateCommercialPartnerSettingsRequestDto request
    ) {
        CommercialPartnerSettingsResponseDto response =
                commercialPartnerSettingsService.updateByUserId(userId, request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Configuración comercial actualizada exitosamente",
                        response,
                        null
                )
        );
    }
}