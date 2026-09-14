package com.sistemadeoperaciones.cajageneral.repository;
import com.sistemadeoperaciones.cajageneral.model.CashGeneralRegister;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
public interface CashGeneralRegisterRepository extends JpaRepository<CashGeneralRegister, Long> {
    // MySQL: el upsert obtiene lock exclusivo aun si la fila existe. INSERT IGNORE
    // tomaría lock compartido y podría causar deadlock al convertirlo a exclusivo.
    // Funciona también con ddl-auto=update, antes de la primera apertura.
    @Modifying @Query(value = "INSERT INTO cash_general_register (id) VALUES (1) ON DUPLICATE KEY UPDATE id = 1", nativeQuery = true)
    void ensureRegister();
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from CashGeneralRegister r where r.id = 1")
    CashGeneralRegister lockRegister();
}
