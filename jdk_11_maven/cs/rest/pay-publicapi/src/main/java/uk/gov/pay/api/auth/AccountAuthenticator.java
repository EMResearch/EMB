package uk.gov.pay.api.auth;

import com.fasterxml.jackson.databind.JsonNode;
import io.dropwizard.auth.Authenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.api.app.config.PublicApiConfig;
import uk.gov.pay.api.model.publicauth.AuthResponse;
import uk.gov.service.payments.commons.model.ErrorIdentifier;

import javax.inject.Inject;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static java.lang.String.format;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static net.logstash.logback.argument.StructuredArguments.kv;

public class AccountAuthenticator implements Authenticator<String, Account> {
    private static Logger logger = LoggerFactory.getLogger(AccountAuthenticator.class);

    private final Client client;
    private final String publicAuthUrl;

    @Inject
    public AccountAuthenticator(Client client, PublicApiConfig configuration) {
        this.client = client;
        this.publicAuthUrl = configuration.getPublicAuthUrl();
    }

    @Override
    public Optional<Account> authenticate(String bearerToken) {

        Response response = client.target(publicAuthUrl).request()
                .header(AUTHORIZATION, "Bearer " + bearerToken)
                .accept(MediaType.APPLICATION_JSON)
                .get();

        if (response.getStatus() == OK.getStatusCode()) {
            AuthResponse authResponse = response.readEntity(AuthResponse.class);
            logger.info(format("Successfully authenticated using API key with token_link %s", authResponse.getTokenLink()),
                    kv("token_link", authResponse.getTokenLink()));
            return Optional.of(new Account(authResponse.getAccountId(), authResponse.getTokenType(), authResponse.getTokenLink()));
        } else if (response.getStatus() == UNAUTHORIZED.getStatusCode()) {
            JsonNode unauthorisedResponse = response.readEntity(JsonNode.class);
            ErrorIdentifier errorIdentifier = ErrorIdentifier.valueOf(unauthorisedResponse.get("error_identifier").asText());
            if (errorIdentifier == ErrorIdentifier.AUTH_TOKEN_REVOKED) {
                String tokenLink = unauthorisedResponse.get("token_link").asText();
                logger.warn(format("Attempt to authenticate using revoked API key with token_link %s", tokenLink), kv("token_link", tokenLink));
            } else {
                logger.warn("Attempt to authenticate using invalid API key with valid checksum");
            }
            response.close();
            return Optional.empty();
        } else {
            response.close();
            logger.warn("Unexpected status code " + response.getStatus() + " from auth.");
            throw new ServiceUnavailableException();
        }
    }
}
