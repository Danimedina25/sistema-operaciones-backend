package com.sistemadeliberacion.auth.service;

import com.sistemadeliberacion.auth.models.User;
import com.sistemadeliberacion.auth.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        User user = userRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));

        List<GrantedAuthority> authorities = user.getRoles().stream()
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                .toList();

        return new org.springframework.security.core.userdetails.User(
                user.getCorreo(),
                user.getPassword(),
                Boolean.TRUE.equals(user.getActivo()),
                true,
                true,
                true,
                authorities
        );
    }
}