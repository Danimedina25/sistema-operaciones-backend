package com.sistemadeoperaciones.transferencias;

import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.usuarios.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transferencia entre dos cuentas propias. Es una fuente más del libro bancario derivado:
 * la misma fila produce la salida de la cuenta origen y la entrada de la destino, así que
 * es imposible que exista una sin la otra. No toca Caja General ni el corte global, donde
 * sería neutra.
 */
@Entity @Getter @Setter
@Table(name = "bank_transfers")
public class BankTransfer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true, length = 36)
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cuenta_origen_id", nullable = false)
    private BankAccount cuentaOrigen;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cuenta_destino_id", nullable = false)
    private BankAccount cuentaDestino;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(length = 300)
    private String referencia;

    @Column(name = "comprobante_url", length = 500)
    private String comprobanteUrl;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "registrado_por", nullable = false)
    private User registradoPor;

    // Fecha contable: la transferencia se registra el día en que ocurre, igual que el resto
    // de las fuentes del libro, y así nunca modifica un corte ya registrado.
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist public void prePersist() { createdAt = LocalDateTime.now(); }
}
