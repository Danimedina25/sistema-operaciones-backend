package com.sistemadeoperaciones.pagos.service;

import com.sistemadeoperaciones.pagos.dto.PaymentOperationFilterDto;
import com.sistemadeoperaciones.pagos.dto.PaymentOperationResponseDto;
import com.sistemadeoperaciones.pagos.dto.retornos.*;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ReturnsOperationService {

    /**
     * SOCIO_COMERCIAL:
     * Solicita cómo quiere recibir el retorno.
     */
    List<ReturnPaymentResponseDto> requestReturnPayment(
            Long operationId,
            CreateReturnPaymentBatchRequestDto request
    );

    ReturnPaymentResponseDto updateRequestReturnPayment(
            Long operationId,
            CreateReturnPaymentRequestDto request
    );

    /**
     * JEFA_CAJAS / GERENTE / ADMIN:
     * Marca el retorno como realizado.
     */
    ReturnPaymentResponseDto realizeReturnPayment(
            Long returnPaymentId,
            RealizeReturnPaymentRequestDto request
    );

    /**
     * Historial de retornos de una operación.
     */
    List<ReturnPaymentResponseDto> findReturnsByOperationId(Long operationId);

    /**
     * Detalle general de la operación.
     */
    PaymentOperationResponseDto findReturnDetailByOperationId(Long operationId);

    /**
     * Para SOCIO_COMERCIAL.
     * Operaciones donde el socio puede solicitar retornos.
     */
    Page<PaymentOperationResponseDto> findOperationsAvailableToRequestReturn(
            PaymentOperationFilterDto filter,
            Pageable pageable
    );

    /**
     * Para JEFA_CAJAS / ADMIN / GERENTE.
     * Operaciones con retornos pendientes por realizar.
     */
    Page<PaymentOperationResponseDto> findOperationsWithRequestedReturns(
            PaymentOperationFilterDto filter,
            Pageable pageable
    );

    List<ReturnDestinationAccountSuggestionDto> findReturnDestinationSuggestionsByClienteId(
            Long clienteId
    );

    ReturnPaymentResponseDto scheduleCashReturnPickup(
            Long returnPaymentId,
            ScheduleCashReturnPickupRequestDto request
    );

    /**
     * JEFA_CAJAS/ADMIN: marca que el efectivo/retiro sin tarjeta ya fue
     * entregado al cliente, habilitando que el socio comercial confirme.
     */
    ReturnPaymentResponseDto markCashReturnAsDelivered(
            Long returnPaymentId,
            MarkCashReturnDeliveredRequestDto request
    );

    /**
     * SOCIO_COMERCIAL (dueño de la operación):
     * Confirma que recibió el retorno en efectivo/retiro sin tarjeta
     * previamente marcado como entregado por jefa de cajas.
     */
    ReturnPaymentResponseDto confirmCashReturnPickup(Long returnPaymentId);

    /**
     * JEFA_CAJAS / ADMIN / GERENTE / DIRECCION:
     * Lista plana (no por operación) de retornos en efectivo/retiro sin
     * tarjeta cuya fecha de recolección programada cae dentro de `fecha`
     * (hoy si es null). `tipos` filtra por tipo de retorno; si es null o
     * vacío usa EFECTIVO y RETIRO_SIN_TARJETA.
     */
    Page<ReturnPaymentResponseDto> findTodayCashDeliveries(
            LocalDate fecha,
            List<PaymentType> tipos,
            Pageable pageable
    );

    /**
     * ADMIN/GERENTE/DIRECCION/JEFA_CAJAS:
     * Retornos en efectivo/retiro sin tarjeta en estatus EN_RECOLECCION
     * cuya fecha de recolección programada ya pasó, sin importar el día
     * (a diferencia de `findTodayCashDeliveries`, que se acota a hoy).
     */
    Page<ReturnPaymentResponseDto> findLateReturns(Pageable pageable);
}
