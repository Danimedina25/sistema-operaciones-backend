package com.sistemadeoperaciones.comisionessocioscomerciales.service;


import com.sistemadeoperaciones.comisionessocioscomerciales.dto.request.PayCommissionBatchRequestDto;
import com.sistemadeoperaciones.comisionessocioscomerciales.dto.request.PayCommissionRequestDto;
import com.sistemadeoperaciones.comisionessocioscomerciales.dto.response.*;
import com.sistemadeoperaciones.comisionessocioscomerciales.enums.CommissionBeneficiaryType;
import com.sistemadeoperaciones.comisionessocioscomerciales.exceptions.CommissionAlreadyPaidException;
import com.sistemadeoperaciones.comisionessocioscomerciales.exceptions.CommissionProofRequiredException;
import com.sistemadeoperaciones.comisionessocioscomerciales.exceptions.CommissionRegenerationNotAllowedException;
import com.sistemadeoperaciones.comisionessocioscomerciales.exceptions.InvalidCommissionStructureException;
import com.sistemadeoperaciones.comisionessocioscomerciales.models.CommercialPartnerCommission;
import com.sistemadeoperaciones.comisionessocioscomerciales.repository.CommercialPartnerCommissionRepository;
import com.sistemadeoperaciones.pagos.enums.CommissionStatus;
import com.sistemadeoperaciones.pagos.enums.OperationStatus;
import com.sistemadeoperaciones.pagos.model.PaymentOperation;
import com.sistemadeoperaciones.pagos.repository.PaymentOperationRepository;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;
import com.sistemadeoperaciones.usuarios.model.User;
import com.sistemadeoperaciones.usuarios.repository.CommercialPartnerSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class CommercialPartnerCommissionServiceImpl implements CommercialPartnerCommissionService {

    private final CommercialPartnerCommissionRepository commissionRepository;
    private final PaymentOperationRepository operationRepository;
    private final CommercialPartnerSettingsRepository commercialPartnerSettingsRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public CommercialPartnerCommissionServiceImpl(
            CommercialPartnerCommissionRepository commissionRepository,
            PaymentOperationRepository operationRepository,
            CommercialPartnerSettingsRepository commercialPartnerSettingsRepository,
            AuthenticatedUserService authenticatedUserService
    ) {
        this.commissionRepository = commissionRepository;
        this.operationRepository = operationRepository;
        this.commercialPartnerSettingsRepository = commercialPartnerSettingsRepository;
        this.authenticatedUserService = authenticatedUserService;
    }


    @Override
    @Transactional(readOnly = true)
    public CommissionOperationDetailResponseDto getOperationDetail(
            Long operationId
    ) {

        PaymentOperation operation =
                operationRepository.findById(operationId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Operación no encontrada"
                                )
                        );

        List<CommercialPartnerCommission> commissions =
                commissionRepository
                        .findByOperationIdOrderByNivelAsc(
                                operationId
                        );

        if (commissions.isEmpty()) {
            throw new ResourceNotFoundException(
                    "La operación no tiene comisiones generadas"
            );
        }

        List<CommissionBeneficiaryResponseDto> beneficiaries =
                commissions.stream()
                        .map(this::mapBeneficiary)
                        .toList();

        BigDecimal totalPercentage =
                operation.getPorcentajeComisionAplicado()
                        .multiply(
                                BigDecimal.valueOf(
                                        operation.getNivelesRedComercial()
                                )
                        );

        BigDecimal totalCommissionAmount = beneficiaries.stream()
                .map(CommissionBeneficiaryResponseDto::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CommissionOperationDetailResponseDto(
                operation.getId(),
                operation.getCliente().getNombre(),
                operation.getMontoTotal(),
                operation.getPorcentajeComisionAplicado(),
                totalPercentage,
                operation.getNivelesRedComercial(),
                beneficiaries,
                totalCommissionAmount
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CommercialPartnerCommissionResponseDto findById(
            Long commissionId
    ) {

        CommercialPartnerCommission commission =
                commissionRepository.findById(
                                commissionId
                        )
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Comisión no encontrada"
                                )
                        );

        return mapToResponse(commission);
    }

    @Override
    @Transactional(readOnly = true)
    public CommissionSummaryResponseDto getSummary(
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Las fechas son obligatorias"
            );
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser mayor a la fecha final"
            );
        }
        LocalDateTime start =
                startDate.atStartOfDay();

        LocalDateTime end =
                endDate.atTime(
                        23,
                        59,
                        59
                );

        List<CommercialPartnerCommission> commissions =
                commissionRepository.findByOperationCreatedAtBetween(
                        start,
                        end
                );

        List<CommissionOperationSummaryResponseDto> operations =
                buildOperationSummaries(
                        commissions
                );

        return buildSummary(
                commissions,
                operations
        );
    }

    @Override
    @Transactional(readOnly = true)
    public MyWeeklyCommissionsResponseDto getMyWeeklyCommissions(
            LocalDate startDate,
            LocalDate endDate
    ) {

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Las fechas son obligatorias"
            );
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser mayor a la fecha final"
            );
        }

        User currentUser =
                authenticatedUserService.getCurrentUser();

        LocalDateTime start =
                startDate.atStartOfDay();

        LocalDateTime end =
                endDate.atTime(
                        23,
                        59,
                        59
                );

        List<CommercialPartnerCommission> myCommissions =
                commissionRepository
                        .findByUserIdAndOperationCreatedAtBetween(
                                currentUser.getId(),
                                start,
                                end
                        );

        if (myCommissions.isEmpty()) {

            return new MyWeeklyCommissionsResponseDto(
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    0,
                    List.of()
            );
        }

        List<Long> operationIds =
                myCommissions.stream()
                        .map(c -> c.getOperation().getId())
                        .distinct()
                        .toList();

        List<CommercialPartnerCommission> allOperationCommissions =
                commissionRepository.findByOperationIdIn(
                        operationIds
                );

        Map<Long, List<CommercialPartnerCommission>>
                commissionsByOperation =
                allOperationCommissions.stream()
                        .collect(
                                Collectors.groupingBy(
                                        c -> c.getOperation().getId()
                                )
                        );

        List<MyWeeklyCommissionOperationDto> operations =
                myCommissions.stream()
                        .map(myCommission -> {

                            PaymentOperation operation =
                                    myCommission.getOperation();

                            List<CommercialPartnerCommission>
                                    operationCommissions =
                                    commissionsByOperation.getOrDefault(
                                            operation.getId(),
                                            List.of()
                                    );

                            CommercialPartnerCommission nivel2 =
                                    operationCommissions.stream()
                                            .filter(c -> c.getNivel() == 2)
                                            .findFirst()
                                            .orElse(null);

                            CommercialPartnerCommission nivel3 =
                                    operationCommissions.stream()
                                            .filter(c -> c.getNivel() == 3)
                                            .findFirst()
                                            .orElse(null);

                            BigDecimal commissionNivel2 =
                                    nivel2 != null
                                            ? nivel2.getCommissionAmount()
                                            : BigDecimal.ZERO;

                            BigDecimal commissionNivel3 =
                                    nivel3 != null
                                            ? nivel3.getCommissionAmount()
                                            : BigDecimal.ZERO;
                            String socioNivel2 =
                                    nivel2 != null
                                            && nivel2.getCommercialPartner() != null
                                            ? nivel2.getCommercialPartner().getNombre()
                                            : null;

                            String socioNivel3 =
                                    nivel3 != null
                                            && nivel3.getCommercialPartner() != null
                                            ? nivel3.getCommercialPartner().getNombre()
                                            : null;
                            CommissionStatus statusNivel1 =
                                    myCommission.getStatus();

                            CommissionStatus statusNivel2 =
                                    nivel2 != null
                                            ? nivel2.getStatus()
                                            : null;

                            CommissionStatus statusNivel3 =
                                    nivel3 != null
                                            ? nivel3.getStatus()
                                            : null;

                            BigDecimal totalComisionRed =
                                    commissionNivel2.add(
                                            commissionNivel3
                                    );

                            return new MyWeeklyCommissionOperationDto(
                                    operation.getId(),
                                    operation.getCliente().getNombre(),
                                    operation.getCreatedAt(),
                                    operation.getMontoTotal(),
                                    operation.getNivelesRedComercial(),
                                    operation.getPorcentajeComisionAplicado(),
                                    myCommission.getCommissionAmount(),
                                    totalComisionRed,
                                    commissionNivel2,
                                    commissionNivel3,
                                    socioNivel2,
                                    socioNivel3,
                                    statusNivel1,
                                    statusNivel2,
                                    statusNivel3
                            );
                        })
                        .sorted(
                                Comparator.comparing(
                                        MyWeeklyCommissionOperationDto::getFechaOperacion
                                ).reversed()
                        )
                        .toList();

        BigDecimal totalGanado =
                operations.stream()
                        .map(
                                MyWeeklyCommissionOperationDto::getMiComision
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        BigDecimal totalGanadoRed =
                operations.stream()
                        .map(
                                MyWeeklyCommissionOperationDto::getComisionRed
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        return new MyWeeklyCommissionsResponseDto(
                totalGanado,
                totalGanadoRed,
                operations.size(),
                operations
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CommissionPartnerSummaryListResponseDto getSummaryByBeneficiary(
            LocalDate startDate,
            LocalDate endDate
    ) {

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Las fechas son obligatorias"
            );
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser mayor a la fecha final"
            );
        }

        LocalDateTime start =
                startDate.atStartOfDay();

        LocalDateTime end =
                endDate.atTime(
                        23,
                        59,
                        59
                );

        List<CommercialPartnerCommission> commissions =
                commissionRepository.findByOperationCreatedAtBetween(
                        start,
                        end
                );

        List<CommissionPartnerSummaryResponseDto> socios =
                buildBeneficiarySummaries(
                        commissions
                );

        BigDecimal totalComisiones =
                socios.stream()
                        .map(
                                CommissionPartnerSummaryResponseDto::getTotalComisiones
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        BigDecimal totalPagadas =
                socios.stream()
                        .map(
                                CommissionPartnerSummaryResponseDto::getTotalPagadas
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        BigDecimal totalPendientes =
                socios.stream()
                        .map(
                                CommissionPartnerSummaryResponseDto::getTotalPendientes
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );
        return new CommissionPartnerSummaryListResponseDto(
                totalComisiones,
                totalPagadas,
                totalPendientes,
                socios.size(),
                socios
        );
    }

    private List<CommissionPartnerSummaryResponseDto>
    buildBeneficiarySummaries(
            List<CommercialPartnerCommission> commissions
    ) {

        return commissions.stream()

                .collect(
                        java.util.stream.Collectors.groupingBy(
                                c -> {

                                    if (c.getUser() != null) {
                                        return "USER_" +
                                                c.getUser().getId();
                                    }

                                    return "PARTNER_" +
                                            c.getCommercialPartner().getId();
                                }
                        )
                )

                .values()

                .stream()

                .filter(group -> !group.isEmpty())

                .map(group -> {

                    CommercialPartnerCommission first =
                            group.get(0);

                    Long beneficiaryId;

                    CommissionBeneficiaryType beneficiaryType;

                    String nombre;

                    String banco;

                    String cuentaBancaria;

                    String titularCuenta;

                    if (first.getUser() != null) {

                        beneficiaryId =
                                first.getUser().getId();

                        beneficiaryType =
                                CommissionBeneficiaryType.USER;

                        nombre =
                                first.getUser().getNombre();

                        var settings =
                                commercialPartnerSettingsRepository
                                        .findByUsuarioId(
                                                first.getUser().getId()
                                        )
                                        .orElse(null);

                        banco =
                                settings != null
                                        ? settings.getBanco()
                                        : null;

                        cuentaBancaria =
                                settings != null
                                        ? settings.getCuentaBancaria()
                                        : null;

                        titularCuenta =
                                settings != null
                                        ? settings.getTitularCuenta()
                                        : null;

                    } else {

                        beneficiaryId =
                                first.getCommercialPartner().getId();

                        beneficiaryType =
                                CommissionBeneficiaryType.COMMERCIAL_PARTNER;

                        nombre =
                                first.getCommercialPartner().getNombre();

                        banco =
                                first.getCommercialPartner().getBanco();

                        cuentaBancaria =
                                first.getCommercialPartner()
                                        .getCuentaBancaria();

                        titularCuenta =
                                first.getCommercialPartner()
                                        .getTitularCuenta();
                    }

                    BigDecimal totalComisiones =
                            group.stream()
                                    .map(
                                            CommercialPartnerCommission::getCommissionAmount
                                    )
                                    .reduce(
                                            BigDecimal.ZERO,
                                            BigDecimal::add
                                    );

                    BigDecimal totalPagadas =
                            group.stream()
                                    .filter(
                                            c -> c.getStatus()
                                                    == CommissionStatus.PAGADA
                                    )
                                    .map(
                                            CommercialPartnerCommission::getCommissionAmount
                                    )
                                    .reduce(
                                            BigDecimal.ZERO,
                                            BigDecimal::add
                                    );

                    BigDecimal totalPendientes =
                            totalComisiones.subtract(
                                    totalPagadas
                            );

                    Integer totalOperaciones =
                            (int) group.stream()
                                    .map(
                                            c -> c.getOperation().getId()
                                    )
                                    .distinct()
                                    .count();

                    List<Long> pendingCommissionIds =
                            group.stream()
                                    .filter(
                                            c -> c.getStatus()
                                                    == CommissionStatus.GENERADA
                                    )
                                    .map(
                                            CommercialPartnerCommission::getId
                                    )
                                    .toList();

                    Integer totalComisionesPendientes =
                            pendingCommissionIds.size();

                    return new CommissionPartnerSummaryResponseDto(
                            beneficiaryId,
                            beneficiaryType,
                            nombre,
                            banco,
                            cuentaBancaria,
                            titularCuenta,
                            totalOperaciones,
                            totalComisiones,
                            totalPendientes,
                            totalPagadas,
                            first.getPaidAt(),
                            totalComisionesPendientes,
                            pendingCommissionIds,
                            first.getPaymentProofUrl()
                    );
                })

                .sorted(
                        Comparator.comparing(
                                CommissionPartnerSummaryResponseDto::getNombre
                        )
                )

                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CommissionSummaryResponseDto getPendingCommissions(
            LocalDate startDate,
            LocalDate endDate
    ) {

        List<CommercialPartnerCommission> commissions =
                commissionRepository
                        .findByGeneratedAtBetweenAndStatus(
                                startDate.atStartOfDay(),
                                endDate.atTime(
                                        23,
                                        59,
                                        59
                                ),
                                CommissionStatus.GENERADA
                        );

        return buildSummary(
                commissions,
                buildOperationSummaries(commissions)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CommissionSummaryResponseDto getPaidCommissions(
            LocalDate startDate,
            LocalDate endDate
    ) {

        List<CommercialPartnerCommission> commissions =
                commissionRepository.findByPaidAtBetweenAndStatus(
                        startDate.atStartOfDay(),
                        endDate.atTime(
                                23,
                                59,
                                59
                        ),
                        CommissionStatus.PAGADA
                );

        return buildSummary(
                commissions,
                buildOperationSummaries(commissions)
        );
    }

    private List<CommissionOperationSummaryResponseDto>
    buildOperationSummaries(
            List<CommercialPartnerCommission> commissions
    ) {

        return commissions.stream()
                .collect(
                        java.util.stream.Collectors.groupingBy(
                                c -> c.getOperation().getId()
                        )
                )
                .values()
                .stream()
                .filter(group -> !group.isEmpty())
                .map(group -> {

                    CommercialPartnerCommission first =
                            group.get(0);

                    BigDecimal totalCommissions =
                            group.stream()
                                    .map(
                                            CommercialPartnerCommission::getCommissionAmount
                                    )
                                    .reduce(
                                            BigDecimal.ZERO,
                                            BigDecimal::add
                                    );

                    return new CommissionOperationSummaryResponseDto(
                            first.getOperation().getId(),
                            first.getOperation().getCliente().getNombre(),
                            first.getOperation().getNivelesRedComercial(),
                            group.size(),
                            first.getOperation().getMontoTotal(),
                            totalCommissions,
                            isFullyPaid(group),
                            isPartialPaid(group),
                            first.getOperation().getCreatedAt().toLocalDate()
                    );
                })
                .sorted(
                        Comparator.comparing(
                                CommissionOperationSummaryResponseDto::getOperationId
                        )
                )
                .toList();
    }


    @Override
    @Transactional(readOnly = true)
    public List<CommissionOperationSummaryResponseDto>
    getWeeklyOperations(
            LocalDate weekDate
    ) {

        LocalDateTime start = getWeekStart(weekDate);
        LocalDateTime end = getWeekEnd(weekDate);

        List<CommercialPartnerCommission> commissions =
                commissionRepository.findByGeneratedAtBetween(
                        start,
                        end
                );

        return buildOperationSummaries(
                commissions
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommissionBeneficiaryResponseDto>
    getOperationBeneficiaries(
            Long operationId
    ) {

        List<CommercialPartnerCommission> commissions =
                commissionRepository
                        .findByOperationIdOrderByNivelAsc(
                                operationId
                        );

        if (commissions.isEmpty()) {
            throw new ResourceNotFoundException(
                    "La operación no tiene beneficiarios"
            );
        }

        return commissions.stream()
                .map(this::mapBeneficiary)
                .toList();
    }

    private CommissionSummaryResponseDto buildSummary(
            List<CommercialPartnerCommission> commissions,
            List<CommissionOperationSummaryResponseDto> operations
    ) {

        BigDecimal totalComisiones =
                commissions.stream()
                        .map(
                                CommercialPartnerCommission::getCommissionAmount
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        BigDecimal totalPagadas =
                commissions.stream()
                        .filter(
                                c -> c.getStatus()
                                        == CommissionStatus.PAGADA
                        )
                        .map(
                                CommercialPartnerCommission::getCommissionAmount
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        BigDecimal totalPendientes =
                totalComisiones.subtract(totalPagadas);

        int totalOperaciones =
                (int) commissions.stream()
                        .map(
                                c -> c.getOperation().getId()
                        )
                        .distinct()
                        .count();

        int totalBeneficiarios =
                (int) commissions.stream()
                        .map(c -> {
                            if (c.getUser() != null) {
                                return "USER_" + c.getUser().getId();
                            }

                            return "PARTNER_" +
                                    c.getCommercialPartner().getId();
                        })
                        .distinct()
                        .count();

        return new CommissionSummaryResponseDto(
                totalComisiones,
                totalPagadas,
                totalPendientes,
                totalOperaciones,
                totalBeneficiarios,
                operations
        );
    }
    private LocalDateTime getWeekStart(
            LocalDate date
    ) {

        LocalDate sunday =
                date.minusDays(
                        date.getDayOfWeek().getValue() % 7
                );

        return sunday.atStartOfDay();
    }

    private LocalDateTime getWeekEnd(
            LocalDate date
    ) {

        LocalDate saturday =
                date.plusDays(
                        6 - (date.getDayOfWeek().getValue() % 7)
                );

        return saturday.atTime(
                23,
                59,
                59
        );
    }

    private BigDecimal calculateCommissionAmount(
            BigDecimal montoTotal,
            BigDecimal percentage
    ) {

        return montoTotal
                .multiply(percentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private void generateCommissionLevel1(
            PaymentOperation operation
    ) {

        CommercialPartnerCommission commission =
                new CommercialPartnerCommission();

        commission.setOperation(operation);

        commission.setUser(
                operation.getSocioComercial()
        );

        commission.setNivel(1);

        commission.setBaseAmount(
                operation.getMontoTotal()
        );

        commission.setCommissionPercentage(
                operation.getPorcentajeComisionAplicado()
        );

        commission.setCommissionAmount(
                calculateCommissionAmount(
                        operation.getMontoTotal(),
                        operation.getPorcentajeComisionAplicado()
                )
        );

        commissionRepository.save(commission);
    }

    private void generateCommissionLevel2(
            PaymentOperation operation
    ) {

        if (operation.getSocioComercialNivel2() == null) {
            return;
        }

        CommercialPartnerCommission commission =
                new CommercialPartnerCommission();

        commission.setOperation(operation);

        commission.setCommercialPartner(
                operation.getSocioComercialNivel2()
        );

        commission.setNivel(2);

        commission.setBaseAmount(
                operation.getMontoTotal()
        );

        commission.setCommissionPercentage(
                operation.getPorcentajeComisionAplicado()
        );

        commission.setCommissionAmount(
                calculateCommissionAmount(
                        operation.getMontoTotal(),
                        operation.getPorcentajeComisionAplicado()
                )
        );

        commissionRepository.save(commission);
    }

    private void generateCommissionLevel3(
            PaymentOperation operation
    ) {

        if (operation.getSocioComercialNivel3() == null) {
            return;
        }

        CommercialPartnerCommission commission =
                new CommercialPartnerCommission();

        commission.setOperation(operation);

        commission.setCommercialPartner(
                operation.getSocioComercialNivel3()
        );

        commission.setNivel(3);

        commission.setBaseAmount(
                operation.getMontoTotal()
        );

        commission.setCommissionPercentage(
                operation.getPorcentajeComisionAplicado()
        );

        commission.setCommissionAmount(
                calculateCommissionAmount(
                        operation.getMontoTotal(),
                        operation.getPorcentajeComisionAplicado()
                )
        );

        commissionRepository.save(commission);
    }

    @Override
    @Transactional
    public void generatePendingCommissions() {

        List<PaymentOperation> operations =
                operationRepository.findByEstatusInAndActivoTrue(
                        List.of(
                                OperationStatus.VALIDADA,
                                OperationStatus.COMPLETADA,
                                OperationStatus.RETORNO_PARCIAL_SOLICITADO,
                                OperationStatus.RETORNO_TOTAL_SOLICITADO,
                                OperationStatus.RETORNO_PARCIAL_ENTREGADO,
                                OperationStatus.RETORNADA,
                                OperationStatus.COMPLETADA
                        )
                );

        for (PaymentOperation operation : operations) {
            if (
                    commissionRepository.existsByOperationId(
                            operation.getId()
                    )
            ) {
                continue;
            }
            validateOperationNetwork(operation);
            generateCommissionLevel1(operation);

            if (
                    operation.getNivelesRedComercial() >= 2
            ) {
                generateCommissionLevel2(operation);
            }

            if (
                    operation.getNivelesRedComercial() >= 3
            ) {
                generateCommissionLevel3(operation);
            }
        }
    }

    @Override
    @Transactional
    public CommercialPartnerCommissionResponseDto markAsPaid(
            Long commissionId,
            PayCommissionRequestDto request
    ) {

        if (request == null) {
            throw new CommissionProofRequiredException();
        }

        CommercialPartnerCommission commission =
                commissionRepository.findById(
                                commissionId
                        )
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Comisión no encontrada"
                                )
                        );

        if (
                commission.getStatus()
                        == CommissionStatus.PAGADA
        ) {
            throw new CommissionAlreadyPaidException();
        }

        if (
                request.getPaymentProofUrl() == null
                        || request.getPaymentProofUrl().isBlank()
        ) {
            throw new CommissionProofRequiredException();
        }

        commission.setStatus(
                CommissionStatus.PAGADA
        );

        commission.setPaymentProofUrl(
                request.getPaymentProofUrl()
        );

        commission.setPaidAt(
                LocalDateTime.now()
        );

        return mapToResponse(
                commissionRepository.save(
                        commission
                )
        );
    }

    @Override
    @Transactional
    public void markBatchAsPaid(
            PayCommissionBatchRequestDto request
    ) {

        if (request == null) {
            throw new CommissionProofRequiredException();
        }

        if (
                request.getPaymentProofUrl() == null
                        || request.getPaymentProofUrl().isBlank()
        ) {
            throw new CommissionProofRequiredException();
        }

        if (
                request.getCommissionIds() == null
                        || request.getCommissionIds().isEmpty()
        ) {
            throw new IllegalArgumentException(
                    "Debe seleccionar al menos una comisión"
            );
        }

        List<CommercialPartnerCommission> commissions =
                commissionRepository.findAllById(
                        request.getCommissionIds()
                );

        if (
                commissions.size()
                        != request.getCommissionIds().size()
        ) {
            throw new ResourceNotFoundException(
                    "Una o más comisiones no existen"
            );
        }

        LocalDateTime paidAt =
                LocalDateTime.now();

        for (
                CommercialPartnerCommission commission
                : commissions
        ) {

            if (
                    commission.getStatus()
                            == CommissionStatus.PAGADA
            ) {
                continue;
            }

            commission.setStatus(
                    CommissionStatus.PAGADA
            );

            commission.setPaymentProofUrl(
                    request.getPaymentProofUrl()
            );

            commission.setPaidAt(
                    paidAt
            );
        }

        commissionRepository.saveAll(
                commissions
        );
    }

    @Override
    @Transactional
    public void regenerateOperationCommissions(
            Long operationId
    ) {

        PaymentOperation operation =
                operationRepository.findById(
                                operationId
                        )
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Operación no encontrada"
                                )
                        );

        if (
                commissionRepository
                        .existsByOperationIdAndStatus(
                                operationId,
                                CommissionStatus.PAGADA
                        )
        ) {
            throw new CommissionRegenerationNotAllowedException();
        }

        List<CommercialPartnerCommission> generated =
                commissionRepository.findByOperationIdAndStatus(
                        operationId,
                        CommissionStatus.GENERADA
                );

        commissionRepository.deleteAll(generated);

        validateOperationNetwork(operation);

        generateCommissionLevel1(operation);

        if (
                operation.getNivelesRedComercial() >= 2
        ) {
            generateCommissionLevel2(operation);
        }

        if (
                operation.getNivelesRedComercial() >= 3
        ) {
            generateCommissionLevel3(operation);
        }
    }

    private CommercialPartnerCommissionResponseDto mapToResponse(
            CommercialPartnerCommission commission
    ) {

        String nombreBeneficiario;

        if (
                commission.getUser() == null
                        && commission.getCommercialPartner() == null
        ) {
            throw new InvalidCommissionStructureException(
                    "La comisión no tiene beneficiario asociado"
            );
        }

        if (commission.getUser() != null) {
            nombreBeneficiario =
                    commission.getUser().getNombre();
        } else {
            nombreBeneficiario =
                    commission.getCommercialPartner().getNombre();
        }

        return new CommercialPartnerCommissionResponseDto(
                commission.getId(),
                commission.getOperation().getId(),
                commission.getUser() != null
                        ? commission.getUser().getId()
                        : null,
                commission.getCommercialPartner() != null
                        ? commission.getCommercialPartner().getId()
                        : null,
                nombreBeneficiario,
                commission.getNivel(),
                commission.getCommissionAmount(),
                commission.getStatus(),
                commission.getPaymentProofUrl(),
                commission.getGeneratedAt(),
                commission.getPaidAt()
        );
    }

    private CommissionBeneficiaryResponseDto mapBeneficiary(
            CommercialPartnerCommission commission
    ) {

        if (commission.getNivel() == 1) {

            var settings =
                    commercialPartnerSettingsRepository
                            .findByUsuarioId(
                                    commission.getUser().getId()
                            )
                            .orElse(null);

            return new CommissionBeneficiaryResponseDto(
                    commission.getId(),
                    commission.getOperation().getId(),
                    commission.getNivel(),
                    commission.getUser().getNombre(),
                    settings != null ? settings.getBanco() : null,
                    settings != null ? settings.getCuentaBancaria() : null,
                    settings != null ? settings.getTitularCuenta() : null,
                    commission.getCommissionAmount(),
                    commission.getStatus(),
                    commission.getPaymentProofUrl(),
                    commission.getCommissionPercentage()
            );
        }

        if (
                commission.getNivel() > 1
                        && commission.getCommercialPartner() == null
        ) {
            throw new InvalidCommissionStructureException(
                    "Comisión sin socio comercial asociado"
            );
        }

        return new CommissionBeneficiaryResponseDto(
                commission.getId(),
                commission.getOperation().getId(),
                commission.getNivel(),
                commission.getCommercialPartner().getNombre(),
                commission.getCommercialPartner().getBanco(),
                commission.getCommercialPartner().getCuentaBancaria(),
                commission.getCommercialPartner().getTitularCuenta(),
                commission.getCommissionAmount(),
                commission.getStatus(),
                commission.getPaymentProofUrl(),
                commission.getCommissionPercentage()
        );
    }

    private boolean isFullyPaid(
            List<CommercialPartnerCommission> commissions
    )
    {
        return commissions.stream()
                .allMatch(
                        c -> c.getStatus()
                                == CommissionStatus.PAGADA
                );
    }

    private boolean isPartialPaid(
            List<CommercialPartnerCommission> commissions
    )
    {
        boolean hasPaid =
                commissions.stream()
                        .anyMatch(
                                c -> c.getStatus()
                                        == CommissionStatus.PAGADA
                        );

        boolean hasPending =
                commissions.stream()
                        .anyMatch(
                                c -> c.getStatus()
                                        == CommissionStatus.GENERADA
                        );

        return hasPaid && hasPending;
    }

    private void validateOperationNetwork(
            PaymentOperation operation
    )
    {
        if (operation == null) {
            throw new InvalidCommissionStructureException(
                    "La operación es obligatoria"
            );
        }

        if (
                operation.getNivelesRedComercial() == null
                        || operation.getNivelesRedComercial() < 1
                        || operation.getNivelesRedComercial() > 3
        ) {
            throw new InvalidCommissionStructureException(
                    "Nivel de red comercial inválido"
            );
        }

        if (operation.getMontoTotal() == null) {
            throw new InvalidCommissionStructureException(
                    "La operación no tiene monto configurado"
            );
        }

        if (
                operation.getMontoTotal()
                        .compareTo(BigDecimal.ZERO) <= 0
        ) {
            throw new InvalidCommissionStructureException(
                    "El monto de la operación debe ser mayor a cero"
            );
        }

        if (operation.getPorcentajeComisionAplicado() == null) {
            throw new InvalidCommissionStructureException(
                    "La operación no tiene porcentaje de comisión configurado"
            );
        }

        /*if (
                operation.getPorcentajeComisionAplicado()
                        .compareTo(BigDecimal.ZERO) <= 0
        ) {
            throw new InvalidCommissionStructureException(
                    "El porcentaje de comisión debe ser mayor a cero"
            );
        }*/

        if (
                operation.getPorcentajeComisionAplicado()
                        .compareTo(BigDecimal.valueOf(100)) > 0
        ) {
            throw new InvalidCommissionStructureException(
                    "El porcentaje de comisión no puede ser mayor a 100"
            );
        }

        if (operation.getSocioComercial() == null) {
            throw new InvalidCommissionStructureException(
                    "La operación no tiene socio comercial nivel 1"
            );
        }

        if (
                operation.getNivelesRedComercial() >= 2
                        && operation.getSocioComercialNivel2() == null
        ) {
            throw new InvalidCommissionStructureException(
                    "La operación requiere socio comercial nivel 2"
            );
        }

        if (
                operation.getNivelesRedComercial() >= 3
                        && operation.getSocioComercialNivel3() == null
        ) {
            throw new InvalidCommissionStructureException(
                    "La operación requiere socio comercial nivel 3"
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BeneficiaryCommissionDetailResponseDto
    getBeneficiaryCommissionDetail(
            Long beneficiaryId,
            CommissionBeneficiaryType beneficiaryType,
            LocalDate startDate,
            LocalDate endDate
    ) {

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Las fechas son obligatorias"
            );
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser mayor a la fecha final"
            );
        }

        LocalDateTime start =
                startDate.atStartOfDay();

        LocalDateTime end =
                endDate.atTime(23, 59, 59);

        List<CommercialPartnerCommission> commissions;

        if (beneficiaryType == CommissionBeneficiaryType.USER) {

            commissions =
                    commissionRepository.findDetailByUser(
                            beneficiaryId,
                            start,
                            end
                    );

        } else {

            commissions =
                    commissionRepository.findDetailByPartner(
                            beneficiaryId,
                            start,
                            end
                    );
        }

        if (commissions.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No existen comisiones para el beneficiario"
            );
        }

        CommercialPartnerCommission first =
                commissions.get(0);

        String beneficiaryName;

        if (beneficiaryType == CommissionBeneficiaryType.USER) {

            beneficiaryName =
                    first.getUser().getNombre();

        } else {

            beneficiaryName =
                    first.getCommercialPartner().getNombre();
        }

        List<BeneficiaryCommissionOperationDto> operations =
                commissions.stream()
                        .map(this::mapBeneficiaryOperation)
                        .toList();

        BigDecimal totalCommission =
                commissions.stream()
                        .map(
                                CommercialPartnerCommission::getCommissionAmount
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        Integer totalOperations =
                (int) commissions.stream()
                        .map(
                                c -> c.getOperation().getId()
                        )
                        .distinct()
                        .count();

        return new BeneficiaryCommissionDetailResponseDto(
                beneficiaryId,
                beneficiaryName,
                beneficiaryType,
                totalOperations,
                totalCommission,
                operations
        );
    }

    private BeneficiaryCommissionOperationDto
    mapBeneficiaryOperation(
            CommercialPartnerCommission commission
    ) {

        PaymentOperation operation =
                commission.getOperation();

        return new BeneficiaryCommissionOperationDto(
                commission.getId(),
                operation.getId(),
                operation.getCliente().getNombre(),
                operation.getCreatedAt(),
                commission.getNivel(),
                operation.getMontoTotal(),
                commission.getCommissionPercentage(),
                commission.getCommissionAmount(),
                commission.getStatus()
        );
    }

    @Transactional
    @Override
    public void generateCommissionsForOperation(Long operationId) {

        PaymentOperation operation =
                operationRepository.findById(operationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Operación no encontrada"
                                )
                        );

        if (commissionRepository.existsByOperationId(operationId)) {
            return;
        }

        validateOperationNetwork(operation);

        generateCommissionLevel1(operation);

        if (operation.getNivelesRedComercial() >= 2) {
            generateCommissionLevel2(operation);
        }

        if (operation.getNivelesRedComercial() >= 3) {
            generateCommissionLevel3(operation);
        }
    }
}