package uk.gov.pay.api.model.search.card;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.api.model.links.RefundLinksForSearch;

import java.util.List;

public class RefundForSearchResult {

    @Schema(name = "payment_id", example = "hu20sqlact5260q2nanm0q8u93", description = "The unique ID GOV.UK Pay associated with this payment when you created it.")
    private String paymentId;
    @Schema(name = "_links")
    private RefundLinksForSearch links;
    @JsonProperty(value = "_embedded")
    @Schema(name = "_embedded")
    private Embedded embedded;

    @Schema(name = "EmbeddedRefunds")
    public class Embedded {
        private List<RefundResult> refunds;

        public List<RefundResult> getRefunds() {
            return refunds;
        }
    }

    public String getPaymentId() {
        return paymentId;
    }

    public RefundLinksForSearch getLinks() {
        return links;
    }

    public Embedded getEmbedded() {
        return embedded;
    }
}
