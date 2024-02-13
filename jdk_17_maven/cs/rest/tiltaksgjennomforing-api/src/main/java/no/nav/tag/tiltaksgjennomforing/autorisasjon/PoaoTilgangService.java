package no.nav.tag.tiltaksgjennomforing.autorisasjon;

import java.util.UUID;

public interface PoaoTilgangService {
    boolean harSkriveTilgang(UUID beslutterAzureUUID, String deltakerFnr);
}
