package com.sistemadeoperaciones.pagos.repository.specification;
import com.sistemadeoperaciones.pagos.model.OperationReturnInstallment;
import com.sistemadeoperaciones.pagos.enums.ReturnInstallmentStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.util.List;
public final class PendingInstallmentSpecification {
    private PendingInstallmentSpecification() {}
    public static Specification<OperationReturnInstallment> pending(boolean confirmation, LocalDate today, List<PaymentType> types) {
        return (root, query, cb) -> cb.and(
            root.get("tipoPago").in(types),
            cb.isTrue(root.get("solicitud").get("operacion").get("activo")),
            root.get("estatus").in(ReturnInstallmentStatus.PROGRAMADA, ReturnInstallmentStatus.ENTREGADA),
            cb.isNull(root.get("fechaEntrega")),
            confirmation ? cb.isNotNull(root.get("fechaConfirmacion")) : cb.and(
                cb.isNull(root.get("fechaConfirmacion")),
                cb.greaterThanOrEqualTo(root.get("fechaHoraRecoleccion"), today.atStartOfDay()),
                cb.lessThan(root.get("fechaHoraRecoleccion"), today.plusDays(1).atStartOfDay())));
    }
}
