package com.sistemadeoperaciones.pagos.service;

import com.sistemadeoperaciones.auth.models.User;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.cuentasbancarias.repository.BankAccountRepository;
import com.sistemadeoperaciones.pagos.dto.CreateOperationPaymentRequestDto;
import com.sistemadeoperaciones.pagos.dto.CreatePaymentOperationRequestDto;
import com.sistemadeoperaciones.pagos.dto.OperationPaymentResponseDto;
import com.sistemadeoperaciones.pagos.dto.PaymentOperationResponseDto;
import com.sistemadeoperaciones.pagos.dto.UpdatePaymentStatusRequestDto;
import com.sistemadeoperaciones.pagos.enums.OperationStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentStatus;
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
import com.sistemadeoperaciones.pagos.repository.PaymentOperationRepository;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.exception.BusinessException;
import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;
import com.sistemadeoperaciones.socioscomerciales.models.CommercialPartner;
import com.sistemadeoperaciones.socioscomerciales.repository.CommercialPartnerRepository;
import com.sistemadeoperaciones.usuarios.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentOperationServiceImpl implements PaymentOperationService {

    private final PaymentOperationRepository paymentOperationRepository;
    private final OperationPaymentRepository operationPaymentRepository;
    private final BankAccountRepository bankAccountRepository;
    private final CommercialPartnerRepository commercialPartnerRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public PaymentOperationServiceImpl(
            PaymentOperationRepository paymentOperationRepository,
            OperationPaymentRepository operationPaymentRepository,
            BankAccountRepository bankAccountRepository,
            CommercialPartnerRepository commercialPartnerRepository,
            UserRepository userRepository,
            AuthenticatedUserService authenticatedUserService
    ) {
        this.paymentOperationRepository = paymentOperationRepository;
        this.operationPaymentRepository = operationPaymentRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.commercialPartnerRepository = commercialPartnerRepository;
        this.userRepository = userRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Override
    @Transactional
    public PaymentOperationResponseDto createOperation(CreatePaymentOperationRequestDto request) {
        BankAccount cuentaDestino = bankAccountRepository.findById(request.getCuentaDestinoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cuenta bancaria no encontrada con id: " + request.getCuentaDestinoId()));

        CommercialPartner socioComercial = commercialPartnerRepository.findById(request.getSocioComercialId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Socio comercial no encontrado con id: " + request.getSocioComercialId()));

        if (!Boolean.TRUE.equals(cuentaDestino.getActivo())) {
            throw new PaymentOperationInactiveAccountException();
        }

        if (!Boolean.TRUE.equals(socioComercial.getActivo())) {
            throw new PaymentOperationInactivePartnerException();
        }

        PaymentOperation operation = new PaymentOperation();
        operation.setClienteNombre(request.getClienteNombre());
        operation.setClienteTelefono(request.getClienteTelefono());
        operation.setMontoTotal(request.getMontoTotal());
        operation.setMontoValidado(BigDecimal.ZERO);
        operation.setSaldoPendiente(request.getMontoTotal());
        operation.setEstatus(OperationStatus.PENDIENTE_VALIDACION);
        operation.setCuentaDestino(cuentaDestino);
        operation.setSocioComercial(socioComercial);
        operation.setObservaciones(request.getObservaciones());

        PaymentOperation saved = paymentOperationRepository.save(operation);
        return mapToOperationResponse(saved);
    }

    @Override
    @Transactional
    public OperationPaymentResponseDto addPayment(CreateOperationPaymentRequestDto request) {
        PaymentOperation operation = paymentOperationRepository.findById(request.getOperacionId())
                .orElseThrow(() -> new PaymentOperationNotFoundException(request.getOperacionId()));

        User registradoPor = authenticatedUserService.getCurrentUser();

        validateOperationCanReceivePayments(operation);
        validateCurrentUserOwnsOperation(operation);
        validateDuplicateReceipt(request.getOperacionId(), request.getComprobanteUrl());
        validatePaymentAmountDoesNotExceedOperation(operation, request.getMonto());

        OperationPayment payment = new OperationPayment();
        payment.setOperacion(operation);
        payment.setMonto(request.getMonto());
        payment.setTipoPago(request.getTipoPago());
        payment.setComprobanteUrl(request.getComprobanteUrl());
        payment.setEstatus(PaymentStatus.PENDIENTE_VALIDACION);
        payment.setObservaciones(request.getObservaciones());
        payment.setRegistradoPor(registradoPor);
        payment.setFechaPago(LocalDateTime.now());

        OperationPayment saved = operationPaymentRepository.save(payment);

        recalculateOperation(operation);
        return mapToPaymentResponse(saved);
    }

    @Override
    @Transactional
    public OperationPaymentResponseDto validatePayment(Long paymentId, UpdatePaymentStatusRequestDto request) {
        OperationPayment payment = operationPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new OperationPaymentNotFoundException(paymentId));

        if (payment.getEstatus() != PaymentStatus.PENDIENTE_VALIDACION) {
            throw new InvalidPaymentStatusException(
                    "Solo se pueden validar pagos en estatus PENDIENTE_VALIDACION");
        }

        if (payment.getComprobanteUrl() == null || payment.getComprobanteUrl().isBlank()) {
            throw new MissingPaymentReceiptException();
        }

        User validadoPor = authenticatedUserService.getCurrentUser();
        validateCurrentUserCanValidatePayments();
        payment.setEstatus(PaymentStatus.VALIDADO);
        payment.setValidadoPor(validadoPor);
        payment.setFechaValidacion(LocalDateTime.now());

        if (request.getObservaciones() != null && !request.getObservaciones().isBlank()) {
            payment.setObservaciones(request.getObservaciones());
        }

        OperationPayment updated = operationPaymentRepository.save(payment);
        recalculateOperation(payment.getOperacion());

        return mapToPaymentResponse(updated);
    }

    @Override
    @Transactional
    public OperationPaymentResponseDto rejectPayment(Long paymentId, UpdatePaymentStatusRequestDto request) {
        OperationPayment payment = operationPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new OperationPaymentNotFoundException(paymentId));

        if (payment.getEstatus() != PaymentStatus.PENDIENTE_VALIDACION) {
            throw new InvalidPaymentStatusException(
                    "Solo se pueden rechazar pagos en estatus PENDIENTE_VALIDACION");
        }

        if (request.getObservaciones() == null || request.getObservaciones().isBlank()) {
            throw new MissingRejectionReasonException();
        }

        User validadoPor = authenticatedUserService.getCurrentUser();
        validateCurrentUserCanValidatePayments();
        payment.setEstatus(PaymentStatus.RECHAZADO);
        payment.setValidadoPor(validadoPor);
        payment.setFechaValidacion(LocalDateTime.now());
        payment.setObservaciones(request.getObservaciones());

        OperationPayment updated = operationPaymentRepository.save(payment);
        recalculateOperation(payment.getOperacion());

        return mapToPaymentResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentOperationResponseDto findById(Long id) {
        PaymentOperation operation = paymentOperationRepository.findById(id)
                .orElseThrow(() -> new PaymentOperationNotFoundException(id));

        return mapToOperationResponse(operation);
    }

    private void validateOperationCanReceivePayments(PaymentOperation operation) {
        if (operation.getEstatus() == OperationStatus.RECHAZADA) {
            throw new OperationDoesNotAcceptPaymentsException(
                    "No se pueden agregar pagos a una operación rechazada");
        }

        if (operation.getEstatus() == OperationStatus.VALIDADA
                || operation.getEstatus() == OperationStatus.FACTURADA
                || operation.getEstatus() == OperationStatus.RETORNO_PENDIENTE
                || operation.getEstatus() == OperationStatus.RETORNO_PARCIAL
                || operation.getEstatus() == OperationStatus.COMPLETADA) {
            throw new OperationDoesNotAcceptPaymentsException(
                    "La operación ya no admite nuevos pagos en su estatus actual");
        }
    }

    private void validateCurrentUserOwnsOperation(PaymentOperation operation) {
        User currentUser = authenticatedUserService.getCurrentUser();

        if (operation.getSocioComercial() == null || operation.getSocioComercial().getUsuario() == null) {
            throw new BusinessException("La operación no tiene un socio comercial asociado a un usuario");
        }

        Long ownerUserId = operation.getSocioComercial().getUsuario().getId();

        if (!ownerUserId.equals(currentUser.getId())) {
            throw new BusinessException("No tienes permiso para registrar pagos en una operación que no pertenece a tu socio comercial");
        }
    }

    private void validateCurrentUserCanValidatePayments() {
        User currentUser = authenticatedUserService.getCurrentUser();

        boolean allowed = currentUser.getRoles().stream()
                .anyMatch(role ->
                        role.getName().name().equals("JEFA_CAJAS")
                                || role.getName().name().equals("GERENTE")
                                || role.getName().name().equals("ADMIN")
                );

        if (!allowed) {
            throw new BusinessException("El usuario autenticado no tiene permiso para validar o rechazar pagos");
        }
    }

    private void validateDuplicateReceipt(Long operacionId, String comprobanteUrl) {
        boolean duplicated = operationPaymentRepository.existsByOperacionIdAndComprobanteUrlAndEstatusNot(
                operacionId,
                comprobanteUrl,
                PaymentStatus.RECHAZADO
        );

        if (duplicated) {
            throw new DuplicatePaymentReceiptException();
        }
    }

    private void validatePaymentAmountDoesNotExceedOperation(PaymentOperation operation, BigDecimal newPaymentAmount) {
        List<OperationPayment> existingPayments = operationPaymentRepository.findByOperacionId(operation.getId());

        BigDecimal accumulated = existingPayments.stream()
                .filter(payment -> payment.getEstatus() != PaymentStatus.RECHAZADO)
                .map(OperationPayment::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal resultingTotal = accumulated.add(newPaymentAmount);

        if (resultingTotal.compareTo(operation.getMontoTotal()) > 0) {
            throw new PaymentAmountExceededException();
        }
    }

    private void recalculateOperation(PaymentOperation operation) {
        List<OperationPayment> validatedPayments =
                operationPaymentRepository.findByOperacionIdAndEstatus(operation.getId(), PaymentStatus.VALIDADO);

        BigDecimal totalValidated = validatedPayments.stream()
                .map(OperationPayment::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal saldoPendiente = operation.getMontoTotal().subtract(totalValidated);

        operation.setMontoValidado(totalValidated);
        operation.setSaldoPendiente(saldoPendiente);

        if (totalValidated.compareTo(BigDecimal.ZERO) == 0) {
            boolean hasRejected = operationPaymentRepository.existsByOperacionIdAndEstatus(
                    operation.getId(), PaymentStatus.RECHAZADO);

            boolean hasPending = operationPaymentRepository.existsByOperacionIdAndEstatus(
                    operation.getId(), PaymentStatus.PENDIENTE_VALIDACION);

            if (hasRejected && !hasPending) {
                operation.setEstatus(OperationStatus.RECHAZADA);
            } else {
                operation.setEstatus(OperationStatus.PENDIENTE_VALIDACION);
            }
        } else if (totalValidated.compareTo(operation.getMontoTotal()) < 0) {
            operation.setEstatus(OperationStatus.PAGO_PARCIAL);
        } else if (totalValidated.compareTo(operation.getMontoTotal()) == 0) {
            operation.setEstatus(OperationStatus.VALIDADA);
        }

        paymentOperationRepository.save(operation);
    }

    private PaymentOperationResponseDto mapToOperationResponse(PaymentOperation operation) {
        List<OperationPaymentResponseDto> payments = operationPaymentRepository.findByOperacionId(operation.getId())
                .stream()
                .map(this::mapToPaymentResponse)
                .toList();

        return new PaymentOperationResponseDto(
                operation.getId(),
                operation.getClienteNombre(),
                operation.getClienteTelefono(),
                operation.getMontoTotal(),
                operation.getMontoValidado(),
                operation.getSaldoPendiente(),
                operation.getEstatus(),
                operation.getCuentaDestino().getId(),
                operation.getCuentaDestino().getBanco(),
                operation.getSocioComercial().getId(),
                operation.getSocioComercial().getNombre(),
                operation.getObservaciones(),
                payments,
                operation.getCreatedAt(),
                operation.getUpdatedAt()
        );
    }

    private OperationPaymentResponseDto mapToPaymentResponse(OperationPayment payment) {
        Long validadoPorId = payment.getValidadoPor() != null ? payment.getValidadoPor().getId() : null;
        String validadoPorNombre = payment.getValidadoPor() != null ? payment.getValidadoPor().getNombre() : null;

        return new OperationPaymentResponseDto(
                payment.getId(),
                payment.getMonto(),
                payment.getTipoPago(),
                payment.getComprobanteUrl(),
                payment.getEstatus(),
                payment.getObservaciones(),
                payment.getRegistradoPor().getId(),
                payment.getRegistradoPor().getNombre(),
                validadoPorId,
                validadoPorNombre,
                payment.getFechaPago(),
                payment.getFechaValidacion(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}