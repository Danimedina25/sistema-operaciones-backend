package com.sistemadeoperaciones.transferencias;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BankTransferRepository extends JpaRepository<BankTransfer, Long> {
    Optional<BankTransfer> findByRequestId(String requestId);

    @EntityGraph(attributePaths = {"cuentaOrigen", "cuentaDestino", "registradoPor"})
    List<BankTransfer> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDescIdDesc(
            LocalDateTime inicio, LocalDateTime fin);
}
