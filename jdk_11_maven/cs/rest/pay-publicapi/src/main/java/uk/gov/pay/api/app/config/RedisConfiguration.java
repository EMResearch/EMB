package uk.gov.pay.api.app.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import io.dropwizard.util.Duration;

import static java.lang.String.format;

public class RedisConfiguration {
    
    @Valid
    @NotNull
    @JsonProperty("endpoint")
    private String endpoint;

    @Valid
    @JsonProperty("ssl")
    private boolean ssl;
    
    @Valid
    @JsonProperty("commandTimeout")
    private Duration commandTimeout;

    @Valid
    @JsonProperty("connectTimeout")
    private Duration connectTimeout;

    public String getUrl() {
        return format("%s://%s", ssl ? "rediss" : "redis", endpoint);
    }

    public Long getCommandTimeout() {
        return commandTimeout.toMilliseconds();
    }

    public Long getConnectTimeout() {
        return connectTimeout.toMilliseconds();
    }
}
