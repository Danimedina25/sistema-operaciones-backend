package com.sistemadeoperaciones.usuarios.repository;

import com.sistemadeoperaciones.auth.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
}