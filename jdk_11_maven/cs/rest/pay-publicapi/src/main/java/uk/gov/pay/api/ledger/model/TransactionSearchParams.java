package uk.gov.pay.api.ledger.model;

import javax.ws.rs.QueryParam;
import java.util.HashMap;
import java.util.Map;

import static uk.gov.pay.api.common.SearchConstants.CARDHOLDER_NAME_KEY;
import static uk.gov.pay.api.common.SearchConstants.CARD_BRAND_KEY;
import static uk.gov.pay.api.common.SearchConstants.DISPLAY_SIZE;
import static uk.gov.pay.api.common.SearchConstants.EMAIL_KEY;
import static uk.gov.pay.api.common.SearchConstants.FIRST_DIGITS_CARD_NUMBER_KEY;
import static uk.gov.pay.api.common.SearchConstants.FROM_DATE_KEY;
import static uk.gov.pay.api.common.SearchConstants.FROM_SETTLED_DATE;
import static uk.gov.pay.api.common.SearchConstants.GATEWAY_ACCOUNT_ID;
import static uk.gov.pay.api.common.SearchConstants.LAST_DIGITS_CARD_NUMBER_KEY;
import static uk.gov.pay.api.common.SearchConstants.PAGE;
import static uk.gov.pay.api.common.SearchConstants.REFERENCE_KEY;
import static uk.gov.pay.api.common.SearchConstants.STATE_KEY;
import static uk.gov.pay.api.common.SearchConstants.TO_DATE_KEY;
import static uk.gov.pay.api.common.SearchConstants.TO_SETTLED_DATE;

public class TransactionSearchParams {

    private String accountId;
    @QueryParam("reference")
    private String reference;
    @QueryParam("email")
    private String email;
    @QueryParam("state")
    private String state;
    @QueryParam("card_brand")
    private String cardBrand;
    @QueryParam("from_date")
    private String fromDate;
    @QueryParam("to_date")
    private String toDate;
    @QueryParam("page")
    private String pageNumber;
    @QueryParam("display_size")
    private String displaySize;
    @QueryParam("cardholder_name")
    private String cardHolderName;
    @QueryParam("first_digits_card_number")
    private String firstDigitsCardNumber;
    @QueryParam("last_digits_card_number")
    private String lastDigitsCardNumber;
    @QueryParam("from_settled_date")
    private String fromSettledDate;
    @QueryParam("to_settled_date")
    private String toSettledDate;

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getReference() {
        return reference;
    }

    public String getEmail() {
        return email;
    }

    public String getState() {
        return state;
    }

    public String getCardBrand() {
        return cardBrand;
    }

    public String getFromDate() {
        return fromDate;
    }

    public String getToDate() {
        return toDate;
    }

    public String getPageNumber() {
        return pageNumber;
    }

    public String getDisplaySize() {
        return displaySize;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public String getFirstDigitsCardNumber() {
        return firstDigitsCardNumber;
    }

    public String getLastDigitsCardNumber() {
        return lastDigitsCardNumber;
    }

    public String getFromSettledDate() {
        return fromSettledDate;
    }

    public String getToSettledDate() {
        return toSettledDate;
    }

    public Map<String, String> getQueryMap() {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put(GATEWAY_ACCOUNT_ID, accountId);
        queryParams.put(REFERENCE_KEY, reference);
        queryParams.put(EMAIL_KEY, email);
        queryParams.put(STATE_KEY, state);
        queryParams.put(CARD_BRAND_KEY, cardBrand);
        queryParams.put(CARDHOLDER_NAME_KEY, cardHolderName);
        queryParams.put(FIRST_DIGITS_CARD_NUMBER_KEY, firstDigitsCardNumber);
        queryParams.put(LAST_DIGITS_CARD_NUMBER_KEY, lastDigitsCardNumber);
        queryParams.put(FROM_DATE_KEY, fromDate);
        queryParams.put(TO_DATE_KEY, toDate);
        queryParams.put(PAGE, pageNumber);
        queryParams.put(DISPLAY_SIZE, displaySize);
        queryParams.put(FROM_SETTLED_DATE, fromSettledDate);
        queryParams.put(TO_SETTLED_DATE, toSettledDate);

        return queryParams;
    }
}
