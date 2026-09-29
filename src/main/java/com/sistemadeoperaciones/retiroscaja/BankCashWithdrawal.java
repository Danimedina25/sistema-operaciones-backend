package com.sistemadeoperaciones.retiroscaja;

import com.sistemadeoperaciones.cajageneral.model.CashGeneralMovement;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.usuarios.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Retiro de efectivo de una cuenta bancaria con destino a Caja General. Lo registra la Jefa
 * de Cuentas y lo confirma la Jefa de Cajas al recibir y contar el efectivo.
 *
 * <p>Mientras está pendiente no toca ningún saldo: no es un movimiento de caja (no hay día ni
 * desglose todavía) ni del banco. Al confirmarse se crea su fila de {@code cash_general_movements},
 * que es a la vez la entrada de efectivo y la salida bancaria del libro derivado.
 */
@Entity @Getter @Setter
@Table(name = "bank_cash_withdrawals")
public class BankCashWithdrawal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "request_id", nullable = false, unique = true, length = 36)
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount cuentaBancaria;

    // Nombre del banco tal como estaba capturado al registrar, para que el histórico se lea igual.
    @Column(name = "banco_snapshot", nullable = false, length = 100)
    private String bancoSnapshot;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private WithdrawalMethod forma;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(length = 300)
    private String referencia;

    @Column(name = "comprobante_url", length = 500)
    private String comprobanteUrl;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private WithdrawalStatus estatus = WithdrawalStatus.PENDIENTE;

    // Motivo del rechazo o de la cancelación.
    @Column(length = 500)
    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "registrado_por", nullable = false)
    private User registradoPor;

    @Column(name = "registrado_en", nullable = false, updatable = false)
    private LocalDateTime registradoEn;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "resuelto_por")
    private User resueltoPor;

    @Column(name = "resuelto_en")
    private LocalDateTime resueltoEn;

    // Entrada de Caja General que produjo la confirmación. Única: un retiro entra una sola vez.
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cash_movement_id", unique = true)
    private CashGeneralMovement movimientoCaja;

    @PrePersist public void prePersist() { registradoEn = LocalDateTime.now(); }
}
