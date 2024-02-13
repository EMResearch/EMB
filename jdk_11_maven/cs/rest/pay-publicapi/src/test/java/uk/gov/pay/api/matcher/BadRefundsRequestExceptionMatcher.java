package uk.gov.pay.api.matcher;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import uk.gov.pay.api.exception.BadRefundsRequestException;
import uk.gov.pay.api.model.RequestError;

public class BadRefundsRequestExceptionMatcher extends TypeSafeMatcher<BadRefundsRequestException> {

    private final String code;
    private final String description;

    private BadRefundsRequestExceptionMatcher(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static BadRefundsRequestExceptionMatcher aBadRefundsRequestExceptionWithError(String code, String description) {
        return new BadRefundsRequestExceptionMatcher(code, description);
    }

    @Override
    protected boolean matchesSafely(BadRefundsRequestException e) {
        RequestError requestError = e.getRequestError();
        return code.equals(requestError.getCode()) &&
                description.equals(requestError.getDescription());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(BadRefundsRequestException.class.getCanonicalName())
                .appendText(" with ")
                .appendText(" RefundError. { code = ")
                .appendValue(code)
                .appendText(", description = ")
                .appendValue(this.description)
                .appendText(" }");
    }
}
