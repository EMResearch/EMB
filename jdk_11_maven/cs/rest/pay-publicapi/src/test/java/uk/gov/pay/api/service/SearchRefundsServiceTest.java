package uk.gov.pay.api.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.exception.RefundsValidationException;
import uk.gov.pay.api.ledger.service.LedgerUriGenerator;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.pay.api.model.search.PaginationDecorator;

import javax.ws.rs.client.Client;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static uk.gov.pay.api.matcher.RefundValidationExceptionMatcher.aValidationExceptionContaining;

@RunWith(MockitoJUnitRunner.class)
public class SearchRefundsServiceTest {
    @Mock
    private PublicApiConfig mockConfiguration;
    @Mock
    private Client mockClient;
    @Mock
    private LedgerUriGenerator mockLedgerUriGenerator;
    private SearchRefundsService searchRefundsService;
    private String ACCOUNT_ID = "888";
    private static final String tokenLink = "a-token-link";

    @Before
    public void setUp() {
        searchRefundsService = new SearchRefundsService(
                new LedgerService(mockClient, mockLedgerUriGenerator),
                new PublicApiUriGenerator(mockConfiguration),
                new PaginationDecorator(mockConfiguration));
    }

    @Test
    public void getSearchResponse_shouldThrowRefundsValidationExceptionWhenParamsAreInvalid() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, tokenLink);
        String invalid = "invalid_param";
        RefundsParams params = new RefundsParams(null, null, invalid, invalid, null, null);

        RefundsValidationException refundsValidationException = assertThrows(RefundsValidationException.class,
                () -> searchRefundsService.searchLedgerRefunds(account, params));

        assertThat(refundsValidationException, aValidationExceptionContaining(
                "P1101",
                format("Invalid parameters: %s. See Public API documentation for the correct data formats",
                        "page, display_size")));
    }

    @Test
    public void getSearchResponseFromLedger_shouldThrowRefundsValidationExceptionWhenParamsAreInvalid() {
        Account account = new Account(ACCOUNT_ID, TokenPaymentType.CARD, tokenLink);
        String invalid = "invalid_param";
        RefundsParams params = new RefundsParams(null, null, invalid, invalid, null, null);

        RefundsValidationException refundsValidationException = assertThrows(RefundsValidationException.class,
                () -> searchRefundsService.searchLedgerRefunds(account, params));

        assertThat(refundsValidationException, aValidationExceptionContaining(
                "P1101",
                format("Invalid parameters: %s. See Public API documentation for the correct data formats",
                        "page, display_size")));
    }
}
