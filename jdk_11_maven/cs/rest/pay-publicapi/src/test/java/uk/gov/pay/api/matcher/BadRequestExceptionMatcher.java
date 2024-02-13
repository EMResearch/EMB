package uk.gov.pay.api.matcher;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import uk.gov.pay.api.exception.BadRequestException;
import uk.gov.pay.api.model.RequestError;

public class BadRequestExceptionMatcher extends TypeSafeMatcher<BadRequestException> {

    private final String code;
    private final String description;

    private BadRequestExceptionMatcher(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static BadRequestExceptionMatcher aBadRequestExceptionWithError(String code, String description) {
        return new BadRequestExceptionMatcher(code, description);
    }

    @Override
    protected boolean matchesSafely(BadRequestException e) {
        RequestError requestError = e.getRequestError();
        return code.equals(requestError.getCode()) &&
                description.equals(requestError.getDescription());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(BadRequestException.class.getCanonicalName())
                .appendText(" with ")
                .appendText(" RequestError. { code = ")
                .appendValue(code)
                .appendText(", description = ")
                .appendValue(this.description)
                .appendText(" }");
    }
}
