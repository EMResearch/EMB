package uk.gov.pay.api.service;

import org.apache.http.HttpStatus;
import uk.gov.pay.api.agreement.model.AgreementCreatedResponse;
import uk.gov.pay.api.agreement.model.CreateAgreementRequest;
import uk.gov.pay.api.auth.Account;
import uk.gov.pay.api.exception.CancelAgreementException;
import uk.gov.pay.api.exception.CreateAgreementException;
import uk.gov.pay.api.exception.GetChargeException;
import uk.gov.pay.api.exception.GetEventsException;
import uk.gov.pay.api.exception.GetRefundException;
import uk.gov.pay.api.exception.GetRefundsException;
import uk.gov.pay.api.model.Charge;
import uk.gov.pay.api.model.ChargeFromResponse;
import uk.gov.pay.api.model.PaymentEvents;
import uk.gov.pay.api.model.RefundFromConnector;
import uk.gov.pay.api.model.RefundsFromConnector;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.client.Entity.json;
import static org.apache.http.HttpStatus.SC_OK;

public class ConnectorService {
    private final Client client;
    private final ConnectorUriGenerator connectorUriGenerator;

    @Inject
    public ConnectorService(Client client, ConnectorUriGenerator connectorUriGenerator) {
        this.client = client;
        this.connectorUriGenerator = connectorUriGenerator;
    }

    public Charge getCharge(Account account, String paymentId) {
        Response response = client
                .target(connectorUriGenerator.chargeURI(account, paymentId))
                .request()
                .get();

        if (response.getStatus() == SC_OK) {
            ChargeFromResponse chargeFromResponse = response.readEntity(ChargeFromResponse.class);
            return Charge.from(chargeFromResponse);
        }

        throw new GetChargeException(response);
    }

    public PaymentEvents getChargeEvents(Account account, String paymentId) {
        Response connectorResponse = client
                .target(connectorUriGenerator.chargeEventsURI(account, paymentId))
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get();

        if (connectorResponse.getStatus() == SC_OK) {
            return connectorResponse.readEntity(PaymentEvents.class);
        }

        throw new GetEventsException(connectorResponse);
    }

    public RefundsFromConnector getPaymentRefunds(String accountId, String paymentId) {
        Response connectorResponse = client
                .target(connectorUriGenerator.refundsForPaymentURI(accountId, paymentId))
                .request()
                .get();

        if (connectorResponse.getStatus() == SC_OK) {
            return connectorResponse.readEntity(RefundsFromConnector.class);
        }

        throw new GetRefundsException(connectorResponse);
    }

    public RefundFromConnector getPaymentRefund(String accountId, String paymentId, String refundId) {
        Response connectorResponse = client
                .target(connectorUriGenerator.refundForPaymentURI(accountId, paymentId, refundId))
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get();

        if (connectorResponse.getStatus() == SC_OK) {
            return connectorResponse.readEntity(RefundFromConnector.class);
        }

        throw new GetRefundException(connectorResponse);
    }

    public AgreementCreatedResponse createAgreement(Account account, CreateAgreementRequest createAgreementRequest) {
        Response response = client
                .target(connectorUriGenerator.getAgreementURI(account))
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .post(buildCreateAgreementRequestPayload(createAgreementRequest));
        if (response.getStatus() != HttpStatus.SC_CREATED) {
            throw new CreateAgreementException(response);
        }
        return response.readEntity(AgreementCreatedResponse.class);
    }

    public void cancelAgreement(Account account, String agreementId) {
        Response response = client
                .target(connectorUriGenerator.cancelAgreementURI(account, agreementId))
                .request()
                .post(null);
        if (response.getStatus() != HttpStatus.SC_NO_CONTENT) {
            throw new CancelAgreementException(response);
        }

        response.close();
    }

    private Entity buildCreateAgreementRequestPayload(CreateAgreementRequest requestPayload) {
        return json(requestPayload.toConnectorPayload());
    }
}
