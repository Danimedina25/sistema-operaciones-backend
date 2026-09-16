package com.sistemadeoperaciones.corte.service;

import com.sistemadeoperaciones.cajageneral.enums.CashDenomination;
import com.sistemadeoperaciones.cajageneral.enums.CashMovementConcept;
import com.sistemadeoperaciones.cajageneral.enums.CashMovementDirection;
import com.sistemadeoperaciones.cajageneral.model.CashGeneralDay;
import com.sistemadeoperaciones.cajageneral.model.CashGeneralMovement;
import com.sistemadeoperaciones.clientes.model.Clientes;
import com.sistemadeoperaciones.corte.dto.BankAccountBalanceDetailResponseDto;
import com.sistemadeoperaciones.corte.repository.BankAccountDailyCutRepository;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.pagos.enums.PaymentStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.model.OperationPayment;
import com.sistemadeoperaciones.pagos.model.PaymentOperation;
import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El corte bancario por cuenta: que el cheque cobrado lo afecte, que la invariante
 * saldo final = saldo inicial + entradas − salidas se cumpla, que el histórico siga
 * siendo reproducible y que la cadena se pueda rehacer sin dejar saldos huérfanos.
 */
@DataJpaTest
@Import({BankAccountDailyCutServiceImpl.class, BankLedgerQuery.class})
class BankAccountDailyCutServiceTest {

    @Autowired TestEntityManager em;
    @Autowired BankAccountDailyCutService service;
    @Autowired BankAccountDailyCutRepository cuts;

    static final LocalDate HOY = LocalDate.now();
    static final LocalDate AYER = HOY.minusDays(1);
    static final LocalDate ANTEAYER = HOY.minusDays(2);

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

        cuenta = account(true);
    }

    BankAccount account(boolean activo) {
        BankAccount a = new BankAccount();
        a.setBanco("BBVA");
        a.setTitular("Operaciones SA");
        a.setNumeroCuenta(UUID.randomUUID().toString().substring(0, 18));
        a.setClabe(UUID.randomUUID().toString().replaceAll("[^0-9]", "0").substring(0, 18));
        a.setActivo(activo);
        em.persist(a);
        return a;
    }

    void payment(String monto, LocalDateTime fechaValidacion) {
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

    CashGeneralMovement chequeCobrado(String monto, LocalDate fecha) {
        CashGeneralDay day = new CashGeneralDay();
        day.setFecha(fecha);
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
        m.setBanco("BBVA");
        m.setCuentaBancaria(cuenta);
        m.setMontoManual(new BigDecimal(monto));
        m.setSaldoAcumulado(new BigDecimal(monto));
        m.setDenominaciones(counts);
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

    static void assertInvariant(BankAccountBalanceDetailResponseDto dto) {
        assertThat(dto.getTotalEntradas()).isEqualByComparingTo(
                dto.getEntradasTransferencia().add(dto.getEntradasDeposito()).add(dto.getEntradasCheque()));
        assertThat(dto.getTotalSalidas()).isEqualByComparingTo(
                dto.getSalidasRetornos().add(dto.getSalidasCheque()).add(dto.getSalidasComisiones()));
        assertThat(dto.getSaldoFinal()).isEqualByComparingTo(
                dto.getSaldoInicial().add(dto.getTotalEntradas()).subtract(dto.getTotalSalidas()));
    }

    @Test
    void todayIncludesTheCashedChequeAsABankExit() {
        payment("1000", HOY.atTime(9, 0));
        chequeCobrado("400", HOY);
        em.flush();

        var dto = service.calculateBalance(cuenta.getId(), HOY);

        assertThat(dto.getEntradasTransferencia()).isEqualByComparingTo("1000");
        assertThat(dto.getSalidasCheque()).isEqualByComparingTo("400");
        assertThat(dto.getSaldoFinal()).isEqualByComparingTo("600");
        assertInvariant(dto);
    }

    @Test
    void aRegisteredCutIsFrozenAndNeverRecomputed() {
        payment("1000", ANTEAYER.atTime(9, 0));
        em.flush();
        service.registerDailyCut(ANTEAYER);
        em.clear();

        // Aparece un movimiento nuevo con fecha de aquel día: el corte guardado NO cambia.
        chequeCobrado("400", ANTEAYER);
        em.flush();

        var historico = service.calculateBalance(cuenta.getId(), ANTEAYER);
        assertThat(historico.getSaldoFinal()).isEqualByComparingTo("1000");
        assertThat(historico.getSalidasCheque()).isEqualByComparingTo("0");
        assertInvariant(historico);
    }

    @Test
    void aMissingHistoricalCutIsReportedInsteadOfGuessed() {
        assertThatThrownBy(() -> service.calculateBalance(cuenta.getId(), ANTEAYER))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No existe corte registrado");
    }

    @Test
    void theOpeningBalanceChainsFromThePreviousCut() {
        payment("1000", ANTEAYER.atTime(9, 0));
        em.flush();
        service.registerDailyCut(ANTEAYER);

        chequeCobrado("400", AYER);
        em.flush();
        service.registerDailyCut(AYER);
        em.clear();

        var ayer = service.calculateBalance(cuenta.getId(), AYER);
        assertThat(ayer.getSaldoInicial()).isEqualByComparingTo("1000");
        assertThat(ayer.getSalidasCheque()).isEqualByComparingTo("400");
        assertThat(ayer.getSaldoFinal()).isEqualByComparingTo("600");
        assertInvariant(ayer);
    }

    @Test
    void recalculatingRebuildsTheChainAfterAnAdministrativeDeletion() {
        payment("1000", ANTEAYER.atTime(9, 0));
        CashGeneralMovement cheque = chequeCobrado("400", ANTEAYER);
        em.flush();
        service.registerDailyCut(ANTEAYER);
        service.registerDailyCut(AYER);
        em.clear();

        assertThat(service.calculateBalance(cuenta.getId(), ANTEAYER).getSaldoFinal()).isEqualByComparingTo("600");
        assertThat(service.calculateBalance(cuenta.getId(), AYER).getSaldoInicial()).isEqualByComparingTo("600");

        // Administración elimina el corte de Caja General: el cheque desaparece.
        em.getEntityManager().createNativeQuery("delete from cash_general_movement_counts where movement_id = :id")
                .setParameter("id", cheque.getId()).executeUpdate();
        em.getEntityManager().createNativeQuery("delete from cash_general_movements where id = :id")
                .setParameter("id", cheque.getId()).executeUpdate();
        em.flush();
        em.clear();

        int recalculados = service.recalculateFrom(cuenta.getId(), ANTEAYER);
        em.clear();

        assertThat(recalculados).isEqualTo(2);
        var anteayer = service.calculateBalance(cuenta.getId(), ANTEAYER);
        var ayer = service.calculateBalance(cuenta.getId(), AYER);
        assertThat(anteayer.getSalidasCheque()).isEqualByComparingTo("0");
        assertThat(anteayer.getSaldoFinal()).isEqualByComparingTo("1000");
        // La cadena vuelve a cuadrar: ningún saldo queda huérfano.
        assertThat(ayer.getSaldoInicial()).isEqualByComparingTo("1000");
        assertThat(ayer.getSaldoFinal()).isEqualByComparingTo("1000");
        assertInvariant(anteayer);
        assertInvariant(ayer);
    }

    @Test
    void recalculatingNeverInventsCutsForDaysThatNeverHadOne() {
        assertThat(service.recalculateFrom(cuenta.getId(), ANTEAYER)).isZero();
        assertThat(cuts.count()).isZero();
    }

    @Test
    void registeringCoversDeactivatedAccountsSoTheirChainStaysContiguous() {
        BankAccount inactiva = account(false);
        em.flush();

        service.registerDailyCut(ANTEAYER);

        assertThat(cuts.findByBankAccountIdAndFecha(inactiva.getId(), ANTEAYER)).isPresent();
        assertThat(cuts.findByBankAccountIdAndFecha(cuenta.getId(), ANTEAYER)).isPresent();
    }
}
