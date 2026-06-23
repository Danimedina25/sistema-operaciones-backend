package com.sistemadeoperaciones.corte.scheduler;

import com.sistemadeoperaciones.corte.exceptions.DailyCashCutAlreadyExistsException;
import com.sistemadeoperaciones.corte.exceptions.InitialCashBalanceRequiredException;
import com.sistemadeoperaciones.corte.service.BankAccountDailyCutService;
import com.sistemadeoperaciones.corte.service.DailyCashCutService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Component
public class DailyCashCutScheduler {

    private final DailyCashCutService dailyCashCutService;

    private final BankAccountDailyCutService bankAccountDailyCutService;

    public DailyCashCutScheduler(
            DailyCashCutService dailyCashCutService,
            BankAccountDailyCutService bankAccountDailyCutService
    ) {
        this.dailyCashCutService = dailyCashCutService;
        this.bankAccountDailyCutService = bankAccountDailyCutService;
    }

    /**
     * Ejecuta todos los días a las 00:05
     * y registra el corte del día anterior.
     */
    @Scheduled(
            cron = "0 5 0 * * *",
            zone = "America/Mexico_City"
    )
   /* @Scheduled(
            cron = "0 * * * * *",
            zone = "America/Mexico_City"
    )*/
    public void registerPreviousDayCashCut() {

        System.out.println("Scheduler ejecutado");

        LocalDate fechaACerrar = LocalDate
                .now(ZoneId.of("America/Mexico_City"))
                .minusDays(1);

        try {

            dailyCashCutService.registerDailyCut(
                    fechaACerrar
            );

            bankAccountDailyCutService.registerDailyCut(
                    fechaACerrar
            );

            log.info(
                    "Corte diario registrado correctamente para {}",
                    fechaACerrar
            );

        } catch (DailyCashCutAlreadyExistsException ex) {

            log.warn(
                    "Ya existe un corte registrado para {}",
                    fechaACerrar
            );

        } catch (InitialCashBalanceRequiredException ex) {

            log.error(
                    "No se pudo registrar el primer corte. Falta saldo inicial manual."
            );

        } catch (Exception ex) {

            log.error(
                    "Error registrando corte diario para {}",
                    fechaACerrar,
                    ex
            );
        }
    }
}