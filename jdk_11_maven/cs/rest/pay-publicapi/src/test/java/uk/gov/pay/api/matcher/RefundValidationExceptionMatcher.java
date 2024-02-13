package uk.gov.pay.api.matcher;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import uk.gov.pay.api.exception.RefundsValidationException;
import uk.gov.pay.api.model.RequestError;

public class RefundValidationExceptionMatcher extends TypeSafeMatcher<RefundsValidationException> {

    private final String code;
    private final String description;

    private RefundValidationExceptionMatcher(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static RefundValidationExceptionMatcher aValidationExceptionContaining(String code, String description) {
        return new RefundValidationExceptionMatcher(code, description);
    }

    @Override
    protected boolean matchesSafely(RefundsValidationException e) {
        RequestError requestError = e.getRequestError();
        return code.equals(requestError.getCode()) &&
                description.equals(requestError.getDescription());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(RefundsValidationException.class.getCanonicalName())
                .appendText(" with ")
                .appendText(" RequestError. { code = ")
                .appendValue(code)
                .appendText(", description = ")
                .appendValue(this.description)
                .appendText(" }");
    }
}
