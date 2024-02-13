package uk.gov.pay.api.model.search.card;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.api.model.links.RefundLinksForSearch;

@Schema(name = "Refund")
public class RefundResult {

    @JsonUnwrapped
    private RefundForSearchRefundsResult refunds;
    @Schema(name = "_links")
    private RefundLinksForSearch links;

    public RefundForSearchRefundsResult getRefunds() {
        return refunds;
    }

    public RefundLinksForSearch getLinks() {
        return links;
    }
}
