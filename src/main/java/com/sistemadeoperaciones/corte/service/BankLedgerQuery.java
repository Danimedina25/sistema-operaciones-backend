package com.sistemadeoperaciones.corte.service;

import com.sistemadeoperaciones.corte.dto.BankLedgerBuckets;
import com.sistemadeoperaciones.corte.dto.BankLedgerFilter;
import com.sistemadeoperaciones.corte.dto.BankLedgerRowDto;
import com.sistemadeoperaciones.corte.dto.BankLedgerTotalsDto;
import com.sistemadeoperaciones.corte.enums.BankLedgerDirection;
import com.sistemadeoperaciones.corte.enums.BankLedgerOrigin;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Libro bancario derivado: la ÚNICA definición de qué cuenta como movimiento de una
 * cuenta bancaria. No existe una tabla de asientos porque no haría falta — las tres
 * fuentes ya son filas persistidas, auditables y con llave foránea real hacia
 * {@code bank_accounts}:
 *
 * <ol>
 *   <li>{@code operation_payments} validados sobre su cuenta destino (ENTRADA).</li>
 *   <li>{@code operation_return_installments} completadas sobre su cuenta origen (SALIDA).</li>
 *   <li>{@code cash_general_movements} que retiran del banco hacia la caja —cheque
 *       cobrado y retiro sin tarjeta— (SALIDA).</li>
 * </ol>
 *
 * Persistir un libro canónico habría exigido escribir por duplicado desde
 * {@code PaymentOperationServiceImpl.validatePayment} y desde
 * {@code ReturnInstallmentServiceImpl.recomputeInstallmentStatus} —que se reejecuta al
 * confirmar, entregar y cancelar—, con su propia idempotencia, asientos compensatorios y
 * un backfill histórico. Esa es justamente la superficie donde aparece la doble
 * contabilización. Derivando, el saldo en vivo, el corte registrado y el historial
 * comparten esta misma consulta por construcción.
 *
 * <p>El cheque cobrado es el caso interesante: la entrada de efectivo en Caja General y la
 * salida bancaria <em>son la misma fila</em>, así que es imposible que exista una sin la otra.
 */
@Component
@Transactional(readOnly = true)
public class BankLedgerQuery {

    /**
     * Unión de las tres fuentes. Cada rama filtra por el estado que vuelve el dinero real:
     * el pago debe estar VALIDADA y la parcialidad COMPLETADA.
     *
     * <p>La ventana del día es SEMIABIERTA: {@code >= inicio} y {@code < inicio del día
     * siguiente}. La anterior cerraba en 23:59:59 y perdía para siempre cualquier movimiento
     * de ese último segundo, porque el día siguiente arrancaba en 00:00:00.
     *
     * <p>Se compara contra instantes y no contra una fecha truncada a propósito: Hibernate
     * convierte el parámetro con la misma zona con la que escribió el valor, así que la
     * comparación es correcta con o sin {@code hibernate.jdbc.time_zone}. Truncar con
     * {@code CAST(... AS DATE)} mezclaría representaciones y movería los movimientos de día.
     *
     * <p>El cheque cobrado se fecha por {@code created_at}: sólo puede capturarse sobre el día
     * de caja abierto, que es siempre el de hoy, así que su instante cae dentro de ese día.
     */
    private static final String UNION = """
            SELECT 'PAGO' AS origen, p.id AS source_id, p.fecha_validacion AS fecha,
                   'ENTRADA' AS direccion, p.tipo_pago AS tipo, p.monto AS monto,
                   p.cuenta_destino_id AS bank_account_id, p.operacion_id AS operacion_id,
                   NULL AS parcialidad_id, NULL AS cash_movement_id,
                   p.validado_por AS usuario_id, NULL AS concepto
            FROM operation_payments p
            WHERE p.estatus = 'VALIDADA'
              AND p.cuenta_destino_id IS NOT NULL
              AND p.fecha_validacion IS NOT NULL
            UNION ALL
            SELECT 'RETORNO', i.id, i.fecha_realizacion,
                   'SALIDA', i.tipo_pago, i.monto,
                   i.cuenta_origen_id, s.operacion_id,
                   i.id, NULL,
                   i.realizado_por, NULL
            FROM operation_return_installments i
            JOIN operation_return_payments s ON s.id = i.solicitud_id
            WHERE i.estatus = 'COMPLETADA'
              AND i.cuenta_origen_id IS NOT NULL
              AND i.fecha_realizacion IS NOT NULL
              AND i.tipo_pago <> 'EFECTIVO'
            UNION ALL
            SELECT 'CAJA_GENERAL', m.id, m.created_at,
                   'SALIDA', m.tipo, m.monto_manual,
                   m.bank_account_id, NULL,
                   NULL, m.id,
                   m.creado_por, m.concepto
            FROM cash_general_movements m
            WHERE m.tipo IN ('CHEQUE', 'RETIRO_SIN_TARJETA')
              AND m.bank_account_id IS NOT NULL
              AND m.monto_manual IS NOT NULL
            """;

    @PersistenceContext
    private EntityManager em;

    /** Inicio del día, inclusivo. */
    public static LocalDateTime startOfDay(LocalDate fecha) {
        return fecha.atStartOfDay();
    }

    /** Inicio del día siguiente, exclusivo. */
    public static LocalDateTime startOfNextDay(LocalDate fecha) {
        return fecha.plusDays(1).atStartOfDay();
    }

    /** Página cronológica de movimientos, del más reciente al más antiguo. */
    public Page<BankLedgerRowDto> search(BankLedgerFilter filter, Pageable pageable) {
        Map<String, Object> params = new LinkedHashMap<>();
        String where = buildWhere(filter, params);

        String sql = """
                SELECT mov.origen, mov.source_id, mov.fecha, mov.direccion, mov.tipo, mov.monto,
                       mov.bank_account_id, b.banco, b.titular, b.numero_cuenta, b.activo,
                       mov.operacion_id, mov.parcialidad_id, mov.cash_movement_id,
                       mov.usuario_id, u.nombre, mov.concepto
                FROM (%s) mov
                JOIN bank_accounts b ON b.id = mov.bank_account_id
                LEFT JOIN users u ON u.id = mov.usuario_id
                %s
                ORDER BY mov.fecha DESC, mov.origen ASC, mov.source_id DESC
                """.formatted(UNION, where);

        Query query = em.createNativeQuery(sql);
        params.forEach(query::setParameter);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        List<BankLedgerRowDto> content = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            content.add(mapRow(row));
        }
        return new PageImpl<>(content, pageable, countAll(filter));
    }

    /** Totales sobre todo el filtro, no sobre la página. */
    public BankLedgerTotalsDto totals(BankLedgerFilter filter) {
        Map<String, Object> params = new LinkedHashMap<>();
        String where = buildWhere(filter, params);

        String sql = """
                SELECT
                    COALESCE(SUM(CASE WHEN mov.direccion = 'ENTRADA' THEN mov.monto ELSE 0 END), 0),
                    COALESCE(SUM(CASE WHEN mov.direccion = 'SALIDA'  THEN mov.monto ELSE 0 END), 0),
                    COUNT(*)
                FROM (%s) mov
                JOIN bank_accounts b ON b.id = mov.bank_account_id
                %s
                """.formatted(UNION, where);

        Query query = em.createNativeQuery(sql);
        params.forEach(query::setParameter);
        Object[] row = (Object[]) query.getSingleResult();
        BigDecimal entradas = money(row[0]);
        BigDecimal salidas = money(row[1]);
        return new BankLedgerTotalsDto(entradas, salidas, entradas.subtract(salidas), number(row[2]));
    }

    /**
     * Los mismos movimientos agrupados en los conceptos del corte bancario por cuenta.
     * La ventana del día es semiabierta para no perder el último segundo.
     */
    public BankLedgerBuckets bucketsForAccount(Long bankAccountId, LocalDate fecha) {
        String sql = """
                SELECT
                    COALESCE(SUM(CASE WHEN mov.origen = 'PAGO' AND mov.tipo = 'TRANSFERENCIA' THEN mov.monto ELSE 0 END), 0),
                    COALESCE(SUM(CASE WHEN mov.origen = 'PAGO' AND mov.tipo = 'DEPOSITO'      THEN mov.monto ELSE 0 END), 0),
                    COALESCE(SUM(CASE WHEN mov.origen = 'PAGO' AND mov.tipo = 'CHEQUE'        THEN mov.monto ELSE 0 END), 0),
                    COALESCE(SUM(CASE WHEN mov.origen = 'RETORNO'      THEN mov.monto ELSE 0 END), 0),
                    COALESCE(SUM(CASE WHEN mov.origen = 'CAJA_GENERAL' THEN mov.monto ELSE 0 END), 0)
                FROM (%s) mov
                WHERE mov.bank_account_id = :bankAccountId
                  AND mov.fecha >= :inicio
                  AND mov.fecha < :fin
                """.formatted(UNION);

        Query query = em.createNativeQuery(sql);
        query.setParameter("bankAccountId", bankAccountId);
        query.setParameter("inicio", startOfDay(fecha));
        query.setParameter("fin", startOfNextDay(fecha));
        Object[] row = (Object[]) query.getSingleResult();
        return new BankLedgerBuckets(money(row[0]), money(row[1]), money(row[2]), money(row[3]), money(row[4]));
    }

    private long countAll(BankLedgerFilter filter) {
        Map<String, Object> params = new LinkedHashMap<>();
        String where = buildWhere(filter, params);
        String sql = """
                SELECT COUNT(*)
                FROM (%s) mov
                JOIN bank_accounts b ON b.id = mov.bank_account_id
                %s
                """.formatted(UNION, where);
        Query query = em.createNativeQuery(sql);
        params.forEach(query::setParameter);
        return number(query.getSingleResult());
    }

    private String buildWhere(BankLedgerFilter filter, Map<String, Object> params) {
        List<String> clauses = new ArrayList<>();
        clauses.add("mov.fecha >= :inicio");
        params.put("inicio", startOfDay(filter.desde()));
        clauses.add("mov.fecha < :fin");
        params.put("fin", startOfNextDay(filter.hasta()));

        if (filter.bankAccountId() != null) {
            clauses.add("mov.bank_account_id = :bankAccountId");
            params.put("bankAccountId", filter.bankAccountId());
        }
        if (filter.banco() != null && !filter.banco().isBlank()) {
            clauses.add("b.banco = :banco");
            params.put("banco", filter.banco().trim());
        }
        if (filter.direccion() != null) {
            clauses.add("mov.direccion = :direccion");
            params.put("direccion", filter.direccion().name());
        }
        if (filter.tipo() != null && !filter.tipo().isBlank()) {
            clauses.add("mov.tipo = :tipo");
            params.put("tipo", filter.tipo().trim());
        }
        return "WHERE " + String.join("\n  AND ", clauses);
    }

    private BankLedgerRowDto mapRow(Object[] row) {
        BankLedgerOrigin origen = BankLedgerOrigin.valueOf((String) row[0]);
        Long sourceId = id(row[1]);
        BankLedgerDirection direccion = BankLedgerDirection.valueOf((String) row[3]);
        String tipo = (String) row[4];
        Long operacionId = id(row[11]);
        String concepto = concepto(origen, tipo, operacionId, (String) row[16]);
        return new BankLedgerRowDto(
                origen.name() + "-" + sourceId,
                origen,
                sourceId,
                timestamp(row[2]),
                direccion,
                tipo,
                concepto,
                money(row[5]),
                id(row[6]),
                (String) row[7],
                (String) row[8],
                mask((String) row[9]),
                bool(row[10]),
                operacionId,
                id(row[12]),
                id(row[13]),
                id(row[14]),
                (String) row[15]
        );
    }

    private String concepto(BankLedgerOrigin origen, String tipo, Long operacionId, String raw) {
        if (raw != null && !raw.isBlank()) {
            return raw;
        }
        String legible = tipo == null ? "" : tipo.charAt(0) + tipo.substring(1).toLowerCase().replace('_', ' ');
        return switch (origen) {
            case PAGO -> "Pago " + legible + " · Operación #" + operacionId;
            case RETORNO -> "Retorno " + legible + " · Operación #" + operacionId;
            case CAJA_GENERAL -> "Retiro de banco hacia Caja General";
        };
    }

    /** El libro no publica el número completo: sólo los últimos cuatro dígitos. */
    private static String mask(String numeroCuenta) {
        if (numeroCuenta == null || numeroCuenta.isBlank()) {
            return null;
        }
        String digits = numeroCuenta.trim();
        return digits.length() <= 4 ? digits : "••••" + digits.substring(digits.length() - 4);
    }

    private static BigDecimal money(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString());
    }

    private static long number(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private static Long id(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static Boolean bool(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof Boolean flag ? flag : ((Number) value).intValue() != 0;
    }

    private static LocalDateTime timestamp(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        return ((java.sql.Timestamp) value).toLocalDateTime();
    }
}
