package uk.gov.pay.api.exception.mapper;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.api.exception.ConnectorResponseErrorException.ConnectorErrorResponse;
import uk.gov.pay.api.exception.CreateChargeException;
import uk.gov.pay.api.model.RequestError;
import uk.gov.service.payments.commons.model.ErrorIdentifier;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.ACCOUNT_DISABLED;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.ACCOUNT_NOT_LINKED_WITH_PSP;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.AGREEMENT_NOT_ACTIVE;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.AGREEMENT_NOT_FOUND;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.AUTHORISATION_API_NOT_ALLOWED;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.INCORRECT_AUTHORISATION_MODE_FOR_SAVE_PAYMENT_INSTRUMENT_TO_AGREEMENT;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.INVALID_ATTRIBUTE_VALUE;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.MISSING_MANDATORY_ATTRIBUTE;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.MOTO_NOT_ALLOWED;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.RECURRING_CARD_PAYMENTS_NOT_ALLOWED;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.TELEPHONE_PAYMENT_NOTIFICATIONS_NOT_ALLOWED;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.UNEXPECTED_ATTRIBUTE;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.ZERO_AMOUNT_NOT_ALLOWED;

@ExtendWith(MockitoExtension.class)
class CreateChargeExceptionMapperTest {

    @Mock
    private Response mockResponse;

    private final CreateChargeExceptionMapper mapper = new CreateChargeExceptionMapper();

    @ParameterizedTest
    @MethodSource
    void testExceptionMapping(ErrorIdentifier errorIdentifier, boolean messageFromConnector, String expectedDescription, int expectedStatusCode, String expectedErrorCode) {
        List<String> connectorErrorMessages = messageFromConnector ? List.of(expectedDescription) : List.of();
        when(mockResponse.readEntity(ConnectorErrorResponse.class))
                .thenReturn(new ConnectorErrorResponse(errorIdentifier, connectorErrorMessages));

        Response returnedResponse = mapper.toResponse(new CreateChargeException(mockResponse));
        RequestError returnedError = (RequestError) returnedResponse.getEntity();

        assertThat(returnedResponse.getStatus(), is(expectedStatusCode));
        assertThat(returnedError.getDescription(), is(expectedDescription));
        assertThat(returnedError.getCode(), is(expectedErrorCode));
    }

    static Stream<Arguments> testExceptionMapping() {
        return Stream.of(
                arguments(ZERO_AMOUNT_NOT_ALLOWED, false, "Invalid attribute value: amount. Must be greater than or equal to 1", 422, "P0102"),
                arguments(MOTO_NOT_ALLOWED, false, "MOTO payments are not enabled for this account. Please contact support if you would like to process MOTO payments - https://www.payments.service.gov.uk/support/ .", 422, "P0196"),
                arguments(ACCOUNT_DISABLED, false, "GOV.UK Pay has disabled payment and refund creation on this account. Contact support with your error code - https://www.payments.service.gov.uk/support/ .", 403, "P0941"),
                arguments(TELEPHONE_PAYMENT_NOTIFICATIONS_NOT_ALLOWED, false, "Access to this resource is not enabled for this account. Contact support with your error code - https://www.payments.service.gov.uk/support/ .", 403, "P0930"),
                arguments(ACCOUNT_NOT_LINKED_WITH_PSP, false, "Account is not fully configured. Please refer to documentation to setup your account or contact support with your error code - https://www.payments.service.gov.uk/support/ .", 403, "P0940"),
                arguments(AUTHORISATION_API_NOT_ALLOWED, false, "Using authorisation_mode of moto_api is not allowed for this account", 422, "P0195"),
                arguments(MISSING_MANDATORY_ATTRIBUTE, true, "An error message from connector", 400, "P0101"),
                arguments(UNEXPECTED_ATTRIBUTE, true, "An error message from connector", 400, "P0104"),
                arguments(INCORRECT_AUTHORISATION_MODE_FOR_SAVE_PAYMENT_INSTRUMENT_TO_AGREEMENT, true, "Unexpected attribute: set_up_agreement", 400, "P0104"),
                arguments(AGREEMENT_NOT_FOUND, false, "Invalid attribute value: agreement_id. Agreement does not exist", 400, "P0102"),
                arguments(AGREEMENT_NOT_ACTIVE, false, "Invalid attribute value: agreement_id. Agreement must be active", 400, "P0102"),
                arguments(INVALID_ATTRIBUTE_VALUE, true, "An error message from connector", 422, "P0102"),
                arguments(RECURRING_CARD_PAYMENTS_NOT_ALLOWED, true, "Recurring card payments are currently disabled for this service. Contact support with your error code - https://www.payments.service.gov.uk/support/", 422, "P0942")
        );
    }

}
