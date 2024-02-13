package uk.gov.pay.api.ledger.service;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.exception.BadRequestException;
import uk.gov.pay.api.exception.SearchPaymentsException;
import uk.gov.pay.api.exception.SearchTransactionsException;
import uk.gov.pay.api.ledger.model.SearchResults;
import uk.gov.pay.api.ledger.model.TransactionSearchParams;
import uk.gov.pay.api.model.RequestError;
import uk.gov.pay.api.model.TransactionResponse;
import uk.gov.pay.api.model.links.Link;
import uk.gov.pay.api.model.links.SearchNavigationLinks;
import uk.gov.pay.api.model.search.card.PaymentForSearchResult;
import uk.gov.pay.api.model.search.card.PaymentSearchResponse;
import uk.gov.pay.api.service.PaymentUriGenerator;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.pay.api.common.SearchConstants.CARDHOLDER_NAME_KEY;
import static uk.gov.pay.api.common.SearchConstants.CARD_BRAND_KEY;
import static uk.gov.pay.api.common.SearchConstants.DISPLAY_SIZE;
import static uk.gov.pay.api.common.SearchConstants.EMAIL_KEY;
import static uk.gov.pay.api.common.SearchConstants.FIRST_DIGITS_CARD_NUMBER_KEY;
import static uk.gov.pay.api.common.SearchConstants.FROM_DATE_KEY;
import static uk.gov.pay.api.common.SearchConstants.LAST_DIGITS_CARD_NUMBER_KEY;
import static uk.gov.pay.api.common.SearchConstants.PAGE;
import static uk.gov.pay.api.common.SearchConstants.REFERENCE_KEY;
import static uk.gov.pay.api.common.SearchConstants.STATE_KEY;
import static uk.gov.pay.api.common.SearchConstants.TO_DATE_KEY;
import static uk.gov.pay.api.validation.PaymentSearchValidator.validateSearchParameters;

public class TransactionSearchService {

    private final String baseUrl;
    private final PaymentUriGenerator paymentApiUriGenerator;
    private final Client client;
    private final LedgerUriGenerator ledgerUriGenerator;
    private final Logger LOGGER = LoggerFactory.getLogger(TransactionSearchService.class);

    @Inject
    public TransactionSearchService(Client client,
                                    PublicApiConfig configuration,
                                    LedgerUriGenerator ledgerUriGenerator,
                                    PaymentUriGenerator paymentApiUriGenerator) {
        this.client = client;
        this.ledgerUriGenerator = ledgerUriGenerator;
        this.paymentApiUriGenerator = paymentApiUriGenerator;
        this.baseUrl = configuration.getBaseUrl();
    }

    public SearchResults<PaymentForSearchResult> doSearch(Account account, TransactionSearchParams searchParams) {
        validateSearchParameters(searchParams.getState(), searchParams.getReference(),
                searchParams.getEmail(), searchParams.getCardBrand(), searchParams.getFromDate(),
                searchParams.getToDate(), searchParams.getPageNumber(), searchParams.getDisplaySize(),
                searchParams.getFirstDigitsCardNumber(), searchParams.getLastDigitsCardNumber(),
                searchParams.getFromSettledDate(), searchParams.getToSettledDate());
        validateSupportedSearchParams(searchParams.getQueryMap());

        searchParams.setAccountId(account.getAccountId());
        String url = ledgerUriGenerator.transactionsURIWithParams(searchParams.getQueryMap());
        Response ledgerResponse = client
                .target(url)
                .request()
                .header(HttpHeaders.ACCEPT, APPLICATION_JSON)
                .get();
        LOGGER.info("response from ledger for transaction search: {}", ledgerResponse);
        if (ledgerResponse.getStatus() == SC_OK) {
            return processResponse(ledgerResponse);
        }
        throw new SearchTransactionsException(ledgerResponse);
    }

    private SearchResults<PaymentForSearchResult> processResponse(Response connectorResponse) {
        PaymentSearchResponse<TransactionResponse> response;
        try {
            response = connectorResponse.readEntity(new GenericType<>() {
            });
        } catch (ProcessingException ex) {
            throw new SearchTransactionsException(ex);
        }

        List<PaymentForSearchResult> chargeFromResponses = response.getPayments()
                .stream()
                .map(this::getPaymentForSearchResult)
                .collect(toList());

        return new SearchResults<>(
                response.getTotal(),
                response.getCount(),
                response.getPage(),
                chargeFromResponses,
                transformLinks(response.getLinks())
        );
    }

    private PaymentForSearchResult getPaymentForSearchResult(TransactionResponse transactionResponse) {
        return PaymentForSearchResult.valueOf(
                transactionResponse,
                paymentApiUriGenerator.getPaymentURI(baseUrl, transactionResponse.getTransactionId()),
                paymentApiUriGenerator.getPaymentEventsURI(baseUrl, transactionResponse.getTransactionId()),
                paymentApiUriGenerator.getPaymentCancelURI(baseUrl, transactionResponse.getTransactionId()),
                paymentApiUriGenerator.getPaymentRefundsURI(baseUrl, transactionResponse.getTransactionId()),
                paymentApiUriGenerator.getPaymentCaptureURI(baseUrl, transactionResponse.getTransactionId()));
    }

    private SearchNavigationLinks transformLinks(SearchNavigationLinks links) {
        final String path = "/v1/transactions";

        try {
            return new SearchNavigationLinks()
                    .withSelfLink(transformIntoPublicUri(baseUrl, links.getSelf(), path))
                    .withFirstLink(transformIntoPublicUri(baseUrl, links.getFirstPage(), path))
                    .withLastLink(transformIntoPublicUri(baseUrl, links.getLastPage(), path))
                    .withPrevLink(transformIntoPublicUri(baseUrl, links.getPrevPage(), path))
                    .withNextLink(transformIntoPublicUri(baseUrl, links.getNextPage(), path));
        } catch (URISyntaxException ex) {
            throw new SearchPaymentsException(ex);
        }
    }

    private String transformIntoPublicUri(String baseUrl, Link link, String path) throws URISyntaxException {
        if (link == null)
            return null;

        return UriBuilder.fromUri(baseUrl)
                .path(path)
                .replaceQuery(new URI(link.getHref()).getQuery())
                .replaceQueryParam("account_id", (Object[]) null)
                .build()
                .toString();
    }

    private void validateSupportedSearchParams(Map<String, String> queryParams) {
        queryParams.entrySet().stream()
                .filter(this::isUnsupportedParamWithNonBlankValue)
                .findFirst()
                .ifPresent(invalidParam -> {
                    throw new BadRequestException(RequestError
                            .aRequestError(RequestError.Code.SEARCH_PAYMENTS_VALIDATION_ERROR, invalidParam.getKey()));
                });
    }

    private boolean isUnsupportedParamWithNonBlankValue(Map.Entry<String, String> queryParam) {
        return !getSupportedSearchParams().contains(queryParam.getKey()) && isNotBlank(queryParam.getValue());
    }

    private Set<String> getSupportedSearchParams() {
        return ImmutableSet.of(REFERENCE_KEY, EMAIL_KEY, STATE_KEY, CARD_BRAND_KEY, CARDHOLDER_NAME_KEY, FIRST_DIGITS_CARD_NUMBER_KEY, LAST_DIGITS_CARD_NUMBER_KEY, FROM_DATE_KEY, TO_DATE_KEY, PAGE, DISPLAY_SIZE);
    }
}
