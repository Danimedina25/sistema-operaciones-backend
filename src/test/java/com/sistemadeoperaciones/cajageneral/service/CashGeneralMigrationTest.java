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

    /**
     * La restricción que agrega 2026-09-16 sobre la cuenta bancaria del cheque cobrado.
     *
     * <p>Se aplica sólo el fragmento que toca cash_general_movements y no el script completo,
     * porque el resto altera tablas (bank_account_daily_cuts, operation_payments) que este
     * esquema mínimo no tiene. Lo que importa aquí es la semántica de la invariante.
     */
    @Test void bankAccountLinkOnlyBelongsToACashedChequeEntry() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:cash_bank_link;MODE=MySQL");
             var statement = connection.createStatement()) {
            statement.execute("create table users(id bigint primary key)");
            statement.execute("create table operation_return_installments(id bigint primary key)");
            statement.execute("create table bank_accounts(id bigint primary key)");
            statement.execute("insert into users values(1)");
            statement.execute("insert into bank_accounts values(1)");
            try (var script = Files.newBufferedReader(Path.of("migrations/2026-09-14_add_caja_general.sql"))) {
                RunScript.execute(connection, script);
            }
            statement.execute("alter table cash_general_movements add column bank_account_id bigint null");
            statement.execute("alter table cash_general_movements add constraint fk_cg_movement_bank_account foreign key (bank_account_id) references bank_accounts(id)");
            statement.execute("alter table cash_general_movements add constraint chk_cg_movement_bank_account check (bank_account_id is null or (tipo in ('CHEQUE','RETIRO_SIN_TARJETA') and direccion = 'ENTRADA'))");
            statement.execute("insert into cash_general_days(id,fecha,saldo_inicial,saldo_actual,abierto_por,created_at,updated_at) values(1,'2026-09-16',0,0,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");

            String movement = "insert into cash_general_movements(day_id,request_id,direccion,tipo,concepto,banco,bank_account_id,monto_manual,saldo_acumulado,creado_por,created_at) values(1,";

            // El cheque cobrado de entrada sí puede llevar cuenta.
            statement.execute(movement + "'ok','ENTRADA','CHEQUE','Cheque cobrado','BBVA',1,100,100,1,CURRENT_TIMESTAMP)");

            // Un cheque de salida, no.
            assertThatThrownBy(() -> statement.execute(movement + "'salida','SALIDA','CHEQUE','Cheque cobrado','BBVA',1,100,0,1,CURRENT_TIMESTAMP)"))
                    .isInstanceOf(SQLException.class);

            // El retiro sin tarjeta de entrada también puede llevar cuenta.
            statement.execute(movement + "'retiro','ENTRADA','RETIRO_SIN_TARJETA','Retiro sin tarjeta',null,1,100,200,1,CURRENT_TIMESTAMP)");

            // El efectivo y el retiro con tarjeta, no.
            assertThatThrownBy(() -> statement.execute(movement + "'efectivo','ENTRADA','EFECTIVO','Efectivo',null,1,100,300,1,CURRENT_TIMESTAMP)"))
                    .isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> statement.execute(movement + "'tarjeta','ENTRADA','RETIRO_CON_TARJETA','Retiro','BBVA',1,100,300,1,CURRENT_TIMESTAMP)"))
                    .isInstanceOf(SQLException.class);

            // Los cheques históricos, sin cuenta vinculada, siguen siendo válidos.
            statement.execute(movement + "'historico','ENTRADA','CHEQUE','Cheque cobrado','Banorte',null,100,300,1,CURRENT_TIMESTAMP)");

            // Y la cuenta vinculada no se puede borrar mientras exista el movimiento.
            assertThatThrownBy(() -> statement.execute("delete from bank_accounts where id = 1"))
                    .isInstanceOf(SQLException.class);
        }
    }
}
