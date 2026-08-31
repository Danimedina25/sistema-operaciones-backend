package com.sistemadeoperaciones.pagos.service;

import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.cuentasbancarias.repository.BankAccountRepository;
import com.sistemadeoperaciones.notifications.enums.NotificationModule;
import com.sistemadeoperaciones.notifications.enums.NotificationPriority;
import com.sistemadeoperaciones.notifications.enums.NotificationReferenceType;
import com.sistemadeoperaciones.notifications.enums.NotificationType;
import com.sistemadeoperaciones.notifications.service.NotificationService;
import com.sistemadeoperaciones.pagos.dto.retornos.CancelReturnInstallmentRequestDto;
import com.sistemadeoperaciones.pagos.dto.retornos.CreateReturnInstallmentRequestDto;
import com.sistemadeoperaciones.pagos.dto.retornos.DeliverReturnInstallmentRequestDto;
import com.sistemadeoperaciones.pagos.dto.retornos.ReturnInstallmentResponseDto;
import com.sistemadeoperaciones.pagos.dto.retornos.ReturnPaymentResponseDto;
import com.sistemadeoperaciones.pagos.dto.retornos.ReturnRequestSummaryDto;
import com.sistemadeoperaciones.pagos.enums.OperationStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.enums.ReturnInstallmentStatus;
import com.sistemadeoperaciones.pagos.enums.ReturnPaymentStatus;
import com.sistemadeoperaciones.pagos.exceptions.InvalidReturnAmountException;
import com.sistemadeoperaciones.pagos.exceptions.ReturnInstallmentAmountExceedsAvailableException;
import com.sistemadeoperaciones.pagos.exceptions.ReturnInstallmentInvalidStatusException;
import com.sistemadeoperaciones.pagos.exceptions.ReturnInstallmentNotCancellableException;
import com.sistemadeoperaciones.pagos.exceptions.ReturnInstallmentNotFoundException;
import com.sistemadeoperaciones.pagos.exceptions.ReturnInstallmentOriginAccountRequiredException;
import com.sistemadeoperaciones.pagos.exceptions.ReturnInstallmentPickupDateRequiredException;
import com.sistemadeoperaciones.pagos.exceptions.ReturnInstallmentReceiptRequiredException;
import com.sistemadeoperaciones.pagos.exceptions.ReturnInstallmentWithdrawalCodeRequiredException;
import com.sistemadeoperaciones.pagos.exceptions.ReturnPaymentNotFoundException;
import com.sistemadeoperaciones.pagos.exceptions.ReturnRequestAlreadyFullyReturnedException;
import com.sistemadeoperaciones.pagos.model.OperationReturnInstallment;
import com.sistemadeoperaciones.pagos.model.OperationReturnPayment;
import com.sistemadeoperaciones.pagos.model.PaymentOperation;
import com.sistemadeoperaciones.pagos.repository.OperationReturnInstallmentRepository;
import com.sistemadeoperaciones.pagos.repository.OperationReturnPaymentRepository;
import com.sistemadeoperaciones.pagos.repository.PaymentOperationRepository;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.usuarios.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReturnInstallmentServiceImpl implements ReturnInstallmentService {

    private static final List<PaymentType> CASH_TYPES =
            List.of(PaymentType.EFECTIVO, PaymentType.RETIRO_SIN_TARJETA);

    private final PaymentOperationRepository paymentOperationRepository;
    private final OperationReturnPaymentRepository returnPaymentRepository;
    private final OperationReturnInstallmentRepository installmentRepository;
    private final BankAccountRepository bankAccountRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final NotificationService notificationService;
    private final ReturnAmountCalculator returnAmountCalculator;
    private final ReturnPaymentDtoMapper returnPaymentDtoMapper;

    public ReturnInstallmentServiceImpl(
            PaymentOperationRepository paymentOperationRepository,
            OperationReturnPaymentRepository returnPaymentRepository,
            OperationReturnInstallmentRepository installmentRepository,
            BankAccountRepository bankAccountRepository,
            AuthenticatedUserService authenticatedUserService,
            NotificationService notificationService,
            ReturnAmountCalculator returnAmountCalculator,
            ReturnPaymentDtoMapper returnPaymentDtoMapper
    ) {
        this.paymentOperationRepository = paymentOperationRepository;
        this.returnPaymentRepository = returnPaymentRepository;
        this.installmentRepository = installmentRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.notificationService = notificationService;
        this.returnAmountCalculator = returnAmountCalculator;
        this.returnPaymentDtoMapper = returnPaymentDtoMapper;
    }

    // ==================================================================
    // Crear parcialidad
    // ==================================================================

    @Override
    @Transactional
    public ReturnInstallmentResponseDto createInstallment(
            Long returnRequestId,
            CreateReturnInstallmentRequestDto request
    ) {
        User currentUser = authenticatedUserService.getCurrentUser();

        OperationReturnPayment solicitud = lockRequestAndOperation(returnRequestId);
        PaymentOperation operation = solicitud.getOperacion();

        if (request.getMonto() == null
                || request.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidReturnAmountException();
        }

        BigDecimal monto = request.getMonto().setScale(2, RoundingMode.HALF_UP);
        BigDecimal montoSolicitado = scaled(solicitud.getMonto());
        BigDecimal completado = scaled(installmentRepository.sumCompletedBySolicitud(returnRequestId));

        if (completado.compareTo(montoSolicitado) >= 0) {
            throw new ReturnRequestAlreadyFullyReturnedException();
        }

        BigDecimal enProceso = scaled(installmentRepository.sumInFlightBySolicitud(returnRequestId));
        BigDecimal disponible = montoSolicitado.subtract(completado).subtract(enProceso);

        if (monto.compareTo(disponible) > 0) {
            throw new ReturnInstallmentAmountExceedsAvailableException();
        }

        OperationReturnInstallment installment = new OperationReturnInstallment();
        installment.setSolicitud(solicitud);
        installment.setMonto(monto);
        installment.setTipoPago(solicitud.getTipoPago());
        installment.setCreadoPor(currentUser);
        installment.setObservaciones(trimToNull(request.getObservaciones()));

        LocalDateTime now = LocalDateTime.now();

        if (isCash(solicitud.getTipoPago())) {
            if (request.getFechaHoraRecoleccion() == null) {
                throw new ReturnInstallmentPickupDateRequiredException();
            }
            installment.setFechaHoraRecoleccion(request.getFechaHoraRecoleccion());
            installment.setEstatus(ReturnInstallmentStatus.PROGRAMADA);

            if (solicitud.getTipoPago() == PaymentType.RETIRO_SIN_TARJETA) {
                installment.setCuentaOrigen(requireBankAccount(request.getCuentaOrigenId()));
                if (isBlank(request.getCodigoRetiroSinTarjeta())) {
                    throw new ReturnInstallmentWithdrawalCodeRequiredException();
                }
                installment.setCodigoRetiroSinTarjeta(request.getCodigoRetiroSinTarjeta().trim());
            }
        } else {
            if (isBlank(request.getComprobanteUrl())) {
                throw new ReturnInstallmentReceiptRequiredException();
            }
            installment.setComprobanteUrl(request.getComprobanteUrl().trim());

            if (solicitud.getTipoPago() == PaymentType.TRANSFERENCIA) {
                installment.setCuentaOrigen(requireBankAccount(request.getCuentaOrigenId()));
            } else if (request.getCuentaOrigenId() != null) {
                installment.setCuentaOrigen(requireBankAccount(request.getCuentaOrigenId()));
            }

            installment.setEstatus(ReturnInstallmentStatus.COMPLETADA);
            installment.setRealizadoPor(currentUser);
            installment.setFechaRealizacion(now);
        }

        OperationReturnInstallment saved = installmentRepository.save(installment);

        recomputeRequestStatus(solicitud);
        recomputeOperationStatus(operation);

        dispatchNotificationsAfterChange(saved, solicitud);

        return mapToResponse(saved);
    }

    // ==================================================================
    // Transiciones
    // ==================================================================

    @Override
    @Transactional
    public ReturnInstallmentResponseDto confirmInstallment(Long installmentId) {
        OperationReturnInstallment installment = loadWithLocks(installmentId);
        OperationReturnPayment solicitud = installment.getSolicitud();
        PaymentOperation operation = solicitud.getOperacion();

        requireCashType(installment);
        if (installment.getEstatus() != ReturnInstallmentStatus.PROGRAMADA) {
            throw new ReturnInstallmentInvalidStatusException(
                    "Solo puede confirmarse una parcialidad programada"
            );
        }

        User currentUser = authenticatedUserService.getCurrentUser();
        boolean isOwner = operation.getSocioComercial() != null
                && operation.getSocioComercial().getId().equals(currentUser.getId());
        if (!isOwner) {
            throw new AccessDeniedException("No tienes permisos para confirmar esta parcialidad");
        }

        installment.setEstatus(ReturnInstallmentStatus.ENTREGADA);
        installment.setFechaConfirmacion(LocalDateTime.now());

        OperationReturnInstallment saved = installmentRepository.save(installment);
        recomputeRequestStatus(solicitud);
        recomputeOperationStatus(operation);

        notifyRoles(
                java.util.List.of(
                        com.sistemadeoperaciones.shared.enums.RoleName.JEFA_CAJAS,
                        com.sistemadeoperaciones.shared.enums.RoleName.ADMIN
                ),
                "El socio confirmó la recolección de una parcialidad",
                "El socio comercial confirmó haber recibido la parcialidad por $"
                        + saved.getMonto() + " de la operación #" + operation.getId()
                        + ". Puedes cerrarla desde Entregas de hoy.",
                NotificationType.RETURN_INSTALLMENT_DELIVERED,
                saved.getId(),
                "/entregas-de-hoy"
        );

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ReturnInstallmentResponseDto deliverInstallment(
            Long installmentId,
            DeliverReturnInstallmentRequestDto request
    ) {
        OperationReturnInstallment installment = loadWithLocks(installmentId);
        OperationReturnPayment solicitud = installment.getSolicitud();
        PaymentOperation operation = solicitud.getOperacion();

        requireCashType(installment);
        if (installment.getEstatus() != ReturnInstallmentStatus.ENTREGADA) {
            throw new ReturnInstallmentInvalidStatusException(
                    "Solo puede cerrarse una parcialidad que el socio ya confirmó"
            );
        }

        User currentUser = authenticatedUserService.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        installment.setEstatus(ReturnInstallmentStatus.COMPLETADA);
        installment.setEntregadoPor(currentUser);
        installment.setFechaEntrega(now);
        installment.setFechaRealizacion(now);
        installment.setComprobanteEntregaUrl(request.getComprobanteEntregaUrl().trim());

        OperationReturnInstallment saved = installmentRepository.save(installment);
        recomputeRequestStatus(solicitud);
        recomputeOperationStatus(operation);

        dispatchNotificationsAfterChange(saved, solicitud);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ReturnInstallmentResponseDto cancelInstallment(
            Long installmentId,
            CancelReturnInstallmentRequestDto request
    ) {
        OperationReturnInstallment installment = loadWithLocks(installmentId);
        OperationReturnPayment solicitud = installment.getSolicitud();
        PaymentOperation operation = solicitud.getOperacion();

        if (installment.getEstatus() != ReturnInstallmentStatus.PROGRAMADA
                && installment.getEstatus() != ReturnInstallmentStatus.ENTREGADA) {
            throw new ReturnInstallmentNotCancellableException();
        }

        User currentUser = authenticatedUserService.getCurrentUser();

        installment.setEstatus(ReturnInstallmentStatus.CANCELADA);
        installment.setCanceladoPor(currentUser);
        installment.setFechaCancelacion(LocalDateTime.now());
        installment.setObservaciones(appendMotivo(installment.getObservaciones(), request.getMotivo()));

        OperationReturnInstallment saved = installmentRepository.save(installment);
        recomputeRequestStatus(solicitud);
        recomputeOperationStatus(operation);

        if (operation.getSocioComercial() != null) {
            notifyUser(
                    operation.getSocioComercial().getId(),
                    "Parcialidad de retorno cancelada",
                    "Se canceló una parcialidad por $" + saved.getMonto()
                            + " de la solicitud de retorno de la operación #" + operation.getId()
                            + ". Motivo: " + request.getMotivo(),
                    NotificationType.RETURN_INSTALLMENT_CANCELLED,
                    operation.getId(),
                    "/operaciones/" + operation.getId() + "?scrollToReturns=true"
            );
        }

        return mapToResponse(saved);
    }

    // ==================================================================
    // Consultas
    // ==================================================================

    @Override
    @Transactional(readOnly = true)
    public List<ReturnInstallmentResponseDto> findInstallmentsByRequest(Long returnRequestId) {
        return installmentRepository.findBySolicitudIdOrderByCreatedAtAsc(returnRequestId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnRequestSummaryDto getRequestSummary(Long returnRequestId) {
        OperationReturnPayment solicitud = returnPaymentRepository.findById(returnRequestId)
                .orElseThrow(ReturnPaymentNotFoundException::new);

        // Mapeo canónico: mismos campos que el listado (datos bancarios del
        // cliente, autorizados, nómina, etc.), no un subconjunto.
        ReturnPaymentResponseDto solicitudDto = returnPaymentDtoMapper.toDto(solicitud);

        List<ReturnInstallmentResponseDto> parcialidades = findInstallmentsByRequest(returnRequestId);
        solicitudDto.setParcialidades(parcialidades);

        return new ReturnRequestSummaryDto(solicitudDto, parcialidades);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReturnInstallmentResponseDto> findTodayPickups(
            LocalDate fecha,
            List<PaymentType> tipos,
            Pageable pageable
    ) {
        LocalDate target = fecha != null ? fecha : LocalDate.now();
        LocalDateTime inicio = target.atStartOfDay();
        LocalDateTime fin = target.atTime(23, 59, 59);

        List<PaymentType> tiposFiltro = (tipos != null && !tipos.isEmpty()) ? tipos : CASH_TYPES;

        List<OperationReturnInstallment> rows = installmentRepository.findPickupInstallments(
                tiposFiltro,
                List.of(ReturnInstallmentStatus.PROGRAMADA, ReturnInstallmentStatus.ENTREGADA),
                inicio,
                fin,
                pageable
        );

        return new PageImpl<>(rows.stream().map(this::mapToResponse).toList(), pageable, rows.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReturnInstallmentResponseDto> findLatePickups(Pageable pageable) {
        List<OperationReturnInstallment> rows = installmentRepository.findPickupInstallments(
                CASH_TYPES,
                List.of(ReturnInstallmentStatus.PROGRAMADA),
                null,
                LocalDateTime.now(),
                pageable
        );

        return new PageImpl<>(rows.stream().map(this::mapToResponse).toList(), pageable, rows.size());
    }

    // ==================================================================
    // Delegación legacy
    // ==================================================================

    @Override
    @Transactional
    public ReturnInstallmentResponseDto legacyRealize(
            Long returnRequestId,
            Long cuentaOrigenId,
            String comprobanteUrl,
            String observaciones
    ) {
        OperationReturnPayment solicitud = returnPaymentRepository.findById(returnRequestId)
                .orElseThrow(ReturnPaymentNotFoundException::new);

        CreateReturnInstallmentRequestDto req = new CreateReturnInstallmentRequestDto();
        req.setMonto(fullPending(solicitud));
        req.setCuentaOrigenId(cuentaOrigenId);
        req.setComprobanteUrl(comprobanteUrl);
        req.setObservaciones(observaciones);

        return createInstallment(returnRequestId, req);
    }

    @Override
    @Transactional
    public ReturnInstallmentResponseDto legacySchedulePickup(
            Long returnRequestId,
            LocalDateTime fechaHoraRecoleccion,
            Long cuentaOrigenId,
            String codigoRetiroSinTarjeta,
            String observaciones
    ) {
        OperationReturnPayment solicitud = lockRequestAndOperation(returnRequestId);

        // Reprogramación: si ya hay una parcialidad PROGRAMADA se actualiza en sitio
        // (semántica del endpoint legacy, que permitía re-agendar).
        OperationReturnInstallment existing = installmentRepository
                .findBySolicitudIdOrderByCreatedAtAsc(returnRequestId)
                .stream()
                .filter(i -> i.getEstatus() == ReturnInstallmentStatus.PROGRAMADA)
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setFechaHoraRecoleccion(fechaHoraRecoleccion);
            if (solicitud.getTipoPago() == PaymentType.RETIRO_SIN_TARJETA) {
                if (cuentaOrigenId != null) {
                    existing.setCuentaOrigen(requireBankAccount(cuentaOrigenId));
                }
                if (!isBlank(codigoRetiroSinTarjeta)) {
                    existing.setCodigoRetiroSinTarjeta(codigoRetiroSinTarjeta.trim());
                }
            }
            if (!isBlank(observaciones)) {
                existing.setObservaciones(trimToNull(observaciones));
            }
            OperationReturnInstallment saved = installmentRepository.save(existing);
            recomputeRequestStatus(solicitud);
            return mapToResponse(saved);
        }

        CreateReturnInstallmentRequestDto req = new CreateReturnInstallmentRequestDto();
        req.setMonto(fullPending(solicitud));
        req.setFechaHoraRecoleccion(fechaHoraRecoleccion);
        req.setCuentaOrigenId(cuentaOrigenId);
        req.setCodigoRetiroSinTarjeta(codigoRetiroSinTarjeta);
        req.setObservaciones(observaciones);

        return createInstallment(returnRequestId, req);
    }

    @Override
    @Transactional
    public ReturnInstallmentResponseDto legacyConfirmPickup(Long returnRequestId) {
        OperationReturnInstallment installment = singleActiveInstallment(
                returnRequestId,
                ReturnInstallmentStatus.PROGRAMADA
        );
        return confirmInstallment(installment.getId());
    }

    @Override
    @Transactional
    public ReturnInstallmentResponseDto legacyMarkDelivered(
            Long returnRequestId,
            String comprobanteEntregaUrl
    ) {
        OperationReturnInstallment installment = singleActiveInstallment(
                returnRequestId,
                ReturnInstallmentStatus.ENTREGADA
        );
        DeliverReturnInstallmentRequestDto req = new DeliverReturnInstallmentRequestDto();
        req.setComprobanteEntregaUrl(comprobanteEntregaUrl);
        return deliverInstallment(installment.getId(), req);
    }

    // ==================================================================
    // Recálculo de estatus
    // ==================================================================

    /**
     * Recalcula el estatus de la solicitud a partir de sus parcialidades.
     * Debe llamarse con la solicitud ya bloqueada.
     */
    void recomputeRequestStatus(OperationReturnPayment solicitud) {
        Long id = solicitud.getId();
        BigDecimal montoSolicitado = scaled(solicitud.getMonto());
        BigDecimal completado = scaled(installmentRepository.sumCompletedBySolicitud(id));

        ReturnPaymentStatus next;
        if (completado.compareTo(BigDecimal.ZERO) == 0) {
            boolean hasEntregada = installmentRepository.countBySolicitudIdAndEstatusIn(
                    id, List.of(ReturnInstallmentStatus.ENTREGADA)) > 0;
            boolean hasProgramada = installmentRepository.countBySolicitudIdAndEstatusIn(
                    id, List.of(ReturnInstallmentStatus.PROGRAMADA)) > 0;
            next = hasEntregada
                    ? ReturnPaymentStatus.ENTREGADO
                    : hasProgramada
                    ? ReturnPaymentStatus.EN_RECOLECCION
                    : ReturnPaymentStatus.SOLICITADO;
        } else if (completado.compareTo(montoSolicitado) >= 0) {
            next = ReturnPaymentStatus.RETORNADO;
        } else {
            next = ReturnPaymentStatus.PARCIALMENTE_RETORNADO;
        }

        solicitud.setEstatus(next);

        // Espejo de campos legacy para compatibilidad con vistas de detalle.
        if (next == ReturnPaymentStatus.RETORNADO && solicitud.getFechaPago() == null) {
            solicitud.setFechaPago(LocalDateTime.now());
        }

        returnPaymentRepository.save(solicitud);
    }

    /**
     * Recalcula el estatus de la operación a partir del total efectivamente
     * retornado (parcialidades COMPLETADA de todas sus solicitudes). Debe
     * llamarse con la operación ya bloqueada.
     */
    void recomputeOperationStatus(PaymentOperation operation) {
        BigDecimal amountToReturn = returnAmountCalculator.amountToReturn(operation);
        BigDecimal totalCompletado = scaled(installmentRepository.sumCompletedByOperation(operation.getId()));

        OperationStatus current = operation.getEstatus();
        OperationStatus next = current;

        if (totalCompletado.compareTo(BigDecimal.ZERO) == 0) {
            if (current == OperationStatus.RETORNO_PARCIAL_ENTREGADO
                    || current == OperationStatus.RETORNADA) {
                BigDecimal totalSolicitado = scaled(
                        returnPaymentRepository.sumRequestedAmountByOperationId(operation.getId()));
                next = totalSolicitado.compareTo(amountToReturn) < 0
                        ? OperationStatus.RETORNO_PARCIAL_SOLICITADO
                        : OperationStatus.RETORNO_TOTAL_SOLICITADO;
            }
        } else if (totalCompletado.compareTo(amountToReturn) >= 0) {
            next = OperationStatus.RETORNADA;
        } else {
            next = OperationStatus.RETORNO_PARCIAL_ENTREGADO;
        }

        if (next != current) {
            operation.setEstatus(next);
            paymentOperationRepository.save(operation);
        }
    }

    // ==================================================================
    // Notificaciones
    // ==================================================================

    private void dispatchNotificationsAfterChange(
            OperationReturnInstallment installment,
            OperationReturnPayment solicitud
    ) {
        PaymentOperation operation = solicitud.getOperacion();
        User socio = operation.getSocioComercial();
        if (socio == null) {
            return;
        }

        BigDecimal montoSolicitado = scaled(solicitud.getMonto());
        BigDecimal retornado = scaled(installmentRepository.sumCompletedBySolicitud(solicitud.getId()));
        BigDecimal pendiente = montoSolicitado.subtract(retornado).max(BigDecimal.ZERO);
        String tipo = installment.getTipoPago().name();

        if (installment.getEstatus() == ReturnInstallmentStatus.COMPLETADA) {
            notifyUser(
                    socio.getId(),
                    "Retorno parcial realizado",
                    "Se realizó un retorno parcial de $" + installment.getMonto()
                            + " mediante " + tipo + " para la solicitud #" + solicitud.getId()
                            + " de la operación #" + operation.getId()
                            + ". Total solicitado: $" + montoSolicitado
                            + ". Total retornado: $" + retornado
                            + ". Pendiente: $" + pendiente + ".",
                    NotificationType.RETURN_INSTALLMENT_COMPLETED,
                    operation.getId(),
                    "/operaciones/" + operation.getId() + "?scrollToReturns=true"
            );

            if (solicitud.getEstatus() == ReturnPaymentStatus.RETORNADO) {
                long numParcialidades = installmentRepository.countBySolicitudIdAndEstatusIn(
                        solicitud.getId(), List.of(ReturnInstallmentStatus.COMPLETADA));
                notifyUser(
                        socio.getId(),
                        "Solicitud de retorno completada",
                        "La solicitud de retorno #" + solicitud.getId()
                                + " de la operación #" + operation.getId()
                                + " fue completada. Se retornaron $" + retornado
                                + " mediante " + numParcialidades + " parcialidad(es). Saldo pendiente: $0.",
                        NotificationType.RETURN_REQUEST_COMPLETED,
                        operation.getId(),
                        "/operaciones/" + operation.getId() + "?scrollToReturns=true"
                );
            }
        } else if (installment.getEstatus() == ReturnInstallmentStatus.PROGRAMADA) {
            NotificationType type = installment.getCodigoRetiroSinTarjeta() != null
                    ? NotificationType.RETURN_INSTALLMENT_CODE_AVAILABLE
                    : NotificationType.RETURN_INSTALLMENT_SCHEDULED;
            notifyUser(
                    socio.getId(),
                    "Recolección de retorno programada",
                    "Se programó la recolección de una parcialidad por $" + installment.getMonto()
                            + " de la operación #" + operation.getId()
                            + " para el " + installment.getFechaHoraRecoleccion() + "."
                            + (installment.getCodigoRetiroSinTarjeta() != null
                                ? " Código: " + installment.getCodigoRetiroSinTarjeta() + "."
                                : ""),
                    type,
                    operation.getId(),
                    "/operaciones/" + operation.getId() + "?scrollToReturns=true"
            );
        }
    }

    private void notifyUser(
            Long userId,
            String titulo,
            String mensaje,
            NotificationType tipo,
            Long operationId,
            String actionUrl
    ) {
        notificationService.createForUser(
                userId, titulo, mensaje, tipo,
                NotificationModule.PAGOS,
                NotificationReferenceType.PAYMENT_OPERATION, operationId,
                actionUrl, NotificationPriority.HIGH
        );
    }

    private void notifyRoles(
            List<com.sistemadeoperaciones.shared.enums.RoleName> roles,
            String titulo,
            String mensaje,
            NotificationType tipo,
            Long installmentId,
            String actionUrl
    ) {
        notificationService.createForRoles(
                roles, titulo, mensaje, tipo,
                NotificationModule.PAGOS,
                NotificationReferenceType.RETURN_INSTALLMENT, installmentId,
                actionUrl, NotificationPriority.HIGH
        );
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private OperationReturnPayment lockRequestAndOperation(Long returnRequestId) {
        OperationReturnPayment preview = returnPaymentRepository.findById(returnRequestId)
                .orElseThrow(ReturnPaymentNotFoundException::new);
        // Orden de locks fijo: operación primero, solicitud después (evita deadlock).
        paymentOperationRepository.findByIdForUpdate(preview.getOperacion().getId())
                .orElseThrow(ReturnPaymentNotFoundException::new);
        return returnPaymentRepository.findByIdForUpdate(returnRequestId)
                .orElseThrow(ReturnPaymentNotFoundException::new);
    }

    private OperationReturnInstallment loadWithLocks(Long installmentId) {
        OperationReturnInstallment preview = installmentRepository.findById(installmentId)
                .orElseThrow(ReturnInstallmentNotFoundException::new);
        lockRequestAndOperation(preview.getSolicitud().getId());
        return installmentRepository.findById(installmentId)
                .orElseThrow(ReturnInstallmentNotFoundException::new);
    }

    private OperationReturnInstallment singleActiveInstallment(
            Long returnRequestId,
            ReturnInstallmentStatus estatus
    ) {
        return installmentRepository.findBySolicitudIdOrderByCreatedAtAsc(returnRequestId)
                .stream()
                .filter(i -> i.getEstatus() == estatus)
                .findFirst()
                .orElseThrow(() -> new ReturnInstallmentInvalidStatusException(
                        "No hay una parcialidad en el estatus requerido para esta acción"));
    }

    private BankAccount requireBankAccount(Long id) {
        if (id == null) {
            throw new ReturnInstallmentOriginAccountRequiredException();
        }
        return bankAccountRepository.findById(id)
                .orElseThrow(ReturnInstallmentOriginAccountRequiredException::new);
    }

    private BigDecimal fullPending(OperationReturnPayment solicitud) {
        BigDecimal montoSolicitado = scaled(solicitud.getMonto());
        BigDecimal completado = scaled(installmentRepository.sumCompletedBySolicitud(solicitud.getId()));
        BigDecimal enProceso = scaled(installmentRepository.sumInFlightBySolicitud(solicitud.getId()));
        return montoSolicitado.subtract(completado).subtract(enProceso).max(BigDecimal.ZERO);
    }

    private void requireCashType(OperationReturnInstallment installment) {
        if (!isCash(installment.getTipoPago())) {
            throw new ReturnInstallmentInvalidStatusException(
                    "Esta acción solo aplica a parcialidades en efectivo o retiro sin tarjeta");
        }
    }

    private boolean isCash(PaymentType tipo) {
        return tipo == PaymentType.EFECTIVO || tipo == PaymentType.RETIRO_SIN_TARJETA;
    }

    private BigDecimal scaled(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String appendMotivo(String observaciones, String motivo) {
        String cancelNote = "[Cancelada] " + motivo;
        if (isBlank(observaciones)) {
            return cancelNote;
        }
        String combined = observaciones + " | " + cancelNote;
        return combined.length() > 500 ? combined.substring(0, 500) : combined;
    }

    ReturnInstallmentResponseDto mapToResponse(OperationReturnInstallment i) {
        ReturnInstallmentResponseDto dto = new ReturnInstallmentResponseDto();
        OperationReturnPayment solicitud = i.getSolicitud();
        PaymentOperation operation = solicitud.getOperacion();

        dto.setId(i.getId());
        dto.setReturnRequestId(solicitud.getId());
        dto.setOperationId(operation.getId());
        dto.setMonto(i.getMonto());
        dto.setTipoPago(i.getTipoPago());
        dto.setEstatus(i.getEstatus());
        dto.setComprobanteUrl(i.getComprobanteUrl());
        dto.setComprobanteEntregaUrl(i.getComprobanteEntregaUrl());
        dto.setCodigoRetiroSinTarjeta(i.getCodigoRetiroSinTarjeta());
        dto.setFechaHoraRecoleccion(i.getFechaHoraRecoleccion());
        dto.setFechaRealizacion(i.getFechaRealizacion());
        dto.setFechaEntrega(i.getFechaEntrega());
        dto.setFechaConfirmacion(i.getFechaConfirmacion());
        dto.setFechaCancelacion(i.getFechaCancelacion());
        dto.setObservaciones(i.getObservaciones());
        dto.setCreatedAt(i.getCreatedAt());
        dto.setUpdatedAt(i.getUpdatedAt());

        dto.setReturnRequestMonto(solicitud.getMonto());
        dto.setReturnRequestEstatus(solicitud.getEstatus());
        dto.setAutorizadoParaRecibir1(solicitud.getAutorizadoParaRecibirEfectivo1());
        dto.setAutorizadoParaRecibir2(solicitud.getAutorizadoParaRecibirEfectivo2());
        dto.setAutorizadoParaRecibir3(solicitud.getAutorizadoParaRecibirEfectivo3());

        if (operation.getCliente() != null) {
            dto.setClienteNombre(operation.getCliente().getNombre());
        }
        if (operation.getSocioComercial() != null) {
            dto.setSocioComercialNombre(operation.getSocioComercial().getNombre());
            dto.setSocioComercialTelefono(operation.getSocioComercial().getTelefono());
        }
        if (i.getCuentaOrigen() != null) {
            dto.setCuentaOrigenId(i.getCuentaOrigen().getId());
            dto.setCuentaOrigenNombre(i.getCuentaOrigen().getBanco());
        }
        if (i.getCreadoPor() != null) {
            dto.setCreadoPorId(i.getCreadoPor().getId());
            dto.setCreadoPorNombre(i.getCreadoPor().getNombre());
        }
        if (i.getRealizadoPor() != null) {
            dto.setRealizadoPorId(i.getRealizadoPor().getId());
            dto.setRealizadoPorNombre(i.getRealizadoPor().getNombre());
        }
        if (i.getEntregadoPor() != null) {
            dto.setEntregadoPorId(i.getEntregadoPor().getId());
            dto.setEntregadoPorNombre(i.getEntregadoPor().getNombre());
        }
        if (i.getCanceladoPor() != null) {
            dto.setCanceladoPorId(i.getCanceladoPor().getId());
            dto.setCanceladoPorNombre(i.getCanceladoPor().getNombre());
        }

        return dto;
    }
}
