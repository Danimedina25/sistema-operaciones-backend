package com.sistemadeoperaciones.clientes.service;

import com.sistemadeoperaciones.clientes.dto.ClienteResponseDto;
import com.sistemadeoperaciones.clientes.dto.CreateClienteRequestDto;
import com.sistemadeoperaciones.clientes.dto.UpdateClienteRequestDto;
import com.sistemadeoperaciones.clientes.exceptions.ClienteAlreadyExistsException;
import com.sistemadeoperaciones.clientes.exceptions.ClienteInvalidCommissionException;
import com.sistemadeoperaciones.clientes.exceptions.ClienteNameRequiredException;
import com.sistemadeoperaciones.clientes.exceptions.ClienteNotFoundException;
import com.sistemadeoperaciones.clientes.model.Clientes;
import com.sistemadeoperaciones.clientes.repository.ClientesRepository;
import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;
import com.sistemadeoperaciones.usuarios.model.User;
import com.sistemadeoperaciones.usuarios.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ClientesServiceImpl implements ClientesService {

    private final ClientesRepository clientesRepository;
    private final UserRepository userRepository;

    public ClientesServiceImpl(ClientesRepository clientesRepository, UserRepository userRepository) {
        this.clientesRepository = clientesRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ClienteResponseDto create(CreateClienteRequestDto request) {

        if (request.getNombre() == null || request.getNombre().trim().isEmpty()) {
            throw new ClienteNameRequiredException();
        }

        if (clientesRepository.existsByNombreIgnoreCase(request.getNombre().trim())) {
            throw new ClienteAlreadyExistsException(request.getNombre());
        }

        if (request.getPorcentajeComisionAplicado().compareTo(BigDecimal.ZERO) < 0) {
            throw new ClienteInvalidCommissionException();
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con id: " + request.getUserId()));

        Clientes cliente = new Clientes();
        cliente.setUser(user);
        cliente.setNombre(request.getNombre().trim());
        cliente.setActivo(true);
        cliente.setNivelesRedComercial(request.getNivelesRedComercial());
        cliente.setPorcentajeComisionAplicado(request.getPorcentajeComisionAplicado());

        Clientes saved = clientesRepository.save(cliente);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDto> findAll() {
        return clientesRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDto> findAllByUserId(Long userId) {
        return clientesRepository.findByUserIdOrderByNombreAsc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDto> findActive() {
        return clientesRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDto findById(Long id) {
        Clientes cliente = findClienteById(id);
        return mapToResponse(cliente);
    }

    @Override
    @Transactional
    public ClienteResponseDto update(Long id, UpdateClienteRequestDto request) {
        Clientes cliente = findClienteById(id);

        if (request.getNombre() == null || request.getNombre().trim().isEmpty()) {
            throw new ClienteNameRequiredException();
        }

        if (clientesRepository.existsByNombreIgnoreCaseAndIdNot(request.getNombre().trim(), id)) {
            throw new ClienteAlreadyExistsException(request.getNombre());
        }

        if (request.getPorcentajeComisionAplicado().compareTo(BigDecimal.ZERO) < 0) {
            throw new ClienteInvalidCommissionException();
        }

        cliente.setNombre(request.getNombre().trim());
        cliente.setActivo(request.getActivo());
        cliente.setNivelesRedComercial(request.getNivelesRedComercial());
        cliente.setPorcentajeComisionAplicado(request.getPorcentajeComisionAplicado());

        Clientes updated = clientesRepository.save(cliente);

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public ClienteResponseDto deactivate(Long id) {
        Clientes cliente = findClienteById(id);
        cliente.setActivo(false);

        Clientes updated = clientesRepository.save(cliente);

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public ClienteResponseDto activate(Long id) {
        Clientes cliente = findClienteById(id);
        cliente.setActivo(true);

        Clientes updated = clientesRepository.save(cliente);

        return mapToResponse(updated);
    }

    private Clientes findClienteById(Long id) {
        return clientesRepository.findById(id)
                .orElseThrow(() -> new ClienteNotFoundException(id));
    }

    private ClienteResponseDto mapToResponse(Clientes cliente) {
        return new ClienteResponseDto(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getActivo(),
                cliente.getNivelesRedComercial(),
                cliente.getPorcentajeComisionAplicado(),
                cliente.getCreatedAt(),
                cliente.getUpdatedAt()
        );
    }
}