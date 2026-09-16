package com.sistemadeoperaciones.corte.service;

import com.sistemadeoperaciones.corte.dto.BankLedgerFilter;
import com.sistemadeoperaciones.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class BankLedgerServiceTest {

    @Mock BankLedgerQuery ledger;
    @InjectMocks BankLedgerService service;

    static final LocalDate HOY = LocalDate.now();

    BankLedgerFilter range(LocalDate desde, LocalDate hasta) {
        return new BankLedgerFilter(desde, hasta, null, null, null, null);
    }

    @Test
    void refusesFutureDates() {
        assertThatThrownBy(() -> service.totals(range(HOY, HOY.plusDays(1))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("fechas futuras");
    }

    @Test
    void refusesAnUnorderedOrOversizedRange() {
        assertThatThrownBy(() -> service.totals(range(HOY, HOY.minusDays(1))))
                .hasMessageContaining("anterior a la inicial");
        assertThatThrownBy(() -> service.totals(range(HOY.minusYears(3), HOY)))
                .hasMessageContaining("un año");
        assertThatThrownBy(() -> service.totals(range(null, HOY)))
                .hasMessageContaining("rango de fechas");
    }

    @Test
    void clampsThePageSize() {
        assertThatThrownBy(() -> service.search(range(HOY, HOY), 0, 500))
                .hasMessageContaining("entre 1 y 100");
        assertThatThrownBy(() -> service.search(range(HOY, HOY), -1, 20))
                .hasMessageContaining("Página inválida");
        assertThatCode(() -> service.search(range(HOY, HOY), 0, 100)).doesNotThrowAnyException();
    }
}
