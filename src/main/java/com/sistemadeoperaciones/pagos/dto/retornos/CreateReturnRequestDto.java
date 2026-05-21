package com.sistemadeoperaciones.pagos.dto.retornos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CreateReturnRequestDto {

    @NotNull(message = "La lista de pagos es obligatoria")
    @Size(min = 1, message = "Debe capturarse al menos un pago de retorno")
    private List<CreateReturnPaymentRequestDto> pagos;

    public List<CreateReturnPaymentRequestDto> getPagos() {
        return pagos;
    }

    public void setPagos(List<CreateReturnPaymentRequestDto> pagos) {
        this.pagos = pagos;
    }
}