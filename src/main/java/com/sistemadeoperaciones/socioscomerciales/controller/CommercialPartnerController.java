package com.sistemadeoperaciones.socioscomerciales.controller;

import com.sistemadeoperaciones.shared.dto.ApiResponse;
import com.sistemadeoperaciones.socioscomerciales.dto.CommercialPartnerRequestDTO;
import com.sistemadeoperaciones.socioscomerciales.dto.CommercialPartnerResponseDto;
import com.sistemadeoperaciones.socioscomerciales.service.CommercialPartnerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/commercial-partners")
public class CommercialPartnerController {

    private final CommercialPartnerService commercialPartnerService;

    public CommercialPartnerController(
            CommercialPartnerService commercialPartnerService
    ) {
        this.commercialPartnerService = commercialPartnerService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION', 'SOCIO_COMERCIAL')")
    public ResponseEntity<ApiResponse<CommercialPartnerResponseDto>> create(
            @Valid @RequestBody CommercialPartnerRequestDTO request
    ) {

        CommercialPartnerResponseDto response =
                commercialPartnerService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(
                        true,
                        "Socio comercial registrado exitosamente",
                        response,
                        null
                )
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION', 'SOCIO_COMERCIAL')")
    public ResponseEntity<ApiResponse<Page<CommercialPartnerResponseDto>>> findAll(

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "id")
            String sortBy,

            @RequestParam(defaultValue = "asc")
            String sortDir,

            @RequestParam(required = false)
            Boolean activo,

            @RequestParam(required = false)
            String nombre,

            @RequestParam(required = false)
            String cuentaBancaria
    ) {

        Page<CommercialPartnerResponseDto> response =
                commercialPartnerService.findAll(
                        page,
                        size,
                        sortBy,
                        sortDir,
                        activo,
                        nombre,
                        cuentaBancaria
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Listado de socios comerciales obtenido exitosamente",
                        response,
                        null
                )
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION', 'SOCIO_COMERCIAL')")
    public ResponseEntity<ApiResponse<CommercialPartnerResponseDto>> findById(
            @PathVariable Long id
    ) {

        CommercialPartnerResponseDto response =
                commercialPartnerService.findById(id);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Socio comercial obtenido exitosamente",
                        response,
                        null
                )
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION', 'SOCIO_COMERCIAL')")
    public ResponseEntity<ApiResponse<CommercialPartnerResponseDto>> update(

            @PathVariable Long id,

            @Valid @RequestBody CommercialPartnerRequestDTO request
    ) {

        CommercialPartnerResponseDto response =
                commercialPartnerService.update(id, request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Socio comercial actualizado exitosamente",
                        response,
                        null
                )
        );
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION')")
    public ResponseEntity<ApiResponse<CommercialPartnerResponseDto>>
    deactivateCommercialPartner(
            @PathVariable Long id
    ) {

        CommercialPartnerResponseDto response =
                commercialPartnerService.deactivate(id);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Socio comercial desactivado exitosamente",
                        response,
                        null
                )
        );
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION')")
    public ResponseEntity<ApiResponse<CommercialPartnerResponseDto>>
    activateCommercialPartner(
            @PathVariable Long id
    ) {

        CommercialPartnerResponseDto response =
                commercialPartnerService.activate(id);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Socio comercial activado exitosamente",
                        response,
                        null
                )
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECCION')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        commercialPartnerService.delete(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Socio comercial eliminado permanentemente", null, null)
        );
    }
}