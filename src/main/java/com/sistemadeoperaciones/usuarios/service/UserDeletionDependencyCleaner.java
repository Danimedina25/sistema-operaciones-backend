package com.sistemadeoperaciones.usuarios.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Limpia datos auxiliares del usuario y conserva el historial operativo
 * reasignando las referencias administrativas a quien realiza la eliminación.
 */
@Component
public class UserDeletionDependencyCleaner {

    private final JdbcTemplate jdbcTemplate;

    public UserDeletionDependencyCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void clean(Long targetUserId, Long replacementUserId) {
        jdbcTemplate.update("DELETE FROM user_notifications WHERE user_id = ?", targetUserId);
        jdbcTemplate.update("DELETE FROM user_activation_tokens WHERE user_id = ?", targetUserId);

        jdbcTemplate.update("UPDATE notifications SET created_by = ? WHERE created_by = ?", replacementUserId, targetUserId);
        jdbcTemplate.update("UPDATE operation_payments SET registrado_por = ? WHERE registrado_por = ?", replacementUserId, targetUserId);
        jdbcTemplate.update("UPDATE operation_payments SET validado_por = ? WHERE validado_por = ?", replacementUserId, targetUserId);
        jdbcTemplate.update("UPDATE operation_return_payments SET solicitado_por = ? WHERE solicitado_por = ?", replacementUserId, targetUserId);
        jdbcTemplate.update("UPDATE operation_return_payments SET pagado_por = ? WHERE pagado_por = ?", replacementUserId, targetUserId);
        jdbcTemplate.update("UPDATE operation_return_payments SET entregado_por = ? WHERE entregado_por = ?", replacementUserId, targetUserId);
        jdbcTemplate.update("UPDATE daily_cash_cuts SET generado_por = ? WHERE generado_por = ?", replacementUserId, targetUserId);
        jdbcTemplate.update("UPDATE commercial_partner_commissions SET user_id = ? WHERE user_id = ?", replacementUserId, targetUserId);
        jdbcTemplate.update("UPDATE commercial_partner_settings SET updated_by = ? WHERE updated_by = ?", replacementUserId, targetUserId);

        jdbcTemplate.update("DELETE FROM commercial_partner_settings WHERE user_id = ?", targetUserId);
        jdbcTemplate.update("DELETE FROM clientes WHERE user_id = ?", targetUserId);
        jdbcTemplate.update("DELETE FROM commercial_partners WHERE user_id = ?", targetUserId);
    }
}
