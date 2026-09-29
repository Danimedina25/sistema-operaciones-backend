package com.sistemadeoperaciones.retiroscaja;

import com.sistemadeoperaciones.notifications.enums.NotificationModule;
import com.sistemadeoperaciones.notifications.enums.NotificationPriority;
import com.sistemadeoperaciones.notifications.enums.NotificationReferenceType;
import com.sistemadeoperaciones.notifications.enums.NotificationType;
import com.sistemadeoperaciones.notifications.service.NotificationService;
import com.sistemadeoperaciones.shared.enums.RoleName;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/** Avisa a la otra área cuando un retiro cambia. Sólo después de confirmada la transacción. */
@Component @lombok.RequiredArgsConstructor @lombok.extern.slf4j.Slf4j
public class WithdrawalNotifications {
    private static final List<RoleName> CAJAS = List.of(RoleName.JEFA_CAJAS, RoleName.ADMIN);
    private static final List<RoleName> CUENTAS = List.of(RoleName.JEFA_CUENTAS, RoleName.AUXILIAR_CUENTAS, RoleName.ADMIN);

    private final BankCashWithdrawalRepository withdrawals;
    private final NotificationService notifications;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void changed(WithdrawalChanged event) {
        // Una falla al avisar no debe reportar como fallido un retiro que ya quedó guardado.
        try {
            BankCashWithdrawal w = withdrawals.findById(event.withdrawalId()).orElseThrow();
            String monto = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX")).format(w.getMonto());
            String detalle = w.getForma().label().toLowerCase(Locale.ROOT) + " de " + monto + " de " + w.getBancoSnapshot();
            switch (event.estatus()) {
                case PENDIENTE -> send(CAJAS, "Nuevo retiro por confirmar",
                        event.actorName() + " registró un " + detalle + " para Caja General.",
                        NotificationType.BANK_WITHDRAWAL_PENDING, w, "/caja-general", NotificationPriority.HIGH);
                case CONFIRMADO -> send(CUENTAS, "Retiro recibido en Caja General",
                        event.actorName() + " confirmó el " + detalle + ".",
                        NotificationType.BANK_WITHDRAWAL_CONFIRMED, w, "/corte", NotificationPriority.MEDIUM);
                case RECHAZADO -> send(CUENTAS, "Retiro rechazado por Caja General",
                        event.actorName() + " rechazó el " + detalle + ". Motivo: " + w.getMotivo(),
                        NotificationType.BANK_WITHDRAWAL_REJECTED, w, "/corte", NotificationPriority.HIGH);
                case CANCELADO -> send(CAJAS, "Retiro cancelado por Cuentas",
                        event.actorName() + " canceló el " + detalle + ". Motivo: " + w.getMotivo(),
                        NotificationType.BANK_WITHDRAWAL_CANCELLED, w, "/caja-general", NotificationPriority.MEDIUM);
            }
        } catch (RuntimeException e) {
            log.error("No se pudo notificar el retiro {}", event.withdrawalId(), e);
        }
    }

    private void send(List<RoleName> roles, String title, String message, NotificationType type,
                      BankCashWithdrawal w, String url, NotificationPriority priority) {
        notifications.createForRoles(roles, title, message, type, NotificationModule.PAGOS,
                NotificationReferenceType.BANK_CASH_WITHDRAWAL, w.getId(), url, priority);
    }
}
