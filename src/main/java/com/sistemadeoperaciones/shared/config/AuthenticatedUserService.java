package com.sistemadeoperaciones.shared.config;

import com.sistemadeoperaciones.auth.models.User;
import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;
import com.sistemadeoperaciones.shared.model.AuthenticatedUser;
import com.sistemadeoperaciones.usuarios.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticatedUserService {

    private final UserRepository userRepository;

    public AuthenticatedUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNotFoundException("No fue posible obtener el usuario autenticado");
        }
        Object principal = authentication.getPrincipal();

        if (!(principal instanceof AuthenticatedUser)) {
            throw new ResourceNotFoundException("El principal autenticado no es válido");
        }

        AuthenticatedUser authenticatedUser = (AuthenticatedUser) principal;

        String correo = authenticatedUser.getEmail();

        return userRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario autenticado no encontrado con correo: " + correo));
    }
}