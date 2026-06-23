package com.sistemadeoperaciones.pagos.service;

import com.sistemadeoperaciones.clientes.exceptions.ClienteNotFoundException;
import com.sistemadeoperaciones.clientes.model.Clientes;
import com.sistemadeoperaciones.clientes.repository.ClientesRepository;
import com.sistemadeoperaciones.comisionessocioscomerciales.service.CommercialPartnerCommissionService;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.cuentasbancarias.repository.BankAccountRepository;
import com.sistemadeoperaciones.notifications.enums.NotificationModule;
import com.sistemadeoperaciones.notifications.enums.NotificationPriority;
import com.sistemadeoperaciones.notifications.enums.NotificationReferenceType;
import com.sistemadeoperaciones.notifications.enums.NotificationType;
import com.sistemadeoperaciones.notifications.service.NotificationService;
import com.sistemadeoperaciones.pagos.dto.*;
import com.sistemadeoperaciones.pagos.enums.OperationStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.exceptions.DuplicatePaymentReceiptException;
import com.sistemadeoperaciones.pagos.exceptions.InvalidPaymentStatusException;
import com.sistemadeoperaciones.pagos.exceptions.MissingPaymentReceiptException;
import com.sistemadeoperaciones.pagos.exceptions.MissingRejectionReasonException;
import com.sistemadeoperaciones.pagos.exceptions.OperationDoesNotAcceptPaymentsException;
import com.sistemadeoperaciones.pagos.exceptions.OperationPaymentNotFoundException;
import com.sistemadeoperaciones.pagos.exceptions.PaymentAmountExceededException;
import com.sistemadeoperaciones.pagos.exceptions.PaymentOperationInactiveAccountException;
import com.sistemadeoperaciones.pagos.exceptions.PaymentOperationInactivePartnerException;
import com.sistemadeoperaciones.pagos.exceptions.PaymentOperationNotFoundException;
import com.sistemadeoperaciones.pagos.model.OperationPayment;
import com.sistemadeoperaciones.pagos.model.PaymentOperation;
import com.sistemadeoperaciones.pagos.repository.OperationPaymentRepository;
import com.sistemadeoperaciones.pagos.repository.OperationReturnPaymentRepository;
import com.sistemadeoperaciones.pagos.repository.PaymentOperationRepository;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.enums.RoleName;
import com.sistemadeoperaciones.shared.exception.BusinessException;
import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;
import com.sistemadeoperaciones.socioscomerciales.models.CommercialPartner;
import com.sistemadeoperaciones.socioscomerciales.repository.CommercialPartnerRepository;
import com.sistemadeoperaciones.usuarios.model.CommercialPartnerSettings;
import com.sistemadeoperaciones.usuarios.model.User;
import com.sistemadeoperaciones.usuarios.repository.CommercialPartnerSettingsRepository;
import com.sistemadeoperaciones.usuarios.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import com.sistemadeoperaciones.pagos.repository.specification.PaymentOperationSpecification;
import org.springframework.data.jpa.domain.Specification;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Service
public class PaymentOperationServiceImpl implements PaymentOperationService {

    private final PaymentOperationRepository paymentOperationRepository;
    private final OperationPaymentRepository operationPaymentRepository;
    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final CommercialPartnerCommissionService commercialPartnerCommissionService;
    private final CommercialPartnerSettingsRepository commercialPartnerSettingsRepository;
    private final OperationReturnPaymentRepository operationReturnPaymentRepository;
    private final NotificationService notificationService;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int MONEY_SCALE = 2;
    private final ClientesRepository clientesRepository;
    private final CommercialPartnerRepository commercialPartnerRepository;
    public PaymentOperationServiceImpl(
            PaymentOperationRepository paymentOperationRepository,
            OperationPaymentRepository operationPaymentRepository,
            OperationReturnPaymentRepository operationReturnPaymentRepository,
            BankAccountRepository bankAccountRepository,
            UserRepository userRepository,
            AuthenticatedUserService authenticatedUserService,
            CommercialPartnerCommissionService commercialPartnerCommissionService,
            CommercialPartnerSettingsRepository commercialPartnerSettingsRepository,
            NotificationService notificationService,
            ClientesRepository clientesRepository,
            CommercialPartnerRepository commercialPartnerRepository
    ) {
        this.paymentOperationRepository = paymentOperationRepository;
        this.operationPaymentRepository = operationPaymentRepository;
        this.operationReturnPaymentRepository = operationReturnPaymentRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.userRepository = userRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.commercialPartnerCommissionService = commercialPartnerCommissionService;
        this.commercialPartnerSettingsRepository = commercialPartnerSettingsRepository;
        this.notificationService = notificationService;
        this.clientesRepository = clientesRepository;
        this.commercialPartnerRepository = commercialPartnerRepository;
    }

    @Override
    @Transactional
    public PaymentOperationResponseDto createOperation(
            CreatePaymentOperationRequestDto request
    ) {

        if (
                request.getSocioComercialNivel3Id() != null &&
                        request.getSocioComercialNivel2Id() == null
        ) {
            throw new BusinessException(
                    "No puede existir un socio nivel 3 sin un socio nivel 2"
            );
        }

        User socioComercial = userRepository.findById(
                request.getSocioComercialId()
        ).orElseThrow(() ->
                new ResourceNotFoundException(
                        "Socio comercial no encontrado con id: "
                                + request.getSocioComercialId()
                )
        );

        if (!Boolean.TRUE.equals(socioComercial.getActivo())) {
            throw new PaymentOperationInactivePartnerException();
        }

        CommercialPartner socioNivel2 = null;
        CommercialPartner socioNivel3 = null;

        if (request.getSocioComercialNivel2Id() != null) {

            socioNivel2 = commercialPartnerRepository
                    .findById(request.getSocioComercialNivel2Id())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Socio comercial nivel 2 no encontrado"
                            )
                    );

            if (!Boolean.TRUE.equals(socioNivel2.getActivo())) {
                throw new BusinessException(
                        "El socio comercial nivel 2 está inactivo"
                );
            }
        }

        if (request.getSocioComercialNivel3Id() != null) {

            socioNivel3 = commercialPartnerRepository
                    .findById(request.getSocioComercialNivel3Id())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Socio comercial nivel 3 no encontrado"
                            )
                    );

            if (!Boolean.TRUE.equals(socioNivel3.getActivo())) {
                throw new BusinessException(
                        "El socio comercial nivel 3 está inactivo"
                );
            }
        }

        validateCommercialPartnersOwnership(
                socioComercial,
                socioNivel2,
                socioNivel3
        );

        Clientes cliente = clientesRepository.findById(
                request.getClienteId()
        ).orElseThrow(() ->
                new ClienteNotFoundException(
                        request.getClienteId()
                )
        );

        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new BusinessException(
                    "El cliente seleccionado está inactivo"
            );
        }


        boolean isAllowedUser = socioComercial.getRoles()
                .stream()
                .anyMatch(role ->
                        role.getName() == RoleName.SOCIO_COMERCIAL
                                || role.getName() == RoleName.ADMIN
                );

        if (!isAllowedUser) {
            throw new BusinessException(
                    "El usuario seleccionado debe tener el rol SOCIO_COMERCIAL o ADMIN"
            );
        }

        CommercialPartnerSettings settings =
                commercialPartnerSettingsRepository
                        .findByUsuarioId(socioComercial.getId())
                        .orElseThrow(() ->
                                new BusinessException(
                                        "El socio comercial no tiene configuración de comisión registrada"
                                )
                        );

        PaymentOperation operation = new PaymentOperation();

        operation.setCliente(cliente);
        operation.setMontoTotal(request.getMontoTotal());
        operation.setMontoValidado(BigDecimal.ZERO);
        operation.setEstatus(OperationStatus.PENDIENTE_VALIDACION);

        operation.setSocioComercial(socioComercial);
        operation.setSocioComercialNivel2(socioNivel2);
        operation.setSocioComercialNivel3(socioNivel3);

        operation.setNivelesRedComercial(
                cliente.getNivelesRedComercial()
        );

        operation.setPorcentajeComisionAplicado(
                cliente.getPorcentajeComisionAplicado()
        );

        operation.setPorcentajeComisionOficina(
                new BigDecimal("1.5")
        );

        operation.setObservaciones(
                request.getObservaciones()
        );

        PaymentOperation saved =
                paymentOperationRepository.save(operation);

        return mapToOperationResponse(saved);
    }

    @Override
    @Transactional
    public PaymentOperationResponseDto updateOperation(
            Long operationId,
            UpdatePaymentOperationRequestDto request
    ) {

        PaymentOperation operation = paymentOperationRepository.findById(operationId)
                .orElseThrow(() ->
                        new PaymentOperationNotFoundException(operationId)
                );

        Clientes cliente = clientesRepository.findById(
                request.getClienteId()
        ).orElseThrow(() ->
                new ClienteNotFoundException(
                        request.getClienteId()
                )
        );

        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new BusinessException(
                    "El cliente seleccionado está inactivo"
            );
        }

        if (
                request.getSocioComercialNivel3Id() != null &&
                        request.getSocioComercialNivel2Id() == null
        ) {
            throw new BusinessException(
                    "No puede existir un socio nivel 3 sin un socio nivel 2"
            );
        }

        User socioComercial = userRepository.findById(
                request.getSocioComercialId()
        ).orElseThrow(() ->
                new ResourceNotFoundException(
                        "Socio comercial no encontrado con id: "
                                + request.getSocioComercialId()
                )
        );

        if (!Boolean.TRUE.equals(socioComercial.getActivo())) {
            throw new PaymentOperationInactivePartnerException();
        }

        boolean isAllowedUser = socioComercial.getRoles()
                .stream()
                .anyMatch(role ->
                        role.getName() == RoleName.SOCIO_COMERCIAL
                                || role.getName() == RoleName.ADMIN
                );

        if (!isAllowedUser) {
            throw new BusinessException(
                    "El usuario seleccionado debe tener el rol SOCIO_COMERCIAL o ADMIN"
            );
        }

        CommercialPartner socioNivel2 = null;
        CommercialPartner socioNivel3 = null;

        if (request.getSocioComercialNivel2Id() != null) {

            socioNivel2 = commercialPartnerRepository
                    .findById(request.getSocioComercialNivel2Id())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Socio comercial nivel 2 no encontrado"
                            )
                    );

            if (!Boolean.TRUE.equals(socioNivel2.getActivo())) {
                throw new BusinessException(
                        "El socio comercial nivel 2 está inactivo"
                );
            }
        }

        if (request.getSocioComercialNivel3Id() != null) {

            socioNivel3 = commercialPartnerRepository
                    .findById(request.getSocioComercialNivel3Id())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Socio comercial nivel 3 no encontrado"
                            )
                    );

            if (!Boolean.TRUE.equals(socioNivel3.getActivo())) {
                throw new BusinessException(
                        "El socio comercial nivel 3 está inactivo"
                );
            }


        }

        validateCommercialPartnersOwnership(
                socioComercial,
                socioNivel2,
                socioNivel3
        );

        List<OperationPayment> payments =
                operationPaymentRepository.findByOperacionId(operationId);

        BigDecimal totalPagosNoRechazados = payments.stream()
                .filter(payment ->
                        payment.getEstatus() != PaymentStatus.RECHAZADA
                )
                .map(OperationPayment::getMonto)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (
                request.getMontoTotal()
                        .compareTo(totalPagosNoRechazados) < 0
        ) {
            throw new BusinessException(
                    "El monto total no puede ser menor al total de pagos registrados no rechazados"
            );
        }

        operation.setCliente(cliente);
        operation.setMontoTotal(request.getMontoTotal());

        operation.setSocioComercial(socioComercial);
        operation.setSocioComercialNivel2(socioNivel2);
        operation.setSocioComercialNivel3(socioNivel3);

        operation.setNivelesRedComercial(
                cliente.getNivelesRedComercial()
        );

        operation.setPorcentajeComisionAplicado(
                cliente.getPorcentajeComisionAplicado()
        );

        operation.setPorcentajeComisionOficina(
                new BigDecimal("1.5")
        );

        operation.setObservaciones(
                request.getObservaciones()
        );

        recalculateOperation(operation);

        PaymentOperation updated =
                paymentOperationRepository.save(operation);

        return mapToOperationResponse(updated);
    }
    @Override
    @Transactional
    public OperationPaymentResponseDto addPayment(CreateOperationPaymentRequestDto request) {

        PaymentOperation operation = paymentOperationRepository.findById(request.getOperacionId())
                .orElseThrow(() -> new PaymentOperationNotFoundException(request.getOperacionId()));

        User registradoPor = authenticatedUserService.getCurrentUser();

        BankAccount cuentaDestino = null;

        if (request.getTipoPago() != PaymentType.EFECTIVO) {
            cuentaDestino = bankAccountRepository.findById(request.getCuentaDestinoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Cuenta bancaria no encontrada con id: " + request.getCuentaDestinoId()
                    ));

            if (!Boolean.TRUE.equals(cuentaDestino.getActivo())) {
                throw new PaymentOperationInactiveAccountException();
            }
        }

        validateOperationCanReceivePayments(operation);
        validateCurrentUserOwnsOperation(operation);
        validateDuplicateReceipt(request.getOperacionId(), request.getComprobanteUrl());
        validatePaymentAmountDoesNotExceedOperation(operation, request.getMonto());

        OperationPayment payment = new OperationPayment();
        payment.setOperacion(operation);
        payment.setMonto(request.getMonto());
        payment.setTipoPago(request.getTipoPago());
        payment.setCuentaDestino(cuentaDestino);
        payment.setComprobanteUrl(request.getComprobanteUrl());
        payment.setEstatus(PaymentStatus.PENDIENTE_VALIDACION);
        payment.setObservaciones(request.getObservaciones());
        payment.setRegistradoPor(registradoPor);
        payment.setFechaPago(LocalDateTime.now());
        payment.setFechaComprobante(request.getFechaComprobante());

        OperationPayment saved = operationPaymentRepository.save(payment);

        recalculateOperation(operation);
        notifyPaymentSubmitted(operation, saved);
        return mapToPaymentResponse(saved);
    }

    @Override
    @Transactional
    public OperationPaymentResponseDto updatePayment(
            Long paymentId,
            UpdateOperationPaymentRequestDto request
    ) {
        OperationPayment payment = operationPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new OperationPaymentNotFoundException(paymentId));

        if (payment.getEstatus() != PaymentStatus.PENDIENTE_VALIDACION) {
            throw new InvalidPaymentStatusException(
                    "Solo se pueden editar comprobantes en estatus PENDIENTE_VALIDACION"
            );
        }

        PaymentOperation operation = payment.getOperacion();

        validateOperationCanReceivePayments(operation);
        validateCurrentUserOwnsOperation(operation);

        BankAccount cuentaDestino = null;

        if (request.getTipoPago() != PaymentType.EFECTIVO) {
            cuentaDestino = bankAccountRepository.findById(request.getCuentaDestinoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Cuenta bancaria no encontrada con id: " + request.getCuentaDestinoId()
                    ));

            if (!Boolean.TRUE.equals(cuentaDestino.getActivo())) {
                throw new PaymentOperationInactiveAccountException();
            }
        }

        validateUpdatedPaymentAmountDoesNotExceedOperation(
                operation,
                payment,
                request.getMonto()
        );

        validateUpdatedDuplicateReceipt(
                operation.getId(),
                payment.getId(),
                request.getComprobanteUrl()
        );

        payment.setMonto(request.getMonto());
        payment.setTipoPago(request.getTipoPago());
        payment.setCuentaDestino(cuentaDestino);
        payment.setComprobanteUrl(request.getComprobanteUrl());
        payment.setFechaPago(LocalDateTime.now());
        payment.setObservaciones(request.getObservaciones());
        payment.setFechaComprobante(request.getFechaComprobante());

        OperationPayment updated = operationPaymentRepository.save(payment);

        recalculateOperation(operation);

        return mapToPaymentResponse(updated);
    }

    private void validateUpdatedPaymentAmountDoesNotExceedOperation(
            PaymentOperation operation,
            OperationPayment paymentToUpdate,
            BigDecimal newAmount
    ) {
        List<OperationPayment> existingPayments =
                operationPaymentRepository.findByOperacionId(operation.getId());

        BigDecimal accumulated = existingPayments.stream()
                .filter(payment -> payment.getEstatus() != PaymentStatus.RECHAZADA)
                .filter(payment -> !payment.getId().equals(paymentToUpdate.getId()))
                .map(OperationPayment::getMonto)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal resultingTotal = accumulated.add(safe(newAmount));

        if (resultingTotal.compareTo(safe(operation.getMontoTotal())) > 0) {
            throw new PaymentAmountExceededException();
        }
    }
    private void validateUpdatedDuplicateReceipt(
            Long operationId,
            Long paymentId,
            String comprobanteUrl
    ) {
        List<OperationPayment> existingPayments =
                operationPaymentRepository.findByOperacionId(operationId);

        boolean duplicated = existingPayments.stream()
                .filter(payment -> !payment.getId().equals(paymentId))
                .filter(payment -> payment.getEstatus() != PaymentStatus.RECHAZADA)
                .anyMatch(payment -> payment.getComprobanteUrl() != null
                        && payment.getComprobanteUrl().equals(comprobanteUrl));

        if (duplicated) {
            throw new DuplicatePaymentReceiptException();
        }
    }

    @Override
    @Transactional
    public OperationPaymentResponseDto validatePayment(Long paymentId, UpdatePaymentStatusRequestDto request) {
        OperationPayment payment = operationPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new OperationPaymentNotFoundException(paymentId));

        if (payment.getEstatus() != PaymentStatus.PENDIENTE_VALIDACION) {
            throw new InvalidPaymentStatusException(
                    "Solo se pueden validar comprobantes en estatus PENDIENTE_VALIDACION");
        }

        if (payment.getComprobanteUrl() == null || payment.getComprobanteUrl().isBlank()) {
            throw new MissingPaymentReceiptException();
        }

        User validadoPor = authenticatedUserService.getCurrentUser();
        validateCurrentUserCanValidatePayments();

        payment.setEstatus(PaymentStatus.VALIDADA);
        payment.setValidadoPor(validadoPor);
        payment.setFechaValidacion(LocalDateTime.now());
        payment.setComprobanteValidacionUrl(request.getComprobanteValidacionUrl());

        if (request.getObservaciones() != null && !request.getObservaciones().isBlank()) {
            payment.setObservaciones(request.getObservaciones());
        }

        OperationPayment updated = operationPaymentRepository.save(payment);
        recalculateOperation(payment.getOperacion());
        PaymentOperation operation =
                paymentOperationRepository
                        .findById(payment.getOperacion().getId())
                        .orElseThrow();

        if (operation.getEstatus() == OperationStatus.VALIDADA) {

            commercialPartnerCommissionService
                    .generateCommissionsForOperation(
                            operation.getId()
                    );
        }
        notifyPaymentValidated(updated);

        return mapToPaymentResponse(updated);
    }

    @Override
    @Transactional
    public OperationPaymentResponseDto rejectPayment(Long paymentId, UpdatePaymentStatusRequestDto request) {
        OperationPayment payment = operationPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new OperationPaymentNotFoundException(paymentId));

        if (payment.getEstatus() != PaymentStatus.PENDIENTE_VALIDACION) {
            throw new InvalidPaymentStatusException(
                    "Solo se pueden rechazar comprobantes en estatus PENDIENTE_VALIDACION");
        }

        if (request.getObservaciones() == null || request.getObservaciones().isBlank()) {
            throw new MissingRejectionReasonException();
        }

        User validadoPor = authenticatedUserService.getCurrentUser();
        validateCurrentUserCanValidatePayments();

        payment.setEstatus(PaymentStatus.RECHAZADA);
        payment.setValidadoPor(validadoPor);
        payment.setFechaValidacion(LocalDateTime.now());
        payment.setObservaciones(request.getObservaciones());

        OperationPayment updated = operationPaymentRepository.save(payment);
        recalculateOperation(payment.getOperacion());
        notifyPaymentRejected(updated);

        return mapToPaymentResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentOperationResponseDto findById(Long id) {
        PaymentOperation operation = paymentOperationRepository.findById(id)
                .orElseThrow(() -> new PaymentOperationNotFoundException(id));

        return mapToOperationResponse(operation);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentOperationResponseDto> findAll(PaymentOperationFilterDto filter, Pageable pageable) {
        LocalDate startDate = resolveStartDate(filter);
        LocalDate endDate = resolveEndDate(filter);

        Specification<PaymentOperation> specification = Specification
                .where(PaymentOperationSpecification.clienteONombreSocioOIdContains(filter.getSearch()))
                .and(PaymentOperationSpecification.hasStatus(filter.getStatus()))
                .and(PaymentOperationSpecification.hasSocioComercialId(filter.getSocioComercialId()))
                .and(PaymentOperationSpecification.createdAtBetween(
                        toStartOfDay(startDate),
                        toEndOfDay(endDate)
                ));

        return paymentOperationRepository.findAll(specification, pageable)
                .map(this::mapToOperationResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentOperationResponseDto> findAllBySocioComercialId(
            Long socioComercialId,
            PaymentOperationFilterDto filter,
            Pageable pageable
    ) {
        User socioComercial = userRepository.findById(socioComercialId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Socio comercial no encontrado con id: " + socioComercialId));

        boolean isSocioComercial = socioComercial.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.SOCIO_COMERCIAL);

        if (!isSocioComercial) {
            throw new BusinessException("El usuario indicado no tiene el rol SOCIO_COMERCIAL");
        }

        LocalDate startDate = resolveStartDate(filter);
        LocalDate endDate = resolveEndDate(filter);

        Specification<PaymentOperation> specification = Specification
                .where(PaymentOperationSpecification.clienteONombreSocioOIdContains(filter.getSearch()))
                .and(PaymentOperationSpecification.hasStatus(filter.getStatus()))
                .and(PaymentOperationSpecification.hasSocioComercialId(socioComercialId))
                .and(PaymentOperationSpecification.createdAtBetween(
                        toStartOfDay(startDate),
                        toEndOfDay(endDate)
                ));

        return paymentOperationRepository.findAll(specification, pageable)
                .map(this::mapToOperationResponse);
    }
    @Override
    @Transactional(readOnly = true)
    public Page<PaymentOperationResponseDto> findMyOperations(
            PaymentOperationFilterDto filter,
            Pageable pageable
    ) {
        User currentUser = authenticatedUserService.getCurrentUser();

        boolean isSocioComercial = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.SOCIO_COMERCIAL);

        if (!isSocioComercial) {
            throw new BusinessException("El usuario autenticado no tiene el rol SOCIO_COMERCIAL");
        }

        LocalDate startDate = resolveStartDate(filter);
        LocalDate endDate = resolveEndDate(filter);

        Specification<PaymentOperation> specification = Specification
                .where(PaymentOperationSpecification.clienteONombreSocioOIdContains(filter.getSearch()))
                .and(PaymentOperationSpecification.hasStatus(filter.getStatus()))
                .and(PaymentOperationSpecification.hasSocioComercialId(currentUser.getId()))
                .and(PaymentOperationSpecification.createdAtBetween(
                        toStartOfDay(startDate),
                        toEndOfDay(endDate)
                ));

        return paymentOperationRepository.findAll(specification, pageable)
                .map(this::mapToOperationResponse);
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
    @Transactional(readOnly = true)
    public List<String> findFrequentClientNames() {
        return paymentOperationRepository.findMostFrequentClientNames(PageRequest.of(0, 10));
    }

    @Override
    @Transactional
    public PaymentOperationResponseDto markAsInvoiced(Long operationId) {
        PaymentOperation operation = paymentOperationRepository.findById(operationId)
                .orElseThrow(() -> new PaymentOperationNotFoundException(operationId));

        validateCurrentUserCanMarkAsInvoiced(operation);

        if (operation.getEstatus() != OperationStatus.VALIDADA) {
            throw new BusinessException(
                    "Solo se puede marcar como facturada una operación con estatus VALIDADA"
            );
        }

        if (safe(operation.getMontoValidado()).compareTo(safe(operation.getMontoTotal())) != 0) {
            throw new BusinessException(
                    "La operación no puede facturarse porque aún no está completamente validada"
            );
        }

        //operation.setEstatus(OperationStatus.FACTURADA);

        PaymentOperation updated = paymentOperationRepository.save(operation);

        return mapToOperationResponse(updated);
    }

    private void validateCurrentUserCanMarkAsInvoiced(PaymentOperation operation) {
        User currentUser = authenticatedUserService.getCurrentUser();

        boolean allowed = currentUser.getRoles().stream()
                .anyMatch(role ->
                        role.getName() == RoleName.ADMIN
                                || role.getName() == RoleName.GERENTE
                                || role.getName() == RoleName.AUXILIAR_CUENTAS
                );

        if (!allowed) {
            throw new BusinessException(
                    "El usuario autenticado no tiene permiso para marcar operaciones como facturadas"
            );
        }
    }

    private void validateOperationCanReceivePayments(PaymentOperation operation) {
        if (operation.getEstatus() == OperationStatus.RECHAZADA) {
            throw new OperationDoesNotAcceptPaymentsException(
                    "No se pueden agregar comprobantes a una operación rechazada");
        }

        if (operation.getEstatus() == OperationStatus.VALIDADA
                || operation.getEstatus() == OperationStatus.RETORNO_PARCIAL
                || operation.getEstatus() == OperationStatus.COMPLETADA) {
            throw new OperationDoesNotAcceptPaymentsException(
                    "La operación ya no admite nuevos comprobantes en su estatus actual");
        }
    }

    private void validateCurrentUserOwnsOperation(PaymentOperation operation) {
        User currentUser = authenticatedUserService.getCurrentUser();

        if (operation.getSocioComercial() == null) {
            throw new BusinessException("La operación no tiene un socio comercial asociado");
        }

        Long ownerUserId = operation.getSocioComercial().getId();

        if (!ownerUserId.equals(currentUser.getId())) {
            throw new BusinessException(
                    "No tienes permiso para registrar comprobantes en una operación que no pertenece a tu socio comercial");
        }
    }

    private void validateCurrentUserCanValidatePayments() {
        User currentUser = authenticatedUserService.getCurrentUser();

        boolean allowed = currentUser.getRoles().stream()
                .anyMatch(role ->
                        role.getName() == RoleName.JEFA_CAJAS
                                || role.getName() == RoleName.GERENTE
                                || role.getName() == RoleName.ADMIN
                );

        if (!allowed) {
            throw new BusinessException("El usuario autenticado no tiene permiso para validar o rechazar comprobantes");
        }
    }

    private void validateDuplicateReceipt(Long operacionId, String comprobanteUrl) {
        boolean duplicated = operationPaymentRepository.existsByOperacionIdAndComprobanteUrlAndEstatusNot(
                operacionId,
                comprobanteUrl,
                PaymentStatus.RECHAZADA
        );

        if (duplicated) {
            throw new DuplicatePaymentReceiptException();
        }
    }

    private void validatePaymentAmountDoesNotExceedOperation(PaymentOperation operation, BigDecimal newPaymentAmount) {
        List<OperationPayment> existingPayments = operationPaymentRepository.findByOperacionId(operation.getId());

        BigDecimal accumulated = existingPayments.stream()
                .filter(payment -> payment.getEstatus() != PaymentStatus.RECHAZADA)
                .map(OperationPayment::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal resultingTotal = accumulated.add(newPaymentAmount);

        if (resultingTotal.compareTo(operation.getMontoTotal()) > 0) {
            throw new PaymentAmountExceededException();
        }
    }

    private void recalculateOperation(PaymentOperation operation) {
        List<OperationPayment> validatedPayments =
                operationPaymentRepository.findByOperacionIdAndEstatus(operation.getId(), PaymentStatus.VALIDADA);

        BigDecimal totalValidated = validatedPayments.stream()
                .map(OperationPayment::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal saldoPendiente = operation.getMontoTotal().subtract(totalValidated);

        operation.setMontoValidado(totalValidated);
        //operation.setSaldoPendiente(saldoPendiente);

        if (totalValidated.compareTo(BigDecimal.ZERO) == 0) {
            boolean hasRejected = operationPaymentRepository.existsByOperacionIdAndEstatus(
                    operation.getId(), PaymentStatus.RECHAZADA);

            boolean hasPending = operationPaymentRepository.existsByOperacionIdAndEstatus(
                    operation.getId(), PaymentStatus.PENDIENTE_VALIDACION);

            operation.setEstatus(OperationStatus.PENDIENTE_VALIDACION);
        } else if (totalValidated.compareTo(operation.getMontoTotal()) < 0) {
            operation.setEstatus(OperationStatus.INGRESO_PARCIAL);
        } else if (totalValidated.compareTo(operation.getMontoTotal()) == 0) {
            operation.setEstatus(OperationStatus.VALIDADA);
        }

        paymentOperationRepository.save(operation);
    }

    private void notifyPaymentSubmitted(PaymentOperation operation, OperationPayment payment) {
        notificationService.createForRoles(
                List.of(RoleName.JEFA_CAJAS, RoleName.GERENTE, RoleName.ADMIN),
                "Nuevo comprobante pendiente de validación",
                "Se registró un nuevo comprobante de pago para la operación #" + operation.getId()
                        + " del cliente " + operation.getCliente().getNombre() + ".",
                NotificationType.PAYMENT_SUBMITTED,
                NotificationModule.PAGOS,
                NotificationReferenceType.PAYMENT_OPERATION,
                operation.getId(),
                "/operaciones/" + operation.getId() + "?scrollToPayments=true",
                NotificationPriority.HIGH
        );
    }

    private void notifyPaymentValidated(OperationPayment payment) {
        PaymentOperation operation = payment.getOperacion();

        if (operation.getSocioComercial() == null) {
            return;
        }

        notificationService.createForUser(
                operation.getSocioComercial().getId(),
                "Comprobante validado",
                "Tu comprobante de la operación #" + operation.getId()
                        + " fue validado correctamente.",
                NotificationType.PAYMENT_VALIDATED,
                NotificationModule.PAGOS,
                NotificationReferenceType.OPERATION_PAYMENT,
                payment.getId(),
                "/operaciones/" + operation.getId() + "?scrollToPayments=true",
                NotificationPriority.MEDIUM
        );
    }

    private void notifyPaymentRejected(OperationPayment payment) {
        PaymentOperation operation = payment.getOperacion();

        if (operation.getSocioComercial() == null) {
            return;
        }

        notificationService.createForUser(
                operation.getSocioComercial().getId(),
                "Comprobante rechazado",
                "Tu comprobante de la operación #" + operation.getId()
                        + " fue rechazado. Revisa las observaciones para más detalle.",
                NotificationType.PAYMENT_REJECTED,
                NotificationModule.PAGOS,
                NotificationReferenceType.OPERATION_PAYMENT,
                payment.getId(),
                "/operaciones/" + operation.getId() + "?scrollToPayments=true",
                NotificationPriority.HIGH
        );
    }

    private PaymentOperationResponseDto mapToOperationResponse(PaymentOperation operation) {
        PaymentOperationResponseDto dto = new PaymentOperationResponseDto();

        List<OperationPayment> operationPayments =
                operationPaymentRepository.findByOperacionId(operation.getId());

        List<OperationPaymentResponseDto> payments = operationPayments
                .stream()
                .map(this::mapToPaymentResponse)
                .toList();

        BigDecimal montoTotal = safe(operation.getMontoTotal());

        BigDecimal totalPagosRegistrados = operationPayments.stream()
                .filter(payment -> payment.getEstatus() != PaymentStatus.RECHAZADA)
                .map(OperationPayment::getMonto)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal saldoPendientePorRegistrar = montoTotal.subtract(totalPagosRegistrados);

        if (saldoPendientePorRegistrar.compareTo(BigDecimal.ZERO) < 0) {
            saldoPendientePorRegistrar = BigDecimal.ZERO;
        }

        dto.setId(operation.getId());

        if (operation.getCliente() != null) {
            dto.setClienteId(operation.getCliente().getId());
            dto.setClienteNombre(operation.getCliente().getNombre());
        }

        BigDecimal saldoPendientePorValidar = montoTotal.subtract(safe(operation.getMontoValidado()));

        if (saldoPendientePorValidar.compareTo(BigDecimal.ZERO) < 0) {
            saldoPendientePorValidar = BigDecimal.ZERO;
        }

        dto.setMontoTotal(operation.getMontoTotal());
        dto.setMontoValidado(operation.getMontoValidado());
        dto.setMontoRegistrado(totalPagosRegistrados);
        dto.setSaldoPendientePorValidar(saldoPendientePorValidar);
        dto.setSaldoPendientePorRegistrar(saldoPendientePorRegistrar);
        dto.setEstatus(operation.getEstatus());

        if (operation.getSocioComercial() != null) {
            dto.setSocioComercialId(operation.getSocioComercial().getId());
            dto.setSocioComercialNombre(operation.getSocioComercial().getNombre());
        }

        if (operation.getSocioComercialNivel2() != null) {

            dto.setSocioComercialNivel2Id(
                    operation.getSocioComercialNivel2().getId()
            );

            dto.setSocioComercialNivel2Nombre(
                    operation.getSocioComercialNivel2().getNombre()
            );
        }

        if (operation.getSocioComercialNivel3() != null) {

            dto.setSocioComercialNivel3Id(
                    operation.getSocioComercialNivel3().getId()
            );

            dto.setSocioComercialNivel3Nombre(
                    operation.getSocioComercialNivel3().getNombre()
            );
        }

        dto.setNivelesRedComercial(operation.getNivelesRedComercial());
        dto.setPorcentajeComisionAplicado(operation.getPorcentajeComisionAplicado());
        dto.setPorcentajeComisionOficina(operation.getPorcentajeComisionOficina());

        BigDecimal porcentajeComisionRedTotal = calculateTotalPercentageByLevels(
                operation.getPorcentajeComisionAplicado(),
                operation.getNivelesRedComercial()
        );

        BigDecimal montoComisionRedTotal = calculateAmountFromPercentage(
                montoTotal,
                porcentajeComisionRedTotal
        );

        BigDecimal porcentajeComisionOficina = operation.getPorcentajeComisionOficina();

        BigDecimal montoComisionOficinaTotal = calculateAmountFromPercentage(
                montoTotal,
                porcentajeComisionOficina
        );

        BigDecimal montoTotalDevolverCliente = montoTotal
                .subtract(montoComisionRedTotal)
                .subtract(montoComisionOficinaTotal)
                .setScale(2, RoundingMode.HALF_UP);

        dto.setPorcentajeComisionRedTotal(porcentajeComisionRedTotal);
        dto.setMontoComisionRedTotal(montoComisionRedTotal);
        dto.setPorcentajeComisionOficinaTotal(porcentajeComisionOficina);
        dto.setMontoComisionOficinaTotal(montoComisionOficinaTotal);
        dto.setMontoTotalDevolverCliente(montoTotalDevolverCliente);

        BigDecimal montoRetornado = safe(
                operationReturnPaymentRepository
                        .sumRealizedAmountByOperationId(operation.getId())
        );

        BigDecimal montoSolicitadoRetorno = safe(
                operationReturnPaymentRepository
                        .sumRequestedAmountByOperationId(operation.getId())
        );

        BigDecimal saldoPendienteRetornar = montoTotalDevolverCliente
                .subtract(montoRetornado)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        dto.setMontoSolicitadoRetorno(montoSolicitadoRetorno);
        dto.setMontoRetornado(montoRetornado);
        dto.setSaldoPendienteRetornar(saldoPendienteRetornar);

        dto.setObservaciones(operation.getObservaciones());
        dto.setPagos(payments);
        dto.setCreatedAt(operation.getCreatedAt());
        dto.setUpdatedAt(operation.getUpdatedAt());

        return dto;
    }


    private void validateCommercialPartnersOwnership(
            User socioComercial,
            CommercialPartner socioNivel2,
            CommercialPartner socioNivel3
    ) {

        if (
                socioComercial.getRoles()
                        .stream()
                        .anyMatch(role -> role.getName() == RoleName.ADMIN)
        ) {
            return;
        }

        if (
                socioNivel2 != null &&
                        !socioNivel2.getSocioComercial().getId()
                                .equals(socioComercial.getId())
        ) {
            throw new BusinessException(
                    "El socio comercial nivel 2 no pertenece al socio comercial de la operación"

            );
        }

        if (
                socioNivel3 != null &&
                        !socioNivel3.getSocioComercial().getId()
                                .equals(socioComercial.getId())
        ) {
            throw new BusinessException(
                    "El socio comercial nivel 3 no pertenece al socio comercial seleccionado"
            );
        }
    }

    private OperationPaymentResponseDto mapToPaymentResponse(OperationPayment payment) {
        Long validadoPorId = payment.getValidadoPor() != null ? payment.getValidadoPor().getId() : null;
        String validadoPorNombre = payment.getValidadoPor() != null ? payment.getValidadoPor().getNombre() : null;

        Long cuentaDestinoId = payment.getCuentaDestino() != null ? payment.getCuentaDestino().getId() : null;
        String cuentaDestinoBanco = payment.getCuentaDestino() != null ? payment.getCuentaDestino().getBanco() : null;
        String cuentaDestinoTitular = payment.getCuentaDestino() != null ? payment.getCuentaDestino().getTitular() : null;

        return new OperationPaymentResponseDto(
                payment.getId(),
                payment.getMonto(),
                payment.getTipoPago(),
                payment.getComprobanteUrl(),
                payment.getComprobanteValidacionUrl(),
                cuentaDestinoId,
                cuentaDestinoBanco,
                cuentaDestinoTitular,
                payment.getEstatus(),
                payment.getObservaciones(),
                payment.getRegistradoPor().getId(),
                payment.getRegistradoPor().getNombre(),
                validadoPorId,
                validadoPorNombre,
                payment.getFechaPago(),
                payment.getFechaValidacion(),
                payment.getFechaComprobante(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal calculateTotalPercentageByLevels(BigDecimal percentagePerLevel, Integer levels) {
        BigDecimal percentage = safe(percentagePerLevel);
        int niveles = levels != null ? levels : 0;

        return percentage.multiply(BigDecimal.valueOf(niveles))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAmountFromPercentage(BigDecimal baseAmount, BigDecimal percentage) {
        return safe(baseAmount)
                .multiply(safe(percentage))
                .divide(ONE_HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
