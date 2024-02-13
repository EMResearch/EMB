package uk.gov.pay.api.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableMap;
import uk.gov.pay.api.auth.Account;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class WiremockStubbing {
    
    public static void stubPublicAuthV1ApiAuth(WireMockRule wireMockRule, Account account, String token) throws JsonProcessingException {
        Map<String, String> entity = ImmutableMap.of("account_id", account.getAccountId(), "token_type", account.getPaymentType().name());
        String json = new ObjectMapper().writeValueAsString(entity);
        wireMockRule.stubFor(get(urlEqualTo("/v1/api/auth"))
                .withHeader(AUTHORIZATION, equalTo("Bearer " + token))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(json)));
    }
}
