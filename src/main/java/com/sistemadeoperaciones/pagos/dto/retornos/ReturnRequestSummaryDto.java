package com.sistemadeoperaciones.pagos.dto.retornos;

import java.util.List;

/**
 * Vista de una solicitud de retorno con sus totales calculados en el servidor y
 * el historial de parcialidades. El frontend solo consume estos valores para
 * presentación; el backend es la fuente de verdad.
 */
public class ReturnRequestSummaryDto {

    private ReturnPaymentResponseDto solicitud;

    private List<ReturnInstallmentResponseDto> parcialidades;

    public ReturnRequestSummaryDto() {
    }

    public ReturnRequestSummaryDto(
            ReturnPaymentResponseDto solicitud,
            List<ReturnInstallmentResponseDto> parcialidades
    ) {
        this.solicitud = solicitud;
        this.parcialidades = parcialidades;
    }

    public ReturnPaymentResponseDto getSolicitud() {
        return solicitud;
    }

    public void setSolicitud(ReturnPaymentResponseDto solicitud) {
        this.solicitud = solicitud;
    }

    public List<ReturnInstallmentResponseDto> getParcialidades() {
        return parcialidades;
    }

    public void setParcialidades(List<ReturnInstallmentResponseDto> parcialidades) {
        this.parcialidades = parcialidades;
    }
}
