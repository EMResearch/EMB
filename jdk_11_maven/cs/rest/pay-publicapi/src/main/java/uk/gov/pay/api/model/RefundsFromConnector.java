package uk.gov.pay.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundsFromConnector {

    public class Embedded {
        private List<RefundFromConnector> refunds;

        public Embedded() {
        }

        public List<RefundFromConnector> getRefunds() {
            return refunds;
        }

        @Override
        public String toString() {
            return "Embedded{" +
                    "refunds=" + refunds +
                    '}';
        }
    }

    @JsonProperty(value = "payment_id")
    private String paymentId;

    @JsonProperty(value = "_embedded")
    private Embedded embedded;

    public RefundsFromConnector() {
    }

    public String getPaymentId() {
        return paymentId;
    }

    public Embedded getEmbedded() {
        return embedded;
    }

    @Override
    public String toString() {
        return "RefundsFromConnector{" +
                "paymentId='" + paymentId + '\'' +
                ", " + embedded +
                '}';
    }
}
