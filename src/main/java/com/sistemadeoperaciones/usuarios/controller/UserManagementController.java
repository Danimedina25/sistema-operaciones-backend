package com.sistemadeoperaciones.usuarios.controller;

import com.sistemadeoperaciones.shared.dto.ApiResponse;
import com.sistemadeoperaciones.usuarios.dto.CreateUserRequestDto;
import com.sistemadeoperaciones.usuarios.dto.UpdateUserRequestDto;
import com.sistemadeoperaciones.usuarios.dto.UserResponseDto;
import com.sistemadeoperaciones.usuarios.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class UserManagementController {

    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponseDto>> create(@Valid @RequestBody CreateUserRequestDto request) {
        UserResponseDto response = userManagementService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(true, "Usuario creado exitosamente", response, null)
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> findAll() {
        List<UserResponseDto> response = userManagementService.findAll();

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Listado de usuarios obtenido exitosamente", response, null)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponseDto>> findById(@PathVariable Long id) {
        UserResponseDto response = userManagementService.findById(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Usuario obtenido exitosamente", response, null)
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponseDto>> update(@PathVariable Long id,
                                                               @Valid @RequestBody UpdateUserRequestDto request) {
        UserResponseDto response = userManagementService.update(id, request);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Usuario actualizado exitosamente", response, null)
        );
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponseDto>> deactivate(@PathVariable Long id) {
        UserResponseDto response = userManagementService.deactivate(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Usuario desactivado exitosamente", response, null)
        );
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponseDto>> activate(@PathVariable Long id) {
        UserResponseDto response = userManagementService.activate(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Usuario activado exitosamente", response, null)
        );
    }
}