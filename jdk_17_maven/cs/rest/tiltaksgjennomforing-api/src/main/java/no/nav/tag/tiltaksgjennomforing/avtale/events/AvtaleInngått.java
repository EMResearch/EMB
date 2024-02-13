package no.nav.tag.tiltaksgjennomforing.avtale.events;

import lombok.Value;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtale;
import no.nav.tag.tiltaksgjennomforing.avtale.Avtalerolle;
import no.nav.tag.tiltaksgjennomforing.avtale.NavIdent;

@Value
public class AvtaleInngått {
    Avtale avtale;
    Avtalerolle utførtAvRolle;
    NavIdent utførtAv;
}
