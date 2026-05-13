package com.sistemadeoperaciones.pagos.service;

import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.cuentasbancarias.repository.BankAccountRepository;
import com.sistemadeoperaciones.pagos.dto.PaymentOperationFilterDto;
import com.sistemadeoperaciones.pagos.dto.PaymentOperationResponseDto;
import com.sistemadeoperaciones.pagos.dto.retornos.CreateReturnPaymentRequestDto;
import com.sistemadeoperaciones.pagos.dto.retornos.ReturnPaymentResponseDto;
import com.sistemadeoperaciones.pagos.enums.OperationStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.model.OperationReturnPayment;
import com.sistemadeoperaciones.pagos.model.PaymentOperation;
import com.sistemadeoperaciones.pagos.repository.OperationReturnPaymentRepository;
import com.sistemadeoperaciones.pagos.repository.PaymentOperationRepository;
import com.sistemadeoperaciones.pagos.repository.specification.PaymentOperationSpecification;
import com.sistemadeoperaciones.pagos.service.ReturnsOperationService;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.usuarios.model.User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

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
    private final BankAccountRepository bankAccountRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public ReturnsOperationServiceImpl(
            PaymentOperationRepository paymentOperationRepository,
            OperationReturnPaymentRepository operationReturnPaymentRepository,
            BankAccountRepository bankAccountRepository,
            AuthenticatedUserService authenticatedUserService
    ) {
        this.paymentOperationRepository = paymentOperationRepository;
        this.operationReturnPaymentRepository = operationReturnPaymentRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentOperationResponseDto> findOperationsReadyForReturn(
            PaymentOperationFilterDto filter,
            Pageable pageable
    ) {
        if (filter == null) {
            filter = new PaymentOperationFilterDto();
        }
        Specification<PaymentOperation> specification = Specification
                .where(PaymentOperationSpecification.hasStatusIn(
                        List.of(
                                OperationStatus.VALIDADA,
                                OperationStatus.RETORNO_PARCIAL
                        )
                ))
                .and(PaymentOperationSpecification.clienteONombreSocioOIdContains(filter.getSearch()))
                .and(PaymentOperationSpecification.hasSocioComercialId(filter.getSocioComercialId()))
                .and(PaymentOperationSpecification.createdAtBetween(
                        toStartOfDay(resolveStartDate(filter)),
                        toEndOfDay(resolveEndDate(filter))
                ));

        return paymentOperationRepository.findAll(specification, pageable)
                .map(this::mapOperationToResponse);
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
    public ReturnPaymentResponseDto registerReturnPayment(
            Long operationId,
            CreateReturnPaymentRequestDto request
    ) {
        PaymentOperation operation = paymentOperationRepository.findById(operationId)
                .orElseThrow(() -> new IllegalArgumentException("Operación no encontrada"));

        validateOperationCanReceiveReturn(operation);
        validateReturnRequest(request);

        BigDecimal totalReturnedBefore =
                operationReturnPaymentRepository.sumReturnedAmountByOperationId(operationId);

        BigDecimal amountToReturn = calculateAmountToReturn(operation);

        BigDecimal newTotalReturned = totalReturnedBefore.add(request.getMonto());

        if (newTotalReturned.compareTo(amountToReturn) > 0) {
            throw new IllegalArgumentException(
                    "El monto del retorno excede el saldo pendiente por devolver"
            );
        }

        User currentUser = authenticatedUserService.getCurrentUser();

        OperationReturnPayment returnPayment = new OperationReturnPayment();
        returnPayment.setOperacion(operation);
        returnPayment.setMonto(request.getMonto());
        returnPayment.setTipoPago(request.getTipoPago());
        returnPayment.setObservaciones(request.getObservaciones());
        returnPayment.setComprobanteUrl(request.getComprobanteUrl());
        returnPayment.setRegistradoPor(currentUser);
        String cuentaDestinoCliente = request.getCuentaDestinoCliente() != null
                ? request.getCuentaDestinoCliente().replaceAll("\\s+", "")
                : null;

        returnPayment.setCuentaDestinoCliente(cuentaDestinoCliente);

        if (request.getTipoPago() != PaymentType.EFECTIVO) {
            BankAccount cuentaOrigen = bankAccountRepository.findById(request.getCuentaOrigenId())
                    .orElseThrow(() -> new IllegalArgumentException("Cuenta origen no encontrada"));

            returnPayment.setCuentaOrigen(cuentaOrigen);
        }

        OperationReturnPayment savedReturn =
                operationReturnPaymentRepository.save(returnPayment);

        updateOperationReturnStatus(operation, newTotalReturned, amountToReturn);

        paymentOperationRepository.save(operation);

        return mapReturnToResponse(savedReturn);
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

        return mapOperationToResponse(operation);
    }

    private void validateOperationCanReceiveReturn(PaymentOperation operation) {
        if (
                operation.getEstatus() != OperationStatus.VALIDADA &&
                operation.getEstatus() != OperationStatus.RETORNO_PARCIAL
        ) {
            throw new IllegalArgumentException(
                    "La operación no está lista para registrar retornos"
            );
        }
    }

    private void validateReturnRequest(CreateReturnPaymentRequestDto request) {

        if (request.getTipoPago() == PaymentType.EFECTIVO) {

            if (
                    request.getCuentaOrigenId() != null ||
                            (request.getCuentaDestinoCliente() != null &&
                                    !request.getCuentaDestinoCliente().isBlank())
            ) {
                throw new IllegalArgumentException(
                        "Para retornos en efectivo no se debe enviar cuenta origen ni cuenta destino"
                );
            }

            return;
        }

        if (request.getCuentaOrigenId() == null) {
            throw new IllegalArgumentException("La cuenta origen es obligatoria");
        }

        String cuentaDestinoCliente = request.getCuentaDestinoCliente();

        if (cuentaDestinoCliente == null || cuentaDestinoCliente.isBlank()) {
            throw new IllegalArgumentException(
                    "La cuenta o CLABE del cliente es obligatoria"
            );
        }

        String cuentaLimpia = cuentaDestinoCliente.replaceAll("\\s+", "");

        if (!cuentaLimpia.matches("\\d+")) {
            throw new IllegalArgumentException(
                    "La cuenta o CLABE del cliente solo debe contener números"
            );
        }

        if (cuentaLimpia.length() != 10 && cuentaLimpia.length() != 18) {
            throw new IllegalArgumentException(
                    "La cuenta debe tener 10 dígitos o la CLABE 18 dígitos"
            );
        }

        if (
                request.getComprobanteUrl() == null ||
                        request.getComprobanteUrl().isBlank()
        ) {
            throw new IllegalArgumentException(
                    "El comprobante es obligatorio"
            );
        }
    }

    private BigDecimal calculateAmountToReturn(PaymentOperation operation) {

        BigDecimal montoValidado = safe(operation.getMontoValidado());

        BigDecimal porcentajeComisionRedTotal = calculateTotalPercentageByLevels(
                operation.getPorcentajeComisionAplicado(),
                operation.getNivelesRedComercial()
        );

        BigDecimal montoComisionRedTotal = calculateAmountFromPercentage(
                montoValidado,
                porcentajeComisionRedTotal
        );

        BigDecimal porcentajeComisionOficinaTotal = calculateTotalPercentageByLevels(
                operation.getPorcentajeComisionOficina(),
                operation.getNivelesRedComercial()
        );

        BigDecimal montoComisionOficinaTotal = calculateAmountFromPercentage(
                montoValidado,
                porcentajeComisionOficinaTotal
        );

        return montoValidado
                .subtract(montoComisionRedTotal)
                .subtract(montoComisionOficinaTotal)
                .setScale(2, RoundingMode.HALF_UP);
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
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private void updateOperationReturnStatus(
            PaymentOperation operation,
            BigDecimal totalReturned,
            BigDecimal amountToReturn
    ) {
        if (totalReturned.compareTo(amountToReturn) >= 0) {
            operation.setEstatus(OperationStatus.COMPLETADA);
        } else {
            operation.setEstatus(OperationStatus.RETORNO_PARCIAL);
        }
    }

    private ReturnPaymentResponseDto mapReturnToResponse(OperationReturnPayment returnPayment) {
        ReturnPaymentResponseDto dto = new ReturnPaymentResponseDto();

        dto.setId(returnPayment.getId());
        dto.setOperationId(returnPayment.getOperacion().getId());
        dto.setMonto(returnPayment.getMonto());
        dto.setTipoPago(returnPayment.getTipoPago());
        dto.setComprobanteUrl(returnPayment.getComprobanteUrl());
        dto.setObservaciones(returnPayment.getObservaciones());
        dto.setFechaRetorno(returnPayment.getFechaRetorno());
        dto.setCreatedAt(returnPayment.getCreatedAt());

        if (returnPayment.getCuentaOrigen() != null) {
            dto.setCuentaOrigenId(returnPayment.getCuentaOrigen().getId());
            dto.setCuentaOrigenNombre(returnPayment.getCuentaOrigen().getBanco());
        }

        dto.setCuentaDestinoCliente(returnPayment.getCuentaDestinoCliente());

        if (returnPayment.getRegistradoPor() != null) {
            dto.setRegistradoPorId(returnPayment.getRegistradoPor().getId());
            dto.setRegistradoPorNombre(returnPayment.getRegistradoPor().getNombre());
        }

        return dto;
    }

    private PaymentOperationResponseDto mapOperationToResponse(PaymentOperation operation) {
        PaymentOperationResponseDto dto = new PaymentOperationResponseDto();

        dto.setId(operation.getId());
        dto.setMontoTotal(operation.getMontoTotal());
        dto.setMontoValidado(operation.getMontoValidado());
        dto.setSaldoPendiente(operation.getSaldoPendiente());
        dto.setEstatus(operation.getEstatus());

        return dto;
    }
}