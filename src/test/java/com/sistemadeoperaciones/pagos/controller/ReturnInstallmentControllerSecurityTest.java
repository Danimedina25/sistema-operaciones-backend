package com.sistemadeoperaciones.pagos.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guarda de regresión sobre los permisos del cierre de una parcialidad de
 * retorno: {@code PATCH /installments/{id}/deliver} lo cierran exactamente
 * ADMIN, GERENTE, DIRECCION y JEFA_CAJAS. La funcionalidad de "persona que
 * recibió" NO amplía esos permisos.
 */
class ReturnInstallmentControllerSecurityTest {

    @Test
    void deliverEndpointKeepsItsRoles() throws NoSuchMethodException {
        Method deliver = ReturnInstallmentController.class.getDeclaredMethod(
                "deliverInstallment",
                Long.class,
                com.sistemadeoperaciones.pagos.dto.retornos.DeliverReturnInstallmentRequestDto.class
        );

        PreAuthorize preAuthorize = deliver.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value())
                .isEqualTo("hasAnyRole('ADMIN', 'GERENTE', 'DIRECCION', 'JEFA_CAJAS')");
    }
}
