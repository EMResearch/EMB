package no.nav.familie.tilbake.historikkinnslag

import no.nav.familie.kontrakter.felles.historikkinnslag.Historikkinnslagstype
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg

private const val VARSELBREV_TEKST = "Varselbrev tilbakebetaling"

enum class TilbakekrevingHistorikkinnslagstype(
    val tittel: String,
    val tekst: String? = null,
    val type: Historikkinnslagstype = Historikkinnslagstype.HENDELSE,
    val steg: Behandlingssteg? = null,
) {

    // Hendelse type
    BEHANDLING_OPPRETTET(tittel = "Behandling opprettet"),
    BEHANDLING_PÅ_VENT(tittel = "Behandling er satt på vent", tekst = "Årsak: "),
    BEHANDLING_GJENOPPTATT(tittel = "Behandling gjenopptatt"),
    KRAVGRUNNLAG_MOTTATT(tittel = "Kravgrunnlag mottatt"),
    KRAVGRUNNLAG_HENT(tittel = "Kravgrunnlag innhentet"),
    VERGE_FJERNET(tittel = "Verge fjernet"),
    BEHANDLING_SENDT_TIL_BESLUTTER(tittel = "Sendt til beslutter"),
    BEHANDLING_SENDT_TILBAKE_TIL_SAKSBEHANDLER(tittel = "Vedtak underkjent"),
    VEDTAK_FATTET(tittel = "Vedtak fattet", tekst = "Resultat: "),
    BEHANDLING_AVSLUTTET(tittel = "Behandling avsluttet"),
    BEHANDLING_HENLAGT(tittel = "Behandling henlagt", tekst = "Årsak: "),
    ENDRET_ENHET(tittel = "Endret enhet", tekst = "Ny enhet: "),
    BEHANDLING_FLYTTET_MED_FORVALTNING(tittel = "Problem i forvaltning", tekst = "Behandling flyttet tilbake til Fakta"),
    BREVMOTTAKER_LAGT_TIL("Brevmottaker er lagt til"),
    BREVMOTTAKER_ENDRET("Brevmottaker er endret"),
    BREVMOTTAKER_FJERNET("Brevmottaker er fjernet"),

    // Skjermlenke type
    VERGE_OPPRETTET(
        tittel = "Verge registert",
        type = Historikkinnslagstype.SKJERMLENKE,
        steg = Behandlingssteg.VERGE,
    ),
    FAKTA_VURDERT(
        tittel = "Fakta vurdert",
        type = Historikkinnslagstype.SKJERMLENKE,
        steg = Behandlingssteg.FAKTA,
    ),
    FORELDELSE_VURDERT(
        tittel = "Foreldelse vurdert",
        type = Historikkinnslagstype.SKJERMLENKE,
        steg = Behandlingssteg.FORELDELSE,
    ),
    VILKÅRSVURDERING_VURDERT(
        tittel = "Vilkår vurdert",
        type = Historikkinnslagstype.SKJERMLENKE,
        steg = Behandlingssteg.VILKÅRSVURDERING,
    ),
    FORESLÅ_VEDTAK_VURDERT(
        tittel = "Vedtak foreslått",
        type = Historikkinnslagstype.SKJERMLENKE,
        steg = Behandlingssteg.FORESLÅ_VEDTAK,
    ),

    // Brev type
    VARSELBREV_SENDT(
        tittel = "Varselbrev sendt",
        tekst = VARSELBREV_TEKST,
        type = Historikkinnslagstype.BREV,
    ),
    VARSELBREV_SENDT_TIL_VERGE(
        tittel = "Varselbrev sendt til verge",
        tekst = VARSELBREV_TEKST,
        type = Historikkinnslagstype.BREV,
    ),
    KORRIGERT_VARSELBREV_SENDT(
        tittel = "Varselbrev sendt",
        tekst = VARSELBREV_TEKST,
        type = Historikkinnslagstype.BREV,
    ),
    KORRIGERT_VARSELBREV_SENDT_TIL_VERGE(
        tittel = "Varselbrev sendt til verge",
        tekst = VARSELBREV_TEKST,
        type = Historikkinnslagstype.BREV,
    ),
    VEDTAKSBREV_SENDT(
        tittel = "Vedtaksbrev sendt",
        tekst = "Vedtak om tilbakebetaling",
        type = Historikkinnslagstype.BREV,
    ),
    VEDTAKSBREV_SENDT_TIL_VERGE(
        tittel = "Vedtaksbrev sendt til verge",
        tekst = "Vedtak om tilbakebetaling",
        type = Historikkinnslagstype.BREV,
    ),
    HENLEGGELSESBREV_SENDT(
        tittel = "Henleggelsesbrev sendt",
        tekst = "Henleggelsesbrev",
        type = Historikkinnslagstype.BREV,
    ),
    HENLEGGELSESBREV_SENDT_TIL_VERGE(
        tittel = "Henleggelsesbrev sendt til verge",
        tekst = "Henleggelsesbrev",
        type = Historikkinnslagstype.BREV,
    ),
    INNHENT_DOKUMENTASJON_BREV_SENDT(
        tittel = "Innhent dokumentasjon sendt",
        tekst = "Innhent dokumentasjon",
        type = Historikkinnslagstype.BREV,
    ),
    INNHENT_DOKUMENTASJON_BREV_SENDT_TIL_VERGE(
        tittel = "Innhent dokumentasjon sendt til verge",
        tekst = "Innhent dokumentasjon",
        type = Historikkinnslagstype.BREV,
    ),
    BREV_IKKE_SENDT_UKJENT_ADRESSE(
        tittel = "Bruker har ukjent adresse, brev ikke sendt",
        tekst = "",
        type = Historikkinnslagstype.BREV,
    ),
    BREV_IKKE_SENDT_DØDSBO_UKJENT_ADRESSE(
        tittel = "Brev ikke distribuert. Ukjent dødsbo",
        tekst = "Mottaker har ukjent dødsboadresse, og brevet blir ikke sendt før adressen er satt",
        type = Historikkinnslagstype.BREV,
    ),
    DISTRIBUSJON_BREV_DØDSBO_FEILET_6_MND(
        tittel = "Distribusjon av brev til dødsbo feilet",
        tekst = "Mottaker har ikke fått dødsboadresse etter 6 måneder",
        type = Historikkinnslagstype.HENDELSE,
    ),
    DISTRIBUSJON_BREV_DØDSBO_SUKSESS(
        tittel = "Distribusjon av brev til dødsbo fullført",
        tekst = "",
        type = Historikkinnslagstype.BREV,
    ),
}
