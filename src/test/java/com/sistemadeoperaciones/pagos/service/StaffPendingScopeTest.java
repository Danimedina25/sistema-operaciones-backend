package com.sistemadeoperaciones.pagos.service;
import com.sistemadeoperaciones.shared.enums.RoleName;
import com.sistemadeoperaciones.usuarios.model.*;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class StaffPendingScopeTest {
    User user(RoleName... names) { var user=new User(); Set<Role> roles=new HashSet<>(); for(var name:names) { var role=new Role(); role.setName(name); roles.add(role); } user.setRoles(roles); return user; }
    @Test void rolesCannotEnterOtherQueues() {
        assertThatThrownBy(() -> StaffPendingScope.types(user(RoleName.JEFA_CAJAS),"BANK_INCOME",true)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> StaffPendingScope.types(user(RoleName.SOCIO_COMERCIAL),"CASH_INCOME",true)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> StaffPendingScope.types(user(RoleName.AUXILIAR_CUENTAS),"CASH_RETURNS",false)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void accountsRolesHaveSameScopeAndMultiRoleUnionsWithoutDuplicates() {
        assertThat(StaffPendingScope.types(user(RoleName.JEFA_CUENTAS),"BANK_RETURNS",false)).isEqualTo(StaffPendingScope.types(user(RoleName.AUXILIAR_CUENTAS),"BANK_RETURNS",false));
        assertThat(StaffPendingScope.returnTypes(user(RoleName.JEFA_CAJAS,RoleName.JEFA_CUENTAS,RoleName.AUXILIAR_CUENTAS),null))
            .containsExactlyInAnyOrder(PaymentType.EFECTIVO,PaymentType.RETIRO_SIN_TARJETA,PaymentType.TRANSFERENCIA,PaymentType.DEPOSITO,PaymentType.CHEQUE);
    }
}
