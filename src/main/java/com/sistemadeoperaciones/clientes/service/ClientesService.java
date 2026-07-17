package com.sistemadeoperaciones.clientes.service;

import com.sistemadeoperaciones.clientes.dto.ClienteResponseDto;
import com.sistemadeoperaciones.clientes.dto.CreateClienteRequestDto;
import com.sistemadeoperaciones.clientes.dto.UpdateClienteRequestDto;

import java.util.List;

public interface ClientesService {

    ClienteResponseDto create(CreateClienteRequestDto request);

    List<ClienteResponseDto> findAll();

    List<ClienteResponseDto> findAllByUserId(Long userId);

    List<ClienteResponseDto> findActive();

    List<ClienteResponseDto> searchActive(String nombre);

    ClienteResponseDto findById(Long id);

    ClienteResponseDto update(Long id, UpdateClienteRequestDto request);

    ClienteResponseDto deactivate(Long id);

    ClienteResponseDto activate(Long id);

    void delete(Long id);
}