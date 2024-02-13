package uk.gov.pay.api.validation;


import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import uk.gov.pay.api.exception.AgreementValidationException;
import uk.gov.pay.api.ledger.model.AgreementSearchParams;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgreementSearchValidatorTest {

    @Test
    public void agreementSearchValidator_shouldNotThrowWithValidParams() {
        var params = validParams();
        assertDoesNotThrow(() -> AgreementSearchValidator.validateSearchParameters(params));
    }

    @Test
    public void agreementSearchValidator_shouldThrowForInvalidReference() {
        var params = validParams();
        params.setReference(RandomStringUtils.randomAlphanumeric(300));
        assertThrows(AgreementValidationException.class, () -> AgreementSearchValidator.validateSearchParameters(params));
    }

    @Test
    public void agreementSearchValidator_shouldThrowForInvalidStatus() {
        var params = validParams();
        params.setStatus("NOT-A-STATUS");
        assertThrows(AgreementValidationException.class, () -> AgreementSearchValidator.validateSearchParameters(params));
    }

    @Test
    public void agreementSearchValidator_shouldThrowForInvalidPageNumber() {
        var params = validParams();
        params.setPageNumber("NOT-A-PAGE-NUMBER");
        assertThrows(AgreementValidationException.class, () -> AgreementSearchValidator.validateSearchParameters(params));
    }

    @Test
    public void agreementSearchValidator_shouldThrowForInvalidDisplaySize() {
        var params = validParams();
        params.setDisplaySize("NOT-A-DISPLAY-SIZE");
        assertThrows(AgreementValidationException.class, () -> AgreementSearchValidator.validateSearchParameters(params));
    }

    private AgreementSearchParams validParams() {
        return new AgreementSearchParams(RandomStringUtils.randomAlphanumeric(200), "created", "1", "1");
    }
}
