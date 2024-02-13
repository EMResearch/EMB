package no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon;

public enum NotifikasjonTekst {
    TILTAK_AVTALE_OPPRETTET("Ny avtale om arbeidstiltak opprettet. Åpne avtale og fyll ut innholdet."),
    TILTAK_AVTALE_INNGATT("Avtale om arbeidstiltak godkjent."),
    TILTAK_AVTALE_KLAR_REFUSJON("Du kan nå søke om refusjon."),
    TILTAK_STILLINGSBESKRIVELSE_ENDRET("Stillingsbeskrivelse i avtale endret av veileder."),
    TILTAK_MÅL_ENDRET("Mål i avtale endret av veileder."),
    TILTAK_INKLUDERINGSTILSKUDD_ENDRET("Inkluderingstilskudd i avtalen endret av veileder."),
    TILTAK_OM_MENTOR_ENDRET("Om mentor i avtale endret av veileder."),
    TILTAK_OPPFØLGING_OG_TILRETTELEGGING_ENDRET("Oppfølging og tilrettelegging i avtale endret av veileder."),
    TILTAK_AVTALE_FORKORTET("Avtale forkortet."),
    TILTAK_AVTALE_FORLENGET("Avtale forlenget av veileder."),
    TILTAK_TILSKUDDSBEREGNING_ENDRET("Tilskuddsberegning i avtale endret av veileder."),
    TILTAK_GODKJENNINGER_OPPHEVET_AV_VEILEDER("Avtalen må godkjennes på nytt."),
    TILTAK_KONTAKTINFORMASJON_ENDRET("Kontaktinformasjon i avtale endret av veileder.");

    private final String tekst;

    NotifikasjonTekst(String tekst) {
        this.tekst = tekst;
    }

    public String getTekst() {
        return tekst;
    }
}
