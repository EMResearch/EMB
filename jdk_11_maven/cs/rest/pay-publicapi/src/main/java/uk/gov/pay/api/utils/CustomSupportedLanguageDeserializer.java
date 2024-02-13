package uk.gov.pay.api.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import uk.gov.service.payments.commons.model.SupportedLanguage;

import java.io.IOException;

public class CustomSupportedLanguageDeserializer extends StdDeserializer<SupportedLanguage> {

    public CustomSupportedLanguageDeserializer() {
        this(null);
    }

    public CustomSupportedLanguageDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public SupportedLanguage deserialize(JsonParser jsonparser, DeserializationContext context) throws IOException {
        return SupportedLanguage.fromIso639AlphaTwoCode(jsonparser.getText());
    }
}
