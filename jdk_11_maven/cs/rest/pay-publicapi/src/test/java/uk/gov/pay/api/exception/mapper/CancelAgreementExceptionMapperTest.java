package uk.gov.pay.api.exception.mapper;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.api.exception.CancelAgreementException;
import uk.gov.pay.api.exception.ConnectorResponseErrorException.ConnectorErrorResponse;
import uk.gov.pay.api.model.RequestError;
import uk.gov.service.payments.commons.model.ErrorIdentifier;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.AGREEMENT_NOT_ACTIVE;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.AGREEMENT_NOT_FOUND;
import static uk.gov.service.payments.commons.model.ErrorIdentifier.GENERIC;

@ExtendWith(MockitoExtension.class)
class CancelAgreementExceptionMapperTest {

    @Mock
    private Response mockResponse;

    private final CancelAgreementExceptionMapper mapper = new CancelAgreementExceptionMapper();

    @ParameterizedTest
    @MethodSource
    void testExceptionMapping(int statusCodeFromConnector, ErrorIdentifier errorIdentifierFromConnector,
                              int expectedStatusCode, String expectedErrorCode, String expectedDescription) {
        when(mockResponse.getStatus()).thenReturn(statusCodeFromConnector);
        when(mockResponse.readEntity(ConnectorErrorResponse.class)).thenReturn(new ConnectorErrorResponse(errorIdentifierFromConnector, List.of()));

        Response returnedResponse = mapper.toResponse(new CancelAgreementException(mockResponse));

        assertThat(returnedResponse.getStatus(), is(expectedStatusCode));

        RequestError returnedError = (RequestError) returnedResponse.getEntity();
        assertThat(returnedError.getDescription(), is(expectedDescription));
        assertThat(returnedError.getCode(), is(expectedErrorCode));
    }

    static Stream<Arguments> testExceptionMapping() {
        return Stream.of(
                arguments(404, AGREEMENT_NOT_FOUND, 404, "P2500", "Not found"),
                arguments(400, AGREEMENT_NOT_ACTIVE, 400, "P2501", "Cancellation of agreement failed"),
                arguments(500, GENERIC, 500, "P2598", "Downstream system error")
        );
    }

}
