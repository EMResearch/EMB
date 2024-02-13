package uk.gov.pay.api.resources;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.pay.api.service.GetPaymentRefundService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class GetPaymentRefundStrategyTest {

    private GetPaymentRefundService mockGetPaymentRefundService = mock(GetPaymentRefundService.class);
    private GetPaymentRefundStrategy getPaymentRefundStrategy;

    private String paymentId = "payment-id";
    private String refundId = "refund-id";
    private Account account = new Account("account-id", TokenPaymentType.CARD, "a-token-link");

    @Test
    public void validateAndExecuteUsesLedgerOnlyStrategy() {
        String strategy = "ledger-only";
        getPaymentRefundStrategy = new GetPaymentRefundStrategy(strategy, account, paymentId, refundId, mockGetPaymentRefundService);
        getPaymentRefundStrategy.validateAndExecute();

        verify(mockGetPaymentRefundService).getLedgerPaymentRefund(account, paymentId, refundId);
        verify(mockGetPaymentRefundService, never()).getConnectorPaymentRefund(account, paymentId, refundId);
        verify(mockGetPaymentRefundService, never()).getPaymentRefund(account, paymentId, refundId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "unknown"})
    public void validateAndExecuteUsesDefaultStrategy(String strategy) {
        getPaymentRefundStrategy = new GetPaymentRefundStrategy(strategy, account, paymentId, refundId, mockGetPaymentRefundService);
        getPaymentRefundStrategy.validateAndExecute();

        verify(mockGetPaymentRefundService).getPaymentRefund(account, paymentId, refundId);
    }

    @Test
    public void validateAndExecuteShouldUseConnectorOnlyStrategy() {
        String strategy = "connector-only";
        getPaymentRefundStrategy = new GetPaymentRefundStrategy(strategy, account, paymentId, refundId, mockGetPaymentRefundService);

        getPaymentRefundStrategy.validateAndExecute();

        verify(mockGetPaymentRefundService).getConnectorPaymentRefund(account, paymentId, refundId);
        verify(mockGetPaymentRefundService, never()).getLedgerPaymentRefund(account, paymentId, refundId);
        verify(mockGetPaymentRefundService, never()).getPaymentRefund(account, paymentId, refundId);
    }
}
