package uk.gov.pay.api.agreement.service;

import org.apache.http.HttpStatus;
import uk.gov.pay.api.agreement.model.Agreement;
import uk.gov.pay.api.agreement.model.AgreementCreatedResponse;
import uk.gov.pay.api.agreement.model.AgreementLedgerResponse;
import uk.gov.pay.api.agreement.model.AgreementSearchResults;
import uk.gov.pay.api.agreement.model.CreateAgreementRequest;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.exception.CancelAgreementException;
import uk.gov.pay.api.exception.CreateAgreementException;
import uk.gov.pay.api.ledger.model.AgreementSearchParams;
import uk.gov.pay.api.ledger.model.SearchResults;
import uk.gov.pay.api.model.search.PaginationDecorator;
import uk.gov.pay.api.service.ConnectorService;
import uk.gov.pay.api.service.LedgerService;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.stream.Collectors;

public class AgreementsService {
    private static final String AGREEMENTS_PATH = "/v1/agreements";
    private final ConnectorService connectorService;
    private final LedgerService ledgerService;
    private final PaginationDecorator paginationDecorator;

    @Inject
    public AgreementsService(ConnectorService connectorService, LedgerService ledgerService, PaginationDecorator paginationDecorator) {
        this.connectorService = connectorService;
        this.ledgerService = ledgerService;
        this.paginationDecorator = paginationDecorator;
    }

    public AgreementCreatedResponse createAgreement(Account account, CreateAgreementRequest createAgreementRequest) {
        return connectorService.createAgreement(account, createAgreementRequest);
    }

    public Response cancelAgreement(Account account, String agreementId) {
        connectorService.cancelAgreement(account, agreementId);

        return Response.noContent().build();
    }

    public AgreementLedgerResponse getAgreement(Account account, String agreementId) {
        return ledgerService.getAgreement(account, agreementId);
    }

    public AgreementSearchResults searchAgreements(Account account, AgreementSearchParams params) {
        SearchResults<AgreementLedgerResponse> ledgerResponse = ledgerService.searchAgreements(account, params);
        return processLedgerResponse(ledgerResponse);
    }

    private AgreementSearchResults processLedgerResponse(SearchResults<AgreementLedgerResponse> searchResults) {
        return new AgreementSearchResults(searchResults.getTotal(),
                searchResults.getCount(),
                searchResults.getPage(),
                searchResults.getResults().stream().map(Agreement::from).collect(Collectors.toUnmodifiableList()),
                paginationDecorator.transformLinksToPublicApiUri(searchResults.getLinks(), AGREEMENTS_PATH));
    }
}
