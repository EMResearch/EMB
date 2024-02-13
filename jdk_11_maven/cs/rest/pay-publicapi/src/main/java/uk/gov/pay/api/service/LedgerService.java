package uk.gov.pay.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.agreement.model.AgreementLedgerResponse;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.exception.GetAgreementException;
import uk.gov.pay.api.exception.GetChargeException;
import uk.gov.pay.api.exception.GetEventsException;
import uk.gov.pay.api.exception.GetRefundsException;
import uk.gov.pay.api.exception.GetTransactionException;
import uk.gov.pay.api.exception.SearchAgreementsException;
import uk.gov.pay.api.exception.SearchDisputesException;
import uk.gov.pay.api.exception.SearchPaymentsException;
import uk.gov.pay.api.exception.SearchRefundsException;
import uk.gov.pay.api.ledger.model.AgreementSearchParams;
import uk.gov.pay.api.ledger.model.SearchResults;
import uk.gov.pay.api.ledger.service.LedgerUriGenerator;
import uk.gov.pay.api.model.Charge;
import uk.gov.pay.api.model.TransactionEvents;
import uk.gov.pay.api.model.TransactionResponse;
import uk.gov.pay.api.model.ledger.RefundTransactionFromLedger;
import uk.gov.pay.api.model.ledger.RefundsFromLedger;
import uk.gov.pay.api.model.ledger.SearchDisputesResponseFromLedger;
import uk.gov.pay.api.model.ledger.SearchRefundsResponseFromLedger;
import uk.gov.pay.api.model.search.card.PaymentSearchResponse;
import uk.gov.pay.api.validation.AgreementSearchValidator;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.pay.api.common.SearchConstants.GATEWAY_ACCOUNT_ID;

public class LedgerService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(LedgerService.class);

    private static final String PARAM_ACCOUNT_ID = "account_id";
    private static final String PARAM_TRANSACTION_TYPE = "transaction_type";
    public static final String PARAM_EXACT_REFERENCE_MATCH = "exact_reference_match";
    private static final String PAYMENT_TRANSACTION_TYPE = "PAYMENT";
    private static final String REFUND_TRANSACTION_TYPE = "REFUND";
    private static final String DISPUTE_TRANSACTION_TYPE = "DISPUTE";

    private final Client client;
    private final LedgerUriGenerator ledgerUriGenerator;

    @Inject
    public LedgerService(Client client, LedgerUriGenerator ledgerUriGenerator) {
        this.client = client;
        this.ledgerUriGenerator = ledgerUriGenerator;
    }

    public Charge getPaymentTransaction(Account account, String paymentId) {
        Response response = client
                .target(ledgerUriGenerator.transactionURI(account, paymentId, PAYMENT_TRANSACTION_TYPE))
                .request()
                .get();

        if (response.getStatus() == SC_OK) {
            TransactionResponse transactionResponse = response.readEntity(TransactionResponse.class);
            return Charge.from(transactionResponse);
        }

        throw new GetChargeException(response);
    }

    public RefundTransactionFromLedger getRefundTransaction(Account account, String transactionId, String parentExternalId) {
        Response response = client
                .target(ledgerUriGenerator.transactionURI(account, transactionId, REFUND_TRANSACTION_TYPE, parentExternalId))
                .request()
                .get();

        if (response.getStatus() == SC_OK) {
            return response.readEntity(RefundTransactionFromLedger.class);
        }

        throw new GetTransactionException(response);
    }

    public TransactionEvents getTransactionEvents(Account account, String paymentId) {
        Response response = client
                .target(ledgerUriGenerator.transactionEventsURI(account, paymentId))
                .request()
                .get();

        if (response.getStatus() == SC_OK) {
            return response.readEntity(TransactionEvents.class);
        }
        throw new GetEventsException(response);
    }

    public RefundsFromLedger getPaymentRefunds(String accountId, String paymentId) {
        Response response = client
                .target(ledgerUriGenerator.transactionsForTransactionURI(accountId, paymentId, REFUND_TRANSACTION_TYPE))
                .request()
                .get();

        if (response.getStatus() == SC_OK) {
            return response.readEntity(RefundsFromLedger.class);
        }

        throw new GetRefundsException(response);
    }

    public SearchRefundsResponseFromLedger searchRefunds(Account account, Map<String, String> paramsAsMap) {

        paramsAsMap.put(PARAM_ACCOUNT_ID, account.getAccountId());
        paramsAsMap.put(PARAM_TRANSACTION_TYPE, REFUND_TRANSACTION_TYPE);

        Response response = client
                .target(ledgerUriGenerator.transactionsURIWithParams(paramsAsMap))
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();

        if (response.getStatus() == SC_OK) {
            try {
                return response.readEntity(SearchRefundsResponseFromLedger.class);
            } catch (ProcessingException exception) {
                throw new SearchRefundsException(exception);
            }
        }

        throw new SearchRefundsException(response);
    }

    public SearchDisputesResponseFromLedger searchDisputes(Account account, Map<String, String> paramsAsMap) {
        paramsAsMap.put(PARAM_ACCOUNT_ID, account.getAccountId());
        paramsAsMap.put(PARAM_TRANSACTION_TYPE, DISPUTE_TRANSACTION_TYPE);

        if (paramsAsMap.containsKey("status")) {
            paramsAsMap.put("state", paramsAsMap.remove("status"));
        }

        Response response = client
                .target(ledgerUriGenerator.transactionsURIWithParams(paramsAsMap))
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();

        if (response.getStatus() == SC_OK) {
            try {
                return response.readEntity(SearchDisputesResponseFromLedger.class);
            } catch (ProcessingException exception) {
                throw new SearchDisputesException(exception);
            }
        }

        throw new SearchDisputesException(response);
    }

    public PaymentSearchResponse<TransactionResponse> searchPayments(Account account, Map<String, String> paramsAsMap) {

        paramsAsMap.put(PARAM_ACCOUNT_ID, account.getAccountId());
        paramsAsMap.put(PARAM_TRANSACTION_TYPE, PAYMENT_TRANSACTION_TYPE);
        paramsAsMap.put(PARAM_EXACT_REFERENCE_MATCH, "true");

        Response response = client
                .target(ledgerUriGenerator.transactionsURIWithParams(paramsAsMap))
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();

        if (response.getStatus() == SC_OK) {
            try {
                return response.readEntity(new GenericType<>() {
                });
            } catch (ProcessingException ex) {
                throw new SearchPaymentsException(ex);
            }
        }

        throw new SearchPaymentsException(response);
    }

    public AgreementLedgerResponse getAgreement(Account account, String agreementId) {
        Response response = client
                .target(ledgerUriGenerator.agreementURI(account, agreementId))
                .request()
                .header("X-Consistent", true)
                .get();

        if (response.getStatus() == SC_OK) {
            return response.readEntity(AgreementLedgerResponse.class);
        }

        throw new GetAgreementException(response);
    }

    public SearchResults<AgreementLedgerResponse> searchAgreements(Account account, AgreementSearchParams searchParams) {
        AgreementSearchValidator.validateSearchParameters(searchParams);

        var params = new HashMap<>(searchParams.getQueryMap());
        params.put(GATEWAY_ACCOUNT_ID, account.getAccountId());
        params.put(PARAM_EXACT_REFERENCE_MATCH, "true");

        String url = ledgerUriGenerator.agreementsURIWithParams(params);
        Response ledgerResponse = client
                .target(url)
                .request()
                .header(HttpHeaders.ACCEPT, APPLICATION_JSON)
                .get();
        LOGGER.info("response from ledger for agreement search: {}", ledgerResponse);

        if (ledgerResponse.getStatus() == SC_OK) {
            try {
                return ledgerResponse.readEntity(new GenericType<>() {
                });
            } catch (ProcessingException ex) {
                throw new SearchAgreementsException(ex);
            }
        }
        throw new SearchAgreementsException(ledgerResponse);
    }
}
