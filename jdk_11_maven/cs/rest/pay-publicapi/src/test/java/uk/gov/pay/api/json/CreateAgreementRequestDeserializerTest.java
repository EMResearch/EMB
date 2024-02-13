package uk.gov.pay.api.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.pay.api.agreement.model.CreateAgreementRequest;
import uk.gov.pay.api.exception.BadRequestException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.pay.api.matcher.BadRequestExceptionMatcher.aBadRequestExceptionWithError;

public class CreateAgreementRequestDeserializerTest {

    @Mock
    private DeserializationContext ctx;

    private JsonFactory jsonFactory = new JsonFactory(new ObjectMapper());
    private CreateAgreementRequestDeserializer deserializer;

    @BeforeEach
    public void setup() {
        deserializer = new CreateAgreementRequestDeserializer();
    }

    @Test
    public void deserialize_shouldDeserializeACreateAgreementRequestWithPayloadSuccessfully() throws Exception {
        String validJson = "{\"reference\": \"Some reference\", \"description\": \"A valid description\", \"user_identifier\": \"a-valid-user-identifier\"}";
        CreateAgreementRequest agreementRequest = deserializer.deserialize(jsonFactory.createParser(validJson), ctx);
        assertThat(agreementRequest.getReference(), is("Some reference"));
    }

    @Test
    public void deserialize_shouldThrowBadRequestException_whenJsonIsNotWellFormed() {
        String invalidJson = "{\"reference\": \"Some reference\"";
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> deserializer.deserialize(jsonFactory.createParser(invalidJson), ctx));
        assertThat(badRequestException, aBadRequestExceptionWithError("P2197", "Unable to parse JSON"));
    }

    @Test
    public void deserialize_shouldThrowBadRequestException_whenReferenceIsMissing() {
        String invalidJson = "{}";
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> deserializer.deserialize(jsonFactory.createParser(invalidJson), ctx));
        assertThat(badRequestException, aBadRequestExceptionWithError("P2101", "Missing mandatory attribute: reference"));
    }
    
    @Test
    public void deserialize_shouldThrowValidationException_whenReferenceIsIsNullValue() {
        String json = "{ \"reference\": null}";
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> deserializer.deserialize(jsonFactory.createParser(json), ctx));
        assertThat(badRequestException, aBadRequestExceptionWithError("P2101", "Missing mandatory attribute: reference"));
    }

    @Test
    public void deserialize_shouldThrowValidationException_whenReferenceIsIsEmptyString() {
        String json = "{ \"reference\": \"\"}";
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> deserializer.deserialize(jsonFactory.createParser(json), ctx));
        assertThat(badRequestException, aBadRequestExceptionWithError("P2101", "Missing mandatory attribute: reference"));
    }

    @Test
    public void deserialize_shouldThrowBadRequestException_whenDescriptionIsMissing() {
        String invalidJson = "{\"reference\": \"Some reference\"}";
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> deserializer.deserialize(jsonFactory.createParser(invalidJson), ctx));
        assertThat(badRequestException, aBadRequestExceptionWithError("P2101", "Missing mandatory attribute: description"));
    }

    @Test
    public void deserialize_shouldThrowValidationException_whenReferenceIsNumericValue() {
        String jsonWithNumericReference = "{\"reference\": 123}";
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> deserializer.deserialize(jsonFactory.createParser(jsonWithNumericReference), ctx));
        assertThat(badRequestException, aBadRequestExceptionWithError("P2102",
                "Invalid attribute value: reference. Must be a valid string format"));
    }
    
    @After
    public void tearDown() {
        verifyNoInteractions(ctx);
    }
}
