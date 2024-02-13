package uk.gov.pay.api.matcher;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import uk.gov.pay.api.exception.PaymentValidationException;
import uk.gov.pay.api.model.RequestError;

public class PaymentValidationExceptionMatcher extends TypeSafeMatcher<PaymentValidationException> {

    private final String code;
    private final String description;

    private PaymentValidationExceptionMatcher(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static PaymentValidationExceptionMatcher aValidationExceptionContaining(String code, String description) {
        return new PaymentValidationExceptionMatcher(code, description);
    }

    @Override
    protected boolean matchesSafely(PaymentValidationException e) {
        RequestError requestError = e.getRequestError();
        return code.equals(requestError.getCode()) &&
                description.equals(requestError.getDescription());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(PaymentValidationException.class.getCanonicalName())
                .appendText(" with ")
                .appendText(" RequestError. { code = ")
                .appendValue(code)
                .appendText(", description = ")
                .appendValue(this.description)
                .appendText(" }");
    }
}
