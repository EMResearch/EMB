package no.nav.tag.tiltaksgjennomforing.avtale;

import lombok.Data;
import no.nav.tag.tiltaksgjennomforing.exceptions.Feilkode;
import no.nav.tag.tiltaksgjennomforing.exceptions.FeilkodeException;

import javax.persistence.Embeddable;

@Data
@Embeddable
public class GodkjentPaVegneAvArbeidsgiverGrunn {
    boolean klarerIkkeGiFaTilgang;
    boolean vetIkkeHvemSomKanGiTilgang;
    boolean farIkkeTilgangPersonvern;
    boolean arenaMigreringArbeidsgiver;

    public void valgtMinstEnGrunn() {
        if (!klarerIkkeGiFaTilgang && !vetIkkeHvemSomKanGiTilgang && !farIkkeTilgangPersonvern && !arenaMigreringArbeidsgiver) {
            throw new FeilkodeException(Feilkode.GODKJENT_PAA_VEGNE_GRUNN_MAA_VELGES);
        }
    }
}