package com.sistemadeoperaciones.configuraciones.controller;

import com.sistemadeoperaciones.configuraciones.dto.ConfiguracionGeneralRequestDTO;
import com.sistemadeoperaciones.configuraciones.dto.ConfiguracionGeneralResponseDto;
import com.sistemadeoperaciones.configuraciones.service.ConfiguracionGeneralService;
import com.sistemadeoperaciones.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/configuraciones")
public class ConfiguracionGeneralController {

    private final ConfiguracionGeneralService configuracionGeneralService;

    public ConfiguracionGeneralController(
            ConfiguracionGeneralService configuracionGeneralService
    ) {
        this.configuracionGeneralService = configuracionGeneralService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION')")
    public ResponseEntity<ApiResponse<ConfiguracionGeneralResponseDto>> obtener() {

        ConfiguracionGeneralResponseDto response =
                configuracionGeneralService.obtenerResponse();

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Configuración general obtenida exitosamente",
                        response,
                        null
                )
        );
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION')")
    public ResponseEntity<ApiResponse<ConfiguracionGeneralResponseDto>> actualizar(
            @Valid @RequestBody ConfiguracionGeneralRequestDTO request
    ) {

        ConfiguracionGeneralResponseDto response =
                configuracionGeneralService.actualizar(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Configuración general actualizada exitosamente",
                        response,
                        null
                )
        );
    }
}
