package no.nav.tag.tiltaksgjennomforing.infrastruktur.auditing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(value = "tiltaksgjennomforing.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class AuditConsoleLogger implements AuditLogger {
    @Override
    public void logg(AuditEntry event) {
        log.info("Audit-event: {}", event);
    }
}
