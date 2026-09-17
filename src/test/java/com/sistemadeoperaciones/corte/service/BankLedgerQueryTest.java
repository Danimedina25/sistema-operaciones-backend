package com.sistemadeoperaciones.corte.service;

import com.sistemadeoperaciones.cajageneral.enums.CashDenomination;
import com.sistemadeoperaciones.cajageneral.enums.CashMovementConcept;
import com.sistemadeoperaciones.cajageneral.enums.CashMovementDirection;
import com.sistemadeoperaciones.cajageneral.model.CashGeneralDay;
import com.sistemadeoperaciones.cajageneral.model.CashGeneralMovement;
import com.sistemadeoperaciones.clientes.model.Clientes;
import com.sistemadeoperaciones.corte.dto.BankLedgerFilter;
import com.sistemadeoperaciones.corte.enums.BankLedgerDirection;
import com.sistemadeoperaciones.corte.enums.BankLedgerOrigin;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.pagos.enums.*;
import com.sistemadeoperaciones.pagos.model.*;
import com.sistemadeoperaciones.usuarios.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La matriz contable, verificada contra la base. Cada renglón de la tabla acordada con
 * negocio tiene aquí su caso, incluidos los que NO deben aparecer en el libro bancario.
 */
@DataJpaTest
@Import(BankLedgerQuery.class)
class BankLedgerQueryTest {

    @Autowired TestEntityManager em;
    @Autowired BankLedgerQuery ledger;

    static final LocalDate HOY = LocalDate.now();

    User user;
    PaymentOperation operation;
    BankAccount cuenta;
    BankAccount otraCuenta;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setNombre("Tester");
        user.setCorreo("tester+" + System.nanoTime() + "@example.com");
        em.persist(user);

        Clientes cliente = new Clientes();
        cliente.setNombre("Cliente Prueba");
        cliente.setUser(user);
        cliente.setPorcentajeComisionSocio(BigDecimal.ZERO);
        cliente.setPorcentajeComisionOficina(BigDecimal.ZERO);
        em.persist(cliente);

        operation = new PaymentOperation();
        operation.setCliente(cliente);
        operation.setSocioComercial(user);
        operation.setMontoTotal(new BigDecimal("1000000.00"));
        operation.setMontoValidado(new BigDecimal("1000000.00"));
        operation.setPorcentajeComisionSocio(BigDecimal.ZERO);
        operation.setPorcentajeComisionOficina(BigDecimal.ZERO);
        em.persist(operation);

        cuenta = account("BBVA", "Operaciones SA");
        otraCuenta = account("Banorte", "Operaciones SA");
    }

    BankAccount account(String banco, String titular) {
        BankAccount a = new BankAccount();
        a.setBanco(banco);
        a.setTitular(titular);
        a.setNumeroCuenta(UUID.randomUUID().toString().substring(0, 18));
        a.setClabe(UUID.randomUUID().toString().replaceAll("[^0-9]", "0").substring(0, 18));
        a.setActivo(true);
        em.persist(a);
        return a;
    }

    OperationPayment payment(PaymentType tipo, String monto, PaymentStatus estatus,
                             BankAccount destino, LocalDateTime fechaValidacion) {
        OperationPayment p = new OperationPayment();
        p.setOperacion(operation);
        p.setMonto(new BigDecimal(monto));
        p.setTipoPago(tipo);
        p.setCuentaDestino(destino);
        p.setComprobanteUrl("https://example.com/c.jpg");
        p.setEstatus(estatus);
        p.setRegistradoPor(user);
        p.setFechaPago(LocalDateTime.now());
        p.setFechaComprobante(LocalDateTime.now());
        p.setValidadoPor(estatus == PaymentStatus.VALIDADA ? user : null);
        p.setFechaValidacion(fechaValidacion);
        em.persist(p);
        return p;
    }

    OperationReturnInstallment installment(PaymentType tipo, String monto, ReturnInstallmentStatus estatus,
                                           BankAccount origen, LocalDateTime fechaRealizacion) {
        OperationReturnPayment r = new OperationReturnPayment();
        r.setOperacion(operation);
        r.setMonto(new BigDecimal(monto));
        r.setTipoPago(tipo);
        r.setSolicitadoPor(user);
        r.setEstatus(ReturnPaymentStatus.SOLICITADO);
        em.persist(r);

        OperationReturnInstallment i = new OperationReturnInstallment();
        i.setSolicitud(r);
        i.setMonto(new BigDecimal(monto));
        i.setTipoPago(tipo);
        i.setEstatus(estatus);
        i.setCuentaOrigen(origen);
        i.setFechaRealizacion(fechaRealizacion);
        i.setRealizadoPor(user);
        i.setCreadoPor(user);
        em.persist(i);
        return i;
    }

    static Map<CashDenomination, Integer> counts() {
        Map<CashDenomination, Integer> values = new EnumMap<>(CashDenomination.class);
        for (CashDenomination d : CashDenomination.values()) values.put(d, 0);
        values.put(CashDenomination.D100, 1);
        return values;
    }

    /** La fecha contable del cheque la da el día de caja, no su created_at. */
    CashGeneralMovement chequeCobrado(BankAccount origen, String monto, LocalDate fecha) {
        CashGeneralDay day = new CashGeneralDay();
        day.setFecha(fecha);
        day.setSaldoInicial(BigDecimal.ZERO);
        day.setSaldoActual(new BigDecimal(monto));
        day.setApertura(new EnumMap<>(CashDenomination.class));
        day.setCierre(new EnumMap<>(CashDenomination.class));
        day.setAbiertoPor(user);
        em.persist(day);

        CashGeneralMovement m = new CashGeneralMovement();
        m.setDia(day);
        m.setRequestId(UUID.randomUUID().toString());
        m.setDireccion(CashMovementDirection.ENTRADA);
        m.setTipo(CashMovementConcept.CHEQUE);
        m.setConcepto("Cheque cobrado");
        m.setBanco(origen.getBanco());
        m.setCuentaBancaria(origen);
        m.setMontoManual(new BigDecimal(monto));
        m.setSaldoAcumulado(new BigDecimal(monto));
        m.setDenominaciones(counts());
        m.setCreadoPor(user);
        em.persist(m);
        em.flush();
        // created_at es updatable=false en la entidad: se retrodata por SQL para poder
        // construir días anteriores en las pruebas.
        em.getEntityManager().createNativeQuery(
                        "update cash_general_movements set created_at = :instante where id = :id")
                .setParameter("instante", fecha.atTime(12, 0))
                .setParameter("id", m.getId())
                .executeUpdate();
        em.flush();
        em.clear();
        return m;
    }

    BankLedgerFilter todo() {
        return new BankLedgerFilter(HOY, HOY, null, null, null, null);
    }

    @Test
    void reproducesTheAgreedAccountingMatrix() {
        // Entradas bancarias: pago validado por transferencia y por depósito.
        payment(PaymentType.TRANSFERENCIA, "1000", PaymentStatus.VALIDADA, cuenta, HOY.atTime(9, 0));
        payment(PaymentType.DEPOSITO, "500", PaymentStatus.VALIDADA, cuenta, HOY.atTime(10, 0));
        // Salidas bancarias: retornos completados por transferencia, depósito y retiro sin tarjeta.
        installment(PaymentType.TRANSFERENCIA, "300", ReturnInstallmentStatus.COMPLETADA, cuenta, HOY.atTime(11, 0));
        installment(PaymentType.DEPOSITO, "200", ReturnInstallmentStatus.COMPLETADA, cuenta, HOY.atTime(12, 0));
        installment(PaymentType.RETIRO_SIN_TARJETA, "100", ReturnInstallmentStatus.COMPLETADA, cuenta, HOY.atTime(13, 0));
        // Salida bancaria: cheque cobrado en Caja General.
        chequeCobrado(cuenta, "400", HOY);

        // Lo que NO debe entrar al libro bancario:
        installment(PaymentType.EFECTIVO, "999", ReturnInstallmentStatus.COMPLETADA, null, HOY.atTime(15, 0));
        payment(PaymentType.EFECTIVO, "888", PaymentStatus.VALIDADA, null, HOY.atTime(16, 0));
        payment(PaymentType.TRANSFERENCIA, "777", PaymentStatus.PENDIENTE_VALIDACION, cuenta, null);
        installment(PaymentType.TRANSFERENCIA, "666", ReturnInstallmentStatus.PROGRAMADA, cuenta, null);
        em.flush();

        var totals = ledger.totals(todo());
        assertThat(totals.totalEntradas()).isEqualByComparingTo("1500");
        assertThat(totals.totalSalidas()).isEqualByComparingTo("1000");
        assertThat(totals.variacionNeta()).isEqualByComparingTo("500");
        assertThat(totals.totalMovimientos()).isEqualTo(6);
    }

    @Test
    void anEfectivoInstallmentWithABankAccountIsNeverCountedTwice() {
        // Hoy ninguna entrega en efectivo lleva cuenta origen, pero nada en el modelo lo
        // impide. Si apareciera, saldría del banco Y de Caja General por el mismo importe.
        installment(PaymentType.EFECTIVO, "500", ReturnInstallmentStatus.COMPLETADA, cuenta, HOY.atTime(9, 0));
        em.flush();

        assertThat(ledger.totals(todo()).totalMovimientos()).isZero();
        assertThat(ledger.bucketsForAccount(cuenta.getId(), HOY).salidasRetornos()).isEqualByComparingTo("0");
    }

    @Test
    void groupsTheSameMovementsIntoTheCutBuckets() {
        payment(PaymentType.TRANSFERENCIA, "1000", PaymentStatus.VALIDADA, cuenta, HOY.atTime(9, 0));
        payment(PaymentType.DEPOSITO, "500", PaymentStatus.VALIDADA, cuenta, HOY.atTime(10, 0));
        payment(PaymentType.CHEQUE, "250", PaymentStatus.VALIDADA, cuenta, HOY.atTime(10, 30));
        installment(PaymentType.TRANSFERENCIA, "300", ReturnInstallmentStatus.COMPLETADA, cuenta, HOY.atTime(11, 0));
        chequeCobrado(cuenta, "400", HOY);
        em.flush();

        var buckets = ledger.bucketsForAccount(cuenta.getId(), HOY);
        assertThat(buckets.entradasTransferencia()).isEqualByComparingTo("1000");
        assertThat(buckets.entradasDeposito()).isEqualByComparingTo("500");
        // El pago CHEQUE del cliente sigue siendo ENTRADA bancaria: no cambia con este trabajo.
        assertThat(buckets.entradasCheque()).isEqualByComparingTo("250");
        assertThat(buckets.salidasRetornos()).isEqualByComparingTo("300");
        assertThat(buckets.salidasCajaGeneral()).isEqualByComparingTo("400");

        // Los buckets del corte y los totales del libro son la misma definición.
        var totals = ledger.totals(new BankLedgerFilter(HOY, HOY, cuenta.getId(), null, null, null));
        assertThat(buckets.totalEntradas()).isEqualByComparingTo(totals.totalEntradas());
        assertThat(buckets.totalSalidas()).isEqualByComparingTo(totals.totalSalidas());
    }

    @Test
    void theDayWindowKeepsItsLastSecond() {
        // La ventana anterior terminaba en 23:59:59 y perdía este movimiento para siempre.
        payment(PaymentType.TRANSFERENCIA, "1000", PaymentStatus.VALIDADA, cuenta,
                HOY.atTime(23, 59, 59, 999_000_000));
        em.flush();

        assertThat(ledger.bucketsForAccount(cuenta.getId(), HOY).entradasTransferencia())
                .isEqualByComparingTo("1000");
        assertThat(ledger.bucketsForAccount(cuenta.getId(), HOY.plusDays(1)).entradasTransferencia())
                .isEqualByComparingTo("0");
    }

    @Test
    void filtersByAccountBankDirectionAndType() {
        payment(PaymentType.TRANSFERENCIA, "1000", PaymentStatus.VALIDADA, cuenta, HOY.atTime(9, 0));
        payment(PaymentType.DEPOSITO, "500", PaymentStatus.VALIDADA, otraCuenta, HOY.atTime(10, 0));
        installment(PaymentType.TRANSFERENCIA, "300", ReturnInstallmentStatus.COMPLETADA, cuenta, HOY.atTime(11, 0));
        em.flush();

        assertThat(ledger.totals(new BankLedgerFilter(HOY, HOY, cuenta.getId(), null, null, null))
                .totalMovimientos()).isEqualTo(2);
        assertThat(ledger.totals(new BankLedgerFilter(HOY, HOY, null, "Banorte", null, null))
                .totalMovimientos()).isEqualTo(1);
        assertThat(ledger.totals(new BankLedgerFilter(HOY, HOY, null, null, BankLedgerDirection.SALIDA, null))
                .totalSalidas()).isEqualByComparingTo("300");
        assertThat(ledger.totals(new BankLedgerFilter(HOY, HOY, null, null, null, "DEPOSITO"))
                .totalMovimientos()).isEqualTo(1);
        assertThat(ledger.totals(new BankLedgerFilter(HOY.minusDays(5), HOY.minusDays(1), null, null, null, null))
                .totalMovimientos()).isZero();
    }

    @Test
    void paginatesChronologicallyAndKeepsTotalsGlobal() {
        payment(PaymentType.TRANSFERENCIA, "1000", PaymentStatus.VALIDADA, cuenta, HOY.atTime(9, 0));
        payment(PaymentType.DEPOSITO, "500", PaymentStatus.VALIDADA, cuenta, HOY.atTime(10, 0));
        installment(PaymentType.TRANSFERENCIA, "300", ReturnInstallmentStatus.COMPLETADA, cuenta, HOY.atTime(11, 0));
        em.flush();

        var page = ledger.search(todo(), PageRequest.of(0, 2));
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        // Del más reciente al más antiguo.
        assertThat(page.getContent().get(0).origen()).isEqualTo(BankLedgerOrigin.RETORNO);
        assertThat(page.getContent().get(0).direccion()).isEqualTo(BankLedgerDirection.SALIDA);
        assertThat(page.getContent().get(0).operacionId()).isEqualTo(operation.getId());
        // Los totales cubren todo el filtro, no la página.
        assertThat(ledger.totals(todo()).totalMovimientos()).isEqualTo(3);
    }

    @Test
    void neverPublishesTheFullAccountNumber() {
        payment(PaymentType.TRANSFERENCIA, "1000", PaymentStatus.VALIDADA, cuenta, HOY.atTime(9, 0));
        em.flush();

        var row = ledger.search(todo(), PageRequest.of(0, 10)).getContent().get(0);
        assertThat(row.cuentaNumero()).doesNotContain(cuenta.getNumeroCuenta());
        assertThat(row.cuentaNumero()).endsWith(cuenta.getNumeroCuenta().substring(14));
        assertThat(row.cuentaBanco()).isEqualTo("BBVA");
        assertThat(row.cuentaTitular()).isEqualTo("Operaciones SA");
    }
}
