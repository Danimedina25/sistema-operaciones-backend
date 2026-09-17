package com.sistemadeoperaciones.cajageneral.repository;
import com.sistemadeoperaciones.cajageneral.model.CashGeneralMovement;
import com.sistemadeoperaciones.pagos.model.OperationReturnInstallment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
public interface CashGeneralMovementRepository extends JpaRepository<CashGeneralMovement, Long> {
    Optional<CashGeneralMovement> findByRequestId(String requestId);
    boolean existsByParcialidadId(Long id);
    Optional<CashGeneralMovement> findByParcialidadId(Long id);
    Optional<CashGeneralMovement> findByPagoId(Long paymentId);
    long countByCuentaBancariaId(Long bankAccountId);

    /**
     * Efectivo retirado del banco hacia la caja en todo el sistema, sin importar la cuenta:
     * cheques cobrados y retiros sin tarjeta. Lo consume el corte bancario global, al que
     * sólo le interesa que el dinero salió de algún banco. Incluye los movimientos históricos
     * que sólo guardan el nombre del banco como texto: también sacaron dinero de una cuenta,
     * aunque no se pueda saber de cuál.
     */
    @Query("""
        select coalesce(sum(m.montoManual), 0)
        from CashGeneralMovement m
        where m.tipo in (com.sistemadeoperaciones.cajageneral.enums.CashMovementConcept.CHEQUE,
                         com.sistemadeoperaciones.cajageneral.enums.CashMovementConcept.RETIRO_SIN_TARJETA)
          and m.direccion = com.sistemadeoperaciones.cajageneral.enums.CashMovementDirection.ENTRADA
          and m.createdAt between :inicio and :fin
        """)
    BigDecimal sumRetiradoHaciaCajaBetween(@Param("inicio") LocalDateTime inicio,
                                           @Param("fin") LocalDateTime fin);
    List<CashGeneralMovement> findByDiaIdOrderByIdAsc(Long dayId);
    @EntityGraph(attributePaths = {"parcialidad", "pago", "dia", "creadoPor", "cuentaBancaria"})
    List<CashGeneralMovement> findByDiaFechaBetweenOrderByIdAsc(LocalDate start, LocalDate end);
    @Query("""
        select i from OperationReturnInstallment i
        where i.tipoPago = com.sistemadeoperaciones.pagos.enums.PaymentType.EFECTIVO
          and i.estatus = com.sistemadeoperaciones.pagos.enums.ReturnInstallmentStatus.COMPLETADA
          and not exists (select m.id from CashGeneralMovement m where m.parcialidad = i)
        order by i.fechaRealizacion asc, i.id asc
        """)
    List<OperationReturnInstallment> findUnlinkedDeliveries(Pageable pageable);
}
