package no.nav.familie.ba.sak.kjerne.brev.domene.maler

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentÅrsak
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstype
import no.nav.familie.kontrakter.felles.objectMapper
import java.time.LocalDate

interface Brev {

    val mal: Brevmal
    val data: BrevData
}

/***
 * Se https://github.com/navikt/familie/blob/master/doc/ba-sak/legg-til-nytt-brev.md
 * for detaljer om alt som skal inn når du legger til en ny brevmal.
 ***/
enum class Brevmal(val erVedtaksbrev: Boolean, val apiNavn: String, val visningsTekst: String) {
    INFORMASJONSBREV_DELT_BOSTED(false, "informasjonsbrevDeltBosted", "Informasjonsbrev delt bosted"),
    INNHENTE_OPPLYSNINGER(false, "innhenteOpplysninger", "Innhente opplysninger"),
    INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED(
        false,
        "innhenteOpplysningerEtterSoknadISED",
        "Innhente opplysninger etter søknad i SED",
    ),
    INNHENTE_OPPLYSNINGER_OG_INFORMASJON_OM_AT_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_HAR_SØKT(
        erVedtaksbrev = false,
        apiNavn = "innhentingOgInfoAnnenForelderMedSelvstendigRettSokt",
        visningsTekst = "Innhente opplysninger og informasjon om at annen forelder med selvstendig rett har søkt",
    ),
    INNHENTE_OPPLYSNINGER_INSTITUSJON(false, "innhenteOpplysningerInstitusjon", "Innhente opplysninger institusjon"),
    HENLEGGE_TRUKKET_SØKNAD(false, "henleggeTrukketSoknad", "Henlegge trukket søknad"),
    VARSEL_OM_REVURDERING(false, "varselOmRevurdering", "Varsel om revurdering"),
    VARSEL_OM_REVURDERING_INSTITUSJON(false, "varselOmRevurderingInstitusjon", "Varsel om revurdering institusjon"),
    VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14(
        false,
        "varselOmRevurderingDeltBostedParagrafFjorten",
        "Varsel om revurdering delt bosted § 14",
    ),
    VARSEL_OM_REVURDERING_SAMBOER(
        false,
        "varselOmRevurderingSamboer",
        "Varsel om revurdering samboer",
    ),
    VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED(
        false,
        "varselOmVedtakEtterSoknadISED",
        "Varsel om vedtak etter søknad i SED",
    ),
    VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS(
        false,
        "varselOmRevurderingFraNasjonalTilEOS",
        "Varsel om revurdering fra nasjonal til EØS",
    ),
    VARSEL_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_SØKT(
        false,
        "varselAnnenForelderMedSelvstendigRettSoekt",
        "Varsel annen forelder med selvstendig rett søkt",
    ),
    VARSEL_OM_ÅRLIG_REVURDERING_EØS(
        false,
        "varselOmAarligRevurderingEos",
        "Varsel om årlig revurdering EØS",
    ),
    VARSEL_OM_ÅRLIG_REVURDERING_EØS_MED_INNHENTING_AV_OPPLYSNINGER(
        false,
        "varselOmAarligRevurderingEosMedInnhentingAvOpplysninger",
        "Varsel om årlig revurdering EØS med innhenting av opplysninger",
    ),

    SVARTIDSBREV(false, "svartidsbrev", "Svartidsbrev"),
    SVARTIDSBREV_INSTITUSJON(false, "svartidsbrevInstitusjon", "Svartidsbrev institusjon"),
    FORLENGET_SVARTIDSBREV(false, "forlengetSvartidsbrev", "Forlenget svartidsbrev"),
    FORLENGET_SVARTIDSBREV_INSTITUSJON(false, "forlengetSvartidsbrevInstitusjon", "Forlenget svartidsbrev institusjon"),
    INFORMASJONSBREV_FØDSEL_MINDREÅRIG(
        false,
        "informasjonsbrevFodselMindreaarig",
        "Informasjonsbrev fødsel mindreårig",
    ),

    INFORMASJONSBREV_FØDSEL_VERGEMÅL(false, "informasjonsbrevFodselVergemaal", "Informasjonsbrev fødsel vergemål"),
    INFORMASJONSBREV_KAN_SØKE(false, "informasjonsbrevKanSoke", "Informasjonsbrev kan søke"),
    INFORMASJONSBREV_KAN_SØKE_EØS(false, "informasjonsbrevKanSokeEOS", "Informasjonsbrev kan søke EØS"),
    INFORMASJONSBREV_FØDSEL_GENERELL(false, "informasjonsbrevFodselGenerell", "Informasjonsbrev fødsel generell"),

    INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HAR_FÅTT_EN_SØKNAD_FRA_ANNEN_FORELDER(
        erVedtaksbrev = false,
        apiNavn = "tilForelderOmfattetNorskLovgivningHarFaattSoknadFraAnnenForelder",
        visningsTekst = "Informasjon til forelder omfattet norsk lovgivning - har fått en søknad fra annen forelder",
    ),

    INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HAR_GJORT_VEDTAK_TIL_ANNEN_FORELDER(
        erVedtaksbrev = false,
        apiNavn = "tilForelderOmfattetNorskLovgivningHarGjortVedtakTilAnnenForelder",
        visningsTekst = "Informasjon til forelder omfattet norsk lovgivning - har gjort vedtak til annen forelder",
    ),

    INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_VARSEL_OM_ÅRLIG_KONTROLL(
        erVedtaksbrev = false,
        apiNavn = "tilForelderOmfattetNorskLovgivningVarselOmAarligKontroll",
        visningsTekst = "Informasjon til forelder omfattet norsk lovgivning - varsel om årlig kontroll",
    ),

    INFORMASJONSBREV_TIL_FORELDER_MED_SELVSTENDIG_RETT_VI_HAR_FÅTT_F016_KAN_SØKE_OM_BARNETRYGD(
        erVedtaksbrev = false,
        apiNavn = "tilForelderMedSelvstendigRettKanSokeOmBarnetrygd",
        visningsTekst = "Informasjon til forelder med selvstendig rett vi har fått F016 - kan søke om barnetrygd",
    ),

    VEDTAK_FØRSTEGANGSVEDTAK(true, "forstegangsvedtak", "Førstegangsvedtak"),
    VEDTAK_ENDRING(true, "vedtakEndring", "Vedtak endring"),
    VEDTAK_OPPHØRT(true, "opphort", "Opphørt"),
    VEDTAK_OPPHØR_MED_ENDRING(true, "opphorMedEndring", "Opphør med endring"),
    VEDTAK_AVSLAG(true, "vedtakAvslag", "Avslag"),
    VEDTAK_FORTSATT_INNVILGET(true, "vedtakFortsattInnvilget", "Vedtak fortstatt innvilget"),
    VEDTAK_KORREKSJON_VEDTAKSBREV(true, "korrigertVedtakEgenBrevmal", "Korrigere vedtak med egen brevmal"),
    VEDTAK_OPPHØR_DØDSFALL(true, "dodsfall", "Dødsfall"),
    VEDTAK_FØRSTEGANGSVEDTAK_INSTITUSJON(true, "foerstegangsvedtakInstitusjon", "Førstegangsvedtak"),
    VEDTAK_ENDRING_INSTITUSJON(true, "vedtakEndringInstitusjon", "Vedtak endring"),
    VEDTAK_OPPHØRT_INSTITUSJON(true, "vedtakOpphoerInstitusjon", "Opphørt"),
    VEDTAK_OPPHØR_MED_ENDRING_INSTITUSJON(true, "opphorMedEndringInstitusjon", "Opphør med endring"),
    VEDTAK_AVSLAG_INSTITUSJON(true, "vedtakAvslagInstitusjon", "Avslag"),
    VEDTAK_FORTSATT_INNVILGET_INSTITUSJON(true, "vedtakFortsattInnvilgetInstitusjon", "Vedtak fortstatt innvilget"),

    AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG(
        true,
        "autovedtakBarn6AarOg18AarOgSmaabarnstillegg",
        "Autovedtak - Barn 6 og 18 år og småbarnstillegg",
    ),
    AUTOVEDTAK_NYFØDT_FØRSTE_BARN(true, "autovedtakNyfodtForsteBarn", "Autovedtak nyfødt - første barn"),
    AUTOVEDTAK_NYFØDT_BARN_FRA_FØR(true, "autovedtakNyfodtBarnFraFor", "Autovedtak nyfødt - barn fra før"),
    ;

    fun skalGenerereForside(): Boolean =
        when (this) {
            INNHENTE_OPPLYSNINGER,
            INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED,
            INNHENTE_OPPLYSNINGER_OG_INFORMASJON_OM_AT_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_HAR_SØKT,
            INNHENTE_OPPLYSNINGER_INSTITUSJON,
            INFORMASJONSBREV_TIL_FORELDER_MED_SELVSTENDIG_RETT_VI_HAR_FÅTT_F016_KAN_SØKE_OM_BARNETRYGD,
            INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HAR_FÅTT_EN_SØKNAD_FRA_ANNEN_FORELDER,
            INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HAR_GJORT_VEDTAK_TIL_ANNEN_FORELDER,
            INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_VARSEL_OM_ÅRLIG_KONTROLL,
            VARSEL_OM_REVURDERING,
            VARSEL_OM_REVURDERING_INSTITUSJON,
            VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14,
            VARSEL_OM_REVURDERING_SAMBOER,
            VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED,
            VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS,
            VARSEL_OM_ÅRLIG_REVURDERING_EØS,
            VARSEL_OM_ÅRLIG_REVURDERING_EØS_MED_INNHENTING_AV_OPPLYSNINGER,
            VARSEL_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_SØKT,
            -> true

            INFORMASJONSBREV_DELT_BOSTED,
            HENLEGGE_TRUKKET_SØKNAD,
            SVARTIDSBREV,
            SVARTIDSBREV_INSTITUSJON,
            FORLENGET_SVARTIDSBREV,
            FORLENGET_SVARTIDSBREV_INSTITUSJON,
            INFORMASJONSBREV_FØDSEL_VERGEMÅL,
            INFORMASJONSBREV_FØDSEL_MINDREÅRIG,
            INFORMASJONSBREV_KAN_SØKE,
            INFORMASJONSBREV_FØDSEL_GENERELL,
            INFORMASJONSBREV_KAN_SØKE_EØS,
            -> false

            VEDTAK_FØRSTEGANGSVEDTAK,
            VEDTAK_ENDRING,
            VEDTAK_OPPHØRT,
            VEDTAK_OPPHØR_MED_ENDRING,
            VEDTAK_AVSLAG,
            VEDTAK_FORTSATT_INNVILGET,
            VEDTAK_KORREKSJON_VEDTAKSBREV,
            VEDTAK_OPPHØR_DØDSFALL,
            VEDTAK_FØRSTEGANGSVEDTAK_INSTITUSJON,
            VEDTAK_AVSLAG_INSTITUSJON,
            VEDTAK_OPPHØRT_INSTITUSJON,
            VEDTAK_ENDRING_INSTITUSJON,
            VEDTAK_FORTSATT_INNVILGET_INSTITUSJON,
            VEDTAK_OPPHØR_MED_ENDRING_INSTITUSJON,
            AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG,
            AUTOVEDTAK_NYFØDT_FØRSTE_BARN,
            AUTOVEDTAK_NYFØDT_BARN_FRA_FØR,
            -> throw Feil("Ikke avgjort om $this skal generere forside")
        }

    fun tilFamilieKontrakterDokumentType(): Dokumenttype =
        when (this) {
            INNHENTE_OPPLYSNINGER -> Dokumenttype.BARNETRYGD_INNHENTE_OPPLYSNINGER
            VARSEL_OM_REVURDERING -> Dokumenttype.BARNETRYGD_VARSEL_OM_REVURDERING
            VARSEL_OM_REVURDERING_INSTITUSJON -> Dokumenttype.BARNETRYGD_VARSEL_OM_REVURDERING_INSTITUSJON
            VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14 -> Dokumenttype.BARNETRYGD_VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14
            VARSEL_OM_REVURDERING_SAMBOER -> Dokumenttype.BARNETRYGD_VARSEL_OM_REVURDERING_SAMBOER
            INFORMASJONSBREV_DELT_BOSTED -> Dokumenttype.BARNETRYGD_INFORMASJONSBREV_DELT_BOSTED
            HENLEGGE_TRUKKET_SØKNAD -> Dokumenttype.BARNETRYGD_HENLEGGE_TRUKKET_SØKNAD
            SVARTIDSBREV -> Dokumenttype.BARNETRYGD_SVARTIDSBREV
            FORLENGET_SVARTIDSBREV -> Dokumenttype.BARNETRYGD_FORLENGET_SVARTIDSBREV
            INFORMASJONSBREV_FØDSEL_VERGEMÅL -> Dokumenttype.BARNETRYGD_INFORMASJONSBREV_FØDSEL_VERGEMÅL
            INFORMASJONSBREV_FØDSEL_MINDREÅRIG -> Dokumenttype.BARNETRYGD_INFORMASJONSBREV_FØDSEL_MINDREÅRIG
            INFORMASJONSBREV_KAN_SØKE -> Dokumenttype.BARNETRYGD_INFORMASJONSBREV_KAN_SØKE
            INFORMASJONSBREV_FØDSEL_GENERELL -> Dokumenttype.BARNETRYGD_INFORMASJONSBREV_FØDSEL_GENERELL
            INFORMASJONSBREV_TIL_FORELDER_MED_SELVSTENDIG_RETT_VI_HAR_FÅTT_F016_KAN_SØKE_OM_BARNETRYGD -> Dokumenttype.BARNETRYGD_INFORMASJONSBREV_TIL_FORELDER_MED_SELVSTENDIG_RETT_VI_HAR_FÅTT_F016_KAN_SØKE_OM_BARNETRYGD
            INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HAR_FÅTT_EN_SØKNAD_FRA_ANNEN_FORELDER -> Dokumenttype.BARNETRYGD_INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HAR_FÅTT_EN_SØKNAD_FRA_ANNEN_FORELDER
            INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HAR_GJORT_VEDTAK_TIL_ANNEN_FORELDER -> Dokumenttype.BARNETRYGD_INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HAR_GJORT_VEDTAK_TIL_ANNEN_FORELDER
            INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_VARSEL_OM_ÅRLIG_KONTROLL -> Dokumenttype.BARNETRYGD_INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_VARSEL_OM_ÅRLIG_KONTROLL
            INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED -> Dokumenttype.BARNETRYGD_INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED
            INNHENTE_OPPLYSNINGER_OG_INFORMASJON_OM_AT_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_HAR_SØKT -> Dokumenttype.BARNETRYGD_INNHENTE_OPPLYSNINGER_OG_INFORMASJON_OM_AT_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_HAR_SØKT
            VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED -> Dokumenttype.BARNETRYGD_VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED
            VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS -> Dokumenttype.BARNETRYGD_VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS
            VARSEL_OM_ÅRLIG_REVURDERING_EØS -> Dokumenttype.BARNETRYGD_VARSEL_OM_ÅRLIG_REVURDERING_EØS
            VARSEL_OM_ÅRLIG_REVURDERING_EØS_MED_INNHENTING_AV_OPPLYSNINGER -> Dokumenttype.BARNETRYGD_VARSEL_OM_ÅRLIG_REVURDERING_EØS_MED_INNHENTING_AV_OPPLYSNINGER
            INFORMASJONSBREV_KAN_SØKE_EØS -> Dokumenttype.BARNETRYGD_INFORMASJONSBREV_KAN_SØKE_EØS
            INNHENTE_OPPLYSNINGER_INSTITUSJON -> Dokumenttype.BARNETRYGD_INNHENTE_OPPLYSNINGER_INSTITUSJON
            SVARTIDSBREV_INSTITUSJON -> Dokumenttype.BARNETRYGD_SVARTIDSBREV_INSTITUSJON
            FORLENGET_SVARTIDSBREV_INSTITUSJON -> Dokumenttype.BARNETRYGD_FORLENGET_SVARTIDSBREV_INSTITUSJON
            VARSEL_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_SØKT -> Dokumenttype.BARNETRYGD_VARSEL_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_SØKT

            VEDTAK_ENDRING,
            VEDTAK_OPPHØRT,
            VEDTAK_OPPHØR_MED_ENDRING,
            VEDTAK_FORTSATT_INNVILGET,
            VEDTAK_AVSLAG,
            VEDTAK_FØRSTEGANGSVEDTAK,
            VEDTAK_KORREKSJON_VEDTAKSBREV,
            VEDTAK_OPPHØR_DØDSFALL,
            VEDTAK_FØRSTEGANGSVEDTAK_INSTITUSJON,
            VEDTAK_AVSLAG_INSTITUSJON,
            VEDTAK_OPPHØRT_INSTITUSJON,
            VEDTAK_ENDRING_INSTITUSJON,
            VEDTAK_FORTSATT_INNVILGET_INSTITUSJON,
            VEDTAK_OPPHØR_MED_ENDRING_INSTITUSJON,
            AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG,
            AUTOVEDTAK_NYFØDT_FØRSTE_BARN,
            AUTOVEDTAK_NYFØDT_BARN_FRA_FØR,
            -> throw Feil("Ingen dokumenttype for $this")
        }

    val distribusjonstype: Distribusjonstype
        get() = when (this) {
            INFORMASJONSBREV_DELT_BOSTED -> Distribusjonstype.VIKTIG
            INNHENTE_OPPLYSNINGER, INNHENTE_OPPLYSNINGER_INSTITUSJON -> Distribusjonstype.VIKTIG
            INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED -> Distribusjonstype.VIKTIG
            INNHENTE_OPPLYSNINGER_OG_INFORMASJON_OM_AT_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_HAR_SØKT -> Distribusjonstype.VIKTIG
            HENLEGGE_TRUKKET_SØKNAD -> Distribusjonstype.ANNET
            VARSEL_OM_REVURDERING -> Distribusjonstype.VIKTIG
            VARSEL_OM_REVURDERING_INSTITUSJON -> Distribusjonstype.VIKTIG
            VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14 -> Distribusjonstype.VIKTIG
            VARSEL_OM_REVURDERING_SAMBOER -> Distribusjonstype.ANNET
            VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED -> Distribusjonstype.VIKTIG
            VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS -> Distribusjonstype.VIKTIG
            VARSEL_OM_ÅRLIG_REVURDERING_EØS -> Distribusjonstype.VIKTIG
            VARSEL_OM_ÅRLIG_REVURDERING_EØS_MED_INNHENTING_AV_OPPLYSNINGER -> Distribusjonstype.VIKTIG
            VARSEL_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_SØKT -> Distribusjonstype.VIKTIG
            SVARTIDSBREV, SVARTIDSBREV_INSTITUSJON -> Distribusjonstype.ANNET
            FORLENGET_SVARTIDSBREV, FORLENGET_SVARTIDSBREV_INSTITUSJON -> Distribusjonstype.ANNET
            INFORMASJONSBREV_FØDSEL_MINDREÅRIG -> Distribusjonstype.ANNET
            INFORMASJONSBREV_FØDSEL_VERGEMÅL -> Distribusjonstype.ANNET
            INFORMASJONSBREV_KAN_SØKE -> Distribusjonstype.ANNET
            INFORMASJONSBREV_KAN_SØKE_EØS -> Distribusjonstype.ANNET
            INFORMASJONSBREV_FØDSEL_GENERELL -> Distribusjonstype.ANNET
            INFORMASJONSBREV_TIL_FORELDER_MED_SELVSTENDIG_RETT_VI_HAR_FÅTT_F016_KAN_SØKE_OM_BARNETRYGD -> Distribusjonstype.VIKTIG
            INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HAR_FÅTT_EN_SØKNAD_FRA_ANNEN_FORELDER -> Distribusjonstype.VIKTIG
            INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HAR_GJORT_VEDTAK_TIL_ANNEN_FORELDER -> Distribusjonstype.VIKTIG
            INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_VARSEL_OM_ÅRLIG_KONTROLL -> Distribusjonstype.VIKTIG
            VEDTAK_FØRSTEGANGSVEDTAK -> Distribusjonstype.VEDTAK
            VEDTAK_ENDRING -> Distribusjonstype.VEDTAK
            VEDTAK_OPPHØRT -> Distribusjonstype.VEDTAK
            VEDTAK_OPPHØR_MED_ENDRING -> Distribusjonstype.VEDTAK
            VEDTAK_AVSLAG -> Distribusjonstype.VEDTAK
            VEDTAK_FORTSATT_INNVILGET -> Distribusjonstype.VEDTAK
            VEDTAK_KORREKSJON_VEDTAKSBREV -> Distribusjonstype.VEDTAK
            VEDTAK_OPPHØR_DØDSFALL -> Distribusjonstype.VEDTAK
            VEDTAK_FØRSTEGANGSVEDTAK_INSTITUSJON -> Distribusjonstype.VEDTAK
            VEDTAK_AVSLAG_INSTITUSJON -> Distribusjonstype.VEDTAK
            VEDTAK_OPPHØRT_INSTITUSJON -> Distribusjonstype.VEDTAK
            VEDTAK_ENDRING_INSTITUSJON -> Distribusjonstype.VEDTAK
            VEDTAK_FORTSATT_INNVILGET_INSTITUSJON -> Distribusjonstype.VEDTAK
            VEDTAK_OPPHØR_MED_ENDRING_INSTITUSJON -> Distribusjonstype.VEDTAK
            AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG -> Distribusjonstype.VEDTAK
            AUTOVEDTAK_NYFØDT_FØRSTE_BARN -> Distribusjonstype.VEDTAK
            AUTOVEDTAK_NYFØDT_BARN_FRA_FØR -> Distribusjonstype.VEDTAK
        }

    fun førerTilOpplysningsplikt(): Boolean =
        when (this) {
            INNHENTE_OPPLYSNINGER,
            INNHENTE_OPPLYSNINGER_INSTITUSJON,
            VARSEL_OM_REVURDERING,
            VARSEL_OM_REVURDERING_INSTITUSJON,
            -> true

            else -> false
        }

    fun setterBehandlingPåVent(): Boolean =
        when (this) {
            FORLENGET_SVARTIDSBREV,
            INNHENTE_OPPLYSNINGER,
            INNHENTE_OPPLYSNINGER_INSTITUSJON,
            VARSEL_OM_REVURDERING,
            VARSEL_OM_REVURDERING_INSTITUSJON,
            VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14,
            INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED,
            VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS,
            VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED,
            SVARTIDSBREV,
            SVARTIDSBREV_INSTITUSJON,
            FORLENGET_SVARTIDSBREV_INSTITUSJON,
            VARSEL_OM_ÅRLIG_REVURDERING_EØS,
            VARSEL_OM_ÅRLIG_REVURDERING_EØS_MED_INNHENTING_AV_OPPLYSNINGER,
            -> true

            else -> false
        }

    fun ventefristDager(manuellFrist: Long? = null, behandlingKategori: BehandlingKategori?): Long =
        when (this) {
            INNHENTE_OPPLYSNINGER,
            INNHENTE_OPPLYSNINGER_INSTITUSJON,
            VARSEL_OM_REVURDERING,
            VARSEL_OM_REVURDERING_INSTITUSJON,
            VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14,
            INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED,
            VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS,
            VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED,
            -> 3 * 7

            SVARTIDSBREV -> when (behandlingKategori) {
                BehandlingKategori.EØS -> 30 * 3
                BehandlingKategori.NASJONAL -> 3 * 7
                else -> throw Feil("Behandlingskategori er ikke satt fot $this")
            }

            SVARTIDSBREV_INSTITUSJON -> 3 * 7
            FORLENGET_SVARTIDSBREV, FORLENGET_SVARTIDSBREV_INSTITUSJON ->
                manuellFrist
                    ?: throw Feil("Ventefrist var ikke satt for $this")

            VARSEL_OM_ÅRLIG_REVURDERING_EØS,
            VARSEL_OM_ÅRLIG_REVURDERING_EØS_MED_INNHENTING_AV_OPPLYSNINGER,
            -> 30 * 2

            else -> throw Feil("Ventefrist ikke definert for brevtype $this")
        }

    fun venteårsak() =
        when (this) {
            FORLENGET_SVARTIDSBREV,
            INNHENTE_OPPLYSNINGER,
            INNHENTE_OPPLYSNINGER_INSTITUSJON,
            VARSEL_OM_REVURDERING,
            VARSEL_OM_REVURDERING_INSTITUSJON,
            VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14,
            INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED,
            VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS,
            VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED,
            SVARTIDSBREV,
            SVARTIDSBREV_INSTITUSJON,
            FORLENGET_SVARTIDSBREV_INSTITUSJON,
            VARSEL_OM_ÅRLIG_REVURDERING_EØS,
            VARSEL_OM_ÅRLIG_REVURDERING_EØS_MED_INNHENTING_AV_OPPLYSNINGER,
            -> SettPåVentÅrsak.AVVENTER_DOKUMENTASJON

            else -> throw Feil("Venteårsak ikke definert for brevtype $this")
        }
}

interface BrevData {

    val delmalData: Any
    val flettefelter: FlettefelterForDokument
    fun toBrevString(): String = objectMapper.writeValueAsString(this)
}

interface FlettefelterForDokument {

    val navn: Flettefelt
    val fodselsnummer: Flettefelt
    val brevOpprettetDato: Flettefelt
    val organisasjonsnummer: Flettefelt
        get() = null
    val gjelder: Flettefelt
        get() = null
}

data class FlettefelterForDokumentImpl(
    override val navn: Flettefelt,
    override val fodselsnummer: Flettefelt,
    override val brevOpprettetDato: Flettefelt = flettefelt(LocalDate.now().tilDagMånedÅr()),
    override val organisasjonsnummer: Flettefelt,
    override val gjelder: Flettefelt,
) : FlettefelterForDokument {

    constructor(
        navn: String,
        fodselsnummer: String,
        organisasjonsnummer: String? = null,
        gjelder: String? = null,
    ) : this(
        navn = flettefelt(navn),
        fodselsnummer = flettefelt(fodselsnummer),
        organisasjonsnummer = flettefelt(organisasjonsnummer),
        gjelder = flettefelt(gjelder),
    )
}

typealias Flettefelt = List<String>?

fun flettefelt(flettefeltData: String?): Flettefelt = if (flettefeltData != null) listOf(flettefeltData) else null
fun flettefelt(flettefeltData: List<String>): Flettefelt = flettefeltData
