package com.sistemadeoperaciones.envioscaja;

import com.sistemadeoperaciones.notifications.enums.*;
import com.sistemadeoperaciones.notifications.service.NotificationService;
import com.sistemadeoperaciones.shared.enums.RoleName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Component @RequiredArgsConstructor @Slf4j
public class CashBankDepositNotifications {
    private final CashBankDepositRepository deposits;
    private final NotificationService notifications;
    private static final List<RoleName> CAJAS = List.of(RoleName.JEFA_CAJAS, RoleName.ADMIN);
    private static final List<RoleName> CUENTAS = List.of(RoleName.JEFA_CUENTAS, RoleName.ADMIN);

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void changed(CashBankDepositChanged event) {
        try {
            CashBankDeposit d = deposits.findById(event.id()).orElseThrow();
            String amount = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX")).format(d.getMonto());
            String detail = amount + " hacia " + d.getCuentaBancaria().getBanco();
            switch (event.status()) {
                case PENDIENTE -> send(CUENTAS, d.getOrigen() == CashBankDepositOrigin.CHEQUE
                                ? "Cheque por acreditar" : "Envío de Caja por confirmar",
                        event.actorName() + (d.getOrigen() == CashBankDepositOrigin.CHEQUE
                                ? " envió un cheque de " : " registró un depósito de ") + detail + ".",
                        NotificationType.CASH_BANK_DEPOSIT_PENDING, d, "/cuentas/entradas?view=deposits", NotificationPriority.HIGH);
                case CONFIRMADO -> send(CAJAS, d.getOrigen() == CashBankDepositOrigin.CHEQUE
                                ? "Cheque acreditado" : "Envío confirmado por Cuentas",
                        event.actorName() + " confirmó " + (d.getOrigen() == CashBankDepositOrigin.CHEQUE
                                ? "el cheque de " : "el depósito de ") + detail + ".",
                        NotificationType.CASH_BANK_DEPOSIT_CONFIRMED, d, "/caja/salidas?view=deposits", NotificationPriority.MEDIUM);
                case RECHAZADO_PENDIENTE_DEVOLUCION -> send(CAJAS, "Envío rechazado: confirma la devolución",
                        event.actorName() + " rechazó el depósito de " + detail + ". Motivo: " + d.getMotivo(),
                        NotificationType.CASH_BANK_DEPOSIT_REJECTED, d, "/caja/salidas?view=deposits", NotificationPriority.HIGH);
                case CANCELADO -> {
                    if (d.getOrigen() == CashBankDepositOrigin.CHEQUE)
                        send(CAJAS, "Cheque no acreditado",
                                event.actorName() + " rechazó el cheque de " + detail + ". Motivo: " + d.getMotivo(),
                                NotificationType.CASH_BANK_DEPOSIT_REJECTED, d, "/caja/entradas", NotificationPriority.HIGH);
                    else send(CUENTAS, "Devolución a Caja confirmada",
                            event.actorName() + " contó la devolución de " + detail + ".",
                            NotificationType.CASH_BANK_DEPOSIT_RETURNED, d, "/cuentas/entradas?view=deposits", NotificationPriority.MEDIUM);
                }
            }
        } catch (RuntimeException exception) {
            log.error("No se pudo notificar el envío {}", event.id(), exception);
        }
    }
    private void send(List<RoleName> roles, String title, String message, NotificationType type,
                      CashBankDeposit d, String path, NotificationPriority priority) {
        notifications.createForRoles(roles, title, message, type, NotificationModule.PAGOS,
                NotificationReferenceType.CASH_BANK_DEPOSIT, d.getId(), path, priority);
    }
}
