package com.sistemadeoperaciones.corte.service;

import com.sistemadeoperaciones.corte.dto.BankAccountBalanceDetailResponseDto;
import com.sistemadeoperaciones.corte.dto.BankAccountBalanceResponseDto;
import com.sistemadeoperaciones.corte.dto.BankGroupBalanceResponseDto;
import com.sistemadeoperaciones.corte.model.BankAccountDailyCut;
import com.sistemadeoperaciones.corte.repository.BankAccountDailyCutRepository;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.cuentasbancarias.repository.BankAccountRepository;
import com.sistemadeoperaciones.pagos.enums.PaymentStatus;
import com.sistemadeoperaciones.pagos.repository.OperationPaymentRepository;
import com.sistemadeoperaciones.pagos.repository.OperationReturnPaymentRepository;
import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BankAccountDailyCutServiceImpl implements BankAccountDailyCutService{

    private final BankAccountRepository bankAccountRepository;

    private final BankAccountDailyCutRepository bankAccountDailyCutRepository;

    private final OperationPaymentRepository operationPaymentRepository;

    private final OperationReturnPaymentRepository operationReturnPaymentRepository;

    public BankAccountDailyCutServiceImpl(
            BankAccountRepository bankAccountRepository,
            BankAccountDailyCutRepository bankAccountDailyCutRepository,
            OperationPaymentRepository operationPaymentRepository,
            OperationReturnPaymentRepository operationReturnPaymentRepository
    ) {
        this.bankAccountRepository = bankAccountRepository;
        this.bankAccountDailyCutRepository = bankAccountDailyCutRepository;
        this.operationPaymentRepository = operationPaymentRepository;
        this.operationReturnPaymentRepository =  operationReturnPaymentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public BankAccountBalanceDetailResponseDto calculateBalance(
            Long bankAccountId,
            LocalDate fecha
    ) {

        if (fecha == null) {
            throw new IllegalArgumentException(
                    "La fecha es obligatoria"
            );
        }

        BankAccount account =
                bankAccountRepository.findById(bankAccountId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Cuenta bancaria no encontrada"
                                )
                        );

        LocalDate hoy = LocalDate.now();

        /*
         * FECHA HISTÓRICA
         */
        if (fecha.isBefore(hoy)) {

            BankAccountDailyCut cut =
                    bankAccountDailyCutRepository
                            .findByBankAccountIdAndFecha(
                                    bankAccountId,
                                    fecha
                            )
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "No existe corte registrado para la fecha "
                                                    + fecha
                                    )
                            );

            return mapToDetailResponse(cut);
        }

        /*
         * HOY = TIEMPO REAL
         */

        BigDecimal saldoInicial =
                obtenerSaldoInicialCuenta(
                        bankAccountId,
                        fecha
                );

        LocalDateTime inicio =
                fecha.atStartOfDay();

        LocalDateTime fin =
                fecha.atTime(
                        23,
                        59,
                        59
                );

        BigDecimal entradasTransferencia =
                nvl(
                        operationPaymentRepository
                                .sumEntradasTransferenciaCuenta(
                                        bankAccountId,
                                        PaymentStatus.VALIDADA,
                                        inicio,
                                        fin
                                )
                );

        BigDecimal entradasDeposito =
                nvl(
                        operationPaymentRepository
                                .sumEntradasDepositoCuenta(
                                        bankAccountId,
                                        PaymentStatus.VALIDADA,
                                        inicio,
                                        fin
                                )
                );

        BigDecimal totalEntradas =
                entradasTransferencia
                        .add(entradasDeposito);

        BigDecimal salidasRetornos =
                nvl(
                        operationReturnPaymentRepository
                                .sumRetornosCuenta(
                                        bankAccountId,
                                        inicio,
                                        fin
                                )
                );

        BigDecimal salidasComisiones =
                BigDecimal.ZERO;

        BigDecimal totalSalidas =
                salidasRetornos
                        .add(salidasComisiones);

        BigDecimal saldoFinal =
                saldoInicial
                        .add(totalEntradas)
                        .subtract(totalSalidas);

        BankAccountBalanceDetailResponseDto dto =
                new BankAccountBalanceDetailResponseDto();

        dto.setBankAccountId(account.getId());

        dto.setBanco(account.getBanco());

        dto.setTitular(account.getTitular());

        dto.setNumeroCuenta(
                account.getNumeroCuenta()
        );

        dto.setClabe(
                account.getClabe()
        );

        dto.setFecha(fecha);

        dto.setSaldoInicial(
                saldoInicial
        );

        dto.setEntradasTransferencia(
                entradasTransferencia
        );

        dto.setEntradasDeposito(
                entradasDeposito
        );

        dto.setTotalEntradas(
                totalEntradas
        );

        dto.setSalidasRetornos(
                salidasRetornos
        );

        dto.setSalidasComisiones(
                salidasComisiones
        );

        dto.setTotalSalidas(
                totalSalidas
        );

        dto.setSaldoFinal(
                saldoFinal
        );

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankAccountBalanceResponseDto> calculateBalances(LocalDate fecha) {

        final LocalDate fechaConsulta =
                fecha == null
                        ? LocalDate.now()
                        : fecha;

        return bankAccountRepository.findByActivoTrue()
                .stream()
                .map(account -> {

                    BankAccountBalanceDetailResponseDto detail =
                            calculateBalance(
                                    account.getId(),
                                    fechaConsulta
                            );

                    BankAccountBalanceResponseDto dto =
                            new BankAccountBalanceResponseDto();

                    dto.setBankAccountId(
                            detail.getBankAccountId()
                    );

                    dto.setBanco(
                            detail.getBanco()
                    );

                    dto.setTitular(
                            detail.getTitular()
                    );

                    dto.setNumeroCuenta(
                            detail.getNumeroCuenta()
                    );

                    dto.setClabe(
                            detail.getClabe()
                    );

                    dto.setFecha(
                            detail.getFecha()
                    );

                    dto.setSaldoFinal(
                            detail.getSaldoFinal()
                    );

                    return dto;
                })
                .toList();
    }

    @Override
    @Transactional
    public List<BankGroupBalanceResponseDto> calculateBalancesGrouped(LocalDate fecha) {

        final LocalDate fechaConsulta =
                fecha == null
                        ? LocalDate.now()
                        : fecha;

        LocalDate hoy = LocalDate.now();

        if (fechaConsulta.isBefore(hoy)) {
            registerMissingDailyCuts(fechaConsulta);
        }

        List<BankAccountBalanceResponseDto> cuentas =
                calculateBalances(fechaConsulta);

        return cuentas.stream()
                .collect(
                        java.util.stream.Collectors.groupingBy(
                                BankAccountBalanceResponseDto::getBanco
                        )
                )
                .entrySet()
                .stream()
                .map(entry -> {
                    String banco = entry.getKey();

                    List<BankAccountBalanceResponseDto> cuentasBanco =
                            entry.getValue();

                    BigDecimal saldoTotal =
                            cuentasBanco.stream()
                                    .map(BankAccountBalanceResponseDto::getSaldoFinal)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BankGroupBalanceResponseDto dto =
                            new BankGroupBalanceResponseDto();

                    dto.setBanco(banco);
                    dto.setSaldoTotalBanco(saldoTotal);
                    dto.setTotalCuentas(cuentasBanco.size());
                    dto.setCuentas(cuentasBanco);

                    return dto;
                })
                .toList();
    }

    @Override
    @Transactional
    public void registerDailyCut(LocalDate fecha) {

        if (fecha == null) {
            throw new IllegalArgumentException(
                    "La fecha es obligatoria"
            );
        }

        List<BankAccount> cuentas =
                bankAccountRepository.findByActivoTrue();

        for (BankAccount account : cuentas) {

            boolean existeCorte =
                    bankAccountDailyCutRepository
                            .existsByBankAccountIdAndFecha(
                                    account.getId(),
                                    fecha
                            );

            if (existeCorte) {
                continue;
            }

            BankAccountBalanceDetailResponseDto calculado =
                    calculateBalanceLive(
                            account,
                            fecha
                    );

            BankAccountDailyCut cut =
                    new BankAccountDailyCut();

            cut.setBankAccount(account);
            cut.setFecha(fecha);

            cut.setSaldoInicial(
                    calculado.getSaldoInicial()
            );

            cut.setEntradasTransferencia(
                    calculado.getEntradasTransferencia()
            );

            cut.setEntradasDeposito(
                    calculado.getEntradasDeposito()
            );

            cut.setSalidasRetornos(
                    calculado.getSalidasRetornos()
            );

            cut.setSalidasComisiones(
                    calculado.getSalidasComisiones()
            );

            cut.setTotalEntradas(
                    calculado.getTotalEntradas()
            );

            cut.setTotalSalidas(
                    calculado.getTotalSalidas()
            );

            cut.setSaldoFinal(
                    calculado.getSaldoFinal()
            );

            bankAccountDailyCutRepository.save(cut);
        }
    }

    private void registerMissingDailyCuts(LocalDate fecha) {

        List<BankAccount> cuentas =
                bankAccountRepository.findByActivoTrue();

        for (BankAccount account : cuentas) {

            boolean existeCorte =
                    bankAccountDailyCutRepository
                            .existsByBankAccountIdAndFecha(
                                    account.getId(),
                                    fecha
                            );

            if (existeCorte) {
                continue;
            }

            BankAccountBalanceDetailResponseDto calculado =
                    calculateBalanceLive(
                            account,
                            fecha
                    );

            BankAccountDailyCut cut =
                    new BankAccountDailyCut();

            cut.setBankAccount(account);
            cut.setFecha(fecha);

            cut.setSaldoInicial(
                    calculado.getSaldoInicial()
            );

            cut.setEntradasTransferencia(
                    calculado.getEntradasTransferencia()
            );

            cut.setEntradasDeposito(
                    calculado.getEntradasDeposito()
            );

            cut.setSalidasRetornos(
                    calculado.getSalidasRetornos()
            );

            cut.setSalidasComisiones(
                    calculado.getSalidasComisiones()
            );

            cut.setTotalEntradas(
                    calculado.getTotalEntradas()
            );

            cut.setTotalSalidas(
                    calculado.getTotalSalidas()
            );

            cut.setSaldoFinal(
                    calculado.getSaldoFinal()
            );

            bankAccountDailyCutRepository.save(cut);
        }
    }

    private BankAccountBalanceDetailResponseDto
    mapToDetailResponse(
            BankAccountDailyCut cut
    ) {

        BankAccountBalanceDetailResponseDto dto =
                new BankAccountBalanceDetailResponseDto();

        dto.setBankAccountId(
                cut.getBankAccount().getId()
        );

        dto.setBanco(
                cut.getBankAccount().getBanco()
        );

        dto.setTitular(
                cut.getBankAccount().getTitular()
        );

        dto.setNumeroCuenta(
                cut.getBankAccount().getNumeroCuenta()
        );

        dto.setClabe(
                cut.getBankAccount().getClabe()
        );

        dto.setFecha(
                cut.getFecha()
        );

        dto.setSaldoInicial(
                cut.getSaldoInicial()
        );

        dto.setEntradasTransferencia(
                cut.getEntradasTransferencia()
        );

        dto.setEntradasDeposito(
                cut.getEntradasDeposito()
        );

        dto.setTotalEntradas(
                cut.getTotalEntradas()
        );

        dto.setSalidasRetornos(
                cut.getSalidasRetornos()
        );

        dto.setSalidasComisiones(
                cut.getSalidasComisiones()
        );

        dto.setTotalSalidas(
                cut.getTotalSalidas()
        );

        dto.setSaldoFinal(
                cut.getSaldoFinal()
        );

        return dto;
    }

    private BigDecimal obtenerSaldoInicialCuenta(
            Long bankAccountId,
            LocalDate fecha
    ) {

        return bankAccountDailyCutRepository
                .findTopByBankAccountIdAndFechaBeforeOrderByFechaDesc(
                        bankAccountId,
                        fecha
                )
                .map(
                        BankAccountDailyCut::getSaldoFinal
                )
                .orElse(BigDecimal.ZERO);
    }
    private BigDecimal nvl(
            BigDecimal value
    ) {
        return value == null
                ? BigDecimal.ZERO
                : value;
    }

    private BankAccountBalanceDetailResponseDto calculateBalanceLive(
            BankAccount account,
            LocalDate fecha
    ) {
        BigDecimal saldoInicial =
                obtenerSaldoInicialCuenta(
                        account.getId(),
                        fecha
                );

        LocalDateTime inicio =
                fecha.atStartOfDay();

        LocalDateTime fin =
                fecha.atTime(
                        23,
                        59,
                        59
                );

        BigDecimal entradasTransferencia =
                nvl(
                        operationPaymentRepository
                                .sumEntradasTransferenciaCuenta(
                                        account.getId(),
                                        PaymentStatus.VALIDADA,
                                        inicio,
                                        fin
                                )
                );

        BigDecimal entradasDeposito =
                nvl(
                        operationPaymentRepository
                                .sumEntradasDepositoCuenta(
                                        account.getId(),
                                        PaymentStatus.VALIDADA,
                                        inicio,
                                        fin
                                )
                );

        BigDecimal totalEntradas =
                entradasTransferencia.add(entradasDeposito);

        BigDecimal salidasRetornos =
                nvl(
                        operationReturnPaymentRepository
                                .sumRetornosCuenta(
                                        account.getId(),
                                        inicio,
                                        fin
                                )
                );

        BigDecimal salidasComisiones =
                BigDecimal.ZERO;

        BigDecimal totalSalidas =
                salidasRetornos.add(salidasComisiones);

        BigDecimal saldoFinal =
                saldoInicial
                        .add(totalEntradas)
                        .subtract(totalSalidas);

        BankAccountBalanceDetailResponseDto dto =
                new BankAccountBalanceDetailResponseDto();

        dto.setBankAccountId(account.getId());
        dto.setBanco(account.getBanco());
        dto.setTitular(account.getTitular());
        dto.setNumeroCuenta(account.getNumeroCuenta());
        dto.setClabe(account.getClabe());
        dto.setFecha(fecha);
        dto.setSaldoInicial(saldoInicial);
        dto.setEntradasTransferencia(entradasTransferencia);
        dto.setEntradasDeposito(entradasDeposito);
        dto.setTotalEntradas(totalEntradas);
        dto.setSalidasRetornos(salidasRetornos);
        dto.setSalidasComisiones(salidasComisiones);
        dto.setTotalSalidas(totalSalidas);
        dto.setSaldoFinal(saldoFinal);

        return dto;
    }
}
