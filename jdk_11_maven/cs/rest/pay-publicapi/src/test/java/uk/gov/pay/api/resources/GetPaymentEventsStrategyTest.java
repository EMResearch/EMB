package uk.gov.pay.api.resources;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.pay.api.service.GetPaymentEventsService;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class GetPaymentEventsStrategyTest {

    @Mock
    private GetPaymentEventsService getPaymentEventsService;

    private GetPaymentEventsStrategy getPaymentEventsStrategy;
    private String mockPaymentId = "some-payment-id";
    private Account mockAccountId = new Account("some-account-id", TokenPaymentType.CARD, "a-token-link");

    @Test
    public void validateAndExecuteUsesLedgerOnlyStrategy() {
        String strategy = "ledger-only";
        getPaymentEventsStrategy = new GetPaymentEventsStrategy(strategy, mockAccountId, mockPaymentId, getPaymentEventsService);
        getPaymentEventsStrategy.validateAndExecute();

        verify(getPaymentEventsService).getPaymentEventsFromLedger(mockAccountId, mockPaymentId);
        verify(getPaymentEventsService, never()).getPaymentEventsFromConnector(mockAccountId, mockPaymentId);
        verify(getPaymentEventsService, never()).getPaymentEvents(mockAccountId, mockPaymentId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "unknown"})
    public void validateAndExecuteUsesDefaultStrategy(String strategy) {
        getPaymentEventsStrategy = new GetPaymentEventsStrategy(strategy, mockAccountId, mockPaymentId, getPaymentEventsService);
        getPaymentEventsStrategy.validateAndExecute();

        verify(getPaymentEventsService).getPaymentEvents(mockAccountId, mockPaymentId);
    }

    @Test
    public void validateAndExecuteShouldUseConnectorOnlyStrategy() {
        String strategy = "connector-only";
        getPaymentEventsStrategy = new GetPaymentEventsStrategy(strategy, mockAccountId, mockPaymentId, getPaymentEventsService);

        getPaymentEventsStrategy.validateAndExecute();

        verify(getPaymentEventsService).getPaymentEventsFromConnector(mockAccountId, mockPaymentId);
        verify(getPaymentEventsService, never()).getPaymentEventsFromLedger(mockAccountId, mockPaymentId);
        verify(getPaymentEventsService, never()).getPaymentEvents(mockAccountId, mockPaymentId);
    }
}
