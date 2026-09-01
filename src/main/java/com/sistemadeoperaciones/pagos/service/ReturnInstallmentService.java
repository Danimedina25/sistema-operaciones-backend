package com.sistemadeoperaciones.pagos.service;

import com.sistemadeoperaciones.pagos.dto.retornos.CancelReturnInstallmentRequestDto;
import com.sistemadeoperaciones.pagos.dto.retornos.CreateReturnInstallmentRequestDto;
import com.sistemadeoperaciones.pagos.dto.retornos.DeliverReturnInstallmentRequestDto;
import com.sistemadeoperaciones.pagos.dto.retornos.ReturnInstallmentResponseDto;
import com.sistemadeoperaciones.pagos.dto.retornos.ReturnRequestSummaryDto;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Administración de parcialidades de una solicitud de retorno.
 *
 * Una parcialidad es un movimiento asociado a una solicitud concreta (por su id).
 * No reemplaza ni edita la solicitud original. El backend es la fuente de verdad
 * de montos, % de avance y estatus (de solicitud y de operación) y garantiza
 * — con bloqueo pesimista sobre operación y solicitud — que la suma de
 * parcialidades no supere el monto solicitado, incluso ante peticiones
 * concurrentes.
 */
public interface ReturnInstallmentService {

    ReturnInstallmentResponseDto createInstallment(
            Long returnRequestId,
            CreateReturnInstallmentRequestDto request
    );

    /** Socio comercial dueño: confirma la recepción del efectivo (PROGRAMADA → ENTREGADA). */
    ReturnInstallmentResponseDto confirmInstallment(Long installmentId);

    /** Jefa de cajas: cierra la entrega del efectivo (ENTREGADA → COMPLETADA) con evidencia. */
    ReturnInstallmentResponseDto deliverInstallment(
            Long installmentId,
            DeliverReturnInstallmentRequestDto request
    );

    /** Cancela una parcialidad no completada (PROGRAMADA/ENTREGADA → CANCELADA). */
    ReturnInstallmentResponseDto cancelInstallment(
            Long installmentId,
            CancelReturnInstallmentRequestDto request
    );

    List<ReturnInstallmentResponseDto> findInstallmentsByRequest(Long returnRequestId);

    ReturnRequestSummaryDto getRequestSummary(Long returnRequestId);

    Page<ReturnInstallmentResponseDto> findTodayPickups(
            LocalDate fecha,
            List<PaymentType> tipos,
            Pageable pageable
    );

    Page<ReturnInstallmentResponseDto> findLatePickups(Pageable pageable);

    // ------------------------------------------------------------------
    // Delegación de los endpoints legacy (a nivel solicitud). Operan sobre
    // "una parcialidad implícita" por el saldo pendiente completo.
    // ------------------------------------------------------------------

    ReturnInstallmentResponseDto legacyRealize(
            Long returnRequestId,
            Long cuentaOrigenId,
            String comprobanteUrl,
            String observaciones
    );

    ReturnInstallmentResponseDto legacySchedulePickup(
            Long returnRequestId,
            LocalDateTime fechaHoraRecoleccion,
            Long cuentaOrigenId,
            String codigoRetiroSinTarjeta,
            String observaciones
    );

    ReturnInstallmentResponseDto legacyConfirmPickup(Long returnRequestId);

    ReturnInstallmentResponseDto legacyMarkDelivered(
            Long returnRequestId,
            String comprobanteEntregaUrl,
            String personaQueRecibioEfectivo
    );
}
