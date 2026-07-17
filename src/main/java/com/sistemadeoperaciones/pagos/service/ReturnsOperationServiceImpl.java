package com.sistemadeoperaciones.pagos.service;

import com.sistemadeoperaciones.notifications.enums.NotificationModule;
import com.sistemadeoperaciones.notifications.enums.NotificationPriority;
import com.sistemadeoperaciones.notifications.enums.NotificationReferenceType;
import com.sistemadeoperaciones.notifications.enums.NotificationType;
import com.sistemadeoperaciones.notifications.service.NotificationService;
import com.sistemadeoperaciones.pagos.dto.PaymentOperationFilterDto;
import com.sistemadeoperaciones.pagos.dto.PaymentOperationResponseDto;
import com.sistemadeoperaciones.pagos.dto.retornos.*;
import com.sistemadeoperaciones.pagos.enums.OperationStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.enums.ReturnPaymentStatus;
import com.sistemadeoperaciones.pagos.exceptions.*;
import com.sistemadeoperaciones.pagos.model.OperationReturnPayment;
import com.sistemadeoperaciones.pagos.model.PaymentOperation;
import com.sistemadeoperaciones.pagos.repository.OperationReturnPaymentRepository;
import com.sistemadeoperaciones.pagos.repository.PaymentOperationRepository;
import com.sistemadeoperaciones.pagos.repository.specification.PaymentOperationSpecification;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.enums.RoleName;
import com.sistemadeoperaciones.usuarios.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.cuentasbancarias.repository.BankAccountRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReturnsOperationServiceImpl implements ReturnsOperationService {

    private final PaymentOperationRepository paymentOperationRepository;
    private final OperationReturnPaymentRepository operationReturnPaymentRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final BankAccountRepository bankAccountRepository;
    private final NotificationService notificationService;

    public ReturnsOperationServiceImpl(
            PaymentOperationRepository paymentOperationRepository,
            OperationReturnPaymentRepository operationReturnPaymentRepository,
            AuthenticatedUserService authenticatedUserService,
            BankAccountRepository bankAccountRepository,
            NotificationService notificationService
    ) {
        this.paymentOperationRepository = paymentOperationRepository;
        this.operationReturnPaymentRepository = operationReturnPaymentRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.bankAccountRepository = bankAccountRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public List<ReturnPaymentResponseDto> requestReturnPayment(
            Long operationId,
            CreateReturnPaymentBatchRequestDto request
    ) {
        PaymentOperation operation = paymentOperationRepository.findById(operationId)
                .orElseThrow(() -> new IllegalArgumentException("Operación no encontrada"));

        validateOperationCanReceiveReturn(operation);
        validateReturnBatchRequest(request);

        BigDecimal totalRequestedBefore =
                operationReturnPaymentRepository.sumRequestedAmountByOperationId(operationId);

        BigDecimal amountToReturn = calculateAmountToReturn(operation);

        request.getPagos().forEach(this::validateReturnRequest);

        BigDecimal totalRequestedNow = request.getPagos()
                .stream()
                .map(CreateReturnPaymentRequestDto::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal newTotalRequested = totalRequestedBefore.add(totalRequestedNow);

        if (newTotalRequested.compareTo(amountToReturn) > 0) {
            throw new ReturnAmountExceedsPendingBalanceException();
        }

        User currentUser = authenticatedUserService.getCurrentUser();

        List<OperationReturnPayment> returnPayments = request.getPagos()
                .stream()
                .map(paymentRequest -> {
                    validateReturnRequest(paymentRequest);

                    OperationReturnPayment returnPayment = new OperationReturnPayment();
                    returnPayment.setOperacion(operation);
                    returnPayment.setMonto(paymentRequest.getMonto());
                    returnPayment.setTipoPago(paymentRequest.getTipoPago());
                    returnPayment.setObservaciones(paymentRequest.getObservaciones());
                    returnPayment.setSolicitadoPor(currentUser);
                    returnPayment.setEstatus(ReturnPaymentStatus.SOLICITADO);
                    returnPayment.setCuentaDestinoBanco(paymentRequest.getBanco());
                    returnPayment.setFechaSolicitud(LocalDateTime.now());
                    returnPayment.setCuentaDestinoTitular(paymentRequest.getTitular());

                    String cuentaDestinoCliente = paymentRequest.getCuenta() != null
                            ? paymentRequest.getCuenta().replaceAll("\\s+", "")
                            : null;
                    String cuentaClabeCliente = paymentRequest.getClabe() != null
                            ? paymentRequest.getClabe().replaceAll("\\s+", "")
                            : null;

                    returnPayment.setCuentaDestinoCliente(cuentaDestinoCliente);
                    returnPayment.setCuentaClabeCliente(cuentaClabeCliente);
                    returnPayment.setAutorizadoParaRecibirEfectivo1(paymentRequest.getAutorizadoParaRecibirEfectivo1());
                    returnPayment.setAutorizadoParaRecibirEfectivo2(paymentRequest.getAutorizadoParaRecibirEfectivo2());
                    returnPayment.setAutorizadoParaRecibirEfectivo3(paymentRequest.getAutorizadoParaRecibirEfectivo3());

                    return returnPayment;
                })
                .toList();

        List<OperationReturnPayment> savedReturns =
                operationReturnPaymentRepository.saveAll(returnPayments);

        if (
                operation.getEstatus() == OperationStatus.VALIDADA ||
                        operation.getEstatus() == OperationStatus.RETORNO_PARCIAL_SOLICITADO
        ) {
            if (newTotalRequested.compareTo(amountToReturn) < 0) {
                operation.setEstatus(OperationStatus.RETORNO_PARCIAL_SOLICITADO);
            } else {
                operation.setEstatus(OperationStatus.RETORNO_TOTAL_SOLICITADO);
            }
        }

        paymentOperationRepository.save(operation);

        notifyReturnRequested(operation, savedReturns);

        return savedReturns
                .stream()
                .map(this::mapReturnToResponse)
                .toList();
    }

    @Override
    @Transactional
    public ReturnPaymentResponseDto updateRequestReturnPayment(
            Long returnPaymentId,
            CreateReturnPaymentRequestDto request
    ) {

        OperationReturnPayment returnPayment =
                operationReturnPaymentRepository.findById(returnPaymentId)
                        .orElseThrow(ReturnPaymentNotFoundException::new);

        validateReturnCanBeEdited(returnPayment);

        validateReturnRequest(request);

        PaymentOperation operation = returnPayment.getOperacion();

        BigDecimal totalRequestedBefore =
                operationReturnPaymentRepository
                        .sumRequestedAmountByOperationId(
                                operation.getId()
                        );

        if (totalRequestedBefore == null) {
            totalRequestedBefore = BigDecimal.ZERO;
        }

        BigDecimal amountToReturn =
                calculateAmountToReturn(operation);

        // Se resta el monto actual porque será reemplazado
        BigDecimal totalWithoutCurrentReturn =
                totalRequestedBefore.subtract(
                        returnPayment.getMonto()
                );

        BigDecimal newTotalRequested =
                totalWithoutCurrentReturn.add(
                        request.getMonto()
                );

        if (newTotalRequested.compareTo(amountToReturn) > 0) {
            throw new ReturnAmountExceedsPendingBalanceException();
        }

        returnPayment.setMonto(request.getMonto());
        returnPayment.setTipoPago(request.getTipoPago());
        returnPayment.setObservaciones(
                request.getObservaciones()
        );

        if (request.getTipoPago() == PaymentType.EFECTIVO || request.getTipoPago() == PaymentType.RETIRO_SIN_TARJETA) {

            returnPayment.setCuentaDestinoBanco(null);
            returnPayment.setCuentaDestinoTitular(null);
            returnPayment.setCuentaDestinoCliente(null);

        } else {

            returnPayment.setCuentaDestinoBanco(
                    request.getBanco()
            );

            returnPayment.setCuentaDestinoTitular(
                    request.getTitular()
            );

            String cuentaDestinoCliente =
                    request.getCuenta()!= null
                            ? request.getCuenta()
                            .replaceAll("\\s+", "")
                            : null;
            String cuentaClabeCliente =
                    request.getClabe() != null
                            ? request.getClabe()
                            .replaceAll("\\s+", "")
                            : null;

            returnPayment.setCuentaDestinoCliente(
                    cuentaDestinoCliente
            );
            returnPayment.setCuentaClabeCliente(
                    cuentaClabeCliente
            );
        }

        OperationReturnPayment updated =
                operationReturnPaymentRepository.save(
                        returnPayment
                );

        if (
                operation.getEstatus() == OperationStatus.VALIDADA ||
                        operation.getEstatus() == OperationStatus.RETORNO_PARCIAL_SOLICITADO ||
                        operation.getEstatus() == OperationStatus.RETORNO_TOTAL_SOLICITADO
        ) {
            if (newTotalRequested.compareTo(amountToReturn) < 0) {
                operation.setEstatus(OperationStatus.RETORNO_PARCIAL_SOLICITADO);
            } else {
                operation.setEstatus(OperationStatus.RETORNO_TOTAL_SOLICITADO);
            }

            paymentOperationRepository.save(operation);
        }

        notifyReturnUpdated(updated);

        return mapReturnToResponse(updated);
    }

    private void validateReturnBatchRequest(CreateReturnPaymentBatchRequestDto request) {
        if (request.getPagos() == null || request.getPagos().isEmpty()) {
            throw new IllegalArgumentException("Debe capturarse al menos un pago de retorno");
        }
    }

    private void validateReturnCanBeEdited(
            OperationReturnPayment returnPayment
    ) {
        if (returnPayment.getEstatus()
                != ReturnPaymentStatus.SOLICITADO) {

            throw new ReturnPaymentCannotBeEditedException();
        }
    }


    @Override
    @Transactional
    public ReturnPaymentResponseDto realizeReturnPayment(
            Long returnPaymentId,
            RealizeReturnPaymentRequestDto request
    ) {
        OperationReturnPayment returnPayment = operationReturnPaymentRepository.findById(returnPaymentId)
                .orElseThrow(() -> new IllegalArgumentException("Retorno no encontrado"));

        if (returnPayment.getTipoPago() == PaymentType.EFECTIVO || returnPayment.getTipoPago() == PaymentType.RETIRO_SIN_TARJETA) {
            throw new CashReturnMustBeScheduledException();
        }

        if (returnPayment.getEstatus() != ReturnPaymentStatus.SOLICITADO) {
            throw new InvalidReturnPaymentStatusException();
        }

        validateRealizeReturnRequest(returnPayment, request);

        User currentUser = authenticatedUserService.getCurrentUser();

        if (returnPayment.getTipoPago() == PaymentType.TRANSFERENCIA) {
            BankAccount cuentaOrigen = bankAccountRepository.findById(request.getCuentaOrigenId())
                    .orElseThrow(() -> new IllegalArgumentException("Cuenta origen no encontrada"));

            returnPayment.setCuentaOrigen(cuentaOrigen);
        } else {
            returnPayment.setCuentaOrigen(null);
        }

        returnPayment.setComprobanteUrl(request.getComprobanteUrl());
        returnPayment.setPagadoPor(currentUser);
        returnPayment.setFechaPago(LocalDateTime.now());
        returnPayment.setEstatus(ReturnPaymentStatus.RETORNADO);
        returnPayment.setFechaHoraRecoleccionEfectivo(request.getFechaHoraRecoleccionEfectivo());

        if (request.getObservaciones() != null && !request.getObservaciones().isBlank()) {
            returnPayment.setObservaciones(request.getObservaciones());
        }

        OperationReturnPayment savedReturn = operationReturnPaymentRepository.save(returnPayment);

        updateOperationStatusAfterReturnRealized(returnPayment.getOperacion());

        notifyReturnRealized(savedReturn);

        return mapReturnToResponse(savedReturn);
    }

    private void validateRealizeReturnRequest(
            OperationReturnPayment returnPayment,
            RealizeReturnPaymentRequestDto request
    ) {
        if ((returnPayment.getTipoPago() == PaymentType.EFECTIVO || returnPayment.getTipoPago() == PaymentType.RETIRO_SIN_TARJETA)
                && request.getFechaHoraRecoleccionEfectivo() == null) {
            throw new IllegalArgumentException(
                    "La fecha y hora de recolección del efectivo es obligatoria"
            );
        }
        if (returnPayment.getTipoPago() != PaymentType.EFECTIVO || returnPayment.getTipoPago() == PaymentType.RETIRO_SIN_TARJETA) {
            request.setFechaHoraRecoleccionEfectivo(null);
        }
        if (returnPayment.getTipoPago() == PaymentType.TRANSFERENCIA && request.getCuentaOrigenId() == null) {
            throw new IllegalArgumentException("La cuenta origen es obligatoria");
        }

        if (request.getComprobanteUrl() == null || request.getComprobanteUrl().isBlank()) {
            throw new IllegalArgumentException("El comprobante es obligatorio");
        }
    }

    private void updateOperationStatusAfterReturnRealized(PaymentOperation operation) {
        BigDecimal amountToReturn = calculateAmountToReturn(operation);

        BigDecimal totalRealized =
                operationReturnPaymentRepository.sumRealizedAmountByOperationId(operation.getId());

        if (totalRealized.compareTo(amountToReturn) >= 0) {
            operation.setEstatus(OperationStatus.RETORNADA);
        } else {
            operation.setEstatus(OperationStatus.RETORNO_PARCIAL_ENTREGADO);
        }

        paymentOperationRepository.save(operation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnPaymentResponseDto> findReturnsByOperationId(Long operationId) {
        return operationReturnPaymentRepository.findByOperacionId(operationId)
                .stream()
                .map(this::mapReturnToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentOperationResponseDto findReturnDetailByOperationId(Long operationId) {
        PaymentOperation operation = paymentOperationRepository.findById(operationId)
                .orElseThrow(() -> new IllegalArgumentException("Operación no encontrada"));

        if (!Boolean.TRUE.equals(operation.getActivo())) {
            throw new PaymentOperationInactiveException();
        }

        return mapOperationToResponse(operation);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentOperationResponseDto> findOperationsAvailableToRequestReturn(
            PaymentOperationFilterDto filter,
            Pageable pageable
    ) {
        if (filter == null) {
            filter = new PaymentOperationFilterDto();
        }

        Specification<PaymentOperation> specification =
                buildReturnOperationSpecification(
                        filter,
                        List.of(
                                OperationStatus.VALIDADA,
                                OperationStatus.RETORNO_PARCIAL_SOLICITADO,
                                OperationStatus.RETORNO_PARCIAL_ENTREGADO
                        )
                );

        User currentUser = authenticatedUserService.getCurrentUser();

        boolean isSocioComercial = currentUser.getRoles()
                .stream()
                .anyMatch(role -> role.getName() == RoleName.SOCIO_COMERCIAL);

        boolean isAdminOrGerente = currentUser.getRoles()
                .stream()
                .anyMatch(role ->
                        role.getName() == RoleName.ADMIN ||
                                role.getName() == RoleName.GERENTE
                );

        if (isSocioComercial && !isAdminOrGerente) {
            specification = specification.and(
                    PaymentOperationSpecification.hasSocioComercialId(currentUser.getId())
            );
        }

        Page<PaymentOperationResponseDto> page =
                paymentOperationRepository.findAll(specification, pageable)
                        .map(this::mapOperationToResponse);

        List<PaymentOperationResponseDto> filteredContent = page.getContent()
                .stream()
                .toList();

        return new PageImpl<>(
                filteredContent,
                pageable,
                filteredContent.size()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentOperationResponseDto> findOperationsWithRequestedReturns(
            PaymentOperationFilterDto filter,
            Pageable pageable
    ) {
        if (filter == null) {
            filter = new PaymentOperationFilterDto();
        }

        Specification<PaymentOperation> specification = buildReturnOperationSpecification(
                filter,
                List.of(
                        OperationStatus.RETORNO_PARCIAL_SOLICITADO,
                        OperationStatus.RETORNO_TOTAL_SOLICITADO,
                        OperationStatus.RETORNO_PARCIAL_ENTREGADO
                )
        ).and(PaymentOperationSpecification.hasReturnWithStatusIn(
                List.of(ReturnPaymentStatus.SOLICITADO, ReturnPaymentStatus.EN_RECOLECCION)
        ));

        User currentUser = authenticatedUserService.getCurrentUser();

        boolean isSocioComercial = currentUser.getRoles()
                .stream()
                .anyMatch(role -> role.getName() == RoleName.SOCIO_COMERCIAL);

        boolean isAdminOrGerente = currentUser.getRoles()
                .stream()
                .anyMatch(role ->
                        role.getName() == RoleName.ADMIN ||
                                role.getName() == RoleName.GERENTE
                );

        if (isSocioComercial && !isAdminOrGerente) {
            specification = specification.and(
                    PaymentOperationSpecification.hasSocioComercialId(currentUser.getId())
            );
        }

        return paymentOperationRepository.findAll(specification, pageable)
                .map(this::mapOperationToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnDestinationAccountSuggestionDto> findReturnDestinationSuggestionsByClienteId(
            Long clienteId
    ) {
        return operationReturnPaymentRepository
                .findTop10DestinationAccountsByClienteId(clienteId);
    }

    private Specification<PaymentOperation> buildReturnOperationSpecification(
            PaymentOperationFilterDto filter,
            List<OperationStatus> statuses
    ) {
        return Specification
                .where(PaymentOperationSpecification.hasStatusIn(statuses))
                .and(PaymentOperationSpecification.clienteONombreSocioOIdContains(filter.getSearch()))
                .and(PaymentOperationSpecification.hasSocioComercialId(filter.getSocioComercialId()))
                .and(PaymentOperationSpecification.createdAtBetween(
                        toStartOfDay(resolveStartDate(filter)),
                        toEndOfDay(resolveEndDate(filter))
                ))
                .and(PaymentOperationSpecification.matchesActivoFilter(filter.getActivo()));
    }

    private void validateOperationCanReceiveReturn(PaymentOperation operation) {
        if (operation.getEstatus() == OperationStatus.RETORNO_TOTAL_SOLICITADO
                || operation.getEstatus() == OperationStatus.RETORNO_PARCIAL_ENTREGADO
                || operation.getEstatus() == OperationStatus.RETORNADA
                || operation.getEstatus() == OperationStatus.COMPLETADA) {
            throw new OperationAlreadyHasFullReturnRequestedException();
        }

        if (operation.getEstatus() != OperationStatus.VALIDADA
                && operation.getEstatus() != OperationStatus.RETORNO_PARCIAL_SOLICITADO
                && operation.getEstatus() != OperationStatus.RETORNO_PARCIAL_ENTREGADO) {
            throw new OperationCannotReceiveReturnException();
        }
    }

    private void validateReturnRequest(
            CreateReturnPaymentRequestDto request
    ) {

        if (request.getMonto() == null
                || request.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidReturnAmountException();
        }

        if (request.getTipoPago() == null) {
            throw new ReturnPaymentTypeRequiredException();
        }

        if (request.getTipoPago() == PaymentType.EFECTIVO || request.getTipoPago() == PaymentType.RETIRO_SIN_TARJETA) {

            if (
                    hasText(request.getBanco()) ||
                            hasText(request.getTitular()) ||
                            hasText(request.getClabe())
            ) {
                throw new ReturnCashPaymentCannotContainBankDataException();
            }

            return;
        }

        if (!hasText(request.getBanco())) {
            throw new ReturnBankRequiredException();
        }

        if (!hasText(request.getTitular())) {
            throw new ReturnAccountHolderRequiredException();
        }

        boolean hasClabe = hasText(request.getClabe());

        if (!hasClabe) {
            throw new ReturnAccountOrClabeRequiredException();
        }

        if (hasClabe) {

            String clabeLimpia =
                    request.getClabe()
                            .replaceAll("\\s+", "");

            if (!clabeLimpia.matches("\\d+")) {
                throw new InvalidReturnClabeException();
            }

            if (clabeLimpia.length() != 18) {
                throw new InvalidReturnClabeLengthException();
            }
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private BigDecimal calculateAmountToReturn(PaymentOperation operation) {
        BigDecimal montoValidado = safe(operation.getMontoValidado());

        BigDecimal porcentajeComisionRedTotal = safe(operation.getPorcentajeComisionSocio())
                .add(safe(operation.getPorcentajeComisionSocioNivel2()))
                .add(safe(operation.getPorcentajeComisionSocioNivel3()))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal montoComisionRedTotal = calculateAmountFromPercentage(
                montoValidado,
                porcentajeComisionRedTotal
        );


        BigDecimal montoComisionOficinaTotal = calculateAmountFromPercentage(
                montoValidado,
                operation.getPorcentajeComisionOficina()
        );

        return montoValidado
                .subtract(montoComisionRedTotal)
                .subtract(montoComisionOficinaTotal)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal calculateAmountFromPercentage(BigDecimal baseAmount, BigDecimal percentage) {
        return safe(baseAmount)
                .multiply(safe(percentage))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private List<RoleName> resolveReturnNotificationRoles(PaymentType tipoPago) {
        if (tipoPago == PaymentType.TRANSFERENCIA || tipoPago == PaymentType.DEPOSITO) {
            return List.of(RoleName.JEFA_CUENTAS, RoleName.ADMIN);
        }

        return List.of(RoleName.JEFA_CAJAS, RoleName.ADMIN);
    }

    private void notifyReturnRequested(
            PaymentOperation operation,
            List<OperationReturnPayment> returnPayments
    ) {
        List<OperationReturnPayment> transferPayments = returnPayments.stream()
                .filter(payment ->
                        payment.getTipoPago() == PaymentType.TRANSFERENCIA
                                || payment.getTipoPago() == PaymentType.DEPOSITO
                )
                .toList();

        List<OperationReturnPayment> cashPayments = returnPayments.stream()
                .filter(payment ->
                        payment.getTipoPago() == PaymentType.EFECTIVO
                                || payment.getTipoPago() == PaymentType.RETIRO_SIN_TARJETA
                )
                .toList();

        if (!transferPayments.isEmpty()) {
            sendReturnRequestedNotification(
                    operation,
                    transferPayments,
                    List.of(RoleName.JEFA_CUENTAS, RoleName.ADMIN)
            );
        }

        if (!cashPayments.isEmpty()) {
            sendReturnRequestedNotification(
                    operation,
                    cashPayments,
                    List.of(RoleName.JEFA_CAJAS, RoleName.ADMIN)
            );
        }
    }

    private void sendReturnRequestedNotification(
            PaymentOperation operation,
            List<OperationReturnPayment> returnPayments,
            List<RoleName> roles
    ) {
        BigDecimal totalRequested = returnPayments.stream()
                .map(OperationReturnPayment::getMonto)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        notificationService.createForRoles(
                roles,
                "Nueva solicitud de retorno",
                "Se solicitó un retorno por $" + totalRequested
                        + " para la operación #" + operation.getId() + ".",
                NotificationType.SYSTEM_ALERT,
                NotificationModule.PAGOS,
                NotificationReferenceType.PAYMENT_OPERATION,
                operation.getId(),
                "/operaciones/" + operation.getId() + "?scrollToReturns=true",
                NotificationPriority.HIGH
        );
    }

    private void notifyReturnRealized(OperationReturnPayment returnPayment) {
        PaymentOperation operation = returnPayment.getOperacion();

        if (operation.getSocioComercial() == null) {
            return;
        }

        notificationService.createForUser(
                operation.getSocioComercial().getId(),
                "Retorno pagado",
                "Se registró el pago del retorno por $" + returnPayment.getMonto()
                        + " para la operación #" + operation.getId() + ".",
                NotificationType.SYSTEM_ALERT,
                NotificationModule.PAGOS,
                NotificationReferenceType.PAYMENT_OPERATION,
                operation.getId(),
                "/operaciones/" + operation.getId() + "?scrollToReturns=true",
                NotificationPriority.HIGH
        );
    }

    private void notifyReturnUpdated(
            OperationReturnPayment returnPayment
    ) {
        PaymentOperation operation =
                returnPayment.getOperacion();

        notificationService.createForRoles(
                resolveReturnNotificationRoles(returnPayment.getTipoPago()),
                "Solicitud de retorno actualizada",
                "Se actualizó la solicitud de retorno por $"
                        + returnPayment.getMonto()
                        + " de la operación #"
                        + operation.getId()
                        + ".",
                NotificationType.SYSTEM_ALERT,
                NotificationModule.PAGOS,
                NotificationReferenceType.PAYMENT_OPERATION,
                operation.getId(),
                "/operaciones/"
                        + operation.getId()
                        + "?scrollToReturns=true",
                NotificationPriority.HIGH
        );
    }

    private ReturnPaymentResponseDto mapReturnToResponse(OperationReturnPayment returnPayment) {
        ReturnPaymentResponseDto dto = new ReturnPaymentResponseDto();

        dto.setId(returnPayment.getId());
        dto.setOperationId(returnPayment.getOperacion().getId());
        dto.setClientId(returnPayment.getOperacion().getCliente().getId());
        dto.setMonto(returnPayment.getMonto());
        dto.setTipoPago(returnPayment.getTipoPago());
        dto.setCuentaDestinoCliente(returnPayment.getCuentaDestinoCliente());
        dto.setCuentaClabeCliente(returnPayment.getCuentaClabeCliente());
        dto.setComprobanteUrl(returnPayment.getComprobanteUrl());
        dto.setObservaciones(returnPayment.getObservaciones());
        dto.setEstatus(returnPayment.getEstatus());
        dto.setFechaSolicitud(returnPayment.getFechaSolicitud());
        dto.setFechaPago(returnPayment.getFechaPago());
        dto.setCreatedAt(returnPayment.getCreatedAt());
        dto.setCuentaDestinoTitular(returnPayment.getCuentaDestinoTitular());
        dto.setCuentaDestinoBanco(returnPayment.getCuentaDestinoBanco());
        dto.setAutorizadoParaRecibirEfectivo1(returnPayment.getAutorizadoParaRecibirEfectivo1());
        dto.setAutorizadoParaRecibirEfectivo2(returnPayment.getAutorizadoParaRecibirEfectivo2());
        dto.setAutorizadoParaRecibirEfectivo3(returnPayment.getAutorizadoParaRecibirEfectivo3());
        dto.setFechaHoraRecoleccionEfectivo(returnPayment.getFechaHoraRecoleccionEfectivo());

        if (returnPayment.getCuentaOrigen() != null) {
            dto.setCuentaOrigenId(returnPayment.getCuentaOrigen().getId());
            dto.setCuentaOrigenNombre(returnPayment.getCuentaOrigen().getBanco());
        }

        if (returnPayment.getSolicitadoPor() != null) {
            dto.setSolicitadoPorId(returnPayment.getSolicitadoPor().getId());
            dto.setSolicitadoPorNombre(returnPayment.getSolicitadoPor().getNombre());
        }

        if (returnPayment.getPagadoPor() != null) {
            dto.setPagadoPorId(returnPayment.getPagadoPor().getId());
            dto.setPagadoPorNombre(returnPayment.getPagadoPor().getNombre());
        }

        return dto;
    }

    private PaymentOperationResponseDto mapOperationToResponse(PaymentOperation operation) {
        BigDecimal montoTotal = safe(operation.getMontoTotal());
        BigDecimal montoValidado = safe(operation.getMontoValidado());

        BigDecimal saldoPendientePorValidar = montoTotal
                .subtract(montoValidado)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        PaymentOperationResponseDto dto = new PaymentOperationResponseDto();

        dto.setId(operation.getId());
        dto.setActivo(operation.getActivo());

        if (operation.getCliente() != null) {
            dto.setClienteId(operation.getCliente().getId());
            dto.setClienteNombre(operation.getCliente().getNombre());
        }

        dto.setMontoTotal(operation.getMontoTotal());
        dto.setMontoValidado(operation.getMontoValidado());
        dto.setSaldoPendientePorValidar(saldoPendientePorValidar);
        dto.setEstatus(operation.getEstatus());

        if (operation.getSocioComercial() != null) {
            dto.setSocioComercialId(operation.getSocioComercial().getId());
            dto.setSocioComercialNombre(operation.getSocioComercial().getNombre());
        }

        dto.setNivelesRedComercial(operation.getNivelesRedComercial());
        dto.setPorcentajeComisionSocio(operation.getPorcentajeComisionSocio());
        dto.setPorcentajeComisionSocioNivel2(operation.getPorcentajeComisionSocioNivel2());
        dto.setPorcentajeComisionSocioNivel3(operation.getPorcentajeComisionSocioNivel3());
        dto.setPorcentajeComisionOficina(operation.getPorcentajeComisionOficina());

        BigDecimal porcentajeNivel1 = safe(operation.getPorcentajeComisionSocio());
        BigDecimal porcentajeNivel2 = safe(operation.getPorcentajeComisionSocioNivel2());
        BigDecimal porcentajeNivel3 = safe(operation.getPorcentajeComisionSocioNivel3());

        BigDecimal montoComisionSocioNivel1 = calculateAmountFromPercentage(montoValidado, porcentajeNivel1);
        BigDecimal montoComisionSocioNivel2 = calculateAmountFromPercentage(montoValidado, porcentajeNivel2);
        BigDecimal montoComisionSocioNivel3 = calculateAmountFromPercentage(montoValidado, porcentajeNivel3);

        dto.setMontoComisionSocioNivel1(montoComisionSocioNivel1);
        dto.setMontoComisionSocioNivel2(montoComisionSocioNivel2);
        dto.setMontoComisionSocioNivel3(montoComisionSocioNivel3);

        BigDecimal porcentajeComisionRedTotal = porcentajeNivel1
                .add(porcentajeNivel2)
                .add(porcentajeNivel3)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal montoComisionRedTotal = montoComisionSocioNivel1
                .add(montoComisionSocioNivel2)
                .add(montoComisionSocioNivel3)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal porcentajeComisionOficinaTotal = safe(operation.getPorcentajeComisionOficina())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal montoComisionOficinaTotal = calculateAmountFromPercentage(
                montoTotal,
                porcentajeComisionOficinaTotal
        );

        BigDecimal montoTotalDevolverCliente = montoTotal
                .subtract(montoComisionRedTotal)
                .subtract(montoComisionOficinaTotal)
                .setScale(2, RoundingMode.HALF_UP);

        dto.setPorcentajeComisionRedTotal(porcentajeComisionRedTotal);
        dto.setMontoComisionRedTotal(montoComisionRedTotal);
        dto.setPorcentajeComisionOficinaTotal(porcentajeComisionOficinaTotal);
        dto.setMontoComisionOficinaTotal(montoComisionOficinaTotal);
        dto.setMontoTotalDevolverCliente(montoTotalDevolverCliente);
        BigDecimal montoRetornado = safe(
                operationReturnPaymentRepository
                        .sumRealizedAmountByOperationId(operation.getId())
        );

        BigDecimal saldoPendienteRetornar = montoTotalDevolverCliente
                .subtract(montoRetornado)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal montoSolicitadoRetorno = safe(
                operationReturnPaymentRepository
                        .sumRequestedAmountByOperationId(operation.getId())
        );
        long numeroRetornosSolicitados =
                operationReturnPaymentRepository.countByOperacionId(operation.getId());
        dto.setMontoSolicitadoRetorno(montoSolicitadoRetorno);
        dto.setNumeroRetornosSolicitados(numeroRetornosSolicitados);
        dto.setMontoRetornado(montoRetornado);
        dto.setSaldoPendienteRetornar(saldoPendienteRetornar);

        dto.setObservaciones(operation.getObservaciones());
        dto.setCreatedAt(operation.getCreatedAt());
        dto.setUpdatedAt(operation.getUpdatedAt());

        boolean contieneRetornosEnEfectivo =
                operationReturnPaymentRepository.existsByOperacionIdAndTipoPago(
                        operation.getId(),
                        PaymentType.EFECTIVO
                );

        boolean contieneRetornosRetiroSinTarjeta =
                operationReturnPaymentRepository.existsByOperacionIdAndTipoPago(
                operation.getId(),
                PaymentType.RETIRO_SIN_TARJETA
        );

        boolean contieneRetornosEnTransferencia =
                operationReturnPaymentRepository.existsByOperacionIdAndTipoPago(
                        operation.getId(),
                        PaymentType.TRANSFERENCIA
                );

        dto.setContieneRetornosEnEfectivo(contieneRetornosEnEfectivo);
        dto.setContieneRetornosRetiroSinTarjeta(contieneRetornosRetiroSinTarjeta);
        dto.setContieneRetornosEnTransferencia(contieneRetornosEnTransferencia);

        return dto;
    }

    private LocalDate resolveStartDate(PaymentOperationFilterDto filter) {
        if (filter.getDateFilter() != null) {
            LocalDate today = LocalDate.now();

            return switch (filter.getDateFilter()) {
                case TODAY -> today;
                case THIS_WEEK -> today.with(DayOfWeek.MONDAY);
                case THIS_MONTH -> today.withDayOfMonth(1);
                case LAST_MONTH -> today.minusMonths(1).withDayOfMonth(1);
            };
        }

        return filter.getStartDate();
    }

    private LocalDate resolveEndDate(PaymentOperationFilterDto filter) {
        if (filter.getDateFilter() != null) {
            LocalDate today = LocalDate.now();

            return switch (filter.getDateFilter()) {
                case TODAY -> today;
                case THIS_WEEK -> today.with(DayOfWeek.SUNDAY);
                case THIS_MONTH -> today.withDayOfMonth(today.lengthOfMonth());
                case LAST_MONTH -> {
                    LocalDate lastMonth = today.minusMonths(1);
                    yield lastMonth.withDayOfMonth(lastMonth.lengthOfMonth());
                }
            };
        }

        return filter.getEndDate();
    }

    private LocalDateTime toStartOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }

    private LocalDateTime toEndOfDay(LocalDate date) {
        return date != null ? date.plusDays(1).atStartOfDay().minusNanos(1) : null;
    }

    @Override
    @Transactional
    public ReturnPaymentResponseDto scheduleCashReturnPickup(
            Long returnPaymentId,
            ScheduleCashReturnPickupRequestDto request
    ) {
        OperationReturnPayment returnPayment =
                operationReturnPaymentRepository.findById(returnPaymentId)
                        .orElseThrow(ReturnPaymentNotFoundException::new);

        if (returnPayment.getEstatus() != ReturnPaymentStatus.SOLICITADO
                && returnPayment.getEstatus() != ReturnPaymentStatus.EN_RECOLECCION) {
            throw new InvalidReturnPaymentStatusException();
        }

        if (returnPayment.getTipoPago() != PaymentType.EFECTIVO
                && returnPayment.getTipoPago() != PaymentType.RETIRO_SIN_TARJETA) {
            throw new InvalidCashReturnPaymentTypeException();
        }

        if (request.getFechaHoraRecoleccionEfectivo() == null) {
            throw new CashReturnPickupDateRequiredException();
        }

        if (returnPayment.getTipoPago() == PaymentType.RETIRO_SIN_TARJETA) {
            if (request.getCuentaOrigenId() == null) {
                throw new CashReturnOriginAccountRequiredException();
            }

            BankAccount cuentaOrigen = bankAccountRepository.findById(request.getCuentaOrigenId())
                    .orElseThrow(() -> new IllegalArgumentException("Cuenta origen no encontrada"));

            returnPayment.setCuentaOrigen(cuentaOrigen);
        }

        returnPayment.setFechaHoraRecoleccionEfectivo(
                request.getFechaHoraRecoleccionEfectivo()
        );

        returnPayment.setEstatus(ReturnPaymentStatus.EN_RECOLECCION);

        if (request.getObservaciones() != null && !request.getObservaciones().isBlank()) {
            returnPayment.setObservaciones(request.getObservaciones());
        }

        OperationReturnPayment saved =
                operationReturnPaymentRepository.save(returnPayment);

        notifyCashReturnPickupScheduled(saved);

        return mapReturnToResponse(saved);
    }

    private void notifyCashReturnPickupScheduled(OperationReturnPayment returnPayment) {
        PaymentOperation operation = returnPayment.getOperacion();

        if (operation.getSocioComercial() == null) {
            return;
        }

        notificationService.createForUser(
                operation.getSocioComercial().getId(),
                "Recolección de retorno programada",
                "Se programó la recolección de tu retorno en efectivo por $"
                        + returnPayment.getMonto()
                        + " de la operación #"
                        + operation.getId()
                        + " para el "
                        + returnPayment.getFechaHoraRecoleccionEfectivo()
                        + ".",
                NotificationType.SYSTEM_ALERT,
                NotificationModule.PAGOS,
                NotificationReferenceType.PAYMENT_OPERATION,
                operation.getId(),
                "/operaciones/" + operation.getId() + "?scrollToReturns=true",
                NotificationPriority.HIGH
        );
    }

    @Override
    @Transactional
    public ReturnPaymentResponseDto confirmCashReturnPickup(Long returnPaymentId) {
        OperationReturnPayment returnPayment =
                operationReturnPaymentRepository.findById(returnPaymentId)
                        .orElseThrow(ReturnPaymentNotFoundException::new);

        if (returnPayment.getTipoPago() != PaymentType.EFECTIVO
                && returnPayment.getTipoPago() != PaymentType.RETIRO_SIN_TARJETA) {
            throw new InvalidCashReturnPaymentTypeException();
        }

        if (returnPayment.getEstatus() != ReturnPaymentStatus.EN_RECOLECCION) {
            throw new InvalidReturnPaymentStatusException();
        }

        User currentUser = authenticatedUserService.getCurrentUser();
        PaymentOperation operation = returnPayment.getOperacion();

        boolean isOwner = operation.getSocioComercial() != null
                && operation.getSocioComercial().getId().equals(currentUser.getId());

        if (!isOwner) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tienes permisos para confirmar este retorno"
            );
        }

        returnPayment.setEstatus(ReturnPaymentStatus.RETORNADO);
        returnPayment.setFechaPago(LocalDateTime.now());
        returnPayment.setPagadoPor(currentUser);

        OperationReturnPayment saved =
                operationReturnPaymentRepository.save(returnPayment);

        updateOperationStatusAfterReturnRealized(operation);

        notifyCashReturnPickupConfirmed(saved);

        return mapReturnToResponse(saved);
    }

    private void notifyCashReturnPickupConfirmed(OperationReturnPayment returnPayment) {
        PaymentOperation operation = returnPayment.getOperacion();

        notificationService.createForRoles(
                List.of(RoleName.JEFA_CAJAS, RoleName.ADMIN),
                "Retorno en efectivo confirmado",
                "El socio comercial confirmó haber recibido el retorno en efectivo por $"
                        + returnPayment.getMonto()
                        + " de la operación #"
                        + operation.getId()
                        + ".",
                NotificationType.SYSTEM_ALERT,
                NotificationModule.PAGOS,
                NotificationReferenceType.PAYMENT_OPERATION,
                operation.getId(),
                "/operaciones/" + operation.getId() + "?scrollToReturns=true",
                NotificationPriority.HIGH
        );
    }
}