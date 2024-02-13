package no.nav.tag.tiltaksgjennomforing.infrastruktur.kafka;

public class Topics {
    //Tilskuddsperioder
    public static final String TILSKUDDSPERIODE_GODKJENT = "arbeidsgiver.tiltak-tilskuddsperiode-godkjent";
    public static final String TILSKUDDSPERIODE_ANNULLERT = "arbeidsgiver.tiltak-tilskuddsperiode-annullert";
    public static final String TILSKUDDSPERIODE_FORKORTET = "arbeidsgiver.tiltak-tilskuddsperiode-forkortet";
    public static final String REFUSJON_ENDRET_STATUS = "arbeidsgiver.tiltak-refusjon-endret-status";
    //Varsel
    public static final String TILTAK_SMS = "arbeidsgiver.tiltak-sms";
    public static final String TILTAK_VARSEL = "arbeidsgiver.tiltak-varsel";
    //Statistikk
    public static final String DVH_MELDING = "arbeidsgiver.tiltak-dvh-melding";

    public static final String AVTALE_HENDELSE = "arbeidsgiver.tiltak-avtale-hendelse";
    public static final String AVTALE_HENDELSE_COMPACT = "arbeidsgiver.tiltak-avtale-hendelse-compact";

    public static final String AUDIT_HENDELSE = "arbeidsgiver.tiltak-audit-hendelse";
}
