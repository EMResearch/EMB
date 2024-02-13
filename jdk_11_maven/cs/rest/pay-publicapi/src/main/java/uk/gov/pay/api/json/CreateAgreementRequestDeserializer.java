package uk.gov.pay.api.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import uk.gov.pay.api.agreement.model.CreateAgreementRequest;
import uk.gov.pay.api.exception.BadRequestException;

import java.io.IOException;

import static uk.gov.pay.api.json.RequestJsonParser.parseAgreementRequest;
import static uk.gov.pay.api.model.RequestError.Code.CREATE_AGREEMENT_PARSING_ERROR;
import static uk.gov.pay.api.model.RequestError.aRequestError;

public class CreateAgreementRequestDeserializer extends StdDeserializer<CreateAgreementRequest> {

    public CreateAgreementRequestDeserializer() {
        super(CreateAgreementRequest.class);
    }

    @Override
    public CreateAgreementRequest deserialize(JsonParser parser, DeserializationContext context) {
        try {
            JsonNode json = parser.readValueAsTree();
            return parseAgreementRequest(json);
        } catch (IOException e) {
            throw new BadRequestException(aRequestError(CREATE_AGREEMENT_PARSING_ERROR));
        }
    }
}
