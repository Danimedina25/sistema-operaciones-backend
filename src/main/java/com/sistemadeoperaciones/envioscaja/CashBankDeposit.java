package com.sistemadeoperaciones.envioscaja;

import com.sistemadeoperaciones.cajageneral.enums.CashDenomination;
import com.sistemadeoperaciones.cajageneral.model.CashGeneralMovement;
import com.sistemadeoperaciones.cuentasbancarias.models.BankAccount;
import com.sistemadeoperaciones.pagos.model.OperationPayment;
import com.sistemadeoperaciones.usuarios.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity @Getter @Setter
@Table(name = "cash_bank_deposits")
public class CashBankDeposit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private Long version;
    @Column(name = "request_id", nullable = false, unique = true, length = 36) private String requestId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private CashBankDepositOrigin origen;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "bank_account_id", nullable = false) private BankAccount cuentaBancaria;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cheque_payment_id") private OperationPayment chequePayment;
    @Column(nullable = false, precision = 15, scale = 2) private BigDecimal monto;
    @Column(nullable = false, length = 40) private String forma;
    @Column(length = 300) private String referencia;
    @Column(name = "comprobante_salida_url", nullable = false, length = 500) private String comprobanteSalidaUrl;
    @Column(name = "comprobante_banco_url", length = 500) private String comprobanteBancoUrl;
    @ElementCollection
    @CollectionTable(name = "cash_bank_deposit_counts", joinColumns = @JoinColumn(name = "deposit_id"))
    @MapKeyEnumerated(EnumType.STRING) @MapKeyColumn(name = "denomination", length = 10)
    @Column(name = "quantity", nullable = false)
    private Map<CashDenomination, Integer> denominaciones;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private CashBankDepositStatus estatus = CashBankDepositStatus.PENDIENTE;
    @Column(length = 500) private String motivo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "registrado_por", nullable = false) private User registradoPor;
    @Column(name = "registrado_en", nullable = false, updatable = false) private LocalDateTime registradoEn;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "resuelto_por") private User resueltoPor;
    @Column(name = "resuelto_en") private LocalDateTime resueltoEn;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "devuelto_por") private User devueltoPor;
    @Column(name = "devuelto_en") private LocalDateTime devueltoEn;
    @ElementCollection
    @CollectionTable(name = "cash_bank_deposit_return_counts", joinColumns = @JoinColumn(name = "deposit_id"))
    @MapKeyEnumerated(EnumType.STRING) @MapKeyColumn(name = "denomination", length = 10)
    @Column(name = "quantity", nullable = false)
    private Map<CashDenomination, Integer> denominacionesDevolucion;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cash_movement_id", unique = true)
    private CashGeneralMovement movimientoCaja;
    @PrePersist void prePersist() { registradoEn = LocalDateTime.now(); }
}
