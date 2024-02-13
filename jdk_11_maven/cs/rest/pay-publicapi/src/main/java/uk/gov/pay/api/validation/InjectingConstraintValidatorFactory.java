package uk.gov.pay.api.validation;

import com.google.inject.Injector;

import javax.inject.Singleton;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;

@Singleton
public class InjectingConstraintValidatorFactory implements ConstraintValidatorFactory {

    private Injector injector;
    
    public InjectingConstraintValidatorFactory(Injector injector) {
        this.injector = injector;
    }

    @Override
    public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
        return injector.getInstance(key);
    }

    @Override
    public void releaseInstance(ConstraintValidator<?, ?> instance) {
    }
}
