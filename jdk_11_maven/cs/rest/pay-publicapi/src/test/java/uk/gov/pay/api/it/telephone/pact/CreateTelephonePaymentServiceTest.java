package uk.gov.pay.api.it.telephone.pact;

import au.com.dius.pact.consumer.PactVerification;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.api.app.RestClientFactory;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.app.config.RestClientConfig;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.model.TokenPaymentType;
import uk.gov.pay.api.model.telephone.CreateTelephonePaymentRequest;
import uk.gov.pay.api.model.telephone.PaymentOutcome;
import uk.gov.pay.api.model.telephone.Supplemental;
import uk.gov.pay.api.model.telephone.TelephonePaymentResponse;
import uk.gov.pay.api.service.ConnectorUriGenerator;
import uk.gov.pay.api.service.telephone.CreateTelephonePaymentService;
import uk.gov.service.payments.commons.testing.pact.consumers.PactProviderRule;
import uk.gov.service.payments.commons.testing.pact.consumers.Pacts;

import javax.ws.rs.client.Client;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CreateTelephonePaymentServiceTest {

    private CreateTelephonePaymentService createTelephonePaymentService;
    private static CreateTelephonePaymentRequest.Builder builder = new CreateTelephonePaymentRequest.Builder();

    @Rule
    public PactProviderRule connectorRule = new PactProviderRule("connector", this);

    @Mock
    private PublicApiConfig configuration;

    @Before
    public void setup() {
        when(configuration.getConnectorUrl()).thenReturn(connectorRule.getUrl()); // We will actually send real requests here, which will be intercepted by pact        

        ConnectorUriGenerator connectorUriGenerator = new ConnectorUriGenerator(configuration);
        Client client = RestClientFactory.buildClient(new RestClientConfig(false));

        createTelephonePaymentService = new CreateTelephonePaymentService(client, connectorUriGenerator);
    }

    @Test
    @PactVerification({"connector"})
    @Pacts(pacts = {"publicapi-connector-create-telephone-payment-notification"})
    public void testCreatePaymentWithMetadata() {
        Account account = new Account("123456", TokenPaymentType.CARD, "a-token-link");
        var createTelephonePaymentNotification =builder
                .withAmount(12000)
                .withReference("MRPC12345")
                .withDescription("New passport application")
                .withCreatedDate("2018-02-21T16:04:25Z")
                .withAuthorisedDate("2018-02-21T16:05:33Z")
                .withProcessorId("183f2j8923j8")
                .withProviderId("17498-8412u9-1273891239")
                .withAuthCode("auth12345")
                .withPaymentOutcome(new PaymentOutcome("failed", "P0010", new Supplemental("ECKOH01234", "textual message describing error code")))
                .withCardType("master-card")
                .withNameOnCard("J Doe")
                .withEmailAddress("j.doe@example.com")
                .withCardExpiry("02/19")
                .withLastFourDigits("1234")
                .withFirstSixDigits("654321")
                .withTelephoneNumber("+447700900796")
                .build();
        Pair<TelephonePaymentResponse, Integer> responseAndStatusCode = createTelephonePaymentService.create(account, createTelephonePaymentNotification);
        TelephonePaymentResponse telephonePaymentResponse = responseAndStatusCode.getLeft();
        int statusCode = responseAndStatusCode.getRight();
        
        assertThat(statusCode, is(201));

        assertThat(telephonePaymentResponse.getAmount(), is(12000L));
        assertThat(telephonePaymentResponse.getReference(), is("MRPC12345"));
        assertThat(telephonePaymentResponse.getDescription(), is("New passport application"));
        assertThat(telephonePaymentResponse.getCreatedDate(), is(Optional.of("2018-02-21T16:04:25.000Z")));
        assertThat(telephonePaymentResponse.getAuthorisedDate(), is(Optional.of("2018-02-21T16:05:33.000Z")));
        assertThat(telephonePaymentResponse.getProcessorId(), is("183f2j8923j8"));
        assertThat(telephonePaymentResponse.getProviderId(), is("17498-8412u9-1273891239"));
        assertThat(telephonePaymentResponse.getAuthCode(), is(Optional.of("auth12345")));
        assertThat(telephonePaymentResponse.getPaymentOutcome().getStatus(), is("failed"));
        assertThat(telephonePaymentResponse.getPaymentOutcome().getCode().get(), is("P0010"));
        assertThat(telephonePaymentResponse.getPaymentOutcome().getSupplemental().get().getErrorCode().get(), is("ECKOH01234"));
        assertThat(telephonePaymentResponse.getPaymentOutcome().getSupplemental().get().getErrorMessage().get(), is("textual message describing error code"));
        assertThat(telephonePaymentResponse.getCardType(), is("master-card"));
        assertThat(telephonePaymentResponse.getNameOnCard(), is(Optional.of("J Doe")));
        assertThat(telephonePaymentResponse.getCardExpiry(), is("02/19"));
        assertThat(telephonePaymentResponse.getLastFourDigits(), is("1234"));
        assertThat(telephonePaymentResponse.getFirstSixDigits(), is("654321"));
        assertThat(telephonePaymentResponse.getEmailAddress(), is(Optional.of("j.doe@example.com")));
        assertThat(telephonePaymentResponse.getTelephoneNumber(), is(Optional.of("+447700900796")));
    }
}
