package com.sistemadeoperaciones.corte.service;

import com.sistemadeoperaciones.cajageneral.enums.CashDenomination;
import com.sistemadeoperaciones.cajageneral.enums.CashMovementConcept;
import com.sistemadeoperaciones.cajageneral.enums.CashMovementDirection;
import com.sistemadeoperaciones.cajageneral.model.CashGeneralDay;
import com.sistemadeoperaciones.cajageneral.model.CashGeneralMovement;
import com.sistemadeoperaciones.clientes.model.Clientes;
import com.sistemadeoperaciones.corte.dto.DailyCashCutResponse;
import com.sistemadeoperaciones.corte.repository.DailyCashCutRepository;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.pagos.enums.PaymentStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.model.OperationPayment;
import com.sistemadeoperaciones.pagos.model.PaymentOperation;
import com.sistemadeoperaciones.usuarios.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El corte diario global es la posición de las CUENTAS BANCARIAS: "Cortes y saldos" es
 * bancos y "Caja General" es efectivo. Por eso el cheque cobrado en ventanilla cuenta aquí
 * como salida —el dinero dejó el banco— y como entrada en el libro de Caja General.
 */
@DataJpaTest
@Import(DailyCashCutServiceImpl.class)
class DailyCashCutServiceTest {

    @Autowired TestEntityManager em;
    @Autowired DailyCashCutService service;
    @Autowired DailyCashCutRepository cuts;

    static final LocalDate HOY = LocalDate.now();

    User user;
    PaymentOperation operation;
    BankAccount cuenta;

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

        cuenta = new BankAccount();
        cuenta.setBanco("BANORTE");
        cuenta.setTitular("AMPARO SANCHEZ CAMILO");
        cuenta.setNumeroCuenta(UUID.randomUUID().toString().substring(0, 18));
        cuenta.setClabe(UUID.randomUUID().toString().replaceAll("[^0-9]", "0").substring(0, 18));
        cuenta.setActivo(true);
        em.persist(cuenta);
    }

    void pagoValidado(String monto, LocalDateTime fechaValidacion) {
        OperationPayment p = new OperationPayment();
        p.setOperacion(operation);
        p.setMonto(new BigDecimal(monto));
        p.setTipoPago(PaymentType.TRANSFERENCIA);
        p.setCuentaDestino(cuenta);
        p.setComprobanteUrl("https://example.com/c.jpg");
        p.setEstatus(PaymentStatus.VALIDADA);
        p.setRegistradoPor(user);
        p.setValidadoPor(user);
        p.setFechaPago(LocalDateTime.now());
        p.setFechaComprobante(LocalDateTime.now());
        p.setFechaValidacion(fechaValidacion);
        em.persist(p);
    }

    CashGeneralMovement chequeCobrado(String monto, BankAccount origen) {
        CashGeneralDay day = new CashGeneralDay();
        day.setFecha(HOY);
        day.setSaldoInicial(BigDecimal.ZERO);
        day.setSaldoActual(new BigDecimal(monto));
        day.setApertura(new EnumMap<>(CashDenomination.class));
        day.setCierre(new EnumMap<>(CashDenomination.class));
        day.setAbiertoPor(user);
        em.persist(day);

        Map<CashDenomination, Integer> counts = new EnumMap<>(CashDenomination.class);
        for (CashDenomination d : CashDenomination.values()) counts.put(d, 0);

        CashGeneralMovement m = new CashGeneralMovement();
        m.setDia(day);
        m.setRequestId(UUID.randomUUID().toString());
        m.setDireccion(CashMovementDirection.ENTRADA);
        m.setTipo(CashMovementConcept.CHEQUE);
        m.setConcepto("Cheque cobrado");
        m.setBanco(origen == null ? "BANORTE" : origen.getBanco());
        m.setCuentaBancaria(origen);
        m.setMontoManual(new BigDecimal(monto));
        m.setSaldoAcumulado(new BigDecimal(monto));
        m.setDenominaciones(counts);
        m.setCreadoPor(user);
        em.persist(m);
        em.flush();
        return m;
    }

    static void assertInvariant(DailyCashCutResponse dto) {
        assertThat(dto.getTotalSalidas()).isEqualByComparingTo(
                dto.getTotalRetornos()
                        .add(dto.getSalidasChequeCobrado())
                        .add(dto.getTotalComisionesSocios()));
        assertThat(dto.getSaldoFinal()).isEqualByComparingTo(
                dto.getSaldoInicial().add(dto.getTotalEntradas()).subtract(dto.getTotalSalidas()));
    }

    @Test
    void aCashedChequeLeavesTheBankPosition() {
        pagoValidado("20000", HOY.atTime(9, 0));
        chequeCobrado("17500", cuenta);
        em.flush();

        DailyCashCutResponse corte = service.calculateDailyCut(HOY);

        assertThat(corte.getEntradasTransferencia()).isEqualByComparingTo("20000");
        assertThat(corte.getSalidasChequeCobrado()).isEqualByComparingTo("17500");
        assertThat(corte.getTotalSalidas()).isEqualByComparingTo("17500");
        assertThat(corte.getSaldoFinal()).isEqualByComparingTo("2500");
        assertInvariant(corte);
    }

    @Test
    void aChequeWithoutALinkedAccountStillLeftSomeBank() {
        // Los cheques capturados antes de la integración sólo guardan el nombre del banco.
        // No se sabe de qué cuenta salieron, pero salieron de alguna: el corte global sí los
        // contempla aunque el corte POR CUENTA no pueda atribuirlos.
        chequeCobrado("5000", null);
        em.flush();

        DailyCashCutResponse corte = service.calculateDailyCut(HOY);

        assertThat(corte.getSalidasChequeCobrado()).isEqualByComparingTo("5000");
        assertThat(corte.getSaldoFinal()).isEqualByComparingTo("-5000");
        assertInvariant(corte);
    }

    @Test
    void withoutChequesNothingChanges() {
        pagoValidado("20000", HOY.atTime(9, 0));
        em.flush();

        DailyCashCutResponse corte = service.calculateDailyCut(HOY);

        assertThat(corte.getSalidasChequeCobrado()).isEqualByComparingTo("0");
        assertThat(corte.getTotalSalidas()).isEqualByComparingTo("0");
        assertThat(corte.getSaldoFinal()).isEqualByComparingTo("20000");
        assertInvariant(corte);
    }

    @Test
    void aRegisteredCutKeepsItsChequesAndIsNotRecomputed() {
        chequeCobrado("17500", cuenta);
        em.flush();

        service.registerDailyCut(HOY);
        em.clear();

        assertThat(cuts.findByFecha(HOY)).isPresent();
        assertThat(cuts.findByFecha(HOY).orElseThrow().getSalidasChequeCobrado())
                .isEqualByComparingTo("17500");
    }
}
