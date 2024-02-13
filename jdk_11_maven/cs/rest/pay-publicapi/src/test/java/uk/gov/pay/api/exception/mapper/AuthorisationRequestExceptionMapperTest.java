package uk.gov.pay.api.exception.mapper;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.api.exception.AuthorisationRequestException;
import uk.gov.pay.api.exception.ConnectorResponseErrorException;
import uk.gov.pay.api.model.RequestError;
import uk.gov.service.payments.commons.model.ErrorIdentifier;

import javax.ws.rs.core.Response;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.pay.api.model.RequestError.Code.CREATE_PAYMENT_CONNECTOR_ERROR;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.AUTHORISATION_ERROR;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.AUTHORISATION_REJECTED;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.AUTHORISATION_TIMEOUT;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.CARD_NUMBER_REJECTED;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.INVALID_ATTRIBUTE_VALUE;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.ONE_TIME_TOKEN_ALREADY_USED;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.ONE_TIME_TOKEN_INVALID;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.TELEPHONE_PAYMENT_NOTIFICATIONS_NOT_ALLOWED;

@ExtendWith(MockitoExtension.class)
class AuthorisationRequestExceptionMapperTest {

    @Mock
    private Response mockResponse;

    private final AuthorisationRequestExceptionMapper mapper = new AuthorisationRequestExceptionMapper();

    static Object[] parametersForMapping() {
        return new Object[]{
                new Object[]{CARD_NUMBER_REJECTED, true, "An error message from connector", 402, "P0010"},
                new Object[]{AUTHORISATION_REJECTED, true, "An error message from connector", 402, "P0010"},
                new Object[]{AUTHORISATION_ERROR, false, "There was an error authorising the payment", 500, "P0050"},
                new Object[]{AUTHORISATION_TIMEOUT, false, "There was an error authorising the payment", 500, "P0050"},
                new Object[]{ONE_TIME_TOKEN_INVALID, true, "An error message from connector", 400, "P1211"},
                new Object[]{ONE_TIME_TOKEN_ALREADY_USED, true, "An error message from connector", 400, "P1212"},
                new Object[]{INVALID_ATTRIBUTE_VALUE, true, "An error message from connector", 422, "P0102"},
                new Object[]{TELEPHONE_PAYMENT_NOTIFICATIONS_NOT_ALLOWED, false, "Downstream system error", 500, "P0198"}

        };
    }

    @ParameterizedTest
    @MethodSource("parametersForMapping")
    void testMapping(ErrorIdentifier errorIdentifier, boolean messageFromConnector, String expectedDescription, int expectedStatusCode, String errorCode) {
        List<String> connectorErrorMessages = messageFromConnector ? List.of(expectedDescription) : List.of("Generic error message");
        when(mockResponse.readEntity(ConnectorResponseErrorException.ConnectorErrorResponse.class))
                .thenReturn(new ConnectorResponseErrorException.ConnectorErrorResponse(errorIdentifier, connectorErrorMessages));
        Response returnedResponse = mapper.toResponse(new AuthorisationRequestException(mockResponse));
        RequestError returnedError = (RequestError) returnedResponse.getEntity();
        assertThat(returnedError.getCode(), is(errorCode));
        assertThat(returnedError.getDescription(), is(expectedDescription));
        assertThat(returnedResponse.getStatus(), is(expectedStatusCode));
    }

}
