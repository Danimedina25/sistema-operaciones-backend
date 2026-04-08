package com.sistemadeoperaciones.usuarios.service;

import com.sistemadeoperaciones.usuarios.dto.*;

import java.util.List;

public interface UserManagementService {

    UserResponseDto create(CreateUserRequestDto request);

    List<UserResponseDto> findAll();

    UserResponseDto findById(Long id);

    UserResponseDto update(Long id, UpdateUserRequestDto request);

    UserResponseDto deactivate(Long id);

    UserResponseDto activate(Long id);
    SocioComercialCreatedResponseDto createSocioComercial(CreateSocioComercialRequestDto request);
    void completeSocioComercialActivation(CompleteSocioComercialActivationRequestDto request);
}