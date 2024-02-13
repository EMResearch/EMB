package uk.gov.pay.api.service;

import black.door.hate.HalRepresentation;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.model.TransactionResponse;
import uk.gov.pay.api.model.search.PaginationDecorator;
import uk.gov.pay.api.model.search.card.PaymentForSearchResult;
import uk.gov.pay.api.model.search.card.PaymentSearchResponse;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.apache.http.HttpHeaders.CACHE_CONTROL;
import static org.apache.http.HttpHeaders.PRAGMA;
import static uk.gov.pay.api.validation.PaymentSearchValidator.validateSearchParameters;

public class PaymentSearchService {

    private static final String PAYMENTS_PATH = "/v1/payments";

    private final PublicApiUriGenerator publicApiUriGenerator;
    private final PaginationDecorator paginationDecorator;
    private LedgerService ledgerService;

    @Inject
    public PaymentSearchService(PublicApiUriGenerator publicApiUriGenerator,
                                PaginationDecorator paginationDecorator,
                                LedgerService ledgerService) {
        this.publicApiUriGenerator = publicApiUriGenerator;
        this.paginationDecorator = paginationDecorator;
        this.ledgerService = ledgerService;
    }

    public Response searchLedgerPayments(Account account, PaymentSearchParams searchParams) {
        validateSearchParameters(searchParams);

        PaymentSearchResponse<TransactionResponse> paymentSearchResponse =
                ledgerService.searchPayments(account, searchParams.getParamsAsMap());
        return processLedgerResponse(paymentSearchResponse);
    }

    private Response processLedgerResponse(PaymentSearchResponse<TransactionResponse> paymentSearchResponse) {
        List<PaymentForSearchResult> chargeFromResponses = paymentSearchResponse.getPayments()
                .stream()
                .map(t -> PaymentForSearchResult.valueOf(
                        t,
                        publicApiUriGenerator.getPaymentURI(t.getTransactionId()),
                        publicApiUriGenerator.getPaymentEventsURI(t.getTransactionId()),
                        publicApiUriGenerator.getPaymentCancelURI(t.getTransactionId()),
                        publicApiUriGenerator.getPaymentRefundsURI(t.getTransactionId()),
                        publicApiUriGenerator.getPaymentCaptureURI(t.getTransactionId())))
                .collect(toList());

        HalRepresentation.HalRepresentationBuilder halRepresentation = HalRepresentation
                .builder()
                .addProperty("results", chargeFromResponses);

        return Response.ok()
                .header(PRAGMA, "no-cache")
                .header(CACHE_CONTROL, "no-store")
                .entity(paginationDecorator
                        .decoratePagination(halRepresentation, paymentSearchResponse, PAYMENTS_PATH)
                        .build()
                        .toString())
                .build();
    }
}
