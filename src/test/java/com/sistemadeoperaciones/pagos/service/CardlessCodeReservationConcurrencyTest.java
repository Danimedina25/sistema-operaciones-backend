package com.sistemadeoperaciones.pagos.service;

import com.sistemadeoperaciones.clientes.model.Clientes;
import com.sistemadeoperaciones.cajageneral.service.CashGeneralService;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.notifications.service.NotificationService;
import com.sistemadeoperaciones.pagos.dto.retornos.CreateReturnInstallmentRequestDto;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.enums.ReturnInstallmentStatus;
import com.sistemadeoperaciones.pagos.enums.ReturnPaymentStatus;
import com.sistemadeoperaciones.pagos.exceptions.ReturnInstallmentAmountExceedsAvailableException;
import com.sistemadeoperaciones.pagos.model.OperationReturnPayment;
import com.sistemadeoperaciones.pagos.model.PaymentOperation;
import com.sistemadeoperaciones.pagos.repository.OperationReturnInstallmentRepository;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.config.RolePermissionPolicy;
import com.sistemadeoperaciones.shared.audit.service.BusinessAuditService;
import com.sistemadeoperaciones.shared.enums.RoleName;
import com.sistemadeoperaciones.usuarios.model.Role;
import com.sistemadeoperaciones.usuarios.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Prueba de concurrencia real contra H2: dos hilos registran parcialidades que
 * individualmente caben en el saldo pero juntas lo superan. Gracias al lock
 * pesimista sobre operación + solicitud, solo una debe confirmarse; la otra
 * falla con {@link ReturnInstallmentAmountExceedsAvailableException}.
 */
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:cardless_code_concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"
})
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
@Import({
        ReturnInstallmentServiceImpl.class, CardlessReturnFlow.class, com.sistemadeoperaciones.retiroscaja.BankAvailability.class, com.sistemadeoperaciones.retiroscaja.BankReservationGuard.class,
        ReturnAmountCalculator.class,
        ReturnRequestTotalsCalculator.class,
        ReturnPaymentDtoMapper.class
})
class CardlessCodeReservationConcurrencyTest {

    @Autowired ReturnInstallmentService service;
    @Autowired OperationReturnInstallmentRepository installmentRepository;
    @Autowired PlatformTransactionManager txManager;
    @Autowired org.springframework.orm.jpa.JpaTransactionManager jpaTxManager;

    @MockBean com.sistemadeoperaciones.corte.service.BankAccountDailyCutService bankBalances;
    @MockBean NotificationService notificationService;
    @MockBean AuthenticatedUserService authenticatedUserService;
    @MockBean CashGeneralService cashGeneralService;
    @MockBean BusinessAuditService businessAuditService;
    @MockBean RolePermissionPolicy rolePermissionPolicy;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // sin transacción de test: los hilos usan las suyas
    void concurrentCodesCannotOvercommitReservedBankFunds() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(txManager);

        // --- Fixture, en su propia transacción confirmada ---
        Long[] ids = tx.execute(status -> {
            var em = jpaTxManager.getEntityManagerFactory().createEntityManager();
            em.getTransaction().begin();

            User u = new User();
            u.setNombre("Jefa");
            u.setCorreo("jefa+" + System.nanoTime() + "@example.com");
            em.persist(u);

            Clientes c = new Clientes();
            c.setNombre("Cliente");
            c.setUser(u);
            c.setPorcentajeComisionSocio(BigDecimal.ZERO);
            c.setPorcentajeComisionOficina(BigDecimal.ZERO);
            em.persist(c);

            PaymentOperation op = new PaymentOperation();
            op.setCliente(c);
            op.setSocioComercial(u);
            op.setMontoTotal(new BigDecimal("50000.00"));
            op.setMontoValidado(new BigDecimal("50000.00"));
            op.setPorcentajeComisionSocio(BigDecimal.ZERO);
            op.setPorcentajeComisionOficina(BigDecimal.ZERO);
            op.setEstatus(com.sistemadeoperaciones.pagos.enums.OperationStatus.RETORNO_TOTAL_SOLICITADO);
            em.persist(op);

            OperationReturnPayment req = new OperationReturnPayment();
            req.setOperacion(op);
            req.setMonto(new BigDecimal("15000.00"));
            req.setTipoPago(PaymentType.RETIRO_SIN_TARJETA);
            req.setSolicitadoPor(u);
            req.setEstatus(ReturnPaymentStatus.SOLICITADO);
            em.persist(req);

            OperationReturnPayment req2 = new OperationReturnPayment();
            req2.setOperacion(op); req2.setMonto(new BigDecimal("15000.00")); req2.setTipoPago(PaymentType.RETIRO_SIN_TARJETA); req2.setSolicitadoPor(u); req2.setEstatus(ReturnPaymentStatus.SOLICITADO); em.persist(req2);
            BankAccount ba = new BankAccount();
            ba.setBanco("BBVA");
            ba.setTitular("Empresa");
            ba.setNumeroCuenta("000" + (System.nanoTime() % 1_000_000_000));
            ba.setClabe("0123456789" + String.format("%08d", System.nanoTime() % 100_000_000));
            ba.setActivo(true);
            em.persist(ba);

            em.getTransaction().commit();
            Long[] result = { req.getId(), ba.getId(), u.getId(), req2.getId() };
            em.close();
            return result;
        });

        Long requestId = ids[0];
        Long cuentaOrigenId = ids[1];
        User actor = new User();
        actor.setId(ids[2]);
        Role role = new Role();
        role.setName(RoleName.JEFA_CUENTAS);
        actor.setRoles(Set.of(role));
        when(authenticatedUserService.getCurrentUser()).thenReturn(actor);
        when(rolePermissionPolicy.allows(actor, RolePermissionPolicy.Action.RETURN_BANK_PAY)).thenReturn(true);

        var bankBalance = new com.sistemadeoperaciones.corte.dto.BankAccountBalanceDetailResponseDto();
        bankBalance.setSaldoFinal(new BigDecimal("20000"));
        when(bankBalances.calculateBalance(org.mockito.ArgumentMatchers.eq(cuentaOrigenId), org.mockito.ArgumentMatchers.any())).thenReturn(bankBalance);
        // --- Dos hilos, cada uno con su transacción vía el @Transactional del servicio ---
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        AtomicReference<Throwable> unexpected = new AtomicReference<>();

        AtomicInteger requestSequence = new AtomicInteger();
        Runnable task = () -> {
            CreateReturnInstallmentRequestDto dto = new CreateReturnInstallmentRequestDto();
            dto.setMonto(new BigDecimal("15000"));
            dto.setCuentaOrigenId(cuentaOrigenId);
            dto.setCodigoRetiroSinTarjeta("CODE-" + Thread.currentThread().getId());
            ready.countDown();
            try {
                go.await();
                service.createInstallment(requestSequence.getAndIncrement() == 0 ? requestId : ids[3], dto);
                ok.incrementAndGet();
            } catch (com.sistemadeoperaciones.shared.exception.BadRequestException e) {
                rejected.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Throwable t) {
                unexpected.set(t);
            }
        };

        pool.submit(task);
        pool.submit(task);
        ready.await(5, TimeUnit.SECONDS);
        go.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(unexpected.get()).isNull();
        assertThat(ok.get()).isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(1);

        BigDecimal completado = installmentRepository.sumCompletedBySolicitud(requestId);
        assertThat(completado).isEqualByComparingTo("0");
        assertThat(installmentRepository.sumReservedCodes(cuentaOrigenId)).isEqualByComparingTo("15000");
        assertThat(installmentRepository.countBySolicitudIdAndEstatusIn(requestId, java.util.List.of(ReturnInstallmentStatus.PROGRAMADA)) + installmentRepository.countBySolicitudIdAndEstatusIn(ids[3], java.util.List.of(ReturnInstallmentStatus.PROGRAMADA))).isEqualTo(1);
    }
}
