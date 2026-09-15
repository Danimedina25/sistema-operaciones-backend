package com.sistemadeoperaciones.cajageneral.repository;
import com.sistemadeoperaciones.cajageneral.model.CashGeneralMovement;
import com.sistemadeoperaciones.pagos.model.OperationReturnInstallment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.*;
public interface CashGeneralMovementRepository extends JpaRepository<CashGeneralMovement, Long> {
    Optional<CashGeneralMovement> findByRequestId(String requestId);
    boolean existsByParcialidadId(Long id);
    Optional<CashGeneralMovement> findByParcialidadId(Long id);
    List<CashGeneralMovement> findByDiaIdOrderByIdAsc(Long dayId);
    @EntityGraph(attributePaths = {"parcialidad", "dia", "creadoPor"})
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
