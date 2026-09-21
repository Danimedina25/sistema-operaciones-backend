package com.sistemadeoperaciones.corte.repository;

import com.sistemadeoperaciones.corte.enums.DailyCashCutStatus;
import com.sistemadeoperaciones.corte.model.DailyCashCut;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyCashCutRepository extends JpaRepository<DailyCashCut, Long> {

    Optional<DailyCashCut> findByFecha(LocalDate fecha);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from DailyCashCut c where c.fecha=:fecha")
    Optional<DailyCashCut> findByFechaForUpdate(@org.springframework.data.repository.query.Param("fecha") LocalDate fecha);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<DailyCashCut> findFirstByFechaGreaterThanEqualAndEstatusOrderByFechaAsc(LocalDate fecha, DailyCashCutStatus estatus);

    boolean existsByFecha(LocalDate fecha);

    Optional<DailyCashCut> findTopByFechaBeforeOrderByFechaDesc(LocalDate fecha);

    Optional<DailyCashCut> findTopByFechaBeforeAndEstatusOrderByFechaDesc(
            LocalDate fecha,
            DailyCashCutStatus estatus
    );

    Optional<DailyCashCut> findTopByFechaOrderByFechaDesc(LocalDate fecha);

    List<DailyCashCut> findByFechaBetweenOrderByFechaAsc(
            LocalDate fechaInicio,
            LocalDate fechaFin
    );

    List<DailyCashCut> findByFechaGreaterThanEqualOrderByFechaAsc(LocalDate fecha);

    boolean existsByFechaAfter(LocalDate fecha);

    boolean existsByFechaAfterAndEstatus(
            LocalDate fecha,
            DailyCashCutStatus estatus
    );

    long countByGeneradoPorId(Long generadoPorId);
}