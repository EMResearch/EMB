package no.nav.tag.tiltaksgjennomforing.avtale;

public enum Tiltakstype {
    ARBEIDSTRENING("Arbeidstrening", "ab0422", "ARBTREN"),
    MIDLERTIDIG_LONNSTILSKUDD("Midlertidig lønnstilskudd", "ab0336", "MIDLONTIL"),
    VARIG_LONNSTILSKUDD("Varig lønnstilskudd", "ab0337", "VARLONTIL"),
    MENTOR("Mentor", "ab0416", "MENTOR"),
    INKLUDERINGSTILSKUDD("Inkluderingstilskudd", "ab0417", "INKLUTILS"),
    SOMMERJOBB("Sommerjobb", "ab0450", null);

    final String beskrivelse;
    final String behandlingstema;
    final String tiltakskodeArena;

    Tiltakstype(String beskrivelse, String behandlingstema, String tiltakskodeArena) {
        this.beskrivelse = beskrivelse;
        this.behandlingstema = behandlingstema;
        this.tiltakskodeArena = tiltakskodeArena;
    }

    public String getBeskrivelse() {
        return this.beskrivelse;
    }

    public String getBehandlingstema() {
        return this.behandlingstema;
    }

    public String getTiltakskodeArena() {
        return tiltakskodeArena;
    }
}
