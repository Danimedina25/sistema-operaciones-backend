package com.sistemadeoperaciones.corte.service;

import com.sistemadeoperaciones.corte.dto.BankAccountBalanceDetailResponseDto;
import com.sistemadeoperaciones.corte.dto.BankAccountBalanceResponseDto;
import com.sistemadeoperaciones.corte.dto.BankGroupBalanceResponseDto;

import java.time.LocalDate;
import java.util.List;

public interface BankAccountDailyCutService {

    BankAccountBalanceDetailResponseDto calculateBalance(
            Long bankAccountId,
            LocalDate fecha
    );

    List<BankAccountBalanceResponseDto> calculateBalances(
            LocalDate fecha
    );

    List<BankGroupBalanceResponseDto> calculateBalancesGrouped(
            LocalDate fecha
    );

    void registerDailyCut(
            LocalDate fecha
    );

    /**
     * Regenera los cortes ya registrados de una cuenta desde una fecha en adelante y
     * devuelve cuántos se rehicieron. Lo usa la eliminación administrativa de un corte de
     * Caja General que contenía cheques cobrados.
     */
    int recalculateFrom(
            Long bankAccountId,
            LocalDate fecha
    );

    /**
     * Regenera los cortes ya registrados de todas las cuentas desde una fecha en adelante.
     * Repara cortes que omitieron movimientos de días sin corte; nunca crea cortes nuevos.
     */
    int recalculateAllFrom(
            LocalDate fecha
    );
}