package no.nav.tag.tiltaksgjennomforing.infrastruktur;

import lombok.experimental.UtilityClass;
import org.slf4j.MDC;
import org.springframework.util.Assert;

import java.util.UUID;

@UtilityClass
public class CorrelationIdSupplier {
    public static final String MDC_CORRELATION_ID_KEY = "correlationId";

    public static void generateToken() {
        MDC.put(MDC_CORRELATION_ID_KEY, UUID.randomUUID().toString());
    }

    public static void set(String token) {
        Assert.hasLength(token, "Token kan ikke være blank");
        MDC.put(MDC_CORRELATION_ID_KEY, token);
    }

    public static String get() {
        return MDC.get(MDC_CORRELATION_ID_KEY);
    }

    public static void remove() {
        MDC.remove(MDC_CORRELATION_ID_KEY);
    }
}
