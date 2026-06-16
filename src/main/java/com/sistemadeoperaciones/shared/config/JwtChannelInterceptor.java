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

        if (!StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Token JWT no proporcionado");
        }

        String jwt = authHeader.substring(7);

        try {
            Claims claims = jwtUtil.extractClaims(jwt);

            Long userId = Long.valueOf(claims.getSubject());
            String email = claims.get("email", String.class);

            List<String> roles = claims.get("roles", List.class);
            if (roles == null) {
                roles = List.of();
            }

            List<GrantedAuthority> authorities = roles.stream()
                    .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();

            AuthenticatedUser authenticatedUser =
                    new AuthenticatedUser(userId, email, roles);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            authenticatedUser,
                            null,
                            authorities
                    );

            StompPrincipal stompPrincipal = new StompPrincipal(userId.toString());
            accessor.setUser(stompPrincipal);

            if (accessor.getSessionAttributes() != null) {
                accessor.getSessionAttributes().put("auth", authentication);
                accessor.getSessionAttributes().put("authenticatedUser", authenticatedUser);
                accessor.getSessionAttributes().put("userId", userId);
            }

            System.out.println("✅ STOMP autenticado. userId=" + userId);
            System.out.println("✅ STOMP principal name=" + accessor.getUser().getName());

            return message;
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Token JWT inválido", e);
        }
    }
}