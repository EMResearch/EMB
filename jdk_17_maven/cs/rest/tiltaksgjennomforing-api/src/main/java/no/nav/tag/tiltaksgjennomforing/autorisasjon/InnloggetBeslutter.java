package no.nav.tag.tiltaksgjennomforing.autorisasjon;

import lombok.Value;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtalerolle;
import no.nav.tag.tiltaksgjennomforing.avtale.NavIdent;
import no.nav.tag.tiltaksgjennomforing.featuretoggles.enhet.NavEnhet;

import java.util.Set;

@Value
public class InnloggetBeslutter implements InnloggetBruker {

    NavIdent identifikator;
    Avtalerolle rolle = Avtalerolle.BESLUTTER;
    boolean erNavAnsatt = true;
    Set<NavEnhet> navEnheter;
}
