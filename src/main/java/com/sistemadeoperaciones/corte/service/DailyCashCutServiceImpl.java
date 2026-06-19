package com.sistemadeoperaciones.corte.service;

import com.sistemadeoperaciones.comisionessocioscomerciales.repository.CommercialPartnerCommissionRepository;
import com.sistemadeoperaciones.corte.dto.CashCutRangeResponse;
import com.sistemadeoperaciones.corte.dto.DailyCashCutRequest;
import com.sistemadeoperaciones.corte.dto.DailyCashCutResponse;
import com.sistemadeoperaciones.corte.enums.DailyCashCutStatus;
import com.sistemadeoperaciones.corte.exceptions.CashCutDateRequiredException;
import com.sistemadeoperaciones.corte.exceptions.DailyCashCutAlreadyExistsException;
import com.sistemadeoperaciones.corte.exceptions.InitialCashBalanceRequiredException;
import com.sistemadeoperaciones.corte.exceptions.InvalidCashCutDateRangeException;
import com.sistemadeoperaciones.corte.model.DailyCashCut;
import com.sistemadeoperaciones.corte.repository.DailyCashCutRepository;
import com.sistemadeoperaciones.pagos.enums.CommissionStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentStatus;
import com.sistemadeoperaciones.pagos.enums.PaymentType;
import com.sistemadeoperaciones.pagos.repository.OperationPaymentRepository;
import com.sistemadeoperaciones.pagos.repository.OperationReturnPaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class DailyCashCutServiceImpl implements DailyCashCutService {

    private final DailyCashCutRepository dailyCashCutRepository;
    private final OperationPaymentRepository operationPaymentRepository;
    private final OperationReturnPaymentRepository operationReturnPaymentRepository;
    private final CommercialPartnerCommissionRepository commercialPartnerCommissionRepository;

    public DailyCashCutServiceImpl(
            DailyCashCutRepository dailyCashCutRepository,
            OperationPaymentRepository operationPaymentRepository,
            OperationReturnPaymentRepository operationReturnPaymentRepository,
            CommercialPartnerCommissionRepository commercialPartnerCommissionRepository
    ) {
        this.dailyCashCutRepository = dailyCashCutRepository;
        this.operationPaymentRepository = operationPaymentRepository;
        this.operationReturnPaymentRepository = operationReturnPaymentRepository;
        this.commercialPartnerCommissionRepository = commercialPartnerCommissionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DailyCashCutResponse calculateDailyCut(LocalDate fecha) {
        if (fecha == null) {
            throw new CashCutDateRequiredException();
        }

        BigDecimal saldoInicial = obtenerSaldoInicial(fecha);

        return calculateDailyCut(fecha, saldoInicial);
    }

    @Override
    @Transactional
    public DailyCashCutResponse registerDailyCut(LocalDate fecha) {
        DailyCashCutRequest request = new DailyCashCutRequest();
        request.setFecha(fecha);

        return registerDailyCut(request);
    }

    @Override
    @Transactional
    public DailyCashCutResponse registerDailyCut(DailyCashCutRequest request) {
        if (request == null || request.getFecha() == null) {
            throw new CashCutDateRequiredException();
        }

        LocalDate fecha = request.getFecha();

        if (dailyCashCutRepository.existsByFecha(fecha)) {
            throw new DailyCashCutAlreadyExistsException(fecha);
        }

        boolean esPrimerCorte = dailyCashCutRepository
                .findTopByFechaBeforeOrderByFechaDesc(fecha)
                .isEmpty();

        BigDecimal saldoInicial;

        if (esPrimerCorte) {
            if (request.getSaldoInicialManual() == null) {
                throw new InitialCashBalanceRequiredException();
            }

            saldoInicial = request.getSaldoInicialManual();
        } else {
            saldoInicial = obtenerSaldoInicial(fecha);
        }

        DailyCashCutResponse calculado = calculateDailyCut(fecha, saldoInicial);

        DailyCashCut corte = new DailyCashCut();

        corte.setFecha(fecha);
        corte.setSaldoInicial(calculado.getSaldoInicial());
        corte.setSaldoFinal(calculado.getSaldoFinal());

        corte.setEntradasTransferencia(calculado.getEntradasTransferencia());
        corte.setEntradasDeposito(calculado.getEntradasDeposito());
        corte.setEntradasEfectivo(calculado.getEntradasEfectivo());
        corte.setTotalEntradas(calculado.getTotalEntradas());

        corte.setRetornosTransferencia(calculado.getRetornosTransferencia());
        corte.setRetornosDeposito(calculado.getRetornosDeposito());
        corte.setRetornosEfectivo(calculado.getRetornosEfectivo());
        corte.setTotalRetornos(calculado.getTotalRetornos());

        corte.setTotalComisionesSocios(calculado.getTotalComisionesSocios());
        corte.setTotalComisionesOficina(calculado.getTotalComisionesOficina());

        corte.setTotalSalidas(calculado.getTotalSalidas());

        corte.setEstatus(DailyCashCutStatus.CERRADO);
        corte.setObservaciones(request.getObservaciones());
        corte.setCerradoAutomaticamente(true);
        corte.setFechaCierre(LocalDateTime.now());

        DailyCashCut saved = dailyCashCutRepository.save(corte);

        calculado.setRegistrado(true);
        calculado.setFechaCierre(saved.getFechaCierre());

        return calculado;
    }

    @Override
    @Transactional(readOnly = true)
    public CashCutRangeResponse calculateRangeCut(
            LocalDate fechaInicio,
            LocalDate fechaFin
    ) {
        if (fechaInicio == null || fechaFin == null) {
            throw new CashCutDateRequiredException(
                    "La fecha inicio y fecha fin son obligatorias"
            );
        }

        if (fechaInicio.isAfter(fechaFin)) {
            throw new InvalidCashCutDateRangeException(
                    "La fecha inicio no puede ser mayor a la fecha fin"
            );
        }

        List<DailyCashCut> cortes = dailyCashCutRepository
                .findByFechaBetweenOrderByFechaAsc(
                        fechaInicio,
                        fechaFin
                );

        BigDecimal saldoInicial = cortes.isEmpty()
                ? obtenerSaldoInicial(fechaInicio)
                : cortes.get(0).getSaldoInicial();

        BigDecimal entradasTransferencia = BigDecimal.ZERO;
        BigDecimal entradasDeposito = BigDecimal.ZERO;
        BigDecimal entradasEfectivo = BigDecimal.ZERO;
        BigDecimal totalEntradas = BigDecimal.ZERO;

        BigDecimal retornosTransferencia = BigDecimal.ZERO;
        BigDecimal retornosDeposito = BigDecimal.ZERO;
        BigDecimal retornosEfectivo = BigDecimal.ZERO;
        BigDecimal totalRetornos = BigDecimal.ZERO;

        BigDecimal totalComisionesSocios = BigDecimal.ZERO;
        BigDecimal totalComisionesOficina = BigDecimal.ZERO;
        BigDecimal totalSalidas = BigDecimal.ZERO;

        for (DailyCashCut corte : cortes) {
            entradasTransferencia = entradasTransferencia
                    .add(corte.getEntradasTransferencia());

            entradasDeposito = entradasDeposito
                    .add(corte.getEntradasDeposito());

            entradasEfectivo = entradasEfectivo
                    .add(corte.getEntradasEfectivo());

            totalEntradas = totalEntradas
                    .add(corte.getTotalEntradas());

            retornosTransferencia = retornosTransferencia
                    .add(corte.getRetornosTransferencia());

            retornosDeposito = retornosDeposito
                    .add(corte.getRetornosDeposito());

            retornosEfectivo = retornosEfectivo
                    .add(corte.getRetornosEfectivo());

            totalRetornos = totalRetornos
                    .add(corte.getTotalRetornos());

            totalComisionesSocios = totalComisionesSocios
                    .add(corte.getTotalComisionesSocios());

            totalComisionesOficina = totalComisionesOficina
                    .add(corte.getTotalComisionesOficina());

            totalSalidas = totalSalidas
                    .add(corte.getTotalSalidas());
        }

        BigDecimal saldoFinal = saldoInicial
                .add(totalEntradas)
                .subtract(totalSalidas);

        CashCutRangeResponse response = new CashCutRangeResponse();

        response.setFechaInicio(fechaInicio);
        response.setFechaFin(fechaFin);

        response.setSaldoInicial(saldoInicial);
        response.setSaldoFinal(saldoFinal);

        response.setEntradasTransferencia(entradasTransferencia);
        response.setEntradasDeposito(entradasDeposito);
        response.setEntradasEfectivo(entradasEfectivo);
        response.setTotalEntradas(totalEntradas);

        response.setRetornosTransferencia(retornosTransferencia);
        response.setRetornosDeposito(retornosDeposito);
        response.setRetornosEfectivo(retornosEfectivo);
        response.setTotalRetornos(totalRetornos);

        response.setTotalComisionesSocios(totalComisionesSocios);
        response.setTotalComisionesOficina(totalComisionesOficina);
        response.setTotalSalidas(totalSalidas);

        response.setIncluyeDiaActualEnVivo(false);

        return response;
    }

    private DailyCashCutResponse calculateDailyCut(
            LocalDate fecha,
            BigDecimal saldoInicial
    ) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(LocalTime.MAX);

        BigDecimal entradasTransferencia = sumEntradaByTipo(
                PaymentType.TRANSFERENCIA,
                inicio,
                fin
        );

        BigDecimal entradasDeposito = sumEntradaByTipo(
                PaymentType.DEPOSITO,
                inicio,
                fin
        );

        BigDecimal entradasEfectivo = sumEntradaByTipo(
                PaymentType.EFECTIVO,
                inicio,
                fin
        );

        BigDecimal totalComisionesOficina = sumComisionesOficina(
                inicio,
                fin
        );

        BigDecimal totalEntradas = entradasTransferencia
                .add(entradasDeposito)
                .add(entradasEfectivo);

        BigDecimal retornosTransferencia = sumRetornoByTipo(
                PaymentType.TRANSFERENCIA,
                inicio,
                fin
        );

        BigDecimal retornosDeposito = sumRetornoByTipo(
                PaymentType.DEPOSITO,
                inicio,
                fin
        );

        BigDecimal retornosEfectivo = sumRetornoByTipo(
                PaymentType.EFECTIVO,
                inicio,
                fin
        );

        BigDecimal totalRetornos = retornosTransferencia
                .add(retornosDeposito)
                .add(retornosEfectivo);

        BigDecimal totalComisionesSocios = nvl(
                commercialPartnerCommissionRepository
                        .sumPaidCommissionsBetween(
                                inicio,
                                fin,
                                CommissionStatus.PAGADA
                        )
        );

        BigDecimal totalSalidas = totalRetornos
                .add(totalComisionesSocios);

        BigDecimal saldoFinal = saldoInicial
                .add(totalEntradas)
                .subtract(totalSalidas);

        DailyCashCutResponse response = new DailyCashCutResponse();

        response.setFecha(fecha);
        response.setSaldoInicial(saldoInicial);
        response.setSaldoFinal(saldoFinal);

        response.setEntradasTransferencia(entradasTransferencia);
        response.setEntradasDeposito(entradasDeposito);
        response.setEntradasEfectivo(entradasEfectivo);
        response.setTotalEntradas(totalEntradas);

        response.setRetornosTransferencia(retornosTransferencia);
        response.setRetornosDeposito(retornosDeposito);
        response.setRetornosEfectivo(retornosEfectivo);
        response.setTotalRetornos(totalRetornos);

        response.setTotalComisionesSocios(totalComisionesSocios);
        response.setTotalComisionesOficina(totalComisionesOficina);
        response.setTotalSalidas(totalSalidas);

        response.setRegistrado(false);

        return response;
    }

    private BigDecimal obtenerSaldoInicial(LocalDate fecha) {
        return dailyCashCutRepository
                .findTopByFechaBeforeOrderByFechaDesc(fecha)
                .map(DailyCashCut::getSaldoFinal)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal sumEntradaByTipo(
            PaymentType tipoPago,
            LocalDateTime inicio,
            LocalDateTime fin
    ) {
        return nvl(
                operationPaymentRepository.sumValidatedPaymentsByTypeBetween(
                        tipoPago,
                        PaymentStatus.VALIDADA,
                        inicio,
                        fin
                )
        );
    }

    private BigDecimal sumRetornoByTipo(
            PaymentType tipoPago,
            LocalDateTime inicio,
            LocalDateTime fin
    ) {
        return nvl(
                operationReturnPaymentRepository.sumPaidReturnsByTypeBetween(
                        tipoPago,
                        inicio,
                        fin
                )
        );
    }

    private BigDecimal sumComisionesOficina(
            LocalDateTime inicio,
            LocalDateTime fin
    ) {
        return nvl(
                operationPaymentRepository.sumOfficeCommissionsBetween(
                        inicio,
                        fin,
                        PaymentStatus.VALIDADA
                )
        );
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}