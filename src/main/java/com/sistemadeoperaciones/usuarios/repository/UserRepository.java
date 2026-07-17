package com.sistemadeoperaciones.usuarios.repository;

import com.sistemadeoperaciones.shared.enums.RoleName;
import com.sistemadeoperaciones.usuarios.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    boolean existsByCorreoAndIdNot(String correo, Long id);
    List<User> findDistinctByRoles_NameInAndActivoTrue(List<RoleName> roleNames);

    long countByRoles_NameAndActivoTrue(RoleName roleName);
}