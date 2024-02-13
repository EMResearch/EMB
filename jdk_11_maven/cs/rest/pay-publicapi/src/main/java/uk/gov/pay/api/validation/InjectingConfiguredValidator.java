package uk.gov.pay.api.validation;

import io.dropwizard.jersey.validation.DropwizardConfiguredValidator;

import javax.inject.Inject;
import javax.validation.Validator;

public class InjectingConfiguredValidator extends DropwizardConfiguredValidator {
    
    @Inject
    public InjectingConfiguredValidator(Validator validator) {
        super(validator);
    }
}
