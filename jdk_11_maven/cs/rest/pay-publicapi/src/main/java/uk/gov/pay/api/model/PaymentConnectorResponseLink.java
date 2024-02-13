package uk.gov.pay.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentConnectorResponseLink {

    private String rel;
    private String href;
    private String method;
    private String type;
    private Map<String, Object> params;

    // required for Jackson
    private PaymentConnectorResponseLink() {
    }

    public PaymentConnectorResponseLink(String rel,
                                        String href,
                                        String method,
                                        String type,
                                        Map<String, Object> params) {
        this.rel = rel;
        this.href = href;
        this.method = method;
        this.type = type;
        this.params = params;
    }

    public String getRel() {
        return rel;
    }

    public String getHref() {
        return href;
    }

    public String getMethod() {
        return method;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentConnectorResponseLink that = (PaymentConnectorResponseLink) o;
        return Objects.equals(rel, that.rel) &&
                Objects.equals(href, that.href) &&
                Objects.equals(method, that.method) &&
                Objects.equals(type, that.type) &&
                Objects.equals(params, that.params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rel, href, method, type, params);
    }
}
