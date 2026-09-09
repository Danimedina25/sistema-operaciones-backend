package com.sistemadeoperaciones.pagos.service;

import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.shared.enums.RoleName;
import com.sistemadeoperaciones.usuarios.model.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;

public final class StaffPendingScope {
    private StaffPendingScope() {}
    public static final List<PaymentType> CASH = List.of(PaymentType.EFECTIVO, PaymentType.RETIRO_SIN_TARJETA);
    public static final List<PaymentType> BANK = List.of(PaymentType.TRANSFERENCIA, PaymentType.DEPOSITO, PaymentType.CHEQUE);
    public static boolean has(User user, RoleName role) {
        return user.getRoles().stream().anyMatch(r -> r.getName() == role);
    }
    public static List<PaymentType> types(User user, String queue, boolean income) {
        if (queue == null || queue.isBlank()) return null;
        String suffix = income ? "_INCOME" : "_RETURNS";
        if (!queue.equals("CASH" + suffix) && !queue.equals("BANK" + suffix))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cola de pendientes inválida");
        boolean cash = queue.startsWith("CASH");
        boolean allowed = cash ? has(user, RoleName.JEFA_CAJAS)
                : has(user, RoleName.JEFA_CUENTAS) || has(user, RoleName.AUXILIAR_CUENTAS);
        if (!allowed) throw new AccessDeniedException("El perfil no puede gestionar esta cola");
        return cash ? (income ? List.of(PaymentType.EFECTIVO) : CASH) : BANK;
    }
    public static List<PaymentType> returnTypes(User user, String queue) {
        if (queue != null && !queue.isBlank()) return types(user, queue, false);
        if (has(user, RoleName.ADMIN) || has(user, RoleName.GERENTE) || has(user, RoleName.DIRECCION)) return null;
        List<PaymentType> result = new ArrayList<>();
        if (has(user, RoleName.JEFA_CAJAS)) result.addAll(CASH);
        if (has(user, RoleName.JEFA_CUENTAS) || has(user, RoleName.AUXILIAR_CUENTAS)) result.addAll(BANK);
        return result.isEmpty() ? null : result;
    }
}
