package com.sistemadeoperaciones.usuarios.repository;

import com.sistemadeoperaciones.auth.models.User;
import com.sistemadeoperaciones.usuarios.model.UserActivationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserActivationTokenRepository extends JpaRepository<UserActivationToken, Long> {

    Optional<UserActivationToken> findByToken(String token);

    Optional<UserActivationToken> findByUser(User user);

    boolean existsByToken(String token);

    List<UserActivationToken> findByUserAndUsedFalse(User user);
}