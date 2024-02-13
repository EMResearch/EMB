package no.nav.tag.tiltaksgjennomforing.avtale;

import lombok.Value;

@Value
public class GodkjentPaVegneAvDeltakerOgArbeidsgiverGrunn {
    GodkjentPaVegneAvArbeidsgiverGrunn godkjentPaVegneAvArbeidsgiverGrunn;
    GodkjentPaVegneGrunn godkjentPaVegneAvDeltakerGrunn;

    public void valgtMinstEnGrunn() {
        godkjentPaVegneAvArbeidsgiverGrunn.valgtMinstEnGrunn();
        godkjentPaVegneAvDeltakerGrunn.valgtMinstEnGrunn();
    }
}