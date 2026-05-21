package com.sistemadeoperaciones.pagos.dto.retornos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class CreateReturnPaymentBatchRequestDto {

    @NotEmpty(message = "Debe capturarse al menos un pago de retorno")
    @Valid
    private List<CreateReturnPaymentRequestDto> pagos;

    public List<CreateReturnPaymentRequestDto> getPagos() {
        return pagos;
    }

    public void setPagos(List<CreateReturnPaymentRequestDto> pagos) {
        this.pagos = pagos;
    }
}