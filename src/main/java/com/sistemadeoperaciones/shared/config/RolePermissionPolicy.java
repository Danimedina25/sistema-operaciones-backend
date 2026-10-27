package com.sistemadeoperaciones.shared.config;

import com.sistemadeoperaciones.shared.enums.RoleName;
import com.sistemadeoperaciones.usuarios.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Business permissions used by method security. ADMIN is a technical
 * superuser; every other role must be listed for the action.
 */
@Component("rolePermissionPolicy")
public class RolePermissionPolicy {
    public enum Action {
        USER_CREATE,
        USER_EDIT,
        USER_DELETE,
        BANK_ACCOUNT_CREATE,
        BANK_ACCOUNT_EDIT,
        CATALOG_CREATE,
        CATALOG_EDIT,
        CATALOG_STATUS,
        CATALOG_DELETE,
        CATALOG_COMMISSION_CONFIGURE,
        OPERATION_CREATE,
        OPERATION_EDIT,
        OPERATION_STATUS,
        OPERATION_DELETE,
        OPERATION_INVOICE,
        PAYMENT_REGISTER,
        PAYMENT_VALIDATE,
        RETURN_REQUEST,
        RETURN_INSTALLMENT_MANAGE,
        RETURN_BANK_PAY,
        RETURN_CASH_DELIVER,
        RETURN_CONFIRM_RECEIPT,
        COMMISSION_GENERATE
    }

    private static final Map<Action, Set<RoleName>> ALLOWED = Map.ofEntries(
            Map.entry(Action.USER_CREATE, EnumSet.of(RoleName.DIRECCION)),
            Map.entry(Action.USER_EDIT, EnumSet.of(RoleName.GERENTE, RoleName.DIRECCION)),
            Map.entry(Action.USER_DELETE, EnumSet.of(RoleName.DIRECCION)),
            Map.entry(Action.BANK_ACCOUNT_CREATE, EnumSet.of(RoleName.GERENTE, RoleName.JEFA_CUENTAS)),
            Map.entry(Action.BANK_ACCOUNT_EDIT, EnumSet.of(RoleName.GERENTE)),
            Map.entry(Action.CATALOG_CREATE, EnumSet.of(RoleName.SOCIO_COMERCIAL)),
            Map.entry(Action.CATALOG_EDIT, EnumSet.of(RoleName.GERENTE, RoleName.SOCIO_COMERCIAL)),
            Map.entry(Action.CATALOG_COMMISSION_CONFIGURE, EnumSet.of(RoleName.GERENTE)),
            Map.entry(Action.CATALOG_STATUS, EnumSet.of(RoleName.GERENTE, RoleName.SOCIO_COMERCIAL)),
            Map.entry(Action.CATALOG_DELETE, EnumSet.noneOf(RoleName.class)),
            Map.entry(Action.OPERATION_CREATE, EnumSet.of(RoleName.SOCIO_COMERCIAL)),
            Map.entry(Action.OPERATION_EDIT, EnumSet.of(RoleName.SOCIO_COMERCIAL)),
            Map.entry(Action.OPERATION_STATUS, EnumSet.noneOf(RoleName.class)),
            Map.entry(Action.OPERATION_DELETE, EnumSet.of(RoleName.GERENTE)),
            Map.entry(Action.OPERATION_INVOICE, EnumSet.of(RoleName.GERENTE)),
            Map.entry(Action.PAYMENT_REGISTER, EnumSet.of(RoleName.SOCIO_COMERCIAL)),
            Map.entry(Action.PAYMENT_VALIDATE, EnumSet.of(RoleName.JEFA_CAJAS, RoleName.JEFA_CUENTAS, RoleName.AUXILIAR_CUENTAS)),
            Map.entry(Action.RETURN_REQUEST, EnumSet.of(RoleName.SOCIO_COMERCIAL)),
            Map.entry(Action.RETURN_INSTALLMENT_MANAGE, EnumSet.of(RoleName.JEFA_CAJAS, RoleName.JEFA_CUENTAS, RoleName.AUXILIAR_CUENTAS)),
            Map.entry(Action.RETURN_BANK_PAY, EnumSet.of(RoleName.JEFA_CUENTAS, RoleName.AUXILIAR_CUENTAS)),
            Map.entry(Action.RETURN_CASH_DELIVER, EnumSet.of(RoleName.JEFA_CAJAS)),
            Map.entry(Action.RETURN_CONFIRM_RECEIPT, EnumSet.of(RoleName.SOCIO_COMERCIAL)),
            Map.entry(Action.COMMISSION_GENERATE, EnumSet.of(RoleName.GERENTE))
    );

    public boolean allows(Authentication authentication, String actionName) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        final Action action;
        try {
            action = Action.valueOf(actionName);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return false;
        }
        Set<RoleName> allowed = ALLOWED.get(action);
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .anyMatch(role -> role.equals(RoleName.ADMIN.name()) || allowed.stream().anyMatch(value -> value.name().equals(role)));
    }

    public boolean allows(User user, Action action) {
        return user != null && user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ADMIN || ALLOWED.get(action).contains(role.getName()));
    }
}
