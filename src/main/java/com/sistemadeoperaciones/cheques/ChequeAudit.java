package com.sistemadeoperaciones.cheques;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
@Entity @Getter @Setter
@Table(name="cheque_audit", uniqueConstraints=@UniqueConstraint(columnNames="request_id"))
public class ChequeAudit {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private Long pagoId;
    @Column(length=36) private String requestId;
    @Column(length=64) private String commandHash;
    @Column(nullable=false) private String accion;
    @Column(nullable=false) private LocalDateTime fecha;
    @Column(nullable=false) private LocalDateTime registradoEn;
    @Column(nullable=false) private Long usuarioId;
    @Column(nullable=false) private String usuarioNombre;
    @Column(length=500) private String motivo;
    @Column(length=500) private String comprobanteUrl;
    @Lob @Column(columnDefinition="LONGTEXT") private String resultado;
    @Lob @Column(columnDefinition="LONGTEXT") private String detalle;
}
