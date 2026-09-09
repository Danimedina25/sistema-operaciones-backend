package com.sistemadeoperaciones.pagos.repository;

import com.sistemadeoperaciones.clientes.model.Clientes;
import com.sistemadeoperaciones.pagos.enums.*;
import com.sistemadeoperaciones.pagos.model.*;
import com.sistemadeoperaciones.pagos.repository.specification.*;
import com.sistemadeoperaciones.usuarios.model.User;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.*;
import org.springframework.data.domain.PageRequest;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
class StaffPendingSpecificationTest {
    @Autowired TestEntityManager em;
    @Autowired PaymentOperationRepository operations;
    @Autowired OperationReturnInstallmentRepository installments;
    User user;
    Clientes client;
    @BeforeEach void setup() {
        user = new User(); user.setNombre("Test"); user.setCorreo("pending@example.com"); em.persist(user);
        client = new Clientes(); client.setNombre("Cliente"); client.setUser(user);
        client.setPorcentajeComisionSocio(BigDecimal.ZERO); client.setPorcentajeComisionOficina(BigDecimal.ZERO); em.persist(client);
    }
    PaymentOperation operation() {
        var op = new PaymentOperation(); op.setCliente(client); op.setSocioComercial(user); op.setActivo(true);
        op.setMontoTotal(new BigDecimal("1000")); op.setMontoValidado(new BigDecimal("1000"));
        op.setPorcentajeComisionSocio(BigDecimal.ZERO); op.setPorcentajeComisionOficina(BigDecimal.ZERO);
        em.persist(op); return op;
    }
    void payment(PaymentOperation op, PaymentType type, PaymentStatus status) {
        var p = new OperationPayment(); p.setOperacion(op); p.setTipoPago(type); p.setEstatus(status);
        p.setFechaComprobante(LocalDateTime.now()); p.setMonto(BigDecimal.TEN); p.setRegistradoPor(user); p.setComprobanteUrl("test"); em.persist(p);
    }
    OperationReturnPayment request(PaymentOperation op, PaymentType type) {
        var r = new OperationReturnPayment(); r.setOperacion(op); r.setTipoPago(type); r.setMonto(new BigDecimal("100"));
        r.setSolicitadoPor(user); r.setEstatus(ReturnPaymentStatus.SOLICITADO); em.persist(r); return r;
    }
    OperationReturnInstallment installment(OperationReturnPayment request, ReturnInstallmentStatus status, String amount) {
        var i = new OperationReturnInstallment(); i.setSolicitud(request); i.setTipoPago(request.getTipoPago());
        i.setMonto(new BigDecimal(amount)); i.setCreadoPor(user); i.setEstatus(status); em.persist(i); return i;
    }
    @Test void paymentTypeAndStatusBelongToSameRowAndTotalsAreDistinctAcrossPages() {
        var mismatch = operation(); payment(mismatch, PaymentType.EFECTIVO, PaymentStatus.VALIDADA);
        payment(mismatch, PaymentType.TRANSFERENCIA, PaymentStatus.PENDIENTE_VALIDACION);
        for (int n=0; n<3; n++) { var op=operation(); payment(op, PaymentType.EFECTIVO, PaymentStatus.PENDIENTE_VALIDACION); payment(op, PaymentType.EFECTIVO, PaymentStatus.PENDIENTE_VALIDACION); }
        em.flush(); em.clear();
        var spec=PaymentOperationSpecification.hasPaymentMatching(List.of(PaymentType.EFECTIVO), PaymentStatus.PENDIENTE_VALIDACION);
        assertThat(operations.findAll(spec, PageRequest.of(0,2)).getTotalElements()).isEqualTo(3);
        assertThat(operations.findAll(spec, PageRequest.of(1,2)).getContent()).hasSize(1);
    }
    @Test void returnsWithAvailableBalanceAreFilteredBeforePagination() {
        var partial = request(operation(), PaymentType.EFECTIVO); installment(partial, ReturnInstallmentStatus.COMPLETADA, "30");
        installment(partial, ReturnInstallmentStatus.CANCELADA, "100");
        var reserved = request(operation(), PaymentType.EFECTIVO); installment(reserved, ReturnInstallmentStatus.PROGRAMADA, "100");
        var complete = request(operation(), PaymentType.EFECTIVO); installment(complete, ReturnInstallmentStatus.COMPLETADA, "100");
        var bank = request(operation(), PaymentType.CHEQUE); installment(bank, ReturnInstallmentStatus.COMPLETADA, "25");
        var duplicate = operation(); request(duplicate, PaymentType.EFECTIVO); request(duplicate, PaymentType.RETIRO_SIN_TARJETA);
        em.flush(); em.clear();
        var spec = PaymentOperationSpecification.hasReturnToPrepare(List.of(PaymentType.EFECTIVO, PaymentType.RETIRO_SIN_TARJETA));
        assertThat(operations.findAll(spec, PageRequest.of(0,1)).getTotalElements()).isEqualTo(2);
        assertThat(operations.findAll(spec, PageRequest.of(1,1)).getContent()).hasSize(1);
        assertThat(operations.count(PaymentOperationSpecification.hasReturnToPrepare(List.of(PaymentType.CHEQUE)))).isEqualTo(1);
    }
    @Test void deliveriesUseIndependentConfirmationMarksAndIncludeOldPendingClosures() {
        var today=LocalDate.of(2026,9,9); var r=request(operation(), PaymentType.EFECTIVO);
        for(int n=0;n<3;n++) installment(r, ReturnInstallmentStatus.PROGRAMADA,"10").setFechaHoraRecoleccion(today.atTime(12,0));
        var old=installment(r,ReturnInstallmentStatus.ENTREGADA,"10"); old.setFechaHoraRecoleccion(today.minusMonths(2).atTime(12,0)); old.setFechaConfirmacion(today.minusDays(1).atTime(10,0));
        var closed=installment(r,ReturnInstallmentStatus.ENTREGADA,"10"); closed.setFechaEntrega(today.atTime(9,0)); closed.setFechaHoraRecoleccion(today.atTime(10,0));
        installment(r,ReturnInstallmentStatus.CANCELADA,"10").setFechaHoraRecoleccion(today.atTime(12,0));
        installment(r,ReturnInstallmentStatus.COMPLETADA,"10").setFechaHoraRecoleccion(today.atTime(12,0));
        em.flush(); em.clear();
        var spec=PendingInstallmentSpecification.pending(false,today,List.of(PaymentType.EFECTIVO));
        assertThat(installments.findAll(spec,PageRequest.of(0,2)).getTotalElements()).isEqualTo(3);
        assertThat(installments.findAll(spec,PageRequest.of(1,2)).getContent()).hasSize(1);
        assertThat(installments.findAll(PendingInstallmentSpecification.pending(true,today,List.of(PaymentType.EFECTIVO)))).extracting(OperationReturnInstallment::getId).containsExactly(old.getId());
    }
}
