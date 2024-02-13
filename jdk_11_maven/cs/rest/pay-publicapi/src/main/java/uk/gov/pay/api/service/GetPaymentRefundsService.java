package uk.gov.pay.api.service;

import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.model.RefundResponse;
import uk.gov.pay.api.model.RefundsFromConnector;
import uk.gov.pay.api.model.RefundsResponse;
import uk.gov.pay.api.model.ledger.RefundsFromLedger;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

public class GetPaymentRefundsService {

    private final ConnectorService connectorService;
    private final LedgerService ledgerService;
    private PublicApiUriGenerator publicApiUriGenerator;

    @Inject
    public GetPaymentRefundsService(ConnectorService connectorService,
                                    LedgerService ledgerService,
                                    PublicApiUriGenerator publicApiUriGenerator) {
        this.connectorService = connectorService;
        this.ledgerService = ledgerService;
        this.publicApiUriGenerator = publicApiUriGenerator;
    }

    public RefundsResponse getConnectorPaymentRefunds(Account account, String paymentId) {
        RefundsFromConnector refundsFromConnector = connectorService.getPaymentRefunds(account.getAccountId(), paymentId);
        List<RefundResponse> refundResponses = processRefunds(paymentId, refundsFromConnector);
        
        return getRefundsResponse(paymentId, refundResponses);
    }

    public RefundsResponse getLedgerTransactionTransactions(Account account, String paymentId) {
        RefundsFromLedger refundsFromLedger = ledgerService.getPaymentRefunds(account.getAccountId(), paymentId);
        List<RefundResponse> refundResponses = processRefunds(paymentId, refundsFromLedger);
        
        return getRefundsResponse(paymentId, refundResponses);
    }

    private List<RefundResponse> processRefunds(String paymentId, RefundsFromLedger refundsFromLedger) {
        return refundsFromLedger.getTransactions()
                .stream()
                .map(refundTransactionFromLedger ->
                        RefundResponse.from(refundTransactionFromLedger,
                                publicApiUriGenerator.getRefundsURI(paymentId,
                                        refundTransactionFromLedger.getTransactionId()),
                                publicApiUriGenerator.getPaymentURI(paymentId)
                        )
                )
                .collect(Collectors.toList());
    }

    private List<RefundResponse> processRefunds(String paymentId, RefundsFromConnector refundsFromConnector) {
        return refundsFromConnector
                .getEmbedded()
                .getRefunds()
                .stream()
                .map(refundFromConnector ->
                        RefundResponse.from(refundFromConnector,
                                publicApiUriGenerator.getRefundsURI(paymentId,
                                        refundFromConnector.getRefundId()),
                                publicApiUriGenerator.getPaymentURI(paymentId)
                        )
                )
                .collect(Collectors.toList());
    }

    private RefundsResponse getRefundsResponse(String paymentId, List<RefundResponse> refunds) {
        return RefundsResponse.from(paymentId, refunds,
                publicApiUriGenerator.getPaymentRefundsURI(paymentId).toString(),
                publicApiUriGenerator.getPaymentURI(paymentId).toString()
        );
    }
}
