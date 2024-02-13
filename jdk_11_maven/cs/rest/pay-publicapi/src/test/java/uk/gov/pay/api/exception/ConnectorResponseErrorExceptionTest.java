package uk.gov.pay.api.exception;

import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.pay.api.exception.ConnectorResponseErrorException.ConnectorErrorResponse;

public class ConnectorResponseErrorExceptionTest {

    @Test
    public void whenCreated_shouldCallCloseConnection_soWeMakeSureThatTheConnectionIsClosedWhenConnectorRespondsWithAnUnexpectedResponse() {
        // It doesn't matter if the connection was closed before,
        // but anyway we make sure it is when an exception of this nature is raised

        Response mockResponse = mock(Response.class);
        when(mockResponse.getStatus())
                .thenReturn(400);

        ConnectorResponseErrorException exception = new ConnectorResponseErrorException(mockResponse);

        assertThat(exception.getErrorStatus(), is(400));
        assertThat(exception.hasReason(), is(false));
        assertThat(exception.getMessage(), is(mockResponse.toString()));
        assertThat(exception.getReason(), is(nullValue()));

        verify(mockResponse).getStatus();
        verify(mockResponse).readEntity(ConnectorErrorResponse.class);
        verify(mockResponse).close();
        verifyNoMoreInteractions(mockResponse);
    }
}
