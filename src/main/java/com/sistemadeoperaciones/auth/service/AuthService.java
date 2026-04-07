package com.sistemadeoperaciones.auth.service;

import com.sistemadeoperaciones.auth.dto.AuthResponse;
import com.sistemadeoperaciones.auth.dto.LoginRequest;
import com.sistemadeoperaciones.auth.exceptions.CredencialesInvalidasException;
import com.sistemadeoperaciones.auth.exceptions.UsuarioInactivoException;
import com.sistemadeoperaciones.auth.models.User;
import com.sistemadeoperaciones.auth.repository.AuthRepository;
import com.sistemadeoperaciones.shared.utils.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AuthRepository authRepository;
    private final JwtUtil jwtUtil;

    public AuthService(AuthenticationManager authenticationManager,
                       AuthRepository authRepository,
                       JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.authRepository = authRepository;
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

            User user = authRepository.findByCorreo(request.getCorreo())
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