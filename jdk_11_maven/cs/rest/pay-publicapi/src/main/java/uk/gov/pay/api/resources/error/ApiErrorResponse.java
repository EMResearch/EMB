package uk.gov.pay.api.resources.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.lang.String.format;

@Schema(name = "ErrorResponse", description = "An error response")
@JsonInclude(NON_NULL)
public class ApiErrorResponse {

    public enum Code {

        TOO_MANY_REQUESTS_ERROR("P0900", "Too many requests");

        private String value;
        private String format;

        Code(String value, String format) {
            this.value = value;
            this.format = format;
        }

        public String value() {
            return value;
        }

        public String getFormat() {
            return format;
        }
    }

    private final Code code;
    private final String description;

    public static ApiErrorResponse anApiErrorResponse(Code code, Object... parameters) {
        return new ApiErrorResponse(code, parameters);
    }

    private ApiErrorResponse(Code code, Object... parameters) {
        this.code = code;
        this.description = format(code.getFormat(), parameters);
    }

    @Schema(example = "P0900", description = "A GOV.UK Pay API error code. " +
            "You can [find out more about this code in our documentation]" +
            "(https://docs.payments.service.gov.uk/api_reference/#gov-uk-pay-api-error-codes).")
    public String getCode() {
        return code.value();
    }

    @Schema(example = "Too many requests", description = "Additional details about the error")
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "ApiErrorResponse{" +
                "code=" + code.value() +
                ", name=" + code +
                ", description='" + description + '\'' +
                '}';
    }
}
