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
}