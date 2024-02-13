package uk.gov.pay.api.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import uk.gov.pay.api.exception.PaymentValidationException;

import java.io.IOException;

import static uk.gov.pay.api.model.RequestError.Code.CREATE_PAYMENT_VALIDATION_ERROR;
import static uk.gov.pay.api.model.RequestError.aRequestError;

public class StringDeserializer extends StdDeserializer<String> {
    
    public StringDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.hasToken(JsonToken.VALUE_STRING)) {
            return p.getText();
        }
        var requestError = aRequestError(p.getCurrentName(), CREATE_PAYMENT_VALIDATION_ERROR, "Must be of type String");
        throw new PaymentValidationException(requestError);
    }
}
