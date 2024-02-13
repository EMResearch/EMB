package uk.gov.pay.api.service.telephone;


import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpStatus;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.exception.CreateChargeException;
import uk.gov.pay.api.model.ChargeFromResponse;
import uk.gov.pay.api.model.telephone.CreateTelephonePaymentRequest;
import uk.gov.pay.api.model.telephone.TelephonePaymentResponse;
import uk.gov.pay.api.service.ConnectorUriGenerator;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.client.Entity.json;

public class CreateTelephonePaymentService {

    private final Client client;
    private final ConnectorUriGenerator connectorUriGenerator;

    @Inject
    public CreateTelephonePaymentService(Client client, ConnectorUriGenerator connectorUriGenerator) {
        this.client = client;
        this.connectorUriGenerator = connectorUriGenerator;
    }
    
    public Pair<TelephonePaymentResponse, Integer> create(Account account, CreateTelephonePaymentRequest createTelephonePaymentRequest) {
        Response connectorResponse = createTelephoneCharge(account, createTelephonePaymentRequest);

        if (!createdSuccessfully(connectorResponse)) {
            throw new CreateChargeException(connectorResponse);
        }

        ChargeFromResponse chargeFromResponse = connectorResponse.readEntity(ChargeFromResponse.class);
        TelephonePaymentResponse telephonePaymentResponse = TelephonePaymentResponse.from(chargeFromResponse);
        return Pair.of(telephonePaymentResponse, connectorResponse.getStatus());
    }

    private boolean createdSuccessfully(Response connectorResponse) {
        return connectorResponse.getStatus() == HttpStatus.SC_CREATED || connectorResponse.getStatus() == HttpStatus.SC_OK;
    }

    private Response createTelephoneCharge(Account account, CreateTelephonePaymentRequest createTelephonePaymentRequest) {
        return client
                .target(connectorUriGenerator.telephoneChargesURI(account))
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .post(buildTelephoneChargeRequestPayload(createTelephonePaymentRequest));
    }

    private Entity buildTelephoneChargeRequestPayload(CreateTelephonePaymentRequest requestPayload) {
        return json(requestPayload.toConnectorPayload());
    }
}
