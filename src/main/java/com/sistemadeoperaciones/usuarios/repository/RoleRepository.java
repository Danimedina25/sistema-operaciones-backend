package com.sistemadeoperaciones.usuarios.repository;

import com.sistemadeoperaciones.usuarios.model.Role;
import com.sistemadeoperaciones.shared.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}