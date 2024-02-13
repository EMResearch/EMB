package no.nav.tag.tiltaksgjennomforing.autorisasjon;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import no.nav.tag.tiltaksgjennomforing.Miljø;

@Service
@Profile(value = { Miljø.DEV_GCP_LABS, Miljø.LOCAL })
public class PoaoTilgangServiceLabs implements PoaoTilgangService {

    public boolean harSkriveTilgang(UUID beslutterAzureUUID, String deltakerFnr) {
        return true;
    }
}
