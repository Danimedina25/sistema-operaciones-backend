package com.sistemadeoperaciones.pagos.service;

import com.sistemadeoperaciones.shared.exception.BusinessException;
import com.sistemadeoperaciones.socioscomerciales.models.CommercialPartner;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

class CommercialNetworkCommissionGuardTest {
    private final PaymentOperationServiceImpl service = mock(PaymentOperationServiceImpl.class, CALLS_REAL_METHODS);

    @Test void pendingCommissionCannotEnterNewOperation() {
        var level2 = partner(2L, false);
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "requireConfiguredNetwork", level2, null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Gerencia");
    }

    @Test void configuredPartnersCanOccupyEitherLevelWithoutParentRelation() {
        var level2 = partner(2L, true);
        var level3 = partner(3L, true);
        assertThatCode(() -> ReflectionTestUtils.invokeMethod(service, "requireConfiguredNetwork", level2, level3)).doesNotThrowAnyException();
        assertThatCode(() -> ReflectionTestUtils.invokeMethod(service, "requireConfiguredNetwork", level3, level2)).doesNotThrowAnyException();
    }

    private static CommercialPartner partner(long id, boolean configured) {
        var partner = new CommercialPartner();
        partner.setId(id); partner.setCommissionConfigured(configured);
        return partner;
    }
}
