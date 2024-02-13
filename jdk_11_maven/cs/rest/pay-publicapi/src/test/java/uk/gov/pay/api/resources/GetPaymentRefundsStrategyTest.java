package uk.gov.pay.api.resources;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.pay.api.service.GetPaymentRefundsService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class GetPaymentRefundsStrategyTest {

    private GetPaymentRefundsService mockGetPaymentRefundsService = mock(GetPaymentRefundsService.class);
    private GetPaymentRefundsStrategy getPaymentRefundsStrategy;

    private String paymentId = "payment-id";
    private Account account = new Account("account-id", TokenPaymentType.CARD, "a-token-link");

    @ParameterizedTest
    @ValueSource(strings = {"ledger-only", "", "unknown"})
    public void validateAndExecuteShouldUseLedgerOnlyForListedStrategies(String strategy) {
        getPaymentRefundsStrategy = new GetPaymentRefundsStrategy(strategy, account, paymentId, mockGetPaymentRefundsService);
        getPaymentRefundsStrategy.validateAndExecute();

        verify(mockGetPaymentRefundsService).getLedgerTransactionTransactions(account, paymentId);
        verify(mockGetPaymentRefundsService, never()).getConnectorPaymentRefunds(account, paymentId);
    }

    @Test
    public void validateAndExecuteShouldUseConnectorOnlyStrategy() {
        getPaymentRefundsStrategy = new GetPaymentRefundsStrategy("connector-only", account, paymentId, mockGetPaymentRefundsService);

        getPaymentRefundsStrategy.validateAndExecute();

        verify(mockGetPaymentRefundsService).getConnectorPaymentRefunds(account, paymentId);
        verify(mockGetPaymentRefundsService, never()).getLedgerTransactionTransactions(account, paymentId);
    }
}
