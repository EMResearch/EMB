package uk.gov.pay.api.service;

import org.apache.http.HttpStatus;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.exception.CreateChargeException;
import uk.gov.pay.api.model.Charge;
import uk.gov.pay.api.model.ChargeFromResponse;
import uk.gov.pay.api.model.CreateCardPaymentRequest;
import uk.gov.pay.api.model.CreatedPaymentWithAllLinks;
import uk.gov.pay.api.model.links.PaymentWithAllLinks;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import java.util.Optional;

import static javax.ws.rs.client.Entity.json;
import static uk.gov.pay.api.model.CreatedPaymentWithAllLinks.WhenCreated.BRAND_NEW;
import static uk.gov.pay.api.model.CreatedPaymentWithAllLinks.WhenCreated.EXISTING;

public class CreatePaymentService {

    private final Client client;
    private final PublicApiUriGenerator publicApiUriGenerator;
    private final ConnectorUriGenerator connectorUriGenerator;

    @Inject
    public CreatePaymentService(Client client, PublicApiUriGenerator publicApiUriGenerator, ConnectorUriGenerator connectorUriGenerator) {
        this.client = client;
        this.publicApiUriGenerator = publicApiUriGenerator;
        this.connectorUriGenerator = connectorUriGenerator;
    }

    public CreatedPaymentWithAllLinks create(Account account, CreateCardPaymentRequest createCardPaymentRequest, String idempotencyKey) {
        Response connectorResponse = createCharge(account, createCardPaymentRequest, idempotencyKey);

        if (connectorCreatedNewPayment(connectorResponse)) {
            ChargeFromResponse chargeFromResponse = connectorResponse.readEntity(ChargeFromResponse.class);
            return CreatedPaymentWithAllLinks.of(buildResponseModel(Charge.from(chargeFromResponse)), BRAND_NEW);
        }

        if (connectorReturnedExistingPayment(connectorResponse)) {
            ChargeFromResponse chargeFromResponse = connectorResponse.readEntity(ChargeFromResponse.class);
            return CreatedPaymentWithAllLinks.of(buildResponseModel(Charge.from(chargeFromResponse)), EXISTING);
        }

        throw new CreateChargeException(connectorResponse);
    }
   private PaymentWithAllLinks buildResponseModel(Charge chargeFromResponse) {
        return PaymentWithAllLinks.getPaymentWithLinks(
                chargeFromResponse,
                publicApiUriGenerator.getPaymentURI(chargeFromResponse.getChargeId()),
                publicApiUriGenerator.getPaymentEventsURI(chargeFromResponse.getChargeId()),
                publicApiUriGenerator.getPaymentCancelURI(chargeFromResponse.getChargeId()),
                publicApiUriGenerator.getPaymentRefundsURI(chargeFromResponse.getChargeId()),
                publicApiUriGenerator.getPaymentCaptureURI(chargeFromResponse.getChargeId()),
                publicApiUriGenerator.getPaymentAuthorisationURI());
    }

    private boolean connectorCreatedNewPayment(Response connectorResponse) {
        return connectorResponse.getStatus() == HttpStatus.SC_CREATED;
    }

    private boolean connectorReturnedExistingPayment(Response connectorResponse) {
        return connectorResponse.getStatus() == HttpStatus.SC_OK;
    }


    private Response createCharge(Account account, CreateCardPaymentRequest createCardPaymentRequest, String idempotencyKey) {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();

        Optional.ofNullable(idempotencyKey)
                .ifPresent(key -> headers.add("Idempotency-Key", idempotencyKey));

        return client
                .target(connectorUriGenerator.chargesURI(account))
                .request()
                .headers(headers)
                .accept(MediaType.APPLICATION_JSON)
                .post(buildChargeRequestPayload(createCardPaymentRequest));
    }

    private Entity buildChargeRequestPayload(CreateCardPaymentRequest requestPayload) {
        return json(requestPayload.toConnectorPayload());
    }
}
