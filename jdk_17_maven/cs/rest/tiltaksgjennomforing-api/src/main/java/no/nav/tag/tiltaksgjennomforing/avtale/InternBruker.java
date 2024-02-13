package no.nav.tag.tiltaksgjennomforing.avtale;

import java.util.UUID;

public interface InternBruker {
    UUID getAzureOid();
    NavIdent getNavIdent();
}
