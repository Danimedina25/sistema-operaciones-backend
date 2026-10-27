package com.sistemadeoperaciones.pagos.service;

import com.sistemadeoperaciones.pagos.dto.retornos.ReturnPaymentResponseDto;
import com.sistemadeoperaciones.pagos.model.OperationReturnPayment;
import org.springframework.stereotype.Component;

/**
 * Mapeo canónico de una solicitud de retorno ({@link OperationReturnPayment}) a
 * su DTO, incluyendo los totales calculados a partir de las parcialidades.
 *
 * NO llena {@code parcialidades} — eso lo hace el llamador (para evitar una
 * dependencia circular con {@code ReturnInstallmentService}). Lo usan
 * {@code ReturnsOperationServiceImpl} (listado/detalle) y
 * {@code ReturnInstallmentServiceImpl} (resumen de una solicitud), para que
 * ambas rutas devuelvan exactamente los mismos campos.
 */
@Component
public class ReturnPaymentDtoMapper {

    private final ReturnRequestTotalsCalculator totalsCalculator;
    private final com.sistemadeoperaciones.pagos.repository.OperationReturnInstallmentRepository installments;

    public ReturnPaymentDtoMapper(ReturnRequestTotalsCalculator totalsCalculator, com.sistemadeoperaciones.pagos.repository.OperationReturnInstallmentRepository installments) {
        this.installments = installments;
        this.totalsCalculator = totalsCalculator;
    }

    public ReturnPaymentResponseDto toDto(OperationReturnPayment returnPayment) {
        ReturnPaymentResponseDto dto = new ReturnPaymentResponseDto();

        dto.setId(returnPayment.getId());
        dto.setOperationId(returnPayment.getOperacion().getId());

        if (returnPayment.getOperacion().getCliente() != null) {
            dto.setClientId(returnPayment.getOperacion().getCliente().getId());
            dto.setClienteNombre(returnPayment.getOperacion().getCliente().getNombre());
        }

        if (returnPayment.getOperacion().getSocioComercial() != null) {
            dto.setSocioComercialNombre(returnPayment.getOperacion().getSocioComercial().getNombre());
            dto.setSocioComercialTelefono(returnPayment.getOperacion().getSocioComercial().getTelefono());
        }

        dto.setMonto(returnPayment.getMonto());
        dto.setTipoPago(returnPayment.getTipoPago());
        dto.setEstatus(returnPayment.getEstatus());
        dto.setCuentaAsignadaEn(returnPayment.getCuentaAsignadaEn());

        dto.setCuentaDestinoCliente(returnPayment.getCuentaDestinoCliente());
        dto.setCuentaClabeCliente(returnPayment.getCuentaClabeCliente());
        dto.setCuentaDestinoTitular(returnPayment.getCuentaDestinoTitular());
        dto.setCuentaDestinoBanco(returnPayment.getCuentaDestinoBanco());

        dto.setComprobanteUrl(returnPayment.getComprobanteUrl());
        dto.setComprobanteEntregaEfectivoUrl(returnPayment.getComprobanteEntregaEfectivoUrl());
        dto.setArchivoNominaUrl(returnPayment.getArchivoNominaUrl());
        dto.setObservaciones(returnPayment.getObservaciones());

        dto.setFechaSolicitud(returnPayment.getFechaSolicitud());
        dto.setFechaPago(returnPayment.getFechaPago());
        dto.setFechaEntrega(returnPayment.getFechaEntrega());
        dto.setFechaConfirmacionRecoleccion(returnPayment.getFechaConfirmacionRecoleccion());
        dto.setCreatedAt(returnPayment.getCreatedAt());

        dto.setAutorizadoParaRecibirEfectivo1(returnPayment.getAutorizadoParaRecibirEfectivo1());
        dto.setAutorizadoParaRecibirEfectivo2(returnPayment.getAutorizadoParaRecibirEfectivo2());
        dto.setAutorizadoParaRecibirEfectivo3(returnPayment.getAutorizadoParaRecibirEfectivo3());
        dto.setFechaHoraRecoleccionEfectivo(returnPayment.getFechaHoraRecoleccionEfectivo());
        dto.setCodigoRetiroSinTarjeta(returnPayment.getCodigoRetiroSinTarjeta());

        if (returnPayment.getCuentaOrigen() != null) {
            dto.setCuentaOrigenId(returnPayment.getCuentaOrigen().getId());
            dto.setCuentaOrigenNombre(returnPayment.getCuentaOrigen().getBanco());
            dto.setCuentaOrigenTitular(returnPayment.getCuentaOrigen().getTitular());
            String numero = returnPayment.getCuentaOrigen().getNumeroCuenta();
            dto.setCuentaOrigenNumero(numero == null ? null : numero.substring(Math.max(0, numero.length() - 4)));
        }

        if (returnPayment.getSolicitadoPor() != null) {
            dto.setSolicitadoPorId(returnPayment.getSolicitadoPor().getId());
            dto.setSolicitadoPorNombre(returnPayment.getSolicitadoPor().getNombre());
        }

        if (returnPayment.getPagadoPor() != null) {
            dto.setPagadoPorId(returnPayment.getPagadoPor().getId());
            dto.setPagadoPorNombre(returnPayment.getPagadoPor().getNombre());
        }

        if (returnPayment.getEntregadoPor() != null) {
            dto.setEntregadoPorId(returnPayment.getEntregadoPor().getId());
            dto.setEntregadoPorNombre(returnPayment.getEntregadoPor().getNombre());
        }

        // Totales calculados a partir de las parcialidades (backend = fuente de verdad).
        totalsCalculator.apply(dto, returnPayment);
        if (returnPayment.getTipoPago() == com.sistemadeoperaciones.pagos.enums.PaymentType.RETIRO_SIN_TARJETA) {
            boolean withoutInstallments = dto.getNumeroParcialidades() == 0;
            dto.setRetiroEtapa(withoutInstallments
                    ? "PENDIENTE_CODIGO"
                    : returnPayment.getEstatus().name());
        }

        if (returnPayment.getTipoPago() == com.sistemadeoperaciones.pagos.enums.PaymentType.RETIRO_SIN_TARJETA && dto.getNumeroParcialidades() > 0 && installments.existsBySolicitudIdAndCodigoParaClienteTrue(returnPayment.getId()))
            dto.setRetiroEtapa(returnPayment.getEstatus() == com.sistemadeoperaciones.pagos.enums.ReturnPaymentStatus.RETORNADO ? "CODIGOS_CONCLUIDOS" : "CODIGOS_EN_SEGUIMIENTO");
        return dto;
    }
}
