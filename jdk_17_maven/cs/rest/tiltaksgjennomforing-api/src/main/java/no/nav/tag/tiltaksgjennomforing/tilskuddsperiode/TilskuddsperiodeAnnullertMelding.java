package no.nav.tag.tiltaksgjennomforing.tilskuddsperiode;

import lombok.Value;

import java.util.UUID;

@Value
public class TilskuddsperiodeAnnullertMelding {
    UUID tilskuddsperiodeId;
    TilskuddsperiodeAnnullertÅrsak årsak;
}

enum TilskuddsperiodeAnnullertÅrsak {
    AVTALE_ANNULLERT, REFUSJON_FRIST_UTGÅTT, REFUSJON_IKKE_SØKT,
}