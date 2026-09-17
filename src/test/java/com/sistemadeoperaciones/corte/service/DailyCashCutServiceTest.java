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
import com.sistemadeoperaciones.pagos.enums.ReturnInstallmentStatus;
import com.sistemadeoperaciones.pagos.enums.ReturnPaymentStatus;
import com.sistemadeoperaciones.pagos.model.OperationPayment;
import com.sistemadeoperaciones.pagos.model.OperationReturnInstallment;
import com.sistemadeoperaciones.pagos.model.OperationReturnPayment;
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
        pagoValidado(PaymentType.TRANSFERENCIA, monto, fechaValidacion);
    }

    void pagoValidado(PaymentType tipo, String monto, LocalDateTime fechaValidacion) {
        OperationPayment p = new OperationPayment();
        p.setOperacion(operation);
        p.setMonto(new BigDecimal(monto));
        p.setTipoPago(tipo);
        p.setCuentaDestino(tipo == PaymentType.EFECTIVO ? null : cuenta);
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

    /** Retorno completado del tipo indicado. */
    void retornoCompletado(PaymentType tipo, String monto, BankAccount origen) {
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
        i.setEstatus(ReturnInstallmentStatus.COMPLETADA);
        i.setCuentaOrigen(origen);
        i.setFechaRealizacion(HOY.atTime(11, 0));
        i.setRealizadoPor(user);
        i.setCreadoPor(user);
        em.persist(i);
    }

    static void assertInvariant(DailyCashCutResponse dto) {
        // El corte es SOLO bancos: el efectivo se informa pero no suma.
        assertThat(dto.getTotalEntradas()).isEqualByComparingTo(
                dto.getEntradasTransferencia()
                        .add(dto.getEntradasDeposito())
                        .add(dto.getEntradasCheque()));
        assertThat(dto.getTotalRetornos()).isEqualByComparingTo(
                dto.getRetornosTransferencia()
                        .add(dto.getRetornosDeposito())
                        .add(dto.getRetornosCheque())
                        .add(dto.getRetornosRetiroSinTarjeta()));
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
    void cashNeverTouchesTheBankPosition() {
        pagoValidado(PaymentType.EFECTIVO, "8000", HOY.atTime(9, 0));
        retornoCompletado(PaymentType.EFECTIVO, "3000", null);
        em.flush();

        DailyCashCutResponse corte = service.calculateDailyCut(HOY);

        // Se siguen informando…
        assertThat(corte.getEntradasEfectivo()).isEqualByComparingTo("8000");
        assertThat(corte.getRetornosEfectivo()).isEqualByComparingTo("3000");
        // …pero no mueven el saldo bancario: ese dinero vive en Caja General.
        assertThat(corte.getTotalEntradas()).isEqualByComparingTo("0");
        assertThat(corte.getTotalSalidas()).isEqualByComparingTo("0");
        assertThat(corte.getSaldoFinal()).isEqualByComparingTo("0");
        assertInvariant(corte);
    }

    @Test
    void aCardlessWithdrawalReturnLeavesTheBank() {
        pagoValidado("20000", HOY.atTime(9, 0));
        retornoCompletado(PaymentType.RETIRO_SIN_TARJETA, "6000", cuenta);
        em.flush();

        DailyCashCutResponse corte = service.calculateDailyCut(HOY);

        assertThat(corte.getRetornosRetiroSinTarjeta()).isEqualByComparingTo("6000");
        assertThat(corte.getTotalRetornos()).isEqualByComparingTo("6000");
        assertThat(corte.getSaldoFinal()).isEqualByComparingTo("14000");
        assertInvariant(corte);
    }

    @Test
    void recalculatingRebuildsTheChainWithTheCurrentDefinition() {
        LocalDate ayer = HOY.minusDays(1);
        // Un corte guardado con la fórmula vieja: sumaba el efectivo al saldo bancario.
        com.sistemadeoperaciones.corte.model.DailyCashCut viejo =
                new com.sistemadeoperaciones.corte.model.DailyCashCut();
        viejo.setFecha(ayer);
        viejo.setSaldoInicial(new BigDecimal("1000"));
        viejo.setEntradasEfectivo(new BigDecimal("5000"));
        viejo.setTotalEntradas(new BigDecimal("5000"));
        viejo.setSaldoFinal(new BigDecimal("6000"));
        viejo.setEstatus(com.sistemadeoperaciones.corte.enums.DailyCashCutStatus.CERRADO);
        em.persist(viejo);
        em.flush();

        int recalculados = service.recalculateFrom(ayer);
        em.clear();

        assertThat(recalculados).isEqualTo(1);
        var rehecho = cuts.findByFecha(ayer).orElseThrow();
        // Se respeta el saldo inicial capturado a mano, pero el efectivo deja de sumar.
        assertThat(rehecho.getSaldoInicial()).isEqualByComparingTo("1000");
        assertThat(rehecho.getTotalEntradas()).isEqualByComparingTo("0");
        assertThat(rehecho.getSaldoFinal()).isEqualByComparingTo("1000");
    }

    @Test
    void recalculatingTwiceGivesTheSameResult() {
        LocalDate ayer = HOY.minusDays(1);
        com.sistemadeoperaciones.corte.model.DailyCashCut viejo =
                new com.sistemadeoperaciones.corte.model.DailyCashCut();
        viejo.setFecha(ayer);
        viejo.setSaldoInicial(new BigDecimal("1000"));
        viejo.setSaldoFinal(new BigDecimal("1000"));
        viejo.setEstatus(com.sistemadeoperaciones.corte.enums.DailyCashCutStatus.CERRADO);
        em.persist(viejo);
        em.flush();

        service.recalculateFrom(ayer);
        var primera = cuts.findByFecha(ayer).orElseThrow().getSaldoFinal();
        service.recalculateFrom(ayer);
        var segunda = cuts.findByFecha(ayer).orElseThrow().getSaldoFinal();

        assertThat(segunda).isEqualByComparingTo(primera);
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
