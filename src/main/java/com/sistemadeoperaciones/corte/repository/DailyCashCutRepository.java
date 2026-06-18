package com.sistemadeoperaciones.corte.repository;

import com.sistemadeoperaciones.corte.enums.DailyCashCutStatus;
import com.sistemadeoperaciones.corte.model.DailyCashCut;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyCashCutRepository extends JpaRepository<DailyCashCut, Long> {

    Optional<DailyCashCut> findByFecha(LocalDate fecha);

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

    boolean existsByFechaAfter(LocalDate fecha);

    boolean existsByFechaAfterAndEstatus(
            LocalDate fecha,
            DailyCashCutStatus estatus
    );
}