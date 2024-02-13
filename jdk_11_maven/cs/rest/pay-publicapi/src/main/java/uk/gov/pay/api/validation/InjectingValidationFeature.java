package uk.gov.pay.api.validation;

import com.google.inject.Injector;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.internal.inject.ConfiguredValidator;

import javax.inject.Singleton;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.Validator;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

public class InjectingValidationFeature implements Feature {
    private Injector injector;

    public InjectingValidationFeature(Injector injector) {
        this.injector = injector;
    }

    @Override
    public boolean configure(FeatureContext context) {
        context.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bindFactory(ValidatorFactory.class).to(Validator.class).in(Singleton.class);
                bind(InjectingConfiguredValidator.class).to(ConfiguredValidator.class).in(Singleton.class);
                bind(new InjectingConstraintValidatorFactory(injector)).to(ConstraintValidatorFactory.class);
            }
        });
        return true;
    }
}
