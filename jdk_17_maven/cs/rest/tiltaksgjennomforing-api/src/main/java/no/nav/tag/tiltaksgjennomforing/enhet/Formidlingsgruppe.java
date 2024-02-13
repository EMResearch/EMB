package no.nav.tag.tiltaksgjennomforing.enhet;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Formidlingsgruppe {
    ARBEIDSSOKER("ARBS"),            // Person er tilgjengelig for alt søk etter   arbeidskraft, ordinær og vikar
    IKKE_ARBEIDSSOKER("IARBS"),      // Person er ikke tilgjengelig for søk etter arbeidskraft
    INAKTIVERT_JOBBSKIFTER("IJOBS"), // Jobbskifter som er inaktivert fra nav.no
    IKKE_SERVICEBEHOV("ISERV"),      // Inaktivering, person mottar ikke bistand fra NAV
    FRA_NAV_NO("JOBBS"),             // Personen er ikke tilgjengelig for søk
    PRE_ARBEIDSSOKER("PARBS"),       // Personen fra nav.no som ønsker å bli arbeidssøker, men som enda ikke er   verifisert
    PRE_REAKTIVERT_ARBEIDSSOKER("RARBS"); //Person som er reaktivert fra nav.no
    private final String formidlingskode;

    Formidlingsgruppe(String formidlingskode) { this.formidlingskode = formidlingskode; }

    @JsonValue
    public String getKode() {
        return formidlingskode;
    }

    public static boolean ugyldigFormidlingsgruppe(Formidlingsgruppe formidlingsgruppe) {
        return switch (formidlingsgruppe) {
            case IKKE_ARBEIDSSOKER, INAKTIVERT_JOBBSKIFTER, IKKE_SERVICEBEHOV -> true;
            case ARBEIDSSOKER, FRA_NAV_NO, PRE_ARBEIDSSOKER, PRE_REAKTIVERT_ARBEIDSSOKER -> false;
        };
    }
}
