package com.sistemadeoperaciones.corte.repository;

import com.sistemadeoperaciones.corte.model.BankAccountDailyCut;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BankAccountDailyCutRepository
        extends JpaRepository<BankAccountDailyCut, Long> {

    /**
     * Obtiene el corte de una cuenta para una fecha específica.
     */
    Optional<BankAccountDailyCut> findByBankAccountIdAndFecha(
            Long bankAccountId,
            LocalDate fecha
    );

    /**
     * Obtiene el último corte anterior a una fecha.
     * Se usa para determinar el saldo inicial.
     */
    Optional<BankAccountDailyCut>
    findTopByBankAccountIdAndFechaBeforeOrderByFechaDesc(
            Long bankAccountId,
            LocalDate fecha
    );

    /**
     * Obtiene todos los cortes de una fecha.
     */
    List<BankAccountDailyCut> findByFecha(
            LocalDate fecha
    );

    /**
     * Obtiene todos los cortes de una cuenta en un rango.
     */
    List<BankAccountDailyCut>
    findByBankAccountIdAndFechaBetweenOrderByFechaAsc(
            Long bankAccountId,
            LocalDate fechaInicio,
            LocalDate fechaFin
    );

    /**
     * Verifica si ya existe un corte para la cuenta y fecha.
     */
    boolean existsByBankAccountIdAndFecha(
            Long bankAccountId,
            LocalDate fecha
    );

    /**
     * Obtiene todos los cortes de una fecha ordenados por banco y cuenta.
     */
    List<BankAccountDailyCut>
    findByFechaOrderByBankAccountBancoAscBankAccountTitularAsc(
            LocalDate fecha
    );

    /**
     * Obtiene todos los cortes de una cuenta.
     * Útil para históricos completos.
     */
    List<BankAccountDailyCut>
    findByBankAccountIdOrderByFechaAsc(
            Long bankAccountId
    );

    Optional<BankAccountDailyCut>
    findTopByBankAccountIdOrderByFechaDesc(
            Long bankAccountId
    );

    List<BankAccountDailyCut>
    findByFechaBetweenOrderByFechaAsc(
            LocalDate fechaInicio,
            LocalDate fechaFin
    );
}