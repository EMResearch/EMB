package no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon;

public enum NotifikasjonOperasjonType {
    SEND_BESKJED("SEND_BESKJED"),
    SEND_OPPGAVE("SEND_OPPGAVE"),
    SETT_OPPGAVE_UTFOERT("SETT_OPPGAVE_UTFOERT"),
    SOFTDELETE_NOTIFIKASJON("SOFTDELETE_NOTIFIKASJON");

    private final String type;

    NotifikasjonOperasjonType(String type) { this.type = type; }

    public String getType() { return type; }
}
