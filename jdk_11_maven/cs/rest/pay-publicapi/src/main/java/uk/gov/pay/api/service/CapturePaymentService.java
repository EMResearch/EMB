package uk.gov.pay.api.service;

import uk.gov.pay.api.auth.Account;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

public class CapturePaymentService {

    private final Client client;
    private final ConnectorUriGenerator connectorUriGenerator;

    @Inject
    public CapturePaymentService(Client client, ConnectorUriGenerator connectorUriGenerator) {
        this.client = client;
        this.connectorUriGenerator = connectorUriGenerator;
    }

    public Response capture(Account account, String chargeId) {
        return client
                .target(connectorUriGenerator.captureURI(account, chargeId))
                .request()
                .post(Entity.json("{}"));
    }

}
