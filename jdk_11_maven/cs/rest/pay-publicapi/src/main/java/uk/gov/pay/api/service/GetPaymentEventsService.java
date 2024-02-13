package uk.gov.pay.api.service;

import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.exception.GetEventsException;
import uk.gov.pay.api.model.PaymentEvents;
import uk.gov.pay.api.model.PaymentEventsResponse;
import uk.gov.pay.api.model.TransactionEvents;

import javax.inject.Inject;
import java.net.URI;

public class GetPaymentEventsService {

    private final PublicApiUriGenerator publicApiUriGenerator;
    private ConnectorService connectorService;
    private LedgerService ledgerService;

    @Inject
    public GetPaymentEventsService(PublicApiUriGenerator publicApiUriGenerator,
                                   ConnectorService connectorService,
                                   LedgerService ledgerService) {
        this.publicApiUriGenerator = publicApiUriGenerator;
        this.connectorService = connectorService;
        this.ledgerService = ledgerService;
    }

    public PaymentEventsResponse getPaymentEventsFromConnector(Account account, String paymentId) {
        PaymentEvents chargeEvents = connectorService.getChargeEvents(account, paymentId);

        URI paymentEventsLink = publicApiUriGenerator.getPaymentEventsURI(paymentId);
        URI paymentLink = publicApiUriGenerator.getPaymentURI(paymentId);

        return PaymentEventsResponse.from(chargeEvents, paymentLink, paymentEventsLink);
    }

    public PaymentEventsResponse getPaymentEventsFromLedger(Account account, String paymentId) {
        TransactionEvents transactionEvents = ledgerService.getTransactionEvents(account, paymentId);

        URI paymentEventsLink = publicApiUriGenerator.getPaymentEventsURI(paymentId);
        URI paymentLink = publicApiUriGenerator.getPaymentURI(paymentId);

        return PaymentEventsResponse.from(transactionEvents, paymentLink, paymentEventsLink);
    }

    public PaymentEventsResponse getPaymentEvents(Account account, String paymentId) {
        try {
            return getPaymentEventsFromConnector(account, paymentId);
        } catch (GetEventsException ex) {
            return getPaymentEventsFromLedger(account, paymentId);
        }
    }
}
