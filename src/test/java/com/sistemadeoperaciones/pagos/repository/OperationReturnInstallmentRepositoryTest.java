package com.sistemadeoperaciones.pagos.repository;

import com.sistemadeoperaciones.clientes.model.Clientes;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.enums.ReturnInstallmentStatus;
import com.sistemadeoperaciones.pagos.enums.ReturnPaymentStatus;
import com.sistemadeoperaciones.pagos.model.OperationReturnInstallment;
import com.sistemadeoperaciones.pagos.model.OperationReturnPayment;
import com.sistemadeoperaciones.pagos.model.PaymentOperation;
import com.sistemadeoperaciones.usuarios.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica las sumas agregadas por parcialidad contra H2, incluyendo las que
 * alimentan el corte de caja.
 */
@DataJpaTest
class OperationReturnInstallmentRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired OperationReturnInstallmentRepository repository;

    PaymentOperation operation;
    User user;

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
        operation.setMontoTotal(new BigDecimal("100000.00"));
        operation.setMontoValidado(new BigDecimal("100000.00"));
        operation.setPorcentajeComisionSocio(BigDecimal.ZERO);
        operation.setPorcentajeComisionOficina(BigDecimal.ZERO);
        em.persist(operation);
    }

    private OperationReturnPayment request(PaymentType tipo, String monto) {
        OperationReturnPayment r = new OperationReturnPayment();
        r.setOperacion(operation);
        r.setMonto(new BigDecimal(monto));
        r.setTipoPago(tipo);
        r.setSolicitadoPor(user);
        r.setEstatus(ReturnPaymentStatus.SOLICITADO);
        em.persist(r);
        return r;
    }

    private OperationReturnInstallment installment(
            OperationReturnPayment solicitud,
            String monto,
            ReturnInstallmentStatus estatus,
            LocalDateTime fechaRealizacion
    ) {
        OperationReturnInstallment i = new OperationReturnInstallment();
        i.setSolicitud(solicitud);
        i.setMonto(new BigDecimal(monto));
        i.setTipoPago(solicitud.getTipoPago());
        i.setEstatus(estatus);
        i.setCreadoPor(user);
        i.setFechaRealizacion(fechaRealizacion);
        em.persist(i);
        return i;
    }

    @Test
    void sumsCompletedAndInFlightPerRequest() {
        OperationReturnPayment sol = request(PaymentType.TRANSFERENCIA, "40000");
        installment(sol, "15000", ReturnInstallmentStatus.COMPLETADA, LocalDateTime.now());
        installment(sol, "20000", ReturnInstallmentStatus.COMPLETADA, LocalDateTime.now());
        installment(sol, "5000", ReturnInstallmentStatus.PROGRAMADA, null);
        installment(sol, "999", ReturnInstallmentStatus.CANCELADA, null);
        em.flush();
        em.clear();

        assertThat(repository.sumCompletedBySolicitud(sol.getId())).isEqualByComparingTo("35000");
        assertThat(repository.sumInFlightBySolicitud(sol.getId())).isEqualByComparingTo("5000");
        assertThat(repository.countBySolicitudIdAndEstatusNot(
                sol.getId(), ReturnInstallmentStatus.CANCELADA)).isEqualTo(3);
    }

    @Test
    void sumsCompletedPerOperationIgnoringNonCompleted() {
        OperationReturnPayment sol1 = request(PaymentType.TRANSFERENCIA, "40000");
        OperationReturnPayment sol2 = request(PaymentType.EFECTIVO, "25000");
        installment(sol1, "40000", ReturnInstallmentStatus.COMPLETADA, LocalDateTime.now());
        installment(sol2, "10000", ReturnInstallmentStatus.COMPLETADA, LocalDateTime.now());
        installment(sol2, "10000", ReturnInstallmentStatus.PROGRAMADA, null);
        em.flush();
        em.clear();

        assertThat(repository.sumCompletedByOperation(operation.getId()))
                .isEqualByComparingTo("50000");
    }

    @Test
    void sumsCompletedByTypeAndDateForCashCut() {
        OperationReturnPayment sol = request(PaymentType.TRANSFERENCIA, "40000");
        LocalDateTime hoy = LocalDateTime.now();
        LocalDateTime ayer = hoy.minusDays(1);

        installment(sol, "10000", ReturnInstallmentStatus.COMPLETADA, hoy);
        installment(sol, "7000", ReturnInstallmentStatus.COMPLETADA, ayer);
        installment(sol, "3000", ReturnInstallmentStatus.PROGRAMADA, null);
        em.flush();
        em.clear();

        BigDecimal soloHoy = repository.sumCompletedByTypeBetween(
                PaymentType.TRANSFERENCIA,
                hoy.toLocalDate().atStartOfDay(),
                hoy.toLocalDate().atTime(23, 59, 59)
        );
        assertThat(soloHoy).isEqualByComparingTo("10000");

        BigDecimal efectivo = repository.sumCompletedByTypeBetween(
                PaymentType.EFECTIVO,
                ayer.minusDays(1),
                hoy.plusDays(1)
        );
        assertThat(efectivo).isEqualByComparingTo("0");
    }
}
