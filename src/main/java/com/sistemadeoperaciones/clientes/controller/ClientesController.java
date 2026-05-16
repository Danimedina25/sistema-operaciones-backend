package com.sistemadeoperaciones.clientes.controller;

import com.sistemadeoperaciones.clientes.dto.ClienteResponseDto;
import com.sistemadeoperaciones.clientes.dto.CreateClienteRequestDto;
import com.sistemadeoperaciones.clientes.dto.UpdateClienteRequestDto;
import com.sistemadeoperaciones.clientes.service.ClientesService;
import com.sistemadeoperaciones.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClientesController {

    private final ClientesService clientesService;

    public ClientesController(ClientesService clientesService) {
        this.clientesService = clientesService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'SOCIO_COMERCIAL')")
    public ResponseEntity<ApiResponse<ClienteResponseDto>> create(
            @Valid @RequestBody CreateClienteRequestDto request
    ) {
        ClienteResponseDto response = clientesService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(true, "Cliente creado exitosamente", response, null)
        );
    }

    @GetMapping("/my_clients/{user_id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'SOCIO_COMERCIAL')")
    public ResponseEntity<ApiResponse<List<ClienteResponseDto>>> findAllByUserId(@PathVariable("user_id") Long userId) {
        List<ClienteResponseDto> response = clientesService.findAllByUserId(userId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Listado de clientes obtenido exitosamente", response, null)
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'SOCIO_COMERCIAL')")
    public ResponseEntity<ApiResponse<List<ClienteResponseDto>>> findAll() {
        List<ClienteResponseDto> response = clientesService.findAll();

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Listado de clientes obtenido exitosamente", response, null)
        );
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'SOCIO_COMERCIAL')")
    public ResponseEntity<ApiResponse<List<ClienteResponseDto>>> findActive() {
        List<ClienteResponseDto> response = clientesService.findActive();

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Listado de clientes activos obtenido exitosamente", response, null)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse<ClienteResponseDto>> findById(@PathVariable Long id) {
        ClienteResponseDto response = clientesService.findById(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Cliente obtenido exitosamente", response, null)
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse<ClienteResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateClienteRequestDto request
    ) {
        ClienteResponseDto response = clientesService.update(id, request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Cliente actualizado exitosamente", response, null)
        );
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse<ClienteResponseDto>> deactivate(@PathVariable Long id) {
        ClienteResponseDto response = clientesService.deactivate(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Cliente desactivado exitosamente", response, null)
        );
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public ResponseEntity<ApiResponse<ClienteResponseDto>> activate(@PathVariable Long id) {
        ClienteResponseDto response = clientesService.activate(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Cliente activado exitosamente", response, null)
        );
    }
}