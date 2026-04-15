package com.sistemadeoperaciones.shared.utils;

import com.sistemadeoperaciones.usuarios.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expirationTime;

    private Key signingKey;
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getCorreo());
        claims.put("roles", user.getRoles().stream()
                .map(role -> role.getName().name())
                .toList());

        // Calcula la fecha de expiración
        Date expirationDate = new Date(System.currentTimeMillis() + expirationTime);

        // Imprime en consola
        log.info("Generando token para usuario: {}", user.getCorreo());
        log.info("Fecha de expiración del token: {}", expirationDate);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getId().toString())
                .setIssuedAt(new Date())
                .setExpiration(expirationDate)
                .signWith(signingKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    public Long extractUserIdFromSubject(String token) {
        return Long.valueOf(extractClaims(token).getSubject());
    }

    public List<String> extractRoles(String token) {
        List<String> roles = extractClaims(token).get("roles", List.class);
        return roles != null ? roles : List.of();
    }

    public String extractEmail(String token) {
        return extractClaims(token).get("email", String.class);
    }

    public boolean validateToken(String token, String email) {
        try {
            Claims claims = extractClaims(token);

            String tokenEmail = claims.get("email", String.class);
            Date expiration = claims.getExpiration();

            return (tokenEmail.equals(email) && expiration.after(new Date()));
        } catch (ExpiredJwtException e) {
            log.info("Token expirado");
        } catch (UnsupportedJwtException e) {
            log.info("Token no soportado");
        } catch (MalformedJwtException e) {
            log.info("Token mal formado");
        } catch (SignatureException e) {
            log.info("Firma inválida");
        } catch (IllegalArgumentException e) {
            log.info("Token vacío o nulo");
        }
        return false;
    }


}
