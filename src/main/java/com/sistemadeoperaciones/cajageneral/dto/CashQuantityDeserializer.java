package com.sistemadeoperaciones.cajageneral.dto;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

/** Evita que Jackson trunque 1.5 billetes a 1 antes de la validación financiera. */
public class CashQuantityDeserializer extends JsonDeserializer<Integer> {
    @Override public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT))
            return (Integer) context.handleUnexpectedToken(Integer.class, parser);
        return parser.getIntValue();
    }
}
