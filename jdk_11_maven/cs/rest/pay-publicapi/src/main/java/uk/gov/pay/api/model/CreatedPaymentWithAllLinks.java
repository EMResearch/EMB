package uk.gov.pay.api.model;

import uk.gov.pay.api.model.links.PaymentWithAllLinks;

public class CreatedPaymentWithAllLinks {

    public enum WhenCreated {
        BRAND_NEW,
        EXISTING
    }

    private final PaymentWithAllLinks payment;
    private final WhenCreated whenCreated;

    private CreatedPaymentWithAllLinks(PaymentWithAllLinks payment, WhenCreated whenCreated) {
        this.payment = payment;
        this.whenCreated = whenCreated;
    }

    public static CreatedPaymentWithAllLinks of(PaymentWithAllLinks payment, WhenCreated whenCreated) {
        return new CreatedPaymentWithAllLinks(payment, whenCreated);
    }

    public PaymentWithAllLinks getPayment() {
        return payment;
    }

    public WhenCreated getWhenCreated() {
        return whenCreated;
    }
}
