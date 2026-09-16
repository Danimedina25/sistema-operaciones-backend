package com.sistemadeoperaciones.corte.dto;

import com.sistemadeoperaciones.corte.enums.BankLedgerDirection;
import com.sistemadeoperaciones.corte.enums.BankLedgerOrigin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Un movimiento bancario tal y como lo ve la consulta. No expone la CLABE: el libro
 * no amplía la información bancaria que ya publican los demás endpoints.
 */
public record BankLedgerRowDto(
        String id,
        BankLedgerOrigin origen,
        Long sourceId,
        LocalDateTime fecha,
        BankLedgerDirection direccion,
        String tipo,
        String concepto,
        BigDecimal monto,
        Long bankAccountId,
        String cuentaBanco,
        String cuentaTitular,
        String cuentaNumero,
        Boolean cuentaActiva,
        Long operacionId,
        Long parcialidadId,
        Long cashMovementId,
        Long usuarioId,
        String usuarioNombre
) {
}
