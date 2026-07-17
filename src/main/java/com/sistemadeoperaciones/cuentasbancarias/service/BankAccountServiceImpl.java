package com.sistemadeoperaciones.cuentasbancarias.service;


import com.sistemadeoperaciones.corte.repository.BankAccountDailyCutRepository;
import com.sistemadeoperaciones.cuentasbancarias.dto.BankAccountRequestDto;
import com.sistemadeoperaciones.cuentasbancarias.dto.BankAccountResponseDto;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.cuentasbancarias.repository.BankAccountRepository;
import com.sistemadeoperaciones.pagos.repository.OperationPaymentRepository;
import com.sistemadeoperaciones.pagos.repository.OperationReturnPaymentRepository;
import com.sistemadeoperaciones.shared.audit.service.DeletionAuditService;
import com.sistemadeoperaciones.shared.config.AuthenticatedUserService;
import com.sistemadeoperaciones.shared.crypto.CryptoService;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
import com.sistemadeoperaciones.shared.exception.EntityHasDependenciesException;
import com.sistemadeoperaciones.shared.exception.ResourceNotFoundException;
import com.sistemadeoperaciones.usuarios.model.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BankAccountServiceImpl implements BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final CryptoService cryptoService;
    private final OperationPaymentRepository operationPaymentRepository;
    private final OperationReturnPaymentRepository operationReturnPaymentRepository;
    private final BankAccountDailyCutRepository bankAccountDailyCutRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final DeletionAuditService deletionAuditService;

    public BankAccountServiceImpl(BankAccountRepository bankAccountRepository,
                                  CryptoService cryptoService,
                                  OperationPaymentRepository operationPaymentRepository,
                                  OperationReturnPaymentRepository operationReturnPaymentRepository,
                                  BankAccountDailyCutRepository bankAccountDailyCutRepository,
                                  AuthenticatedUserService authenticatedUserService,
                                  DeletionAuditService deletionAuditService) {
        this.bankAccountRepository = bankAccountRepository;
        this.cryptoService = cryptoService;
        this.operationPaymentRepository = operationPaymentRepository;
        this.operationReturnPaymentRepository = operationReturnPaymentRepository;
        this.bankAccountDailyCutRepository = bankAccountDailyCutRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.deletionAuditService = deletionAuditService;
    }

    @Override
    public BankAccountResponseDto create(BankAccountRequestDto request) {
        validateUniqueFields(request, null);

        BankAccount bankAccount = new BankAccount();
        bankAccount.setBanco(request.getBanco());
        bankAccount.setTitular(request.getTitular());
        bankAccount.setNumeroCuenta(request.getNumeroCuenta());
        bankAccount.setClabe(request.getClabe());
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
        bankAccount.setNumeroCuenta(request.getNumeroCuenta());
        bankAccount.setClabe(request.getClabe());

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

    @Override
    @Transactional
    public void delete(Long id) {
        BankAccount bankAccount = bankAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta bancaria no encontrada con id: " + id));

        Map<String, Long> dependencies = new LinkedHashMap<>();

        long pagosDestino = operationPaymentRepository.countByCuentaDestinoId(id);
        if (pagosDestino > 0) dependencies.put("pagosComoDestino", pagosDestino);

        long retornosOrigen = operationReturnPaymentRepository.countByCuentaOrigenId(id);
        if (retornosOrigen > 0) dependencies.put("retornosComoOrigen", retornosOrigen);

        long cortes = bankAccountDailyCutRepository.countByBankAccountId(id);
        if (cortes > 0) dependencies.put("cortesDiarios", cortes);

        if (!dependencies.isEmpty()) {
            throw new EntityHasDependenciesException(
                    "No se puede eliminar la cuenta bancaria porque tiene movimientos o cortes diarios asociados",
                    dependencies
            );
        }

        User currentUser = authenticatedUserService.getCurrentUser();
        deletionAuditService.record("BANK_ACCOUNT", bankAccount.getId(), bankAccount.getBanco() + " - " + bankAccount.getTitular(), currentUser);

        try {
            bankAccountRepository.delete(bankAccount);
            bankAccountRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("No se puede eliminar la cuenta bancaria porque tiene información relacionada");
        }
    }

    private void validateUniqueFields(BankAccountRequestDto request, Long id) {
        String numeroCuenta = request.getNumeroCuenta();
        String clabe = request.getClabe();

        if (id == null) {
            if (bankAccountRepository.existsByNumeroCuenta(numeroCuenta)) {
                throw new BadRequestException("Ya existe una cuenta con ese número de cuenta");
            }

            if (bankAccountRepository.existsByClabe(clabe)) {
                throw new BadRequestException("Ya existe una cuenta con esa CLABE");
            }
        } else {
            if (bankAccountRepository.existsByNumeroCuentaAndIdNot(numeroCuenta, id)) {
                throw new BadRequestException("Ya existe otra cuenta con ese número de cuenta");
            }

            if (bankAccountRepository.existsByClabeAndIdNot(clabe, id)) {
                throw new BadRequestException("Ya existe otra cuenta con esa CLABE");
            }
        }
    }

    private BankAccountResponseDto mapToResponseFull(BankAccount bankAccount) {
        String numeroCuenta = bankAccount.getNumeroCuenta();
        String clabe = bankAccount.getClabe();

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
        String numeroCuenta = bankAccount.getNumeroCuenta();
        String clabe = bankAccount.getClabe();

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