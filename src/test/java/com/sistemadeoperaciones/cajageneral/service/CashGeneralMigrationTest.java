package com.sistemadeoperaciones.cajageneral.service;

import org.h2.tools.RunScript;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import static org.assertj.core.api.Assertions.*;

class CashGeneralMigrationTest {
    @Test void migrationCreatesFinancialConstraintsAndCanBeAppliedAgain() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:cash_migration;MODE=MySQL");
             var statement = connection.createStatement()) {
            statement.execute("create table users(id bigint primary key)");
            statement.execute("create table operation_return_installments(id bigint primary key)");
            statement.execute("insert into users values(1)");
            statement.execute("insert into operation_return_installments values(1)");
            for (int i = 0; i < 2; i++) {
                try (var script = Files.newBufferedReader(Path.of("migrations/2026-09-14_add_caja_general.sql"))) {
                    RunScript.execute(connection, script);
                }
            }
            statement.execute("insert into cash_general_days(id,fecha,saldo_inicial,saldo_actual,abierto_por,created_at,updated_at) values(1,'2026-09-14',100,100,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
            assertThatThrownBy(() -> statement.execute("update cash_general_days set saldo_actual=-1 where id=1")).isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> statement.execute("update cash_general_days set closed_at=CURRENT_TIMESTAMP,saldo_contado=90,diferencia=0,cerrado_por=1 where id=1")).isInstanceOf(SQLException.class);
            statement.execute("update cash_general_days set closed_at=CURRENT_TIMESTAMP,saldo_contado=90,diferencia=-10,cerrado_por=1 where id=1");
            assertThatThrownBy(() -> statement.execute("insert into cash_general_movements(day_id,request_id,direccion,tipo,concepto,monto_manual,installment_id,saldo_acumulado,creado_por,created_at) values(1,'a','SALIDA','EFECTIVO','Duplicado',100,1,0,1,CURRENT_TIMESTAMP)"))
                    .isInstanceOf(SQLException.class);
        }
    }
}
