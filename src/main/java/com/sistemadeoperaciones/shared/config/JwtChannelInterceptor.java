package com.sistemadeoperaciones.shared.config;

import com.sistemadeoperaciones.shared.model.AuthenticatedUser;
import com.sistemadeoperaciones.shared.utils.JwtUtil;
import com.sistemadeoperaciones.shared.websocket.StompPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    public JwtChannelInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            System.out.println("=== STOMP CONNECT recibido ===");

            String authHeader = accessor.getFirstNativeHeader("Authorization");
            System.out.println("Authorization header: " + authHeader);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                System.out.println("❌ Token JWT no proporcionado o mal formado");
                throw new IllegalArgumentException("Token JWT no proporcionado");
            }

            String jwt = authHeader.substring(7);

            try {
                Claims claims = jwtUtil.extractClaims(jwt);

                Long userId = Long.valueOf(claims.getSubject());
                String email = claims.get("email", String.class);

                System.out.println("✅ Usuario autenticado en WS: userId=" + userId + ", email=" + email);

                List<String> roles = claims.get("roles", List.class);
                if (roles == null) {
                    roles = List.of();
                }

                System.out.println("Roles en WS: " + roles);

                List<GrantedAuthority> authorities = roles.stream()
                        .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();

                AuthenticatedUser principal = new AuthenticatedUser(userId, email, roles);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                authorities
                        );

                accessor.setUser(new StompPrincipal(userId.toString()));
                System.out.println("Principal asignado a STOMP: " + userId);

                if (accessor.getSessionAttributes() != null) {
                    accessor.getSessionAttributes().put("auth", authentication);
                    accessor.getSessionAttributes().put("authenticatedUser", principal);
                    System.out.println("Session attributes STOMP guardados correctamente");
                } else {
                    System.out.println("⚠️ Session attributes es null");
                }
            } catch (JwtException | IllegalArgumentException e) {
                System.out.println("❌ Error autenticando STOMP: " + e.getMessage());
                throw new IllegalArgumentException("Token JWT inválido", e);
            }
        }

        return message;
    }
}