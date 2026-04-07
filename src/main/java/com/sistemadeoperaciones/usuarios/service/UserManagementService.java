package com.sistemadeoperaciones.usuarios.service;

import com.sistemadeoperaciones.usuarios.dto.CreateUserRequestDto;
import com.sistemadeoperaciones.usuarios.dto.UpdateUserRequestDto;
import com.sistemadeoperaciones.usuarios.dto.UserResponseDto;

import java.util.List;

public interface UserManagementService {

    UserResponseDto create(CreateUserRequestDto request);

    List<UserResponseDto> findAll();

    UserResponseDto findById(Long id);

    UserResponseDto update(Long id, UpdateUserRequestDto request);

    UserResponseDto deactivate(Long id);

    UserResponseDto activate(Long id);
}