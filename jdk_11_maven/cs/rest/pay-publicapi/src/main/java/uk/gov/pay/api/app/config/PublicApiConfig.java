package uk.gov.pay.api.app.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.Optional;

public class PublicApiConfig extends Configuration {

    @NotNull
    private String baseUrl;

    @NotNull
    private String connectorUrl;

    @NotNull
    private String ledgerUrl;

    @NotNull
    private String publicAuthUrl;

    @NotNull
    private Boolean allowHttpForReturnUrl;

    private String apiKeyHmacSecret;

    @NotNull
    private CaffeineSpec authenticationCachePolicy;

    @Valid
    @NotNull
    @JsonProperty("jerseyClientConfig")
    private RestClientConfig restClientConfig;

    @NotNull
    @JsonProperty
    private RedisConfiguration redis;

    @Valid
    @NotNull
    @JsonProperty("rateLimiter")
    private RateLimiterConfig rateLimiterConfig;

    @JsonProperty("ecsContainerMetadataUriV4")
    private URI ecsContainerMetadataUriV4;

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getConnectorUrl() {
        return connectorUrl;
    }

    public String getLedgerUrl() {
        return ledgerUrl;
    }

    public String getPublicAuthUrl() {
        return publicAuthUrl;
    }

    public Boolean getAllowHttpForReturnUrl() {
        return allowHttpForReturnUrl;
    }

    public String getApiKeyHmacSecret() {
        return apiKeyHmacSecret;
    }

    public RestClientConfig getRestClientConfig() {
        return restClientConfig;
    }

    public RateLimiterConfig getRateLimiterConfig() {
        return rateLimiterConfig;
    }

    public CaffeineSpec getAuthenticationCachePolicy() {
        return authenticationCachePolicy;
    }

    public RedisConfiguration getRedisConfiguration() {
        return redis;
    }

    public Optional<URI> getEcsContainerMetadataUriV4() {
        return Optional.ofNullable(ecsContainerMetadataUriV4);
    }
}
