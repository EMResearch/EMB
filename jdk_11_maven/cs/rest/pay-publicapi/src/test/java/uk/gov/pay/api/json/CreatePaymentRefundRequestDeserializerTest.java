package uk.gov.pay.api.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.api.exception.BadRequestException;
import uk.gov.pay.api.exception.PaymentValidationException;
import uk.gov.pay.api.model.CreatePaymentRefundRequest;
import uk.gov.pay.api.validation.PaymentRefundRequestValidator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.pay.api.matcher.BadRequestExceptionMatcher.aBadRequestExceptionWithError;
import static uk.gov.pay.api.matcher.PaymentValidationExceptionMatcher.aValidationExceptionContaining;

@RunWith(MockitoJUnitRunner.class)
public class CreatePaymentRefundRequestDeserializerTest {

    @Mock
    private DeserializationContext ctx;

    private JsonFactory jsonFactory = new JsonFactory(new ObjectMapper());
    private CreatePaymentRefundRequestDeserializer deserializer;

    @Before
    public void setup() {
        deserializer = new CreatePaymentRefundRequestDeserializer(new PaymentRefundRequestValidator());
    }

    @Test
    public void deserialize_shouldDeserializeARequestSuccessfully() throws Exception {

        String validJson = "{" +
                "  \"amount\" : 12345" +
                "}";

        CreatePaymentRefundRequest paymentRefundRequest = deserializer.deserialize(jsonFactory.createParser(validJson), ctx);

        assertThat(paymentRefundRequest.getAmount(), is(12345));
    }

    @Test
    public void deserialize_shouldThrowBadRequestException_whenJsonIsNotWellFormed() throws Exception {

        String invalidJson = "{" +
                "  \"amount\" : " +
                "}";

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> deserializer.deserialize(jsonFactory.createParser(invalidJson), ctx));

        assertThat(badRequestException, aBadRequestExceptionWithError("P0697", "Unable to parse JSON"));
    }

    @Test
    public void deserialize_shouldThrowBadRequestException_whenAmountIsMissing() throws Exception {

        String json = "{}";

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> deserializer.deserialize(jsonFactory.createParser(json), ctx));

        assertThat(badRequestException, aBadRequestExceptionWithError("P0601", "Missing mandatory attribute: amount"));
    }

    @Test
    public void deserialize_shouldThrowValidationException_asAmountIsMissing_whenAmountIsNullValue() throws Exception {

        String json = "{" +
                "  \"amount\" : null" +
                "}";

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> deserializer.deserialize(jsonFactory.createParser(json), ctx));

        assertThat(badRequestException, aBadRequestExceptionWithError("P0601", "Missing mandatory attribute: amount"));
    }

    @Test
    public void deserialize_shouldThrowValidationException_whenAmountIsNotInteger() throws Exception {

        String json = "{" +
                "  \"amount\" : \"\"" +
                "}";

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> deserializer.deserialize(jsonFactory.createParser(json), ctx));

        assertThat(badRequestException, aBadRequestExceptionWithError("P0602",
                "Invalid attribute value: amount. Must be a valid numeric format"));
    }

    @Test
    public void deserialize_shouldThrowValidationException_whenAmountIsLessThanMinimum() throws Exception {

        String json = "{" +
                "  \"amount\" : 0" +
                "}";

        PaymentValidationException exception = assertThrows(PaymentValidationException.class,
                () -> deserializer.deserialize(jsonFactory.createParser(json), ctx));

        assertThat(exception, aValidationExceptionContaining("P0602",
                "Invalid attribute value: amount. Must be greater than or equal to 1"));
    }

    @Test
    public void deserialize_shouldThrowValidationException_whenAmountIsMoreThanMaximum() throws Exception {

        String json = "{" +
                "  \"amount\" : 10000001" +
                "}";

        PaymentValidationException exception = assertThrows(PaymentValidationException.class,
                () -> deserializer.deserialize(jsonFactory.createParser(json), ctx));

        assertThat(exception, aValidationExceptionContaining("P0602",
                "Invalid attribute value: amount. Must be less than or equal to 10000000"));
    }

    @After
    public void tearDown() {
        verifyNoInteractions(ctx);
    }
}
