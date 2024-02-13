package no.nav.tag.tiltaksgjennomforing.enhet;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum Kvalifiseringsgruppe {
    SPESIELT_TILPASSET_INNSATS("BATT"),      // Personen har nedsatt arbeidsevne og har et identifisert behov for kvalifisering og/eller tilrettelegging. Aktivitetsplan skal utformes.
    SITUASJONSBESTEMT_INNSATS("BFORM"),      // Personen har moderat bistandsbehov
    VARIG_TILPASSET_INNSATS("VARIG"),        // Personen har varig nedsatt arbeidsevne
    BEHOV_FOR_ARBEIDSEVNEVURDERING("BKART"), // Personen har behov for arbeidsevnevurdering
    STANDARD_INNSATS("IKVAL"),               // Personen har behov for ordinær bistand
    IKKE_VURDERT("IVURD"),                   // Ikke vurdert
    RETTIGHETER_ETTER_FTRL_KAP11("KAP11"),   // Rettigheter etter Ftrl. Kapittel 11
    HELSERELATERT_ARBEIDSRETTET_OPPFOLGING_I_NAV("OPPFI"), // Helserelatert arbeidsrettet oppfølging i NAV
    SYKMELDT_OPPFOLGING_PA_ARBEIDSPLASSEN("VURDI"),        // Sykmeldt, oppfølging på arbeidsplassen
    SYKMELDT_UTEN_ARBEIDSGIVER("VURDU");                   // Sykmeldt uten arbeidsgiver


    private final String kvalifiseringskode;

    Kvalifiseringsgruppe(String kvalifiseringskode) {
        this.kvalifiseringskode = kvalifiseringskode;
    }

    @JsonValue
    public String getKvalifiseringskode() {
        return kvalifiseringskode;
    }

    public static boolean ugyldigKvalifiseringsgruppe(Kvalifiseringsgruppe kvalifiseringsgruppe) {
        return switch (kvalifiseringsgruppe) {
            case STANDARD_INNSATS, BEHOV_FOR_ARBEIDSEVNEVURDERING, IKKE_VURDERT -> true;
            case RETTIGHETER_ETTER_FTRL_KAP11, HELSERELATERT_ARBEIDSRETTET_OPPFOLGING_I_NAV,
                    SYKMELDT_OPPFOLGING_PA_ARBEIDSPLASSEN, SYKMELDT_UTEN_ARBEIDSGIVER,
                    SPESIELT_TILPASSET_INNSATS, SITUASJONSBESTEMT_INNSATS, VARIG_TILPASSET_INNSATS -> false;
        };
    }

    public static boolean kvalifisererTilMidlertidiglonnstilskuddOgSommerjobbOgMentor(Kvalifiseringsgruppe kvalifiseringsgruppe) {
        return switch (kvalifiseringsgruppe) {
            case SPESIELT_TILPASSET_INNSATS, SITUASJONSBESTEMT_INNSATS, VARIG_TILPASSET_INNSATS -> true;
            default -> false;
        };
    }

    public static boolean kvalifisererTilVariglonnstilskudd(Kvalifiseringsgruppe kvalifiseringsgruppe) {
        return kvalifiseringsgruppe == VARIG_TILPASSET_INNSATS;
    }

    public Integer finnLonntilskuddProsentsatsUtifraKvalifiseringsgruppe(Integer prosentsatsLiten, Integer prosentsatsStor) {
        switch (this) {
            case SPESIELT_TILPASSET_INNSATS, VARIG_TILPASSET_INNSATS:
                return prosentsatsStor;
            case SITUASJONSBESTEMT_INNSATS:
                return prosentsatsLiten;
            default: {
                log.warn("feilet med setting av kvalifiseringsgruppe. Kvalifiseringsgruppe: {}", this);
                return null;
            }
        }
    }
}
