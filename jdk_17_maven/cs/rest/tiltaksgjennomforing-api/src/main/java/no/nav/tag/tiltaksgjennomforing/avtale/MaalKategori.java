package no.nav.tag.tiltaksgjennomforing.avtale;

public enum MaalKategori {
    FÅ_JOBB_I_BEDRIFTEN("Få jobb i bedriften"),
    ARBEIDSERFARING("Arbeidserfaring"),
    UTPRØVING("Utprøving"),
    SPRÅKOPPLÆRING("Språkopplæring"),
    OPPNÅ_FAGBREV_KOMPETANSEBEVIS("Oppnå fagbrev/kompetansebevis"),
    ANNET("Annet");

    private final String verdi;

    MaalKategori(String verdi) {
        this.verdi = verdi;
    }

    public String getVerdi() {
        return verdi;
    }
}
