package uk.gov.pay.api.ledger.model;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.ws.rs.QueryParam;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static uk.gov.pay.api.common.SearchConstants.DISPLAY_SIZE;
import static uk.gov.pay.api.common.SearchConstants.PAGE;
import static uk.gov.pay.api.common.SearchConstants.REFERENCE_KEY;
import static uk.gov.pay.api.common.SearchConstants.STATUS_KEY;

public class AgreementSearchParams {
    @QueryParam("reference")
    @Parameter(name = "reference", description = "Returns agreements with a `reference` that exactly matches the value you sent. " +
            "This parameter is not case sensitive. " + 
            "A `reference` was associated with the agreement when that agreement was created.",
            example = "CT-22-23-0001")
    private String reference;

    @QueryParam("status")
    @Parameter(name = "status", description = "Returns agreements in a matching `status`. " +
            "`status` reflects where an agreement is in its lifecycle. " +
            "You can [read more about the meanings of the different agreement status values]" +
            "(https://docs.payments.service.gov.uk/recurring_payments/#understanding-agreement-status).",
            schema = @Schema(allowableValues = {"created", "active", "cancelled", "inactive"}))
    private String status;

    @QueryParam("page")
    @Parameter(name = "page", 
            description = "Returns a specific page of results. Defaults to `1`. " +
                    "You can [read about search pagination](https://docs.payments.service.gov.uk/api_reference/#pagination)",
            example = "1")
    private String pageNumber;

    @QueryParam("display_size")
    @Parameter(name = "display_size",
            description = "The number of agreements returned per results page. Defaults to `500`. " +
                    "Maximum value is `500`. You can [read about search pagination](https://docs.payments.service.gov.uk/api_reference/#pagination)",
            example = "50")
    private String displaySize;

    public AgreementSearchParams() {
        // Framework gubbins
    }
    
    public AgreementSearchParams(String reference, String status, String pageNumber, String displaySize) {
        this.reference = reference;
        this.status = status;
        this.pageNumber = pageNumber;
        this.displaySize = displaySize;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(String pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getDisplaySize() {
        return displaySize;
    }

    public void setDisplaySize(String displaySize) {
        this.displaySize = displaySize;
    }

    public Map<String, String> getQueryMap() {
        var queryParams = new HashMap<String, String>();
        Optional.ofNullable(reference).ifPresent(reference -> queryParams.put(REFERENCE_KEY, reference));
        Optional.ofNullable(status).ifPresent(status -> queryParams.put(STATUS_KEY, status));
        Optional.ofNullable(pageNumber).ifPresent(pageNumber -> queryParams.put(PAGE, pageNumber));
        Optional.ofNullable(displaySize).ifPresent(displaySize -> queryParams.put(DISPLAY_SIZE, displaySize));
        return Map.copyOf(queryParams);
    }
}
