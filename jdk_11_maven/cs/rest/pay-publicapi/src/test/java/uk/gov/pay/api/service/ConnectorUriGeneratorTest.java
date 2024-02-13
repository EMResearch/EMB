package uk.gov.pay.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.auth.Account;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static uk.gov.pay.api.model.TokenPaymentType.CARD;

@ExtendWith(MockitoExtension.class)
public class ConnectorUriGeneratorTest {
    @Mock
    private PublicApiConfig mockPublicApiConfig;

    private ConnectorUriGenerator connectorUriGenerator;

    private final Account cardAccount = new Account("accountId", CARD, "a-token-link");

    private final String chargeId = "charge_id_123";

    @BeforeEach
    public void setUp() {
        connectorUriGenerator = new ConnectorUriGenerator(mockPublicApiConfig);
        when(mockPublicApiConfig.getConnectorUrl()).thenReturn("https://bla.test");
    }

    @Test
    public void shouldGenerateTheRightChargeURIForCardConnector() {
        String uri = connectorUriGenerator.chargesURI(cardAccount);
        assertThat(uri, is("https://bla.test/v1/api/accounts/accountId/charges"));
    }

    @Test
    public void shouldGenerateTheRightChargeURI() {
        String uri = connectorUriGenerator.chargeURI(cardAccount, chargeId);
        assertThat(uri, is("https://bla.test/v1/api/accounts/accountId/charges/" + chargeId));
    }

    @Test
    public void shouldGenerateTheRightTelephoneChargeURI() {
        String uri = connectorUriGenerator.telephoneChargesURI(cardAccount);
        assertThat(uri, is("https://bla.test/v1/api/accounts/accountId/telephone-charges"));
    }

    @Test
    public void shouldGenerateTheRightCancelURI() {
        String uri = connectorUriGenerator.cancelURI(cardAccount, chargeId);
        assertThat(uri, is("https://bla.test/v1/api/accounts/accountId/charges/" + chargeId + "/cancel"));
    }

    @Test
    public void shouldGenerateTheRightCaptureURI() {
        String uri = connectorUriGenerator.captureURI(cardAccount, chargeId);
        assertThat(uri, is("https://bla.test/v1/api/accounts/" + cardAccount.getAccountId() + "/charges/" + chargeId + "/capture"));
    }

    @Test
    public void shouldGenerateTheRightEventsURI() {
        String uri = connectorUriGenerator.chargeEventsURI(cardAccount, chargeId);
        assertThat(uri, is("https://bla.test/v1/api/accounts/accountId/charges/" + chargeId + "/events"));
    }

    @Test
    public void shouldGenerateTheRightAuthoriseURI() {
        String uri = connectorUriGenerator.authorisationURI();
        assertThat(uri, is("https://bla.test/v1/api/charges/authorise"));
    }
}
