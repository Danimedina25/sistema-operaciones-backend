package com.sistemadeoperaciones.usuarios.repository;

import com.sistemadeoperaciones.usuarios.model.User;
import com.sistemadeoperaciones.usuarios.model.UserActivationToken;
import com.sistemadeoperaciones.usuarios.enums.UserTokenType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserActivationTokenRepository extends JpaRepository<UserActivationToken, Long> {

    Optional<UserActivationToken> findByToken(String token);

    Optional<UserActivationToken> findByTokenAndType(String token, UserTokenType type);

    List<UserActivationToken> findByUserAndUsedFalse(User user);

    List<UserActivationToken> findByUserAndUsedFalseAndType(User user, UserTokenType type);

    long countByUserId(Long userId);
}