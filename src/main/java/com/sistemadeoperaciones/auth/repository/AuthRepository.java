package com.sistemadeoperaciones.auth.repository;

import com.sistemadeoperaciones.auth.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRepository extends JpaRepository<User, Long> {
    Optional<User> findByCorreo(String correo);

}