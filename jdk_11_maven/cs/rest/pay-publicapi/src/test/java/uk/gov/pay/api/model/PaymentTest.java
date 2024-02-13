package uk.gov.pay.api.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.gov.pay.api.model.links.PaymentWithAllLinks;

import java.io.IOException;
import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class PaymentTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void shouldNotPrintEmailAndCardDetailsWhenToString() throws IOException {

        URI selfUri = URI.create("http://self.link.com");
        URI eventsUri = URI.create("http://self.link.com/events");
        URI cancelUri = URI.create("http://self.link.com/cancel");
        URI refundsUri = URI.create("http://self.link.com/cancel");
        URI captureUri = URI.create("self.link.com/capture");
        URI authUri = URI.create("self.link.com/auth");

        // language=JSON
        ChargeFromResponse paymentFromConnector = objectMapper.readValue("{\n" +
                "  \"email\": \"user@example.com\",\n" +
                "  \"card_details\": {\n" +
                "    \"card_brand\": \"Visa\",\n" +
                "    \"expiry_date\": \"12/19\",\n" +
                "    \"cardholder_name\": \"Mr. payment\",\n" +
                "    \"billing_address\": {\n" +
                "      \"line1\": \"line1\",\n" +
                "      \"postcode\": \"NR25 6EG\",\n" +
                "      \"country\": \"UK\"\n" +
                "    },\n" +
                "    \"last_digits_card_number\": \"4321\",\n" +
                "    \"first_digits_card_number\": \"654321\"\n" +
                "  },\n" +
                "  \"amount\": 500,\n" +
                "  \"language\": \"en\",\n" +
                "  \"state\": {\n" +
                "    \"status\": \"created\",\n" +
                "    \"finished\": false\n" +
                "  }\n" +
                "}", ChargeFromResponse.class);

        PaymentWithAllLinks payment = PaymentWithAllLinks.valueOf(Charge.from(paymentFromConnector), selfUri, eventsUri,
                cancelUri, refundsUri, captureUri, authUri);

        assertThat(payment.toString(), not(containsString("user@example.com")));
        assertThat(payment.toString(), not(containsString("last_digits_card_number")));
        assertThat(payment.toString(), not(containsString("first_digits_card_number")));
        assertThat(payment.toString(), not(containsString("654321")));
        assertThat(payment.toString(), not(containsString("4321")));
        assertThat(payment.toString(), not(containsString("12/19")));
        assertThat(payment.toString(), not(containsString("Mr. payment")));
        assertThat(payment.toString(), not(containsString("NR25 6EG")));
        assertThat(payment.toString(), not(containsString("UK")));
    }
}
