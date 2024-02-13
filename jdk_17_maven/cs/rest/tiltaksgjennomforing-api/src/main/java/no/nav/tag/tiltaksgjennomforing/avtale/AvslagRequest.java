package no.nav.tag.tiltaksgjennomforing.avtale;

import lombok.Value;

import java.util.EnumSet;

@Value
public class AvslagRequest {
    EnumSet<Avslagsårsak> avslagsårsaker;
    String avslagsforklaring;
}
