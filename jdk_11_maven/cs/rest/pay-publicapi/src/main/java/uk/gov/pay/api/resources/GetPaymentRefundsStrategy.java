package uk.gov.pay.api.resources;

import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.model.RefundsResponse;
import uk.gov.pay.api.service.GetPaymentRefundsService;

public class GetPaymentRefundsStrategy extends LedgerOrConnectorStrategyTemplate<RefundsResponse> {

    private final Account account;
    private final String paymentId;
    private final GetPaymentRefundsService getPaymentRefundsService;

    public GetPaymentRefundsStrategy(String strategy, Account account, String paymentId,
                                     GetPaymentRefundsService getPaymentRefundsService) {
        super(strategy);
        this.account = account;
        this.paymentId = paymentId;
        this.getPaymentRefundsService = getPaymentRefundsService;
    }

    @Override
    protected RefundsResponse executeLedgerOnlyStrategy() {
        return getPaymentRefundsService.getLedgerTransactionTransactions(account, paymentId);
    }

    @Override
    protected RefundsResponse executeDefaultStrategy() {
        return executeLedgerOnlyStrategy();
    }

    @Override
    protected RefundsResponse executeConnectorOnlyStrategy() {
        return getPaymentRefundsService.getConnectorPaymentRefunds(account, paymentId);
    }
}
