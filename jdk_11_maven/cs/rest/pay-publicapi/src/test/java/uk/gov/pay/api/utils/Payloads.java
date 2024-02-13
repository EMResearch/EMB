package uk.gov.pay.api.utils;

import uk.gov.pay.api.utils.mocks.CreateAgreementRequestParams;

public class Payloads {

    public static String aSuccessfulPaymentPayload() {
        int amount = 100;
        String returnUrl = "https://somewhere.gov.uk/rainbow/1";
        String reference = "a reference";
        String description = "a description";
        return aSuccessfulPaymentPayload(amount, returnUrl, description, reference);
    }

    public static String aSuccessfulPaymentPayload(int amount, String returnUrl, String description, String reference) {
        return new JsonStringBuilder()
                .add("amount", amount)
                .add("reference", reference)
                .add("description", description)
                .add("return_url", returnUrl)
                .build();
    }

    public static String agreementPayload(CreateAgreementRequestParams params) {
        var stringBuilder = new JsonStringBuilder()
                .add("reference", params.getReference())
                .add("description", params.getDescription());
        if (params.getUserIdentifier() != null) {
            stringBuilder.add("user_identifier", params.getUserIdentifier());
        }
        return stringBuilder.build();
    }
}
