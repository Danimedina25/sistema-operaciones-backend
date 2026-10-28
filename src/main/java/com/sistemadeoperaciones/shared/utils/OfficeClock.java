package com.sistemadeoperaciones.shared.utils;

import java.time.LocalDate;
import java.time.ZoneId;

/** El "día" del negocio es el de la oficina, sin importar la zona horaria del servidor. */
public final class OfficeClock {

    public static final ZoneId ZONE = ZoneId.of("America/Mexico_City");

    private OfficeClock() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
