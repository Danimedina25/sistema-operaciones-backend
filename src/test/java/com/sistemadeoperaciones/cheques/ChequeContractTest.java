package com.sistemadeoperaciones.cheques;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistemadeoperaciones.pagos.dto.*;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import static org.assertj.core.api.Assertions.*;
class ChequeContractTest {
    @Test void everyControllerEndpointIsProtected() {
        String expression=ChequeController.class.getAnnotation(PreAuthorize.class).value();
        assertThat(expression).contains("ADMIN","JEFA_CAJAS","JEFA_CUENTAS","AUXILIAR_CUENTAS","GERENTE","DIRECCION").doesNotContain("SOCIO_COMERCIAL");
    }
    @Test void captureCannotAcceptCollectionFields() {
        var mapper=new ObjectMapper().findAndRegisterModules();
        assertThatThrownBy(() -> mapper.readValue("{\"chequeEstado\":\"COBRADO\"}",CreateOperationPaymentRequestDto.class)).isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);
    }
    @Test void bankPaymentsStillRequireAnAccountAndChequesDoNot() {
        var r=new CreateOperationPaymentRequestDto();
        r.setTipoPago(PaymentType.CHEQUE); assertThat(r.isCuentaDestinoValid()).isTrue();
        r.setCuentaDestinoId(1L); assertThat(r.isCuentaDestinoValid()).isFalse();
        r.setCuentaDestinoId(null); r.setTipoPago(PaymentType.TRANSFERENCIA); assertThat(r.isCuentaDestinoValid()).isFalse();
        r.setTipoPago(PaymentType.DEPOSITO); assertThat(r.isCuentaDestinoValid()).isFalse();
    }
    @Test void emptyStateFilterMeansAllNotDefaultPending() {
        var service=org.mockito.Mockito.mock(ChequeService.class);
        new ChequeController(service).list("","","",null,"",null,null,0);
        org.mockito.Mockito.verify(service).list("","","",null,"",null,null,0);
    }
}
