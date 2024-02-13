package uk.gov.pay.api.agreement.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.api.model.CreateAgreementRequestBuilder;
import uk.gov.pay.api.utils.JsonStringBuilder;
import javax.validation.constraints.Size;
import java.util.Objects;

@Schema(description = "The Agreement Request Payload")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateAgreementRequest {
    
    public static final String USER_IDENTIFIER_FIELD = "user_identifier";

    public static final int MIN_LENGTH = 1;
    public static final int MAX_LENGTH = 255;

    @JsonProperty("reference")
    @Size(min= MIN_LENGTH, max = MAX_LENGTH, message = "Must be less than or equal to {max} characters length")
    private String reference;

    @JsonProperty("description")
    @Size(min= MIN_LENGTH, max = MAX_LENGTH, message = "Must be less than or equal to {max} characters length")
    private String description;

    @JsonProperty(USER_IDENTIFIER_FIELD)
    @Size(min= MIN_LENGTH, max = MAX_LENGTH, message = "Must be less than or equal to {max} characters length")
    private String userIdentifier;

    public CreateAgreementRequest() {
        // for Jackson
    }
    
    public CreateAgreementRequest(CreateAgreementRequestBuilder builder) {
        this.reference = builder.getReference();
        this.description = builder.getDescription();
        this.userIdentifier = builder.getUserIdentifier();
    }

    @Schema(description = "Associate a reference with this agreement to help you identify it. Limited to 255 characters.",
    example = "CT-22-23-0001")
    public String getReference() {
        return reference;
    }

    @Schema(description = "A human-readable description of the purpose of the agreement for recurring payments. " +
            "We’ll show the description to your user when they make their first payment to activate this agreement. " +
            "Limited to 255 characters.",
            example = "Dorset Council 2022/23 council tax subscription.")
    public String getDescription() {
        return description;
    }

   @Schema(description = "Associate an identifier with the user who will enter into this agreement with your service." +
           "user_identifier is not unique – multiple agreements can have identical user_identifier values." +
           "You should not include personal data in user_identifier.",
            example = "user-3fb81107-76b7-4910")
    public String getUserIdentifier() {
        return userIdentifier;
    }

    public String toConnectorPayload() {
        var stringBuilder = new JsonStringBuilder()
                .add("reference", this.getReference())
                .add("description", this.getDescription());

        if (this.getUserIdentifier() != null) {
            stringBuilder.add("user_identifier", this.getUserIdentifier());
        }
        return stringBuilder.build();
    }

    @Override
    public boolean equals(Object o) {   
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreateAgreementRequest that = (CreateAgreementRequest) o;
        return Objects.equals(reference, that.reference) && Objects.equals(description, that.description) && Objects.equals(userIdentifier, that.userIdentifier);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(reference, description, userIdentifier);
    }
}
