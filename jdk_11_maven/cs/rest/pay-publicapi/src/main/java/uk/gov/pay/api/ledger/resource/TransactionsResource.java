package uk.gov.pay.api.ledger.resource;

import com.codahale.metrics.annotation.Timed;
import io.dropwizard.auth.Auth;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.ledger.model.SearchResults;
import uk.gov.pay.api.ledger.model.TransactionSearchParams;
import uk.gov.pay.api.ledger.service.TransactionSearchService;
import uk.gov.pay.api.model.search.card.PaymentForSearchResult;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/")
@Produces({"application/json"})
public class TransactionsResource {

    private final TransactionSearchService transactionSearchService;

    @Inject
    public TransactionsResource(TransactionSearchService transactionSearchService) {
        this.transactionSearchService = transactionSearchService;
    }

    @GET
    @Timed
    @Path("/v1/transactions")
    @Produces(APPLICATION_JSON)
    public SearchResults<PaymentForSearchResult> getTransactions(@Auth Account account, @BeanParam TransactionSearchParams searchParams) {
        return transactionSearchService.doSearch(account, searchParams);
    }
}
