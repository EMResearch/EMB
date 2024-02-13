package no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon;

public enum NotifikasjonMerkelapp {
    LONNTILSKUDD("Lønnstilskudd"),
    MENTOR("Mentor"),
    SOMMERJOBB("Sommerjobb"),
    INKLUDERINGSTILSKUDD("Inkluderingstilskudd"),
    ARBEIDSTRENING("Arbeidstrening");

    private final String merkelapp;

    NotifikasjonMerkelapp(String merkelapp) {
        this.merkelapp = merkelapp;
    }

    public String getValue() {
        return merkelapp;
    }

    public static NotifikasjonMerkelapp getMerkelapp(String merkelapp) {
        switch (merkelapp) {
            case "Midlertidig lønnstilskudd":
            case "Varig lønnstilskudd":
                return NotifikasjonMerkelapp.LONNTILSKUDD;
            case "Mentor":
                return NotifikasjonMerkelapp.MENTOR;
            case "Sommerjobb":
                return NotifikasjonMerkelapp.SOMMERJOBB;
            case "Inkluderingstilskudd":
                return NotifikasjonMerkelapp.INKLUDERINGSTILSKUDD;
            case "Arbeidstrening":
            default:
                return NotifikasjonMerkelapp.ARBEIDSTRENING;
        }
    }
}
