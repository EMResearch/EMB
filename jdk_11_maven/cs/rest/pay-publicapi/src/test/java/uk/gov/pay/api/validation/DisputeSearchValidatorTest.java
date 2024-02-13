package uk.gov.pay.api.validation;

import org.junit.jupiter.api.Test;
import uk.gov.pay.api.exception.DisputesValidationException;
import uk.gov.pay.api.service.DisputesSearchParams;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.pay.api.validation.DisputeSearchValidator.validateDisputeParameters;

class DisputeSearchValidatorTest {
    private static String VALIDATION_EXCEPTION_MESSAGE = "Invalid parameters: %s. See Public API documentation for the correct data formats";
    @Test
    void validateSearchParameters_shouldSuccessValidation() {
        DisputesSearchParams params = new DisputesSearchParams.Builder()
                .withFromDate("2022-07-22T11:23:55Z")
                .withToDate("2022-07-22T12:23:55Z")
                .withDisplaySize("1")
                .withFromSettledDate("2022-07-20")
                .withToSettledDate("2022-07-21")
                .withPage("1")
                .withStatus("won")
                .build();

        validateDisputeParameters(params);
    }

    @Test
    void validateParams_shouldNotGiveAnErrorValidation_ForMissingPageDisplaySize() {
        DisputesSearchParams params = new DisputesSearchParams.Builder()
                .withFromDate("2022-07-22T11:23:55Z")
                .withToDate("2022-07-22T12:23:55Z")
                .withDisplaySize(null)
                .withFromSettledDate("2022-07-20")
                .withToSettledDate("2022-07-21")
                .withPage(null)
                .withStatus("lost")
                .build();
        validateDisputeParameters(params);
    }

    @Test
    void validateSearchParameters_shouldGiveAValidationError_ForNonValidToDate() {
        DisputesSearchParams params = new DisputesSearchParams.Builder()
                .withToDate("alpha")
                .build();
        DisputesValidationException validationException = assertThrows(DisputesValidationException.class,
                () -> validateDisputeParameters(params));
        assertThat(validationException.getRequestError().getCode(), is("P0401"));
        assertThat(validationException.getRequestError().getDescription(), is(format(VALIDATION_EXCEPTION_MESSAGE, "to_date")));
    }

    @Test
    void validateSearchParameters_shouldGiveAValidationError_ForNonValidFromDate() {
        DisputesSearchParams params = new DisputesSearchParams.Builder()
                .withFromDate("bravo")
                .build();
        DisputesValidationException validationException = assertThrows(DisputesValidationException.class,
                () -> validateDisputeParameters(params));
        assertThat(validationException.getRequestError().getCode(), is("P0401"));
        assertThat(validationException.getRequestError().getDescription(), is(format(VALIDATION_EXCEPTION_MESSAGE, "from_date")));
    }

    @Test
    void validateSearchParameters_shouldGiveAValidationError_ForNonNumericPageAndSize() {
        DisputesSearchParams params = new DisputesSearchParams.Builder()
                .withDisplaySize("charlie")
                .withPage("delta")
                .build();
        DisputesValidationException validationException = assertThrows(DisputesValidationException.class,
                () -> validateDisputeParameters(params));
        assertThat(validationException.getRequestError().getCode(), is("P0401"));
        assertThat(validationException.getRequestError().getDescription(), is(format(VALIDATION_EXCEPTION_MESSAGE, "page, display_size")));
    }

    @Test
    void validateParams_shouldGiveAnErrorValidation_forZeroPageDisplay() {
        DisputesSearchParams params = new DisputesSearchParams.Builder()
                .withDisplaySize("0")
                .withPage("0")
                .build();
        DisputesValidationException validationException = assertThrows(DisputesValidationException.class,
                () -> validateDisputeParameters(params));
        assertThat(validationException.getRequestError().getCode(), is("P0401"));
        assertThat(validationException.getRequestError().getDescription(), is(format(VALIDATION_EXCEPTION_MESSAGE, "page, display_size")));
    }

    @Test
    void validateParams_shouldGiveAnErrorValidation_forMaxedOutValuesPageDisplaySize() {
        DisputesSearchParams params = new DisputesSearchParams.Builder()
                .withDisplaySize(String.valueOf(Integer.MAX_VALUE + 1))
                .withPage(String.valueOf(Integer.MAX_VALUE + 1))
                .build();
        DisputesValidationException validationException = assertThrows(DisputesValidationException.class,
                () -> validateDisputeParameters(params));
        assertThat(validationException.getRequestError().getCode(), is("P0401"));
        assertThat(validationException.getRequestError().getDescription(), is(format(VALIDATION_EXCEPTION_MESSAGE, "page, display_size")));
    }

    @Test
    void validateParams_shouldGiveAnErrorValidation_forTooLargeDisplaySize() {
        DisputesSearchParams params = new DisputesSearchParams.Builder()
                .withDisplaySize("501")
                .build();
        DisputesValidationException validationException = assertThrows(DisputesValidationException.class,
                () -> validateDisputeParameters(params));
        assertThat(validationException.getRequestError().getCode(), is("P0401"));
        assertThat(validationException.getRequestError().getDescription(), is(format(VALIDATION_EXCEPTION_MESSAGE, "display_size")));
    }

    @Test
    void validateSearchParameters_shouldGiveAValidationError_ForNonValidToSettledDate() {
        DisputesSearchParams params = new DisputesSearchParams.Builder()
                .withToSettledDate("January 7")
                .build();
        DisputesValidationException validationException = assertThrows(DisputesValidationException.class,
                () -> validateDisputeParameters(params));
        assertThat(validationException.getRequestError().getCode(), is("P0401"));
        assertThat(validationException.getRequestError().getDescription(), is(format(VALIDATION_EXCEPTION_MESSAGE, "to_settled_date")));
    }

    @Test
    void validateSearchParameters_shouldGiveAValidationError_ForNonValidFromSettledDate() {
        DisputesSearchParams params = new DisputesSearchParams.Builder()
                .withFromSettledDate("19th September")
                .build();
        DisputesValidationException validationException = assertThrows(DisputesValidationException.class,
                () -> validateDisputeParameters(params));
        assertThat(validationException.getRequestError().getCode(), is("P0401"));
        assertThat(validationException.getRequestError().getDescription(), is(format(VALIDATION_EXCEPTION_MESSAGE, "from_settled_date")));
    }

    @Test
    void validateSearchParameters_shouldGiveAValidationError_ForNonExistentStatus() {
        DisputesSearchParams params = new DisputesSearchParams.Builder()
                .withStatus("blew it")
                .build();
        DisputesValidationException validationException = assertThrows(DisputesValidationException.class,
                () -> validateDisputeParameters(params));
        assertThat(validationException.getRequestError().getCode(), is("P0401"));
        assertThat(validationException.getRequestError().getDescription(), is(format(VALIDATION_EXCEPTION_MESSAGE, "state")));
    }
}