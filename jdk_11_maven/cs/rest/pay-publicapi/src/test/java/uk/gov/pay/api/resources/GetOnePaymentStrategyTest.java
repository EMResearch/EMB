package uk.gov.pay.api.resources;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.pay.api.service.GetPaymentService;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class GetOnePaymentStrategyTest {

    @Mock
    private GetPaymentService mockGetPaymentService;

    private GetOnePaymentStrategy getOnePaymentStrategy;
    private String mockPaymentId = "some-payment-id";
    private Account mockAccountId = new Account("some-account-id", TokenPaymentType.CARD, "a-token-link");

    @Test
    public void validateAndExecuteUsesLedgerOnlyStrategy() {
        String strategy = "ledger-only";
        getOnePaymentStrategy = new GetOnePaymentStrategy(strategy, mockAccountId, mockPaymentId, mockGetPaymentService);
        getOnePaymentStrategy.validateAndExecute();

        verify(mockGetPaymentService).getLedgerTransaction(mockAccountId, mockPaymentId);
        verify(mockGetPaymentService, never()).getConnectorCharge(mockAccountId, mockPaymentId);
        verify(mockGetPaymentService, never()).getPayment(mockAccountId, mockPaymentId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "unknown"})
    public void validateAndExecuteUsesDefaultStrategy(String strategy) {
        getOnePaymentStrategy = new GetOnePaymentStrategy(strategy, mockAccountId, mockPaymentId, mockGetPaymentService);
        getOnePaymentStrategy.validateAndExecute();

        verify(mockGetPaymentService).getPayment(mockAccountId, mockPaymentId);
    }

    @Test
    public void validateAndExecuteShouldUseConnectorOnlyStrategy() {
        String strategy = "connector-only";
        getOnePaymentStrategy = new GetOnePaymentStrategy(strategy, mockAccountId, mockPaymentId, mockGetPaymentService);

        getOnePaymentStrategy.validateAndExecute();

        verify(mockGetPaymentService).getConnectorCharge(mockAccountId, mockPaymentId);
        verify(mockGetPaymentService, never()).getLedgerTransaction(mockAccountId, mockPaymentId);
        verify(mockGetPaymentService, never()).getPayment(mockAccountId, mockPaymentId);
    }
}
