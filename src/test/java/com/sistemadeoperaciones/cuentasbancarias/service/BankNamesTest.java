package com.sistemadeoperaciones.cuentasbancarias.service;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class BankNamesTest {

    final BankNames catalog = BankNames.of(List.of(
            "BANORTE", "Banorte ", "banorte",
            "SCOTIABANK PFAE", "SCOTIA NOMINA", "Scotiabank",
            "Banco del Bajío", "BAJIO",
            "INBURSA", "BBVA", "BBVA Bancomer", "Kapital", "HSBC", "Banco Azteca"));

    @Test
    void theSameBankWrittenDifferentlyIsOneGroup() {
        assertThat(catalog.display("Banorte")).isEqualTo("BANORTE");
        assertThat(catalog.display("  BANORTE ")).isEqualTo("BANORTE");
        assertThat(catalog.display("BBVA Bancomer")).isEqualTo("BBVA");
    }

    @Test
    void aPartOfTheNameStillMatches() {
        assertThat(catalog.display("SCOTIA NOMINA")).isEqualTo("SCOTIABANK");
        assertThat(catalog.display("SCOTIABANK PFAE")).isEqualTo("SCOTIABANK");
        assertThat(catalog.display("Scotia")).isEqualTo("SCOTIABANK");
    }

    @Test
    void genericWordsAndAccentsDoNotMatter() {
        assertThat(catalog.display("Banco del Bajío")).isEqualTo("BAJIO");
        assertThat(catalog.display("BanBajío")).isEqualTo("BAJIO");
        assertThat(catalog.display("Banco Azteca")).isEqualTo("AZTECA");
    }

    @Test
    void differentBanksStayApart() {
        assertThat(catalog.display("INBURSA")).isEqualTo("INBURSA");
        assertThat(catalog.display("HSBC")).isEqualTo("HSBC");
        assertThat(catalog.display("Kapital")).isEqualTo("KAPITAL");
        assertThat(catalog.display("Banorte")).isNotEqualTo(catalog.display("Banco Azteca"));
    }

    @Test
    void anUnknownNameIsItsOwnBank() {
        assertThat(catalog.display("Santander")).isEqualTo("SANTANDER");
        assertThat(catalog.display(null)).isEmpty();
    }
}
