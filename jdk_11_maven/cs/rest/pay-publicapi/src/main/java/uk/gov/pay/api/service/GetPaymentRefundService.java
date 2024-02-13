package uk.gov.pay.api.service;

import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.exception.GetRefundException;
import uk.gov.pay.api.exception.GetTransactionException;
import uk.gov.pay.api.model.RefundFromConnector;
import uk.gov.pay.api.model.RefundResponse;
import uk.gov.pay.api.model.ledger.RefundTransactionFromLedger;

import javax.inject.Inject;

public class GetPaymentRefundService {

    private final ConnectorService connectorService;
    private final LedgerService ledgerService;
    private PublicApiUriGenerator publicApiUriGenerator;

    @Inject
    public GetPaymentRefundService(ConnectorService connectorService,
                                   LedgerService ledgerService,
                                   PublicApiUriGenerator publicApiUriGenerator) {
        this.connectorService = connectorService;
        this.ledgerService = ledgerService;
        this.publicApiUriGenerator = publicApiUriGenerator;
    }

    public RefundResponse getConnectorPaymentRefund(Account account, String paymentId, String refundId) {
        RefundFromConnector refundFromConnector = connectorService.getPaymentRefund(account.getAccountId(), paymentId, refundId);

        return RefundResponse.from(
                refundFromConnector,
                publicApiUriGenerator.getRefundsURI(paymentId, refundId),
                publicApiUriGenerator.getPaymentURI(paymentId)
        );
    }

    public RefundResponse getLedgerPaymentRefund(Account account, String paymentId, String refundId) {
        try {
            RefundTransactionFromLedger refund = ledgerService.getRefundTransaction(account, refundId, paymentId);

            return RefundResponse.from(
                    refund,
                    publicApiUriGenerator.getRefundsURI(paymentId, refundId),
                    publicApiUriGenerator.getPaymentURI(paymentId)
            );
        } catch (GetTransactionException exception) {
            throw new GetRefundException(exception);
        }
    }

    public RefundResponse getPaymentRefund(Account account, String paymentId, String refundId) {
        try {
            return getConnectorPaymentRefund(account, paymentId, refundId);
        } catch (GetRefundException ex) {
            return getLedgerPaymentRefund(account, paymentId, refundId);
        }
    }
}
