package no.nav.tag.tiltaksgjennomforing.varsel.notifikasjon.response;

public enum MutationStatus {
    NY_OPPGAVE_VELLYKKET("NyOppgaveVellykket"),
    NY_BESKJED_VELLYKKET("NyBeskjedVellykket"),
    OPPGAVE_UTFOERT_VELLYKKET("OppgaveUtfoertVellykket"),
    SOFT_DELETE_NOTIFIKASJON_VELLYKKET("SoftDeleteNotifikasjonVellykket"),
    HARD_DELETE_NOTIFIKASJON_VELLYKKET("HardDeleteNotifikasjonVellykket"),
    NOTIFIKASJON_FINNES_IKKE("NotifikasjonFinnesIkke"),
    DUPLIKAT_ID_OG_MERKELAPP("DuplikatEksternIdOgMerkelapp"),
    UGYLDIG_MOTTAKER("UgyldigMottaker"),
    UKJENT_PRODUSENT("UkjentProdusent"),
    UGYLDIG_MERKELAPP("UgyldigMerkelapp");

    private final String status;

    MutationStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
