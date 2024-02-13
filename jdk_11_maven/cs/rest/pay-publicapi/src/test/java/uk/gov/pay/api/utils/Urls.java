package uk.gov.pay.api.utils;

public class Urls {
    
    public static String paymentLocationFor(String publicApiBaseUrl, String chargeId) {
        return publicApiBaseUrl + "v1/payments/" + chargeId;
    }
}
