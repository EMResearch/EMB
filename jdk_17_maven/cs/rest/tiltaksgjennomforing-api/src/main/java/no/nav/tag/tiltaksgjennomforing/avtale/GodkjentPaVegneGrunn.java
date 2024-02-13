package no.nav.tag.tiltaksgjennomforing.avtale;

import lombok.Data;
import no.nav.tag.tiltaksgjennomforing.exceptions.Feilkode;
import no.nav.tag.tiltaksgjennomforing.exceptions.FeilkodeException;

import javax.persistence.Embeddable;

@Data
@Embeddable
public class GodkjentPaVegneGrunn {
    private boolean ikkeBankId;
    private boolean reservert;
    private boolean digitalKompetanse;
    private boolean arenaMigreringDeltaker;

    public void valgtMinstEnGrunn() {
        if (!ikkeBankId && !reservert && !digitalKompetanse && !arenaMigreringDeltaker) {
            throw new FeilkodeException(Feilkode.GODKJENT_PAA_VEGNE_GRUNN_MAA_VELGES);
        }
    }
}

