package uk.gov.pay.api.exception.mapper;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.api.exception.ConnectorResponseErrorException;
import uk.gov.pay.api.exception.CreateRefundException;
import uk.gov.pay.api.model.RequestError;
import uk.gov.service.payments.commons.model.ErrorIdentifier;

import javax.ws.rs.core.Response;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.ACCOUNT_DISABLED;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.GENERIC;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.REFUND_AMOUNT_AVAILABLE_MISMATCH;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.REFUND_NOT_AVAILABLE;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.REFUND_NOT_AVAILABLE_DUE_TO_DISPUTE;

@ExtendWith(MockitoExtension.class)
class CreateRefundExceptionMapperTest {

    @Mock
    private Response mockResponse;

    private final CreateRefundExceptionMapper mapper = new CreateRefundExceptionMapper();

    static Object[] parametersForMapping() {
        return new Object[] {
                new Object[]{ACCOUNT_DISABLED, null, "GOV.UK Pay has disabled payment and refund creation on this account. Contact support with your error code - https://www.payments.service.gov.uk/support/ .", 403, "P0941"},
                new Object[]{REFUND_AMOUNT_AVAILABLE_MISMATCH, null, "Refund amount available mismatch.", 412, "P0604"},
                new Object[]{REFUND_NOT_AVAILABLE, "This is a reason", "The payment is not available for refund. Payment refund status: This is a reason", 400, "P0603"},
                new Object[]{REFUND_NOT_AVAILABLE, null, "Downstream system error", 500, "P0698"},
                new Object[]{REFUND_NOT_AVAILABLE_DUE_TO_DISPUTE, null, "The payment is disputed and cannot be refunded", 400, "P0603"},
                new Object[]{GENERIC, null, "Downstream system error", 500, "P0698"},
        };
    }

    @ParameterizedTest
    @MethodSource("parametersForMapping")
    void testExceptionMapping(ErrorIdentifier errorIdentifier, String reason, String expectedDescription, int expectedStatusCode, String expectedErrorCode) {
        when(mockResponse.readEntity(ConnectorResponseErrorException.ConnectorErrorResponse.class))
                .thenReturn(new ConnectorResponseErrorException.ConnectorErrorResponse(errorIdentifier, reason, Collections.emptyList()));

        Response returnedResponse = mapper.toResponse(new CreateRefundException(mockResponse));
        RequestError returnedError = (RequestError) returnedResponse.getEntity();

        assertThat(returnedResponse.getStatus(), is(expectedStatusCode));
        assertThat(returnedError.getDescription(), is(expectedDescription));
        assertThat(returnedError.getCode(), is(expectedErrorCode));
    }
    
}
