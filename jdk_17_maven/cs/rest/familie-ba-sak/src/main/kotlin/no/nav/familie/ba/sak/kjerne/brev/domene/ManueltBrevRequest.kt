package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.Utils.storForbokstav
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.EnkeltInformasjonsbrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.FlettefelterForDokumentImpl
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.ForlengetSvartidsbrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.HenleggeTrukketSøknadBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.HenleggeTrukketSøknadData
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.InformasjonsbrevDeltBostedBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.InformasjonsbrevDeltBostedData
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.InformasjonsbrevKanSøke
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.InformasjonsbrevTilForelderBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.InformasjonsbrevTilForelderData
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.InnhenteOpplysningerBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.InnhenteOpplysningerData
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.InnhenteOpplysningerOmBarn
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.SignaturDelmal
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Svartidsbrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.VarselOmRevurderingDeltBostedParagraf14Brev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.VarselOmRevurderingDeltBostedParagraf14Data
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.VarselOmRevurderingSamboerBrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.VarselOmRevurderingSamboerData
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.VarselbrevMedÅrsaker
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.VarselbrevÅrlegKontrollEøs
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.VarselbrevMedÅrsakerOgBarn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import java.time.LocalDate

interface Person {
    val navn: String
    val fødselsnummer: String
}

data class ManueltBrevRequest(
    val brevmal: Brevmal,
    val multiselectVerdier: List<String> = emptyList(),
    val mottakerIdent: String,
    val barnIBrev: List<String> = emptyList(),
    val datoAvtale: String? = null,
    // Settes av backend ved utsending fra behandling
    val mottakerMålform: Målform = Målform.NB,
    val mottakerNavn: String = "",
    val enhet: Enhet? = null,
    val antallUkerSvarfrist: Int? = null,
    val barnasFødselsdager: List<LocalDate>? = null,
    val behandlingKategori: BehandlingKategori? = null,
    val vedrørende: Person? = null,
    val mottakerlandSed: List<String> = emptyList(),
) {

    override fun toString(): String {
        return "${ManueltBrevRequest::class}, $brevmal"
    }

    fun enhetNavn(): String = this.enhet?.enhetNavn ?: error("Finner ikke enhetsnavn på manuell brevrequest")

    fun mottakerlandSED(): List<String> {
        if (this.mottakerlandSed.contains("NO")) {
            throw FunksjonellFeil(
                frontendFeilmelding = "Norge kan ikke velges som mottakerland.",
                melding = "Ugyldig mottakerland for brevtype 'varsel om årlig revurdering EØS'",
            )
        }
        return this.mottakerlandSed.takeIf { it.isNotEmpty() }
            ?: error("Finner ikke noen mottakerland for SED på manuell brevrequest")
    }
}

fun ManueltBrevRequest.byggMottakerdata(
    behandling: Behandling,
    persongrunnlagService: PersongrunnlagService,
    arbeidsfordelingService: ArbeidsfordelingService,
): ManueltBrevRequest {
    val hentPerson = { ident: String ->
        persongrunnlagService.hentPersonerPåBehandling(listOf(ident), behandling).singleOrNull()
            ?: error("Fant flere eller ingen personer med angitt personident på behandling $behandling")
    }
    val enhet = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandling.id).run {
        Enhet(enhetId = behandlendeEnhetId, enhetNavn = behandlendeEnhetNavn)
    }
    return when {
        erTilInstitusjon ->
            this.copy(
                enhet = enhet,
                vedrørende = object : Person {
                    override val fødselsnummer = behandling.fagsak.aktør.aktivFødselsnummer()
                    override val navn = hentPerson(fødselsnummer).navn
                },
            )

        else -> hentPerson(mottakerIdent).let { mottakerPerson ->
            this.copy(
                enhet = enhet,
                mottakerMålform = mottakerPerson.målform,
                mottakerNavn = mottakerPerson.navn,
            )
        }
    }
}

fun ManueltBrevRequest.leggTilEnhet(arbeidsfordelingService: ArbeidsfordelingService): ManueltBrevRequest {
    val arbeidsfordelingsenhet = arbeidsfordelingService.hentArbeidsfordelingsenhetPåIdenter(
        søkerIdent = mottakerIdent,
        barnIdenter = barnIBrev,
    )
    return this.copy(
        enhet = Enhet(
            enhetNavn = arbeidsfordelingsenhet.enhetNavn,
            enhetId = arbeidsfordelingsenhet.enhetId,
        ),
    )
}

fun ManueltBrevRequest.tilBrev(saksbehandlerNavn: String, hentLandkoder: (() -> Map<String, String>)): Brev {
    val signaturDelmal = SignaturDelmal(
        enhet = this.enhetNavn(),
        saksbehandlerNavn = saksbehandlerNavn,
    )

    return when (this.brevmal) {
        Brevmal.INFORMASJONSBREV_DELT_BOSTED ->
            InformasjonsbrevDeltBostedBrev(
                data = InformasjonsbrevDeltBostedData(
                    delmalData = InformasjonsbrevDeltBostedData.DelmalData(
                        signatur = signaturDelmal,
                    ),
                    flettefelter = InformasjonsbrevDeltBostedData.Flettefelter(
                        navn = this.mottakerNavn,
                        fodselsnummer = this.mottakerIdent,
                        barnMedDeltBostedAvtale = this.multiselectVerdier,
                    ),
                ),
            )

        Brevmal.INFORMASJONSBREV_TIL_FORELDER_MED_SELVSTENDIG_RETT_VI_HAR_FÅTT_F016_KAN_SØKE_OM_BARNETRYGD,
        Brevmal.INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HAR_FÅTT_EN_SØKNAD_FRA_ANNEN_FORELDER,
        Brevmal.INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_HAR_GJORT_VEDTAK_TIL_ANNEN_FORELDER,
        Brevmal.INFORMASJONSBREV_TIL_FORELDER_OMFATTET_NORSK_LOVGIVNING_VARSEL_OM_ÅRLIG_KONTROLL,
        ->
            InformasjonsbrevTilForelderBrev(
                mal = this.brevmal,
                data = InformasjonsbrevTilForelderData(
                    delmalData = InformasjonsbrevTilForelderData.DelmalData(
                        signatur = signaturDelmal,
                    ),
                    flettefelter = InformasjonsbrevTilForelderData.Flettefelter(
                        navn = this.mottakerNavn,
                        fodselsnummer = this.mottakerIdent,
                        barnSøktFor = this.multiselectVerdier,
                    ),
                ),
            )

        Brevmal.INNHENTE_OPPLYSNINGER,
        Brevmal.INNHENTE_OPPLYSNINGER_INSTITUSJON,
        ->
            InnhenteOpplysningerBrev(
                mal = brevmal,
                data = InnhenteOpplysningerData(
                    delmalData = InnhenteOpplysningerData.DelmalData(signatur = signaturDelmal),
                    flettefelter = InnhenteOpplysningerData.Flettefelter(
                        navn = this.mottakerNavn,
                        fodselsnummer = this.vedrørende?.fødselsnummer ?: mottakerIdent,
                        organisasjonsnummer = if (erTilInstitusjon) mottakerIdent else null,
                        gjelder = this.vedrørende?.navn,
                        dokumentliste = this.multiselectVerdier,
                    ),
                ),
            )

        Brevmal.HENLEGGE_TRUKKET_SØKNAD ->
            HenleggeTrukketSøknadBrev(
                data = HenleggeTrukketSøknadData(
                    delmalData = HenleggeTrukketSøknadData.DelmalData(signatur = signaturDelmal),
                    flettefelter = FlettefelterForDokumentImpl(
                        navn = this.mottakerNavn,
                        fodselsnummer = this.mottakerIdent,
                    ),
                ),
            )

        Brevmal.VARSEL_OM_REVURDERING ->
            VarselbrevMedÅrsaker(
                mal = Brevmal.VARSEL_OM_REVURDERING,
                navn = this.mottakerNavn,
                fødselsnummer = this.mottakerIdent,
                varselÅrsaker = this.multiselectVerdier,
                enhet = this.enhetNavn(),
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.VARSEL_OM_REVURDERING_INSTITUSJON ->
            VarselbrevMedÅrsaker(
                mal = Brevmal.VARSEL_OM_REVURDERING_INSTITUSJON,
                navn = this.mottakerNavn,
                fødselsnummer = this.vedrørende?.fødselsnummer ?: mottakerIdent,
                varselÅrsaker = this.multiselectVerdier,
                enhet = this.enhetNavn(),
                organisasjonsnummer = mottakerIdent,
                gjelder = this.vedrørende?.navn,
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.VARSEL_OM_REVURDERING_DELT_BOSTED_PARAGRAF_14 ->
            VarselOmRevurderingDeltBostedParagraf14Brev(
                data = VarselOmRevurderingDeltBostedParagraf14Data(
                    delmalData = VarselOmRevurderingDeltBostedParagraf14Data.DelmalData(signatur = signaturDelmal),
                    flettefelter = VarselOmRevurderingDeltBostedParagraf14Data.Flettefelter(
                        navn = this.mottakerNavn,
                        fodselsnummer = this.mottakerIdent,
                        barnMedDeltBostedAvtale = this.multiselectVerdier,
                    ),
                ),
            )

        Brevmal.VARSEL_OM_REVURDERING_SAMBOER ->
            if (this.datoAvtale == null) {
                throw FunksjonellFeil(
                    frontendFeilmelding = "Du må sette dato for samboerskap for å sende dette brevet.",
                    melding = "Dato er ikke satt for brevtype 'varsel om revurdering samboer'",
                )
            } else {
                VarselOmRevurderingSamboerBrev(
                    data = VarselOmRevurderingSamboerData(
                        delmalData = VarselOmRevurderingSamboerData.DelmalData(signatur = signaturDelmal),
                        flettefelter = VarselOmRevurderingSamboerData.Flettefelter(
                            navn = this.mottakerNavn,
                            fodselsnummer = this.mottakerIdent,
                            datoAvtale = LocalDate.parse(this.datoAvtale).tilDagMånedÅr(),
                        ),
                    ),
                )
            }

        Brevmal.VARSEL_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_SØKT ->
            VarselbrevMedÅrsakerOgBarn(
                mal = Brevmal.VARSEL_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_SØKT,
                navn = this.mottakerNavn,
                fødselsnummer = this.vedrørende?.fødselsnummer ?: mottakerIdent,
                varselÅrsaker = this.multiselectVerdier,
                barnasFødselsdager = this.barnasFødselsdager.tilFormaterteFødselsdager(),
                enhet = this.enhetNavn(),
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.SVARTIDSBREV ->
            Svartidsbrev(
                navn = this.mottakerNavn,
                fodselsnummer = this.mottakerIdent,
                enhet = this.enhetNavn(),
                mal = Brevmal.SVARTIDSBREV,
                erEøsBehandling = if (this.behandlingKategori == null) {
                    throw Feil("Trenger å vite om behandling er EØS for å sende ut svartidsbrev.")
                } else {
                    this.behandlingKategori == BehandlingKategori.EØS
                },
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.SVARTIDSBREV_INSTITUSJON ->
            Svartidsbrev(
                navn = this.mottakerNavn,
                fodselsnummer = this.vedrørende?.fødselsnummer ?: mottakerIdent,
                enhet = this.enhetNavn(),
                mal = Brevmal.SVARTIDSBREV_INSTITUSJON,
                erEøsBehandling = false,
                organisasjonsnummer = mottakerIdent,
                gjelder = this.vedrørende?.navn,
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.FORLENGET_SVARTIDSBREV,
        Brevmal.FORLENGET_SVARTIDSBREV_INSTITUSJON,
        ->
            ForlengetSvartidsbrev(
                mal = brevmal,
                navn = this.mottakerNavn,
                fodselsnummer = this.vedrørende?.fødselsnummer ?: mottakerIdent,
                enhetNavn = this.enhetNavn(),
                årsaker = this.multiselectVerdier,
                antallUkerSvarfrist = this.antallUkerSvarfrist ?: throw FunksjonellFeil(
                    melding = "Antall uker svarfrist er ikke satt",
                    frontendFeilmelding = "Antall uker svarfrist er ikke satt",
                ),
                organisasjonsnummer = if (erTilInstitusjon) mottakerIdent else null,
                gjelder = this.vedrørende?.navn,
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.INFORMASJONSBREV_FØDSEL_MINDREÅRIG ->
            EnkeltInformasjonsbrev(
                navn = this.mottakerNavn,
                fodselsnummer = this.mottakerIdent,
                enhet = this.enhetNavn(),
                mal = Brevmal.INFORMASJONSBREV_FØDSEL_MINDREÅRIG,
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.INFORMASJONSBREV_FØDSEL_VERGEMÅL ->
            EnkeltInformasjonsbrev(
                navn = this.mottakerNavn,
                fodselsnummer = this.mottakerIdent,
                enhet = this.enhetNavn(),
                mal = Brevmal.INFORMASJONSBREV_FØDSEL_VERGEMÅL,
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.INFORMASJONSBREV_FØDSEL_GENERELL ->
            EnkeltInformasjonsbrev(
                navn = this.mottakerNavn,
                fodselsnummer = this.mottakerIdent,
                enhet = this.enhetNavn(),
                mal = Brevmal.INFORMASJONSBREV_FØDSEL_GENERELL,
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.INFORMASJONSBREV_KAN_SØKE ->
            InformasjonsbrevKanSøke(
                navn = this.mottakerNavn,
                fodselsnummer = this.mottakerIdent,
                enhet = this.enhetNavn(),
                dokumentliste = this.multiselectVerdier,
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED ->
            VarselbrevMedÅrsakerOgBarn(
                mal = Brevmal.VARSEL_OM_VEDTAK_ETTER_SØKNAD_I_SED,
                navn = this.mottakerNavn,
                fødselsnummer = this.mottakerIdent,
                enhet = this.enhetNavn(),
                varselÅrsaker = this.multiselectVerdier,
                barnasFødselsdager = this.barnasFødselsdager.tilFormaterteFødselsdager(),
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS ->
            VarselbrevMedÅrsaker(
                mal = Brevmal.VARSEL_OM_REVURDERING_FRA_NASJONAL_TIL_EØS,
                navn = this.mottakerNavn,
                fødselsnummer = this.mottakerIdent,
                varselÅrsaker = this.multiselectVerdier,
                enhet = this.enhetNavn(),
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.VARSEL_OM_ÅRLIG_REVURDERING_EØS ->
            VarselbrevÅrlegKontrollEøs(
                mal = Brevmal.VARSEL_OM_ÅRLIG_REVURDERING_EØS,
                navn = this.mottakerNavn,
                fødselsnummer = this.mottakerIdent,
                enhet = this.enhetNavn(),
                mottakerlandSed = Utils.slåSammen(this.mottakerlandSED().map { tilLandNavn(hentLandkoder(), it) }),
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.VARSEL_OM_ÅRLIG_REVURDERING_EØS_MED_INNHENTING_AV_OPPLYSNINGER ->
            VarselbrevÅrlegKontrollEøs(
                mal = Brevmal.VARSEL_OM_ÅRLIG_REVURDERING_EØS_MED_INNHENTING_AV_OPPLYSNINGER,
                navn = this.mottakerNavn,
                fødselsnummer = this.mottakerIdent,
                enhet = this.enhetNavn(),
                mottakerlandSed = Utils.slåSammen(this.mottakerlandSED().map { tilLandNavn(hentLandkoder(), it) }),
                dokumentliste = this.multiselectVerdier,
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED ->
            InnhenteOpplysningerOmBarn(
                mal = Brevmal.INNHENTE_OPPLYSNINGER_ETTER_SØKNAD_I_SED,
                navn = this.mottakerNavn,
                fødselsnummer = this.mottakerIdent,
                dokumentliste = this.multiselectVerdier,
                enhet = this.enhetNavn(),
                barnasFødselsdager = this.barnasFødselsdager.tilFormaterteFødselsdager(),
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.INNHENTE_OPPLYSNINGER_OG_INFORMASJON_OM_AT_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_HAR_SØKT ->
            InnhenteOpplysningerOmBarn(
                mal = Brevmal.INNHENTE_OPPLYSNINGER_OG_INFORMASJON_OM_AT_ANNEN_FORELDER_MED_SELVSTENDIG_RETT_HAR_SØKT,
                navn = this.mottakerNavn,
                fødselsnummer = this.mottakerIdent,
                dokumentliste = this.multiselectVerdier,
                enhet = this.enhetNavn(),
                barnasFødselsdager = this.barnasFødselsdager.tilFormaterteFødselsdager(),
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.INFORMASJONSBREV_KAN_SØKE_EØS ->
            EnkeltInformasjonsbrev(
                navn = this.mottakerNavn,
                fodselsnummer = this.mottakerIdent,
                enhet = this.enhetNavn(),
                mal = Brevmal.INFORMASJONSBREV_KAN_SØKE_EØS,
                saksbehandlerNavn = saksbehandlerNavn,
            )

        Brevmal.VEDTAK_FØRSTEGANGSVEDTAK,
        Brevmal.VEDTAK_ENDRING,
        Brevmal.VEDTAK_OPPHØRT,
        Brevmal.VEDTAK_OPPHØR_MED_ENDRING,
        Brevmal.VEDTAK_AVSLAG,
        Brevmal.VEDTAK_FORTSATT_INNVILGET,
        Brevmal.VEDTAK_KORREKSJON_VEDTAKSBREV,
        Brevmal.VEDTAK_OPPHØR_DØDSFALL,
        Brevmal.VEDTAK_FØRSTEGANGSVEDTAK_INSTITUSJON,
        Brevmal.VEDTAK_AVSLAG_INSTITUSJON,
        Brevmal.VEDTAK_OPPHØRT_INSTITUSJON,
        Brevmal.VEDTAK_ENDRING_INSTITUSJON,
        Brevmal.VEDTAK_FORTSATT_INNVILGET_INSTITUSJON,
        Brevmal.VEDTAK_OPPHØR_MED_ENDRING_INSTITUSJON,
        Brevmal.AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG,
        Brevmal.AUTOVEDTAK_NYFØDT_FØRSTE_BARN,
        Brevmal.AUTOVEDTAK_NYFØDT_BARN_FRA_FØR,
        -> throw Feil("Kan ikke mappe fra manuel brevrequest til ${this.brevmal}.")
    }
}

private fun tilLandNavn(landkoderISO2: Map<String, String>, landKode: String): String {
    if (landKode.length != 2) {
        throw Feil("LandkoderISO2 forventer en landkode med to tegn")
    }

    val landNavn = (
        landkoderISO2[landKode]
            ?: throw Feil("Fant ikke navn for landkode $landKode ")
        )

    return landNavn.storForbokstav()
}

private fun List<LocalDate>?.tilFormaterteFødselsdager() = Utils.slåSammen(
    this?.map { it.tilKortString() }
        ?: throw Feil("Fikk ikke med barna sine fødselsdager"),
)

val ManueltBrevRequest.erTilInstitusjon
    get() = when {
        erOrgNr(mottakerIdent) -> true
        else -> false
    }

private fun erOrgNr(ident: String): Boolean = ident.length == 9 && ident.all { it.isDigit() }
