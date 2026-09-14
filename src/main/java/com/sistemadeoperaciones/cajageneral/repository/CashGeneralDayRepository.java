package com.sistemadeoperaciones.cajageneral.repository;
import com.sistemadeoperaciones.cajageneral.model.CashGeneralDay;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;
public interface CashGeneralDayRepository extends JpaRepository<CashGeneralDay, Long> {
    Optional<CashGeneralDay> findFirstByOrderByFechaDesc();
    List<CashGeneralDay> findByFechaBetweenOrderByFechaAsc(LocalDate start, LocalDate end);
}
