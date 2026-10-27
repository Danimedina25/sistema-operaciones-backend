package com.sistemadeoperaciones.cuentasbancarias.repository;

import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from BankAccount a where a.id = :id")
    Optional<BankAccount> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsByNumeroCuenta(String numeroCuenta);

    boolean existsByClabe(String clabe);

    Optional<BankAccount> findByNumeroCuenta(String numeroCuenta);

    Optional<BankAccount> findByClabe(String clabe);

    boolean existsByNumeroCuentaAndIdNot(String numeroCuenta, Long id);

    boolean existsByClabeAndIdNot(String clabe, Long id);

    List<BankAccount> findByActivoTrue();
}
