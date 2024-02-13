package no.nav.tag.tiltaksgjennomforing.avtale;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Avslagsårsak {
    FEIL_I_FAKTA("Feil i fakta"),
    FEIL_I_REGELFORSTÅELSE("Feil i regelforståelse"),
    ANNET("Annet"),
    FEIL_I_PROSENTSATS("Feil i prosentsats");

    private final String tekst;
}
