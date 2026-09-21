package com.sistemadeoperaciones.cheques;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
public record ChequeView(Long id, Long pagoId, Long operacionId, String clienteNombre,
        String numeroCheque, String bancoEmisor, String emisor, String beneficiario,
        BigDecimal monto, String moneda, String estado, boolean requiereConciliacion, long version,
        LocalDateTime fechaRecepcion, Long cuentaDestinoId, String cuentaDestinoEtiqueta,
        String destinoCobro, String comprobanteUrl, List<History> historial) {
    public record History(Long id, String accion, LocalDateTime fecha, String usuarioNombre, String motivo, String comprobanteUrl) {}
    public record Total(String moneda, BigDecimal porCobrar, BigDecimal depositados, BigDecimal cobrados) {}
    public record Page(List<ChequeView> content, int totalPages, long totalElements, List<Total> totales) {}
}
