package uk.gov.pay.api.app.config;

import io.dropwizard.Configuration;

public class RestClientConfig extends Configuration {
    
    private String disabledSecureConnection = "false";

    public RestClientConfig() {
    }

    public RestClientConfig(boolean disabledSecureConnection) {
        this.disabledSecureConnection = Boolean.valueOf(disabledSecureConnection).toString();
    }

    public Boolean isDisabledSecureConnection() {
        return "true".equals(disabledSecureConnection);
    }

}
