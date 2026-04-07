package com.sistemadeoperaciones.cuentasbancarias.service;


import com.sistemadeoperaciones.cuentasbancarias.dto.BankAccountRequestDto;
import com.sistemadeoperaciones.cuentasbancarias.dto.BankAccountResponseDto;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.cuentasbancarias.repository.BankAccountRepository;
import com.sistemadeoperaciones.shared.crypto.CryptoService;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BankAccountServiceImpl implements BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final CryptoService cryptoService;

    public BankAccountServiceImpl(BankAccountRepository bankAccountRepository,
                                  CryptoService cryptoService) {
        this.bankAccountRepository = bankAccountRepository;
        this.cryptoService = cryptoService;
    }

    @Override
    public BankAccountResponseDto create(BankAccountRequestDto request) {
        validateUniqueFields(request, null);

        BankAccount bankAccount = new BankAccount();
        bankAccount.setBanco(request.getBanco());
        bankAccount.setTitular(request.getTitular());
        bankAccount.setNumeroCuenta(cryptoService.encrypt(request.getNumeroCuenta()));
        bankAccount.setClabe(cryptoService.encrypt(request.getClabe()));
        bankAccount.setActivo(request.getActivo() != null ? request.getActivo() : true);

        BankAccount saved = bankAccountRepository.save(bankAccount);
        return mapToResponseFull(saved);
    }

    @Override
    public List<BankAccountResponseDto> findAll() {
        return bankAccountRepository.findAll()
                .stream()
                .map(this::mapToResponseMasked)
                .toList();
    }

    @Override
    public BankAccountResponseDto findById(Long id) {
        BankAccount bankAccount = bankAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta bancaria no encontrada con id: " + id));

        if (canViewSensitiveData()) {
            return mapToResponseFull(bankAccount);
        }
        return mapToResponseMasked(bankAccount);
    }

    @Override
    public BankAccountResponseDto update(Long id, BankAccountRequestDto request) {
        BankAccount bankAccount = bankAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta bancaria no encontrada con id: " + id));

        validateUniqueFields(request, id);

        bankAccount.setBanco(request.getBanco());
        bankAccount.setTitular(request.getTitular());
        bankAccount.setNumeroCuenta(cryptoService.encrypt(request.getNumeroCuenta()));
        bankAccount.setClabe(cryptoService.encrypt(request.getClabe()));

        if (request.getActivo() != null) {
            bankAccount.setActivo(request.getActivo());
        }

        BankAccount updated = bankAccountRepository.save(bankAccount);
        return mapToResponseFull(updated);
    }

    @Override
    public BankAccountResponseDto deactivate(Long id) {
        BankAccount bankAccount = bankAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta bancaria no encontrada con id: " + id));

        if (!bankAccount.getActivo()) {
            throw new BadRequestException("La cuenta bancaria ya se encuentra inactiva");
        }

        bankAccount.setActivo(false);

        BankAccount updated = bankAccountRepository.save(bankAccount);
        return mapToResponseMasked(updated);
    }

    @Override
    public BankAccountResponseDto activate(Long id) {
        BankAccount bankAccount = bankAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta bancaria no encontrada con id: " + id));

        if (bankAccount.getActivo()) {
            throw new BadRequestException("La cuenta bancaria ya se encuentra activa");
        }

        bankAccount.setActivo(true);

        BankAccount updated = bankAccountRepository.save(bankAccount);
        return mapToResponseMasked(updated);
    }

    private void validateUniqueFields(BankAccountRequestDto request, Long id) {
        String encryptedNumeroCuenta = cryptoService.encrypt(request.getNumeroCuenta());
        String encryptedClabe = cryptoService.encrypt(request.getClabe());

        if (id == null) {
            if (bankAccountRepository.existsByNumeroCuenta(encryptedNumeroCuenta)) {
                throw new BadRequestException("Ya existe una cuenta con ese número de cuenta");
            }
            if (bankAccountRepository.existsByClabe(encryptedClabe)) {
                throw new BadRequestException("Ya existe una cuenta con esa CLABE");
            }
        } else {
            if (bankAccountRepository.existsByNumeroCuentaAndIdNot(encryptedNumeroCuenta, id)) {
                throw new BadRequestException("Ya existe otra cuenta con ese número de cuenta");
            }
            if (bankAccountRepository.existsByClabeAndIdNot(encryptedClabe, id)) {
                throw new BadRequestException("Ya existe otra cuenta con esa CLABE");
            }
        }
    }

    private BankAccountResponseDto mapToResponseFull(BankAccount bankAccount) {
        String numeroCuenta = cryptoService.decrypt(bankAccount.getNumeroCuenta());
        String clabe = cryptoService.decrypt(bankAccount.getClabe());

        return new BankAccountResponseDto(
                bankAccount.getId(),
                bankAccount.getBanco(),
                bankAccount.getTitular(),
                numeroCuenta,
                clabe,
                bankAccount.getActivo(),
                bankAccount.getCreatedAt(),
                bankAccount.getUpdatedAt()
        );
    }

    private BankAccountResponseDto mapToResponseMasked(BankAccount bankAccount) {
        String numeroCuenta = cryptoService.decrypt(bankAccount.getNumeroCuenta());
        String clabe = cryptoService.decrypt(bankAccount.getClabe());

        return new BankAccountResponseDto(
                bankAccount.getId(),
                bankAccount.getBanco(),
                bankAccount.getTitular(),
                maskAccountNumber(numeroCuenta),
                maskClabe(clabe),
                bankAccount.getActivo(),
                bankAccount.getCreatedAt(),
                bankAccount.getUpdatedAt()
        );
    }

    private String maskAccountNumber(String numeroCuenta) {
        if (numeroCuenta == null || numeroCuenta.length() <= 4) {
            return "****";
        }
        return "*".repeat(numeroCuenta.length() - 4) + numeroCuenta.substring(numeroCuenta.length() - 4);
    }

    private String maskClabe(String clabe) {
        if (clabe == null || clabe.length() <= 4) {
            return "****";
        }
        return "*".repeat(clabe.length() - 4) + clabe.substring(clabe.length() - 4);
    }

    private boolean canViewSensitiveData() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_GERENTE"));
    }
}