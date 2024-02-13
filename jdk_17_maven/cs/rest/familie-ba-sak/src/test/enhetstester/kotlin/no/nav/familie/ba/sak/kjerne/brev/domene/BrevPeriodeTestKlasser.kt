import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.domene.LandNavn
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertAnnenVurdering
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertKompetanse
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertRestEndretAndel
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertRestPersonResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertUregistrertBarn
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertUtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertVilkårResultat
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.EØSBegrunnelseDataMedKompetanse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.MinimertRestPerson
import no.nav.familie.ba.sak.kjerne.vedtak.domene.SøkersRettTilUtvidet
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import java.math.BigDecimal
import java.time.LocalDate

data class BrevPeriodeTestConfig(
    val beskrivelse: String,

    val fom: LocalDate?,
    val tom: LocalDate?,
    val vedtaksperiodetype: Vedtaksperiodetype,
    val begrunnelser: List<Standardbegrunnelse>,
    val eøsBegrunnelser: List<EØSStandardbegrunnelse>?,
    val fritekster: List<String>,

    val personerPåBehandling: List<BrevPeriodeTestPerson>,

    val uregistrerteBarn: List<MinimertUregistrertBarn>,
    val erFørsteVedtaksperiodePåFagsak: Boolean = false,
    val brevMålform: Målform,

    val kompetanser: List<BrevPeriodeTestKompetanse>? = null,
    val kompetanserSomStopperRettFørPeriode: List<BrevPeriodeTestKompetanse>? = null,

    val forventetOutput: BrevPeriodeOutput?,
) {
    fun hentPersonerMedReduksjonFraForrigeBehandling(): List<BrevPeriodeTestPerson> =
        this.personerPåBehandling.filter { it.harReduksjonFraForrigeBehandling }

    fun hentBarnMedReduksjonFraForrigeBehandling() =
        hentPersonerMedReduksjonFraForrigeBehandling().filter { it.type == PersonType.BARN }
}

data class BrevPeriodeTestKompetanse(
    val id: String,
    val søkersAktivitet: KompetanseAktivitet,
    val søkersAktivitetsland: String?,
    val annenForeldersAktivitet: KompetanseAktivitet,
    val annenForeldersAktivitetsland: String,
    val barnetsBostedsland: String,
    val resultat: KompetanseResultat,
) {
    fun tilMinimertKompetanse(personer: List<BrevPeriodeTestPerson>): MinimertKompetanse {
        return MinimertKompetanse(
            søkersAktivitet = this.søkersAktivitet,
            annenForeldersAktivitet = this.annenForeldersAktivitet,
            annenForeldersAktivitetslandNavn = LandNavn(this.annenForeldersAktivitetsland),
            barnetsBostedslandNavn = LandNavn(this.barnetsBostedsland),
            resultat = this.resultat,
            personer = personer.filter { it.kompetanseIder?.contains(this.id) == true }.map { it.tilMinimertPerson() },
            søkersAktivitetsland = this.søkersAktivitetsland?.let { LandNavn(this.søkersAktivitetsland) },
        )
    }
}

data class BrevPeriodeTestPerson(
    val personIdent: String = randomFnr(),
    val fødselsdato: LocalDate,
    val type: PersonType,
    val overstyrteVilkårresultater: List<MinimertVilkårResultat>,
    val andreVurderinger: List<MinimertAnnenVurdering>,
    val endredeUtbetalinger: List<EndretRestUtbetalingAndelPåPerson>,
    val utbetalinger: List<UtbetalingPåPerson>,
    val harReduksjonFraForrigeBehandling: Boolean = false,
    val kompetanseIder: List<String>? = null,
) {
    fun tilMinimertPerson() = MinimertRestPerson(personIdent = personIdent, fødselsdato = fødselsdato, type = type)
    fun tilUtbetalingsperiodeDetaljer() = utbetalinger.map {
        it.tilMinimertUtbetalingsperiodeDetalj(this.tilMinimertPerson())
    }

    fun tilMinimerteEndredeUtbetalingAndeler() =
        endredeUtbetalinger.map { it.tilMinimertRestEndretUtbetalingAndel(this.personIdent) }

    fun tilMinimertePersonResultater(): MinimertRestPersonResultat {
        return MinimertRestPersonResultat(
            personIdent = this.personIdent,
            minimerteVilkårResultater = hentVilkårForPerson(),
            minimerteAndreVurderinger = this.andreVurderinger,
        )
    }

    private fun hentVilkårForPerson() =
        this.overstyrteVilkårresultater +
            Vilkår.hentVilkårFor(
                personType = this.type,
                fagsakType = FagsakType.NORMAL,
                behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
            )
                .filter { vilkår -> !this.overstyrteVilkårresultater.any { it.vilkårType == vilkår } }
                .map { vilkår ->
                    MinimertVilkårResultat(
                        vilkårType = vilkår,
                        periodeFom = this.fødselsdato,
                        periodeTom = null,
                        resultat = Resultat.OPPFYLT,
                        utdypendeVilkårsvurderinger = emptyList(),
                        erEksplisittAvslagPåSøknad = false,
                        standardbegrunnelser = emptyList(),
                    )
                }
}

data class UtbetalingPåPerson(
    val ytelseType: YtelseType,
    val utbetaltPerMnd: Int,
    val erPåvirketAvEndring: Boolean = false,
    val prosent: BigDecimal = BigDecimal(100),
    val endringsårsak: Årsak? = null,
) {
    fun tilMinimertUtbetalingsperiodeDetalj(minimertRestPerson: MinimertRestPerson) =
        MinimertUtbetalingsperiodeDetalj(
            person = minimertRestPerson,
            utbetaltPerMnd = this.utbetaltPerMnd,
            prosent = this.prosent,
            erPåvirketAvEndring = this.erPåvirketAvEndring,
            ytelseType = this.ytelseType,
            endringsårsak = endringsårsak,
        )
}

data class EndretRestUtbetalingAndelPåPerson(
    val periode: MånedPeriode,
    val årsak: Årsak,
    val søknadstidspunkt: LocalDate = LocalDate.now(),
    val avtaletidspunktDeltBosted: LocalDate? = null,
) {
    fun tilMinimertRestEndretUtbetalingAndel(personIdent: String) =
        MinimertRestEndretAndel(
            personIdent = personIdent,
            periode = periode,
            årsak = årsak,
            søknadstidspunkt = søknadstidspunkt,
            avtaletidspunktDeltBosted = avtaletidspunktDeltBosted,
        )
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    defaultImpl = BegrunnelseDataTestConfig::class,
)
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(value = FritekstBegrunnelseTestConfig::class, name = "fritekst"),
        JsonSubTypes.Type(value = EØSBegrunnelseTestConfig::class, name = "eøsbegrunnelse"),
    ],
)
interface TestBegrunnelse

data class FritekstBegrunnelseTestConfig(val fritekst: String) : TestBegrunnelse

data class BegrunnelseDataTestConfig(
    val gjelderSoker: Boolean,
    val barnasFodselsdatoer: String,
    val fodselsdatoerBarnOppfyllerTriggereOgHarUtbetaling: String,
    val fodselsdatoerBarnOppfyllerTriggereOgHarNullutbetaling: String,
    val antallBarn: Int,
    val antallBarnOppfyllerTriggereOgHarUtbetaling: Int,
    val antallBarnOppfyllerTriggereOgHarNullutbetaling: Int,
    val maanedOgAarBegrunnelsenGjelderFor: String?,
    val maalform: String,
    val apiNavn: String,
    val belop: Int,
    val soknadstidspunkt: String?,
    val avtaletidspunktDeltBosted: String?,
    val sokersRettTilUtvidet: String?,
) : TestBegrunnelse {

    fun tilBegrunnelseData() = BegrunnelseData(
        belop = Utils.formaterBeløp(this.belop),
        gjelderSoker = this.gjelderSoker,
        barnasFodselsdatoer = this.barnasFodselsdatoer,
        fodselsdatoerBarnOppfyllerTriggereOgHarUtbetaling = this.fodselsdatoerBarnOppfyllerTriggereOgHarUtbetaling,
        fodselsdatoerBarnOppfyllerTriggereOgHarNullutbetaling = this.fodselsdatoerBarnOppfyllerTriggereOgHarNullutbetaling,
        antallBarn = this.antallBarn,
        antallBarnOppfyllerTriggereOgHarUtbetaling = this.antallBarnOppfyllerTriggereOgHarUtbetaling,
        antallBarnOppfyllerTriggereOgHarNullutbetaling = this.antallBarnOppfyllerTriggereOgHarNullutbetaling,
        maanedOgAarBegrunnelsenGjelderFor = this.maanedOgAarBegrunnelsenGjelderFor,
        maalform = this.maalform,
        apiNavn = this.apiNavn,
        soknadstidspunkt = this.soknadstidspunkt ?: "",
        avtaletidspunktDeltBosted = this.avtaletidspunktDeltBosted ?: "",
        sokersRettTilUtvidet = this.sokersRettTilUtvidet
            ?: SøkersRettTilUtvidet.SØKER_HAR_IKKE_RETT.tilSanityFormat(),
        vedtakBegrunnelseType = Standardbegrunnelse.values()
            .find { it.sanityApiNavn == this.apiNavn }?.vedtakBegrunnelseType
            ?: throw Feil("Fant ikke Standardbegrunnelse med apiNavn ${this.apiNavn}"),
    )
}

data class EØSBegrunnelseTestConfig(
    val apiNavn: String,
    val annenForeldersAktivitet: KompetanseAktivitet,
    val annenForeldersAktivitetsland: String,
    val barnetsBostedsland: String,
    val barnasFodselsdatoer: String,
    val antallBarn: Int,
    val maalform: String,
    val sokersAktivitet: KompetanseAktivitet,
    val sokersAktivitetsland: String?,
) : TestBegrunnelse {
    fun tilEØSBegrunnelseData(): EØSBegrunnelseDataMedKompetanse = EØSBegrunnelseDataMedKompetanse(
        apiNavn = this.apiNavn,
        annenForeldersAktivitet = this.annenForeldersAktivitet,
        annenForeldersAktivitetsland = this.annenForeldersAktivitetsland,
        barnetsBostedsland = this.barnetsBostedsland,
        barnasFodselsdatoer = this.barnasFodselsdatoer,
        antallBarn = this.antallBarn,
        maalform = this.maalform,
        vedtakBegrunnelseType = EØSStandardbegrunnelse.values()
            .find { it.sanityApiNavn == this.apiNavn }?.vedtakBegrunnelseType
            ?: throw Feil("Fant ikke EØSStandardbegrunnelse med apiNavn ${this.apiNavn}"),
        sokersAktivitet = this.sokersAktivitet,
        sokersAktivitetsland = this.sokersAktivitetsland,
    )
}

data class BrevPeriodeOutput(
    val fom: String?,
    val tom: String?,
    val belop: Int?,
    val antallBarn: String?,
    val barnasFodselsdager: String?,
    val begrunnelser: List<TestBegrunnelse>,
    val type: String,
)
