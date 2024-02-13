package uk.gov.pay.api.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.model.Wallet;

import java.io.IOException;

import static java.lang.String.format;

public class WalletDeserializer extends JsonDeserializer<Wallet> {

    private static final Logger logger = LoggerFactory.getLogger(WalletDeserializer.class);

    @Override
    public Wallet deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        String value = jsonParser.getText().toUpperCase();
        try {
            return Wallet.valueOf(value);
        } catch (IllegalArgumentException e) {
            logger.error(format("Value [%s] matches no known wallet types", value));
            return null;
        }
    }
}
