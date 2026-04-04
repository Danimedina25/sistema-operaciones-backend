package com.sistemadeliberacion.auth.service;

import com.sistemadeliberacion.auth.dto.AuthResponse;
import com.sistemadeliberacion.auth.dto.LoginRequest;
import com.sistemadeliberacion.auth.exceptions.CredencialesInvalidasException;
import com.sistemadeliberacion.auth.exceptions.UsuarioInactivoException;
import com.sistemadeliberacion.auth.exceptions.UsuarioNoEncontradoException;
import com.sistemadeliberacion.auth.models.User;
import com.sistemadeliberacion.auth.repository.UserRepository;
import com.sistemadeliberacion.shared.utils.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getCorreo(),
                            request.getPassword()
                    )
            );

            User user = userRepository.findByCorreo(request.getCorreo())
                    .orElseThrow(CredencialesInvalidasException::new);

            String token = jwtUtil.generateToken(user);

            List<String> roles = user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .toList();

            return new AuthResponse(
                    token,
                    user.getId(),
                    user.getCorreo(),
                    user.getNombre(),
                    roles
            );

        } catch (DisabledException e) {
            throw new UsuarioInactivoException();
        } catch (AuthenticationException e) {
            throw new CredencialesInvalidasException();
        }
    }
}