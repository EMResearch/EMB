package uk.gov.pay.api.resources;

import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.model.links.PaymentWithAllLinks;
import uk.gov.pay.api.service.GetPaymentService;

public class GetOnePaymentStrategy extends LedgerOrConnectorStrategyTemplate<PaymentWithAllLinks> {

    private final Account account;
    private final String paymentId;
    private final GetPaymentService getPaymentService;

    public GetOnePaymentStrategy(String strategy, Account account, String paymentId, GetPaymentService getPaymentService) {
        super(strategy);
        this.account = account;
        this.paymentId = paymentId;
        this.getPaymentService = getPaymentService;
    }

    @Override
    protected PaymentWithAllLinks executeLedgerOnlyStrategy() {
        return getPaymentService.getLedgerTransaction(account, paymentId);
    }

    @Override
    protected PaymentWithAllLinks executeDefaultStrategy() {
        return getPaymentService.getPayment(account, paymentId);
    }

    @Override
    protected PaymentWithAllLinks executeConnectorOnlyStrategy() {
        return getPaymentService.getConnectorCharge(account, paymentId);
    }
}
