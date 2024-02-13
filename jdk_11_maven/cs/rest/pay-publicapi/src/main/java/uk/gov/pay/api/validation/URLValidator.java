package uk.gov.pay.api.validation;

import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.DomainValidator.ArrayType;
import org.apache.commons.validator.routines.UrlValidator;

import java.util.List;

import static org.apache.commons.validator.routines.UrlValidator.ALLOW_2_SLASHES;
import static org.apache.commons.validator.routines.UrlValidator.ALLOW_LOCAL_URLS;

public class URLValidator {
    private UrlValidator urlValidator;
    
    private URLValidator(boolean allowInsecureConnection) {
        long options = ALLOW_LOCAL_URLS + ALLOW_2_SLASHES;
        urlValidator = new UrlValidator(
                schemes(allowInsecureConnection),
                null, 
                options, 
                domainValidator()
        );
    }

    private String[] schemes(boolean allowInsecureConnection) {
        if (allowInsecureConnection) {
            return new String[]{"http", "https"};
        } else {
            return new String[]{"https"};
        }
    }

    private DomainValidator domainValidator() {
        String[] otherValidTlds = new String[]{"internal", "local"};
        DomainValidator.Item item = new DomainValidator.Item(ArrayType.GENERIC_PLUS, otherValidTlds);
        return DomainValidator.getInstance(true, List.of(item));
    }

    public static URLValidator urlValidatorValueOf(boolean allowInsecureConnection) {
        return new URLValidator(allowInsecureConnection);
    }

    public boolean isValid(String value) {
        return urlValidator.isValid(value);
    }
}
