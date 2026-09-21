package com.sistemadeoperaciones.cheques;
import com.sistemadeoperaciones.pagos.service.PaymentOperationServiceImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
@Component @lombok.RequiredArgsConstructor @lombok.extern.slf4j.Slf4j
public class ChequeNotifications {
    private final PaymentOperationServiceImpl payments;
    @TransactionalEventListener
    public void collected(ChequeService.Changed event) {
        // A delivery failure must not report a committed collection as failed to the client.
        try { payments.notifyChequeChanged(event.paymentId(), event.action()); }
        catch (RuntimeException e) { log.error("No se pudo notificar la acción del cheque {}", event.paymentId(), e); }
    }
}
