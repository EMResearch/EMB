package uk.gov.pay.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.service.payments.commons.model.Source;

import java.util.Optional;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Internal {

    @JsonProperty("source")
    private Source source;

    public Optional<Source> getSource() {
        return Optional.ofNullable(source);
    }

    public void setSource(Source source) {
        this.source = source;
    }
}
