package com.sistemadeoperaciones.clientes.service;

import com.sistemadeoperaciones.clientes.dto.ClienteResponseDto;
import com.sistemadeoperaciones.clientes.dto.CreateClienteRequestDto;
import com.sistemadeoperaciones.clientes.dto.UpdateClienteRequestDto;
import com.sistemadeoperaciones.clientes.exceptions.ClienteAlreadyExistsException;
import com.sistemadeoperaciones.clientes.exceptions.ClienteNameRequiredException;
import com.sistemadeoperaciones.clientes.exceptions.ClienteNotFoundException;
import com.sistemadeoperaciones.clientes.model.Clientes;
import com.sistemadeoperaciones.clientes.repository.ClientesRepository;
import com.sistemadeoperaciones.pagos.repository.PaymentOperationRepository;
import com.sistemadeoperaciones.shared.audit.service.DeletionAuditService;
import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
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

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ClientesServiceImpl implements ClientesService {

    private final ClientesRepository clientesRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final PaymentOperationRepository paymentOperationRepository;
    private final DeletionAuditService deletionAuditService;
    private final BusinessAuditService businessAuditService;

    public ClientesServiceImpl(
            ClientesRepository clientesRepository,
            UserRepository userRepository,
            AuthenticatedUserService authenticatedUserService,
            PaymentOperationRepository paymentOperationRepository,
            DeletionAuditService deletionAuditService,
            BusinessAuditService businessAuditService
    ) {
        this.clientesRepository = clientesRepository;
        this.userRepository = userRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.paymentOperationRepository = paymentOperationRepository;
        this.deletionAuditService = deletionAuditService;
        this.businessAuditService = businessAuditService;
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

        User currentUser = authenticatedUserService.getCurrentUser();
        boolean canChooseOwner = hasAnyRole(currentUser, RoleName.ADMIN, RoleName.GERENTE, RoleName.DIRECCION);
        Long ownerId = canChooseOwner ? request.getUserId() : currentUser.getId();

        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con id: " + ownerId));

        if (!Boolean.TRUE.equals(user.getActivo()) || !hasAnyRole(user, RoleName.SOCIO_COMERCIAL)) {
            throw new BadRequestException("El cliente debe asignarse a un socio comercial activo");
        }

        Clientes cliente = new Clientes();
        cliente.setUser(user);
        cliente.setNombre(request.getNombre().trim());
        cliente.setActivo(true);
        cliente.setNivelesRedComercial(request.getNivelesRedComercial());

        Clientes saved = clientesRepository.save(cliente);
        audit("CLIENT_CREATE", saved, null, auditSnapshot(saved));

        return mapToResponse(saved);
    }

    private boolean hasAnyRole(User user, RoleName... roles) {
        return user.getRoles().stream().anyMatch(role ->
                java.util.Arrays.asList(roles).contains(role.getName())
        );
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
        validateCanEdit(cliente);
        Map<String, Object> beforeAudit = auditSnapshot(cliente);

        if (request.getNombre() == null || request.getNombre().trim().isEmpty()) {
            throw new ClienteNameRequiredException();
        }

        if (clientesRepository.existsByNombreIgnoreCaseAndIdNot(request.getNombre().trim(), id)) {
            throw new ClienteAlreadyExistsException(request.getNombre());
        }

        cliente.setNombre(request.getNombre().trim());
        cliente.setActivo(request.getActivo());
        cliente.setNivelesRedComercial(request.getNivelesRedComercial());

        Clientes updated = clientesRepository.save(cliente);
        audit("CLIENT_UPDATE", updated, beforeAudit, auditSnapshot(updated));

        return mapToResponse(updated);
    }

    private void validateCanEdit(Clientes cliente) {

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

    }

    @Override
    @Transactional
    public ClienteResponseDto deactivate(Long id) {
        Clientes cliente = findClienteById(id);
        validateCanEdit(cliente);
        Map<String, Object> beforeAudit = auditSnapshot(cliente);
        cliente.setActivo(false);

        Clientes updated = clientesRepository.save(cliente);
        audit("CLIENT_DEACTIVATE", updated, beforeAudit, auditSnapshot(updated));

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public ClienteResponseDto activate(Long id) {
        Clientes cliente = findClienteById(id);
        validateCanEdit(cliente);
        Map<String, Object> beforeAudit = auditSnapshot(cliente);
        cliente.setActivo(true);

        Clientes updated = clientesRepository.save(cliente);
        audit("CLIENT_ACTIVATE", updated, beforeAudit, auditSnapshot(updated));

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Clientes cliente = findClienteById(id);
        Map<String, Object> beforeAudit = auditSnapshot(cliente);

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
            audit("CLIENT_DELETE", cliente, beforeAudit, null);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("No se puede eliminar el cliente porque tiene información relacionada");
        }
    }

    private Map<String, Object> auditSnapshot(Clientes cliente) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("name", cliente.getNombre());
        snapshot.put("ownerUserId", cliente.getUser() == null ? null : cliente.getUser().getId());
        snapshot.put("networkLevels", cliente.getNivelesRedComercial());
        snapshot.put("active", cliente.getActivo());
        return snapshot;
    }

    private void audit(String action, Clientes cliente, Map<String, Object> before, Map<String, Object> after) {
        businessAuditService.record(authenticatedUserService.getCurrentUser().getId(), action,
                "CLIENT", cliente.getId(), before, after);
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
                cliente.getCreatedAt(),
                cliente.getUpdatedAt()
        );
    }
}
