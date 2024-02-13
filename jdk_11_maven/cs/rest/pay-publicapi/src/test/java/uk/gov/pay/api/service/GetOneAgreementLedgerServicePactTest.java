package uk.gov.pay.api.service;

import au.com.dius.pact.consumer.PactVerification;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.api.agreement.model.AgreementLedgerResponse;
import uk.gov.pay.api.app.RestClientFactory;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.app.config.RestClientConfig;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.exception.GetAgreementException;
import uk.gov.pay.api.ledger.service.LedgerUriGenerator;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.service.payments.commons.testing.pact.consumers.PactProviderRule;
import uk.gov.service.payments.commons.testing.pact.consumers.Pacts;

import javax.ws.rs.client.Client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GetOneAgreementLedgerServicePactTest {

    private final Account ACCOUNT = new Account("3456", TokenPaymentType.CARD, "a-token-link");

    @Rule
    public PactProviderRule ledgerRule = new PactProviderRule("ledger", this);

    private LedgerService ledgerService;
    @Mock
    private PublicApiConfig mockConfiguration;

    @Before
    public void setUp() {
        when(mockConfiguration.getLedgerUrl()).thenReturn(ledgerRule.getUrl());
        LedgerUriGenerator ledgerUriGenerator = new LedgerUriGenerator(mockConfiguration);
        Client client = RestClientFactory.buildClient(new RestClientConfig(false));
        ledgerService = new LedgerService(client, ledgerUriGenerator);
    }

    @Test
    @PactVerification({"ledger"})
    @Pacts(pacts = {"publicapi-ledger-get-one-agreement"})
    public void getOneAgreement() {
        AgreementLedgerResponse getAgreementResponse = ledgerService.getAgreement(ACCOUNT, "abcdefghijklmnopqrstuvwxyz");
        assertThat(getAgreementResponse.getStatus(), is("ACTIVE"));
        assertThat(getAgreementResponse.getExternalId(), is("abcdefghijklmnopqrstuvwxyz"));
        assertThat(getAgreementResponse.getPaymentInstrument().getAgreementExternalId(), is("abcdefghijklmnopqrstuvwxyz"));
    }

    @Test
    @PactVerification({"ledger"})
    @Pacts(pacts = {"publicapi-ledger-get-one-agreement-not-found"})
    public void getOneAgreementNotFound() {
        GetAgreementException getAgreementException = assertThrows(GetAgreementException.class,
        () -> ledgerService.getAgreement(ACCOUNT, "non-existent-agreement-id"));
        assertThat(getAgreementException, hasProperty("errorStatus", is(404)));
    }
}
