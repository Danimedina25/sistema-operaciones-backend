package com.sistemadeoperaciones.usuarios.service;

import com.sistemadeoperaciones.usuarios.dto.request.*;
import com.sistemadeoperaciones.usuarios.dto.response.UserCreatedResponseDto;
import com.sistemadeoperaciones.usuarios.dto.response.UserResponseDto;

import java.util.List;

public interface UserManagementService {

    UserCreatedResponseDto create(CreateUserRequestDto request);

    void completeUserActivation(CompleteUserActivationRequestDto request);
    void completeEmailVerification(CompleteEmailVerificationRequestDto request);
    void resendActivationEmail(Long userId);

    void updateUserEmail(Long userId, UpdateUserEmailRequestDto request);

    List<UserResponseDto> findAll();

    UserResponseDto findById(Long id);

    UserResponseDto update(Long id, UpdateUserRequestDto request);

    UserResponseDto deactivate(Long id);

    UserResponseDto activate(Long id);

    void delete(Long id);

}