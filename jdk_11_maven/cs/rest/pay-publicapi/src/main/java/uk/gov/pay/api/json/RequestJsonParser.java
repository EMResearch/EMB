package uk.gov.pay.api.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import uk.gov.pay.api.agreement.model.CreateAgreementRequest;
import uk.gov.pay.api.exception.BadRequestException;
import uk.gov.pay.api.exception.PaymentValidationException;
import uk.gov.pay.api.model.CreateAgreementRequestBuilder;
import uk.gov.pay.api.model.CreateCardPaymentRequest;
import uk.gov.pay.api.model.CreateCardPaymentRequestBuilder;
import uk.gov.pay.api.model.CreatePaymentRefundRequest;
import uk.gov.pay.api.model.RequestError;
import uk.gov.pay.api.model.RequestError.Code;
import uk.gov.service.payments.commons.model.AuthorisationMode;
import uk.gov.service.payments.commons.model.Source;
import uk.gov.service.payments.commons.model.SupportedLanguage;
import uk.gov.service.payments.commons.model.charge.ExternalMetadata;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static uk.gov.pay.api.agreement.model.CreateAgreementRequest.USER_IDENTIFIER_FIELD;
import static uk.gov.pay.api.model.CreateCardPaymentRequest.AGREEMENT_ID_FIELD_NAME;
import static uk.gov.pay.api.model.CreateCardPaymentRequest.AMOUNT_FIELD_NAME;
import static uk.gov.pay.api.model.CreateCardPaymentRequest.AUTHORISATION_MODE;
import static uk.gov.pay.api.model.CreateCardPaymentRequest.DELAYED_CAPTURE_FIELD_NAME;
import static uk.gov.pay.api.model.CreateCardPaymentRequest.DESCRIPTION_FIELD_NAME;
import static uk.gov.pay.api.model.CreateCardPaymentRequest.EMAIL_FIELD_NAME;
import static uk.gov.pay.api.model.CreateCardPaymentRequest.INTERNAL;
import static uk.gov.pay.api.model.CreateCardPaymentRequest.LANGUAGE_FIELD_NAME;
import static uk.gov.pay.api.model.CreateCardPaymentRequest.METADATA;
import static uk.gov.pay.api.model.CreateCardPaymentRequest.MOTO_FIELD_NAME;
import static uk.gov.pay.api.model.CreateCardPaymentRequest.PREFILLED_ADDRESS_CITY_FIELD_NAME;
import static uk.gov.pay.api.model.CreateCardPaymentRequest.PREFILLED_ADDRESS_COUNTRY_FIELD_NAME;
import static uk.gov.pay.api.model.CreateCardPaymentRequest.PREFILLED_ADDRESS_LINE1_FIELD_NAME;
import static uk.gov.pay.api.model.CreateCardPaymentRequest.PREFILLED_ADDRESS_LINE2_FIELD_NAME;
import static uk.gov.pay.api.model.CreateCardPaymentRequest.PREFILLED_ADDRESS_POSTCODE_FIELD_NAME;
import static uk.gov.pay.api.model.CreateCardPaymentRequest.PREFILLED_BILLING_ADDRESS_FIELD_NAME;
import static uk.gov.pay.api.model.CreateCardPaymentRequest.PREFILLED_CARDHOLDER_DETAILS_FIELD_NAME;
import static uk.gov.pay.api.model.CreateCardPaymentRequest.PREFILLED_CARDHOLDER_NAME_FIELD_NAME;
import static uk.gov.pay.api.model.CreateCardPaymentRequest.REFERENCE_FIELD_NAME;
import static uk.gov.pay.api.model.CreateCardPaymentRequest.RETURN_URL_FIELD_NAME;
import static uk.gov.pay.api.model.CreateCardPaymentRequest.SET_UP_AGREEMENT_FIELD_NAME;
import static uk.gov.pay.api.model.CreateCardPaymentRequest.SOURCE_FIELD_NAME;
import static uk.gov.pay.api.model.CreatePaymentRefundRequest.REFUND_AMOUNT_AVAILABLE;
import static uk.gov.pay.api.model.RequestError.Code.CREATE_AGREEMENT_MISSING_FIELD_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.CREATE_AGREEMENT_VALIDATION_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.CREATE_PAYMENT_MISSING_FIELD_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.CREATE_PAYMENT_REFUND_MISSING_FIELD_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.CREATE_PAYMENT_REFUND_VALIDATION_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.CREATE_PAYMENT_UNEXPECTED_FIELD_ERROR;
import static uk.gov.pay.api.model.RequestError.Code.CREATE_PAYMENT_VALIDATION_ERROR;
import static uk.gov.pay.api.model.RequestError.aRequestError;
import static uk.gov.service.payments.commons.model.Source.CARD_AGENT_INITIATED_MOTO;
import static uk.gov.service.payments.commons.model.Source.CARD_API;
import static uk.gov.service.payments.commons.model.Source.CARD_PAYMENT_LINK;

class RequestJsonParser {

    private static final Set<Source> ALLOWED_SOURCES = EnumSet.of(CARD_PAYMENT_LINK, CARD_AGENT_INITIATED_MOTO);
    public static final Set<AuthorisationMode> ALLOWED_AUTHORISATION_MODES = EnumSet.of(AuthorisationMode.WEB, AuthorisationMode.MOTO_API, AuthorisationMode.AGREEMENT);

    private static ObjectMapper objectMapper = new ObjectMapper();
    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    static CreatePaymentRefundRequest parseRefundRequest(JsonNode rootNode) {
        Integer amount = validateAndGetAmount(rootNode, CREATE_PAYMENT_REFUND_VALIDATION_ERROR, CREATE_PAYMENT_REFUND_MISSING_FIELD_ERROR);
        Integer refundAmountAvailable = rootNode.get(REFUND_AMOUNT_AVAILABLE) == null ? null : rootNode.get(REFUND_AMOUNT_AVAILABLE).asInt();
        return new CreatePaymentRefundRequest(amount, refundAmountAvailable);
    }

    static CreateCardPaymentRequest parsePaymentRequest(JsonNode paymentRequest) {

        var builder = CreateCardPaymentRequestBuilder.builder()
                .amount(validateAndGetAmount(paymentRequest, CREATE_PAYMENT_VALIDATION_ERROR, CREATE_PAYMENT_MISSING_FIELD_ERROR))
                .reference(validateAndGetPaymentReference(paymentRequest))
                .description(validateAndGetPaymentDescription(paymentRequest));

        if (paymentRequest.has(RETURN_URL_FIELD_NAME)) {
            String returnUrl = validateSkipNullValueAndGetString(paymentRequest.get(RETURN_URL_FIELD_NAME),
                    aRequestError(RETURN_URL_FIELD_NAME, CREATE_PAYMENT_VALIDATION_ERROR, "Must be a valid URL format"));
            builder.returnUrl(returnUrl);
        }
        
        if (paymentRequest.has(MOTO_FIELD_NAME)) {
            builder.moto(validateAndGetMoto(paymentRequest));
        }

        if(paymentRequest.has(SET_UP_AGREEMENT_FIELD_NAME)) {
            builder.setUpAgreement(validateAndGetSetUpAgreement(paymentRequest));
        }

        AuthorisationMode authorisationMode = null;
        if (paymentRequest.has(AUTHORISATION_MODE)) {
            authorisationMode = validateAndGetAuthorisationMode(paymentRequest);
            builder.authorisationMode(authorisationMode);
        }

        if (paymentRequest.has(AGREEMENT_ID_FIELD_NAME)) {
            if (AuthorisationMode.AGREEMENT == authorisationMode) {
                builder.agreementId(validateAndGetString(
                        paymentRequest.get(AGREEMENT_ID_FIELD_NAME),
                        aRequestError(AGREEMENT_ID_FIELD_NAME, CREATE_PAYMENT_VALIDATION_ERROR, "Must be a valid string format"),
                        aRequestError(AGREEMENT_ID_FIELD_NAME, CREATE_PAYMENT_MISSING_FIELD_ERROR)));
            } else {
                throw new BadRequestException(aRequestError(CREATE_PAYMENT_UNEXPECTED_FIELD_ERROR, AGREEMENT_ID_FIELD_NAME));
            }
        }

        if (paymentRequest.has(LANGUAGE_FIELD_NAME)) {
            builder.language(validateAndGetLanguage(paymentRequest));
        }

        if (paymentRequest.has(DELAYED_CAPTURE_FIELD_NAME)) {
            builder.delayedCapture(validateAndGetDelayedCapture(paymentRequest));
        }

        if (paymentRequest.has(EMAIL_FIELD_NAME)) {
            String email = validateSkipNullValueAndGetString(paymentRequest.get(EMAIL_FIELD_NAME),
                    aRequestError(EMAIL_FIELD_NAME, CREATE_PAYMENT_VALIDATION_ERROR, "Field must be a string"));
            builder.email(email);
        }

        if (paymentRequest.has(PREFILLED_CARDHOLDER_DETAILS_FIELD_NAME)) {
            JsonNode prefilledNode = paymentRequest.get(PREFILLED_CARDHOLDER_DETAILS_FIELD_NAME);
            validatePrefilledCardholderDetails(prefilledNode, builder);
        }

        if (paymentRequest.has(METADATA)) {
            builder.metadata(validateAndGetMetadata(paymentRequest));
        }

        builder.source(validateAndGetSource(paymentRequest));

        return builder.build();
    }

    static CreateAgreementRequest parseAgreementRequest(JsonNode agreementRequest) {
        var builder = CreateAgreementRequestBuilder.builder()
                .reference(validateAndGetAgreementReference(agreementRequest))
                .description(validateAndGetAgreementDescription(agreementRequest));

        if (agreementRequest.has(USER_IDENTIFIER_FIELD)) {
            String userIdentifier = validateSkipNullValueAndGetString(agreementRequest.get(USER_IDENTIFIER_FIELD),
                    aRequestError(USER_IDENTIFIER_FIELD, CREATE_AGREEMENT_VALIDATION_ERROR, "Field must be a string"));
            builder.userIdentifier(userIdentifier);
        }

        return builder.build();
    }
    
    private static SupportedLanguage validateAndGetLanguage(JsonNode paymentRequest) {
        String errorMessage = "Must be \"en\" or \"cy\"";
        RequestError requestError = aRequestError(LANGUAGE_FIELD_NAME, CREATE_PAYMENT_VALIDATION_ERROR, errorMessage);
        String language = validateAndGetString(paymentRequest.get(LANGUAGE_FIELD_NAME), requestError, requestError);
        try {
            return SupportedLanguage.fromIso639AlphaTwoCode(language);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(Response.status(SC_UNPROCESSABLE_ENTITY).entity(requestError).build());
        }
    }

    private static AuthorisationMode validateAndGetAuthorisationMode(JsonNode paymentRequest) {
        String errorMessage = "Must be one of " + ALLOWED_AUTHORISATION_MODES.stream()
                .map(AuthorisationMode::getName)
                .collect(Collectors.joining(", "));
        RequestError requestError = aRequestError(AUTHORISATION_MODE, CREATE_PAYMENT_VALIDATION_ERROR, errorMessage);
        String value = validateAndGetString(paymentRequest.get(AUTHORISATION_MODE), requestError, requestError);

        try {
            AuthorisationMode authorisationMode = AuthorisationMode.of(value);
            if (ALLOWED_AUTHORISATION_MODES.contains(authorisationMode)) {
                return authorisationMode;
            } else {
                throw new PaymentValidationException(requestError);
            }
        } catch (IllegalArgumentException e) {
            throw new PaymentValidationException(requestError);
        }
    }

    private static String validateAndGetPaymentDescription(JsonNode paymentRequest) {
        return validateAndGetDescription(paymentRequest, CREATE_PAYMENT_VALIDATION_ERROR, CREATE_PAYMENT_MISSING_FIELD_ERROR);
    }

    private static String validateAndGetAgreementDescription(JsonNode paymentRequest) {
        return validateAndGetDescription(paymentRequest, CREATE_AGREEMENT_VALIDATION_ERROR, CREATE_AGREEMENT_MISSING_FIELD_ERROR);
    }

    private static String validateAndGetDescription(JsonNode request, RequestError.Code validationError, RequestError.Code missingFieldError) {
        return validateAndGetString(
                request.get(DESCRIPTION_FIELD_NAME),
                aRequestError(DESCRIPTION_FIELD_NAME, validationError, "Must be a valid string format"),
                aRequestError(DESCRIPTION_FIELD_NAME, missingFieldError));
    }

    private static String validateAndGetPaymentReference(JsonNode paymentRequest) {
        return validateAndGetReference(paymentRequest, CREATE_PAYMENT_VALIDATION_ERROR, CREATE_PAYMENT_MISSING_FIELD_ERROR);
    }

    private static String validateAndGetAgreementReference(JsonNode agreementRequest) {
        return validateAndGetReference(agreementRequest, CREATE_AGREEMENT_VALIDATION_ERROR, CREATE_AGREEMENT_MISSING_FIELD_ERROR);
    }

    private static String validateAndGetReference(JsonNode request, RequestError.Code validationError, RequestError.Code missingFieldError) {
        return validateAndGetString(
                request.get(REFERENCE_FIELD_NAME),
                aRequestError(REFERENCE_FIELD_NAME, validationError, "Must be a valid string format"),
                aRequestError(REFERENCE_FIELD_NAME, missingFieldError));
    }

    private static String validateAndGetString(JsonNode jsonNode, RequestError validationError, RequestError missingError) {
        String value = validateAndGetValue(jsonNode, validationError, missingError, JsonNode::isTextual, JsonNode::asText);
        check(isNotBlank(value), missingError);
        return value;
    }

    private static Boolean validateAndGetDelayedCapture(JsonNode paymentRequest) {
        return validateAndGetValue(
                paymentRequest.get(DELAYED_CAPTURE_FIELD_NAME),
                aRequestError(DELAYED_CAPTURE_FIELD_NAME, CREATE_PAYMENT_VALIDATION_ERROR, "Must be true or false"),
                aRequestError(DELAYED_CAPTURE_FIELD_NAME, CREATE_PAYMENT_VALIDATION_ERROR, "Must be true or false"),
                JsonNode::isBoolean,
                JsonNode::booleanValue);
    }

    private static Boolean validateAndGetMoto(JsonNode paymentRequest) {
        return validateAndGetValue(
                paymentRequest.get(MOTO_FIELD_NAME),
                aRequestError(MOTO_FIELD_NAME, CREATE_PAYMENT_VALIDATION_ERROR, "Must be true or false"),
                aRequestError(MOTO_FIELD_NAME, CREATE_PAYMENT_VALIDATION_ERROR, "Must be true or false"),
                JsonNode::isBoolean,
                JsonNode::booleanValue);
    }

    private static String validateAndGetSetUpAgreement(JsonNode paymentRequest) {
        return paymentRequest.get(SET_UP_AGREEMENT_FIELD_NAME).textValue();
    }

    private static Integer validateAndGetAmount(JsonNode paymentRequest, Code validationError, Code missingError) {
        return validateAndGetValue(
                paymentRequest.get(AMOUNT_FIELD_NAME),
                aRequestError(AMOUNT_FIELD_NAME, validationError, "Must be a valid numeric format"),
                aRequestError(AMOUNT_FIELD_NAME, missingError),
                JsonNode::isInt,
                JsonNode::intValue);
    }

    private static ExternalMetadata validateAndGetMetadata(JsonNode paymentRequest) {
        Map<String, Object> metadataMap;
        try {
            metadataMap = objectMapper.convertValue(paymentRequest.get("metadata"), Map.class);
        } catch (IllegalArgumentException e) {
            RequestError requestError = aRequestError(METADATA, CREATE_PAYMENT_VALIDATION_ERROR,
                    "Must be an object of JSON key-value pairs");
            throw new WebApplicationException(Response.status(SC_UNPROCESSABLE_ENTITY).entity(requestError).build());
        }

        if (metadataMap == null) {
            RequestError requestError = aRequestError(METADATA, CREATE_PAYMENT_VALIDATION_ERROR,
                    "Value must not be null");
            throw new WebApplicationException(Response.status(SC_UNPROCESSABLE_ENTITY).entity(requestError).build());
        }

        ExternalMetadata metadata = new ExternalMetadata(metadataMap);
        Set<ConstraintViolation<ExternalMetadata>> violations = validator.validate(metadata);
        if (violations.size() > 0) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .map(msg -> msg.replace("Field [metadata] ", ""))
                    .map(StringUtils::capitalize)
                    .collect(Collectors.joining(". "));
            RequestError requestError = aRequestError(METADATA, CREATE_PAYMENT_VALIDATION_ERROR, message);
            throw new WebApplicationException(Response.status(SC_UNPROCESSABLE_ENTITY).entity(requestError).build());
        }
        return metadata;
    }

    private static void validatePrefilledCardholderDetails(JsonNode prefilledNode, CreateCardPaymentRequestBuilder builder) {
        if (prefilledNode.has(PREFILLED_CARDHOLDER_NAME_FIELD_NAME)) {
            String cardHolderName = validateSkipNullValueAndGetString(prefilledNode.get(PREFILLED_CARDHOLDER_NAME_FIELD_NAME),
                    aRequestError(PREFILLED_CARDHOLDER_NAME_FIELD_NAME, CREATE_PAYMENT_VALIDATION_ERROR, "Field must be a string"));
            builder.cardholderName(cardHolderName);
        }
        if (prefilledNode.has(PREFILLED_BILLING_ADDRESS_FIELD_NAME)) {
            JsonNode addressNode = prefilledNode.get(PREFILLED_BILLING_ADDRESS_FIELD_NAME);
            if (addressNode.has(PREFILLED_ADDRESS_LINE1_FIELD_NAME)) {
                String addressLine1 = validateSkipNullValueAndGetString(addressNode.get(PREFILLED_ADDRESS_LINE1_FIELD_NAME),
                        aRequestError(PREFILLED_ADDRESS_LINE1_FIELD_NAME, CREATE_PAYMENT_VALIDATION_ERROR, "Field must be a string"));
                builder.addressLine1(addressLine1);
            }
            if (addressNode.has(PREFILLED_ADDRESS_LINE2_FIELD_NAME)) {
                String addressLine1 = validateSkipNullValueAndGetString(addressNode.get(PREFILLED_ADDRESS_LINE2_FIELD_NAME),
                        aRequestError(PREFILLED_ADDRESS_LINE2_FIELD_NAME, CREATE_PAYMENT_VALIDATION_ERROR, "Field must be a string"));
                builder.addressLine2(addressLine1);
            }
            if (addressNode.has(PREFILLED_ADDRESS_CITY_FIELD_NAME)) {
                String addressCity = validateSkipNullValueAndGetString(addressNode.get(PREFILLED_ADDRESS_CITY_FIELD_NAME),
                        aRequestError(PREFILLED_ADDRESS_CITY_FIELD_NAME, CREATE_PAYMENT_VALIDATION_ERROR, "Field must be a string"));
                builder.city(addressCity);
            }
            if (addressNode.has(PREFILLED_ADDRESS_POSTCODE_FIELD_NAME)) {
                String addressPostcode = validateSkipNullValueAndGetString(addressNode.get(PREFILLED_ADDRESS_POSTCODE_FIELD_NAME),
                        aRequestError(PREFILLED_ADDRESS_POSTCODE_FIELD_NAME, CREATE_PAYMENT_VALIDATION_ERROR, "Field must be a string"));
                builder.postcode(addressPostcode);
            }
            if (addressNode.has(PREFILLED_ADDRESS_COUNTRY_FIELD_NAME)) {
                String countryCode = validateSkipNullValueAndGetString(addressNode.get(PREFILLED_ADDRESS_COUNTRY_FIELD_NAME),
                        aRequestError(PREFILLED_ADDRESS_COUNTRY_FIELD_NAME, CREATE_PAYMENT_VALIDATION_ERROR, "Field must be a string"));
                builder.country(countryCode);
            }
        }
    }

    private static Source validateAndGetSource(JsonNode paymentRequest) {
        if (paymentRequest.has(INTERNAL)) {
            JsonNode internalNode = paymentRequest.get(INTERNAL);

            if (internalNode.has(SOURCE_FIELD_NAME)) {
                String errorMessage = "Accepted values are only CARD_PAYMENT_LINK, CARD_AGENT_INITIATED_MOTO";
                RequestError requestError = aRequestError(SOURCE_FIELD_NAME, CREATE_PAYMENT_VALIDATION_ERROR, errorMessage);
                String sourceString = validateSkipNullValueAndGetString(internalNode.get(SOURCE_FIELD_NAME), requestError);

                try {
                    Source source = Source.valueOf(sourceString);
                    if (ALLOWED_SOURCES.contains(source)) {
                        return source;
                    }
                    throw new BadRequestException(requestError);
                } catch (IllegalArgumentException e) {
                    throw new WebApplicationException(Response.status(SC_UNPROCESSABLE_ENTITY).entity(requestError).build());
                }
            }
        }

        return CARD_API;
    }

    private static <T> T validateAndGetValue(JsonNode jsonNode,
                                             RequestError validationError,
                                             RequestError missingError,
                                             Function<JsonNode, Boolean> isExpectedType,
                                             Function<JsonNode, T> valueFromJsonNode) {
        if (jsonNode != null && !jsonNode.isNull()) {
            check(isExpectedType.apply(jsonNode), validationError);
            return valueFromJsonNode.apply(jsonNode);
        }
        throw new BadRequestException(missingError);
    }

    private static String validateSkipNullValueAndGetString(JsonNode jsonNode, RequestError validationError) {
        return validateSkipNullAndGetValue(jsonNode, validationError, JsonNode::isTextual, JsonNode::asText);
    }

    private static <T> T validateSkipNullAndGetValue(JsonNode jsonNode,
                                                     RequestError validationError,
                                                     Function<JsonNode, Boolean> isExpectedType,
                                                     Function<JsonNode, T> valueFromJsonNode) {
        if (jsonNode == null || jsonNode.isNull()) {
            return null;
        }
        check(isExpectedType.apply(jsonNode), validationError);
        return valueFromJsonNode.apply(jsonNode);
    }

    private static void check(boolean condition, RequestError error) {
        if (!condition) {
            throw new BadRequestException(error);
        }
    }
}
