package uk.gov.pay.api.model.links;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;
import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(name = "Link", description = "A link related to a payment")
@JsonInclude(Include.NON_NULL)
public class Link {

    @JsonProperty(value = "href")
    @Schema(example = "https://an.example.link/from/payment/platform", 
            description = "A URL that lets you perform additional actions to this payment " +
                    "when combined with the associated `method`.",
            accessMode = READ_ONLY)
    private String href;
    @JsonProperty(value = "method")
    @Schema(example = "GET", 
            description = "An API method that lets you perform additional actions to this payment" +
                    "when combined with the associated `href`.",
            accessMode = READ_ONLY)
    private String method;

    public Link(String href, String method) {
        this.href = href;
        this.method = method;
    }

    public Link(String href) {
        this.href = href;
    }

    public Link() {}

    public String getHref() {
        return href;
    }

    public String getMethod() {
        return method;
    }

    @Override
    public String toString() {
        return "Link{" +
                "href='" + href + '\'' +
                ", method='" + method + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Link link = (Link) o;
        return Objects.equals(href, link.href) &&
                Objects.equals(method, link.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(href, method);
    }
}
