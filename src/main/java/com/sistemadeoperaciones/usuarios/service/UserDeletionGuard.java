package com.sistemadeoperaciones.usuarios.service;

import com.sistemadeoperaciones.shared.enums.RoleName;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
import com.sistemadeoperaciones.shared.exception.EntityHasDependenciesException;
import com.sistemadeoperaciones.usuarios.model.User;
import com.sistemadeoperaciones.usuarios.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class UserDeletionGuard {

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    public UserDeletionGuard(
            UserRepository userRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void assertCanDelete(User currentUser, User target) {
        if (currentUser.getId().equals(target.getId())) {
            throw new BadRequestException("No puede eliminar su propio usuario");
        }

        boolean isAdmin = target.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ADMIN);
        boolean isDireccion = target.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.DIRECCION);

        if (isAdmin && Boolean.TRUE.equals(target.getActivo())
                && userRepository.countByRoles_NameAndActivoTrue(RoleName.ADMIN) <= 1) {
            throw new BadRequestException("No se puede eliminar al último administrador activo del sistema");
        }

        if (isDireccion && Boolean.TRUE.equals(target.getActivo())
                && userRepository.countByRoles_NameAndActivoTrue(RoleName.DIRECCION) <= 1) {
            throw new BadRequestException("No se puede eliminar al último usuario con rol Dirección activo del sistema");
        }

        Map<String, Long> dependencies = new LinkedHashMap<>();
        putIfPositive(dependencies, "operacionesComoSocioComercial", countRelatedOperations(target.getId()));

        if (!dependencies.isEmpty()) {
            throw new EntityHasDependenciesException(
                    "No se puede eliminar el socio comercial porque tiene operaciones relacionadas en el sistema",
                    dependencies
            );
        }
    }

    private long countRelatedOperations(Long userId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT po.id)
                FROM payment_operations po
                LEFT JOIN clientes c ON c.id = po.cliente_id
                LEFT JOIN commercial_partners cp2 ON cp2.id = po.socio_comercial_id_nivel_2
                LEFT JOIN commercial_partners cp3 ON cp3.id = po.socio_comercial_id_nivel_3
                WHERE po.socio_comercial_id = ?
                   OR c.user_id = ?
                   OR cp2.user_id = ?
                   OR cp3.user_id = ?
                   OR EXISTS (
                       SELECT 1
                       FROM commercial_partner_commissions cpc
                       JOIN commercial_partners cp ON cp.id = cpc.commercial_partner_id
                       WHERE cpc.operation_id = po.id AND cp.user_id = ?
                   )
                   OR EXISTS (
                       SELECT 1
                       FROM operation_commissions oc
                       JOIN commercial_partners cp ON cp.id = oc.socio_comercial_id
                       WHERE oc.operacion_id = po.id AND cp.user_id = ?
                   )
                """, Long.class, userId, userId, userId, userId, userId, userId);
        return count == null ? 0 : count;
    }

    private void putIfPositive(Map<String, Long> map, String key, long value) {
        if (value > 0) {
            map.put(key, value);
        }
    }
}
