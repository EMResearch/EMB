package no.nav.tag.tiltaksgjennomforing.infrastruktur.auditing;

public interface AuditLogger {
    void logg(AuditEntry event);
}
