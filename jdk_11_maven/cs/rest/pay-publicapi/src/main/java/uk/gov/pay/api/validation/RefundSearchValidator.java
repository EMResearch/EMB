package uk.gov.pay.api.validation;

import uk.gov.pay.api.exception.RefundsValidationException;
import uk.gov.pay.api.service.RefundsParams;

import java.util.LinkedList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.join;
import static uk.gov.pay.api.model.RequestError.Code.SEARCH_REFUNDS_VALIDATION_ERROR;
import static uk.gov.pay.api.model.RequestError.aRequestError;
import static uk.gov.pay.api.validation.SearchValidator.validateDisplaySizeIfNotNull;
import static uk.gov.pay.api.validation.SearchValidator.validateFromDate;
import static uk.gov.pay.api.validation.SearchValidator.validateFromSettledDate;
import static uk.gov.pay.api.validation.SearchValidator.validatePageIfNotNull;
import static uk.gov.pay.api.validation.SearchValidator.validateToDate;
import static uk.gov.pay.api.validation.SearchValidator.validateToSettledDate;


public class RefundSearchValidator {

    public static void validateSearchParameters(RefundsParams params) {
        String pageNumber = params.getPage();
        String displaySize = params.getDisplaySize();
        String fromSettledDate = params.getFromSettledDate();
        String toSettledDate = params.getToSettledDate();
        String fromDate = params.getFromDate();
        String toDate = params.getToDate();
        
        List<String> validationErrors = new LinkedList<>();
        try {
            validateFromDate(fromDate, validationErrors);
            validateToDate(toDate, validationErrors);
            validateFromSettledDate(fromSettledDate, validationErrors);
            validateToSettledDate(toSettledDate, validationErrors);
            validatePageIfNotNull(pageNumber, validationErrors);
            validateDisplaySizeIfNotNull(displaySize, validationErrors);
        } catch (Exception e) {
            throw new RefundsValidationException(aRequestError(SEARCH_REFUNDS_VALIDATION_ERROR, join(validationErrors, ", "), e.getMessage()));
        }
        if (!validationErrors.isEmpty()) {
            throw new RefundsValidationException(aRequestError(SEARCH_REFUNDS_VALIDATION_ERROR, join(validationErrors, ", ")));
        }
    }


}
