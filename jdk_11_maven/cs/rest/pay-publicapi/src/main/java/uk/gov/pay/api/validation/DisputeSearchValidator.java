package uk.gov.pay.api.validation;

import com.google.common.collect.ImmutableList;
import uk.gov.pay.api.exception.DisputesValidationException;
import uk.gov.pay.api.service.DisputesSearchParams;

import java.util.LinkedList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.join;
import static org.eclipse.jetty.util.StringUtil.isBlank;
import static uk.gov.pay.api.model.RequestError.Code.SEARCH_DISPUTES_VALIDATION_ERROR;
import static uk.gov.pay.api.model.RequestError.aRequestError;
import static uk.gov.pay.api.validation.SearchValidator.validateDisplaySizeIfNotNull;
import static uk.gov.pay.api.validation.SearchValidator.validateFromDate;
import static uk.gov.pay.api.validation.SearchValidator.validateFromSettledDate;
import static uk.gov.pay.api.validation.SearchValidator.validatePageIfNotNull;
import static uk.gov.pay.api.validation.SearchValidator.validateToDate;
import static uk.gov.pay.api.validation.SearchValidator.validateToSettledDate;


public class DisputeSearchValidator {
    private static final List<String> VALID_DISPUTE_STATES = ImmutableList.of("needs_response", "under_review",
            "lost", "won");

    public static void validateDisputeParameters(DisputesSearchParams params) {
        String pageNumber = params.getPage();
        String displaySize = params.getDisplaySize();
        String fromSettledDate = params.getFromSettledDate();
        String toSettledDate = params.getToSettledDate();
        String fromDate = params.getFromDate();
        String toDate = params.getToDate();
        String state = params.getState();
        
        List<String> validationErrors = new LinkedList<>();
        try {
            validateFromDate(fromDate, validationErrors);
            validateToDate(toDate, validationErrors);
            validateFromSettledDate(fromSettledDate, validationErrors);
            validateToSettledDate(toSettledDate, validationErrors);
            validatePageIfNotNull(pageNumber, validationErrors);
            validateDisplaySizeIfNotNull(displaySize, validationErrors);
            validateState(state, validationErrors);
        } catch (Exception e) {
            throw new DisputesValidationException(aRequestError(SEARCH_DISPUTES_VALIDATION_ERROR, join(validationErrors, ", "), e.getMessage()));
        }
        if (!validationErrors.isEmpty()) {
            throw new DisputesValidationException(aRequestError(SEARCH_DISPUTES_VALIDATION_ERROR, join(validationErrors, ", ")));
        }
    }

    private static void validateState(String state, List<String> validationErrors) {
        if (!validateState(state)) {
            validationErrors.add("state");
        }
    }

    private static boolean validateState(String state) {
        return isBlank(state) || VALID_DISPUTE_STATES.contains(state);
    }
}
