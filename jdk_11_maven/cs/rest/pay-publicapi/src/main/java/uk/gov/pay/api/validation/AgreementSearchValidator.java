package uk.gov.pay.api.validation;

import uk.gov.pay.api.exception.AgreementValidationException;
import uk.gov.pay.api.exception.BadRequestException;
import uk.gov.pay.api.ledger.model.AgreementSearchParams;
import uk.gov.pay.api.model.RequestError;
import uk.gov.service.payments.commons.model.agreement.AgreementStatus;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static java.util.Locale.ENGLISH;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.join;
import static org.eclipse.jetty.util.StringUtil.isBlank;
import static uk.gov.pay.api.common.SearchConstants.DISPLAY_SIZE;
import static uk.gov.pay.api.common.SearchConstants.PAGE;
import static uk.gov.pay.api.common.SearchConstants.REFERENCE_KEY;
import static uk.gov.pay.api.common.SearchConstants.STATUS_KEY;
import static uk.gov.pay.api.model.CreateCardPaymentRequest.REFERENCE_MAX_LENGTH;
import static uk.gov.pay.api.model.RequestError.Code.SEARCH_AGREEMENTS_VALIDATION_ERROR;
import static uk.gov.pay.api.model.RequestError.aRequestError;
import static uk.gov.pay.api.validation.MaxLengthValidator.isInvalid;
import static uk.gov.pay.api.validation.SearchValidator.validateDisplaySizeIfNotNull;
import static uk.gov.pay.api.validation.SearchValidator.validatePageIfNotNull;

public class AgreementSearchValidator {

    private static final Set<String> SUPPORTED_SEARCH_PARAMS = Set.of(REFERENCE_KEY, STATUS_KEY, PAGE, DISPLAY_SIZE);

    public static void validateSearchParameters(AgreementSearchParams searchParams) {
        List<String> validationErrors = new LinkedList<>();
        try {
            validateStatus(searchParams.getStatus(), validationErrors);
            validateReference(searchParams.getReference(), validationErrors);
            validatePageIfNotNull(searchParams.getPageNumber(), validationErrors);
            validateDisplaySizeIfNotNull(searchParams.getDisplaySize(), validationErrors);
        } catch (Exception e) {
            throw new AgreementValidationException(aRequestError(SEARCH_AGREEMENTS_VALIDATION_ERROR, join(validationErrors, ", "), e.getMessage()));
        }
        if (!validationErrors.isEmpty()) {
            throw new AgreementValidationException(aRequestError(SEARCH_AGREEMENTS_VALIDATION_ERROR, join(validationErrors, ", ")));
        }

        searchParams.getQueryMap().entrySet().stream()
                .filter(queryParam -> !SUPPORTED_SEARCH_PARAMS.contains(queryParam.getKey()) && isNotBlank(queryParam.getValue()))
                .findFirst()
                .ifPresent(invalidParam -> {
                    throw new BadRequestException(RequestError.aRequestError(SEARCH_AGREEMENTS_VALIDATION_ERROR, invalidParam.getKey()));
                });
    }

    private static void validateReference(String reference, List<String> validationErrors) {
        if (isInvalid(reference, REFERENCE_MAX_LENGTH)) {
            validationErrors.add("reference");
        }
    }

    private static void validateStatus(String status, List<String> validationErrors) {
        if (!(isBlank(status) || Arrays.stream(AgreementStatus.values()).anyMatch(validStatus -> validStatus.name().toLowerCase(ENGLISH).equals(status)))) {
            validationErrors.add("status");
        }
    }

}
