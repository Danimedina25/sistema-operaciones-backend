package com.sistemadeoperaciones.cuentasbancarias.service;

import com.sistemadeoperaciones.cuentasbancarias.dto.BankAccountRequestDto;
import com.sistemadeoperaciones.cuentasbancarias.dto.BankAccountResponseDto;

import java.util.List;

public interface BankAccountService {

    BankAccountResponseDto create(BankAccountRequestDto request);

    List<BankAccountResponseDto> findAll();

    BankAccountResponseDto findById(Long id);

    BankAccountResponseDto update(Long id, BankAccountRequestDto request);

    BankAccountResponseDto deactivate(Long id);

    BankAccountResponseDto activate(Long id);

    void delete(Long id);
}