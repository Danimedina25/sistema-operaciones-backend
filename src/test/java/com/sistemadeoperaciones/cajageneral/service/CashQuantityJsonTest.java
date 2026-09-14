package com.sistemadeoperaciones.cajageneral.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistemadeoperaciones.cajageneral.dto.OpenCashDayRequest;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class CashQuantityJsonTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    @Test void rejectsFractionalAndStringQuantitiesWithoutTruncation() {
        for (String invalid : new String[]{"1.5", "\"1\"", "2147483648"}) {
            String json = "{\"fecha\":\"2026-09-14\",\"saldoInicial\":100,\"denominaciones\":{\"D100\":" + invalid + "}}";
            assertThatThrownBy(() -> mapper.readValue(json,OpenCashDayRequest.class)).isInstanceOf(java.io.IOException.class);
        }
    }
}
