package uk.gov.pay.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.api.model.links.PaymentLinksForEvents;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

@Schema(name = "PaymentEvents", description = "A List of Payment Events information")
public class PaymentEventsResponse {
    @JsonProperty("payment_id")
    private final String paymentId;

    private final List<PaymentEventResponse> events;

    @JsonProperty("_links")
    private PaymentLinksForEvents links;

    private PaymentEventsResponse(String paymentId, List<PaymentEventResponse> events, PaymentLinksForEvents links) {
        this.paymentId = paymentId;
        this.events = events;
        this.links = links;
    }

    public static PaymentEventsResponse from(PaymentEvents paymentEvents, URI paymentEventsLink, URI eventsLink) {
        List<PaymentEventResponse> events = paymentEvents.getEvents().stream()
                .map(paymentEvent -> PaymentEventResponse.from(paymentEvent, paymentEvents.getChargeId(), paymentEventsLink.toString()))
                .collect(Collectors.toList());
        PaymentLinksForEvents paymentLinksForEvents = new PaymentLinksForEvents();
        paymentLinksForEvents.addSelf(eventsLink.toString());
        return new PaymentEventsResponse(paymentEvents.getChargeId(), events, paymentLinksForEvents);
    }

    public static PaymentEventsResponse from(TransactionEvents transactionEvents, URI paymentEventsLink, URI eventsLink) {
        List<PaymentEventResponse> events = transactionEvents.getEvents().stream()
                .map(paymentEvent -> PaymentEventResponse.from(paymentEvent, transactionEvents.getTransactionId(), paymentEventsLink.toString()))
                .collect(Collectors.toList());
        PaymentLinksForEvents paymentLinksForEvents = new PaymentLinksForEvents();
        paymentLinksForEvents.addSelf(eventsLink.toString());
        return new PaymentEventsResponse(transactionEvents.getTransactionId(), events, paymentLinksForEvents);
    }

    @Schema(example = "hu20sqlact5260q2nanm0q8u93", 
            description = "The unique ID GOV.UK Pay automatically associated with this payment when you created it.",
            accessMode = READ_ONLY)
    public String getPaymentId() {
        return paymentId;
    }

    public List<PaymentEventResponse> getEvents() {
        return events;
    }

    public PaymentLinksForEvents getLinks() {
        return links;
    }

    @Override
    public String toString() {
        return "PaymentEvents{" +
                "paymentId='" + paymentId + '\'' +
                ", events=" + events +
                ", links=" + links +
                '}';
    }
}
