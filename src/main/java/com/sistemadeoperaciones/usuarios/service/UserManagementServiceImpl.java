package com.sistemadeoperaciones.usuarios.service;

import com.sistemadeoperaciones.auth.models.Role;
import com.sistemadeoperaciones.auth.models.User;
import com.sistemadeoperaciones.usuarios.repository.RoleRepository;
import com.sistemadeoperaciones.usuarios.repository.UserRepository;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;
import com.sistemadeoperaciones.usuarios.dto.CreateUserRequestDto;
import com.sistemadeoperaciones.usuarios.dto.UpdateUserRequestDto;
import com.sistemadeoperaciones.usuarios.dto.UserResponseDto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserManagementServiceImpl(UserRepository userRepository,
                                     RoleRepository roleRepository,
                                     PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponseDto create(CreateUserRequestDto request) {
        if (userRepository.existsByCorreo(request.getCorreo())) {
            throw new BadRequestException("Ya existe un usuario con ese correo");
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con id: " + request.getRoleId()));

        User user = new User();
        user.setNombre(request.getNombre());
        user.setCorreo(request.getCorreo());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setActivo(request.getActivo() != null ? request.getActivo() : true);
        user.setRoles(Set.of(role));

        User saved = userRepository.save(user);
        return mapToResponse(saved);
    }

    @Override
    public List<UserResponseDto> findAll() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public UserResponseDto findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        return mapToResponse(user);
    }

    @Override
    public UserResponseDto update(Long id, UpdateUserRequestDto request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        if (userRepository.existsByCorreoAndIdNot(request.getCorreo(), id)) {
            throw new BadRequestException("Ya existe otro usuario con ese correo");
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con id: " + request.getRoleId()));

        user.setNombre(request.getNombre());
        user.setCorreo(request.getCorreo());

        if (request.getActivo() != null) {
            user.setActivo(request.getActivo());
        }

        user.setRoles(Set.of(role));

        User updated = userRepository.save(user);
        return mapToResponse(updated);
    }

    @Override
    public UserResponseDto deactivate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        if (!user.getActivo()) {
            throw new BadRequestException("El usuario ya se encuentra inactivo");
        }

        user.setActivo(false);

        User updated = userRepository.save(user);
        return mapToResponse(updated);
    }

    @Override
    public UserResponseDto activate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        if (user.getActivo()) {
            throw new BadRequestException("El usuario ya se encuentra activo");
        }

        user.setActivo(true);

        User updated = userRepository.save(user);
        return mapToResponse(updated);
    }

    private UserResponseDto mapToResponse(User user) {
        Role role = user.getRoles().stream().findFirst().orElse(null);

        Long roleId = role != null ? role.getId() : null;
        String roleName = role != null ? role.getName().name() : null;

        return new UserResponseDto(
                user.getId(),
                user.getNombre(),
                user.getCorreo(),
                user.getActivo(),
                roleId,
                roleName
        );
    }
}