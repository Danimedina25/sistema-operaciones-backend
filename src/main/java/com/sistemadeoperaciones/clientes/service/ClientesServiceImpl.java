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
import com.sistemadeoperaciones.pagos.repository.PaymentOperationRepository;
import com.sistemadeoperaciones.shared.audit.service.DeletionAuditService;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.enums.RoleName;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
import com.sistemadeoperaciones.shared.exception.EntityHasDependenciesException;
import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;
import com.sistemadeoperaciones.usuarios.model.User;
import com.sistemadeoperaciones.usuarios.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class ClientesServiceImpl implements ClientesService {

    private final ClientesRepository clientesRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final PaymentOperationRepository paymentOperationRepository;
    private final DeletionAuditService deletionAuditService;

    public ClientesServiceImpl(
            ClientesRepository clientesRepository,
            UserRepository userRepository,
            AuthenticatedUserService authenticatedUserService,
            PaymentOperationRepository paymentOperationRepository,
            DeletionAuditService deletionAuditService
    ) {
        this.clientesRepository = clientesRepository;
        this.userRepository = userRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.paymentOperationRepository = paymentOperationRepository;
        this.deletionAuditService = deletionAuditService;
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

        if (request.getPorcentajeComisionSocio().compareTo(BigDecimal.ZERO) < 0) {
            throw new ClienteInvalidCommissionException(
                    "El porcentaje de comisión por socio comercial no puede ser negativo"
            );
        }

        if (request.getPorcentajeComisionOficina().compareTo(BigDecimal.ZERO) < 0) {
            throw new ClienteInvalidCommissionException(
                    "El porcentaje de comisión de oficina no puede ser negativo"
            );
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con id: " + request.getUserId()));

        Clientes cliente = new Clientes();
        cliente.setUser(user);
        cliente.setNombre(request.getNombre().trim());
        cliente.setActivo(true);
        cliente.setNivelesRedComercial(request.getNivelesRedComercial());
        cliente.setPorcentajeComisionSocio(request.getPorcentajeComisionSocio());
        cliente.setPorcentajeComisionOficina(request.getPorcentajeComisionOficina());

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
    public List<ClienteResponseDto> searchActive(String nombre) {
        if (nombre == null || nombre.trim().length() < 2) {
            return List.of();
        }

        return clientesRepository
                .findTop20ByActivoTrueAndNombreContainingIgnoreCaseOrderByNombreAsc(nombre.trim())
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

        User currentUser = authenticatedUserService.getCurrentUser();

        boolean isAdminOrManager = currentUser.getRoles().stream()
                .anyMatch(role ->
                        role.getName() == RoleName.ADMIN
                                || role.getName() == RoleName.GERENTE
                );

        if (!isAdminOrManager) {
            Long ownerId = cliente.getUser() != null
                    ? cliente.getUser().getId()
                    : null;

            if (!currentUser.getId().equals(ownerId)) {
                throw new BadRequestException(
                        "No tiene permisos para acceder a este cliente"
                );
            }
        }

        if (request.getNombre() == null || request.getNombre().trim().isEmpty()) {
            throw new ClienteNameRequiredException();
        }

        if (clientesRepository.existsByNombreIgnoreCaseAndIdNot(request.getNombre().trim(), id)) {
            throw new ClienteAlreadyExistsException(request.getNombre());
        }

        if (request.getPorcentajeComisionSocio().compareTo(BigDecimal.ZERO) < 0) {
            throw new ClienteInvalidCommissionException(
                    "El porcentaje de comisión por socio comercial no puede ser negativo"
            );
        }

        if (request.getPorcentajeComisionOficina().compareTo(BigDecimal.ZERO) < 0) {
            throw new ClienteInvalidCommissionException(
                    "El porcentaje de comisión de oficina no puede ser negativo"
            );
        }

        cliente.setNombre(request.getNombre().trim());
        cliente.setActivo(request.getActivo());
        cliente.setNivelesRedComercial(request.getNivelesRedComercial());
        cliente.setPorcentajeComisionSocio(request.getPorcentajeComisionSocio());
        cliente.setPorcentajeComisionOficina(request.getPorcentajeComisionOficina());

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

    @Override
    @Transactional
    public void delete(Long id) {
        Clientes cliente = findClienteById(id);

        long operaciones = paymentOperationRepository.countByClienteId(id);
        if (operaciones > 0) {
            throw new EntityHasDependenciesException(
                    "No se puede eliminar el cliente porque tiene operaciones de pago registradas",
                    Map.of("operaciones", operaciones)
            );
        }

        User currentUser = authenticatedUserService.getCurrentUser();
        deletionAuditService.record("CLIENTE", cliente.getId(), cliente.getNombre(), currentUser);

        try {
            clientesRepository.delete(cliente);
            clientesRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("No se puede eliminar el cliente porque tiene información relacionada");
        }
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
                cliente.getPorcentajeComisionSocio(),
                cliente.getPorcentajeComisionOficina(),
                cliente.getCreatedAt(),
                cliente.getUpdatedAt()
        );
    }
}