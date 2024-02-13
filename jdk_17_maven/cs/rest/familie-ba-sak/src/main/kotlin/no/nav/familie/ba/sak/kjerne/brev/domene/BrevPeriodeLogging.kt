package no.nav.familie.ba.sak.kjerne.brev.domene

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import java.math.BigDecimal
import java.time.LocalDate

data class BrevPeriodeForLogging(
    val fom: LocalDate?,
    val tom: LocalDate?,
    val vedtaksperiodetype: Vedtaksperiodetype,
    val begrunnelser: List<BrevBegrunnelseGrunnlagForLogging>,
    val fritekster: List<String>,

    val personerPåBehandling: List<BrevPeriodePersonForLogging>,

    val uregistrerteBarn: List<MinimertUregistrertBarn>,
    val erFørsteVedtaksperiodePåFagsak: Boolean = false,
    val brevMålform: Målform,
)

data class BrevPeriodePersonForLogging(
    val fødselsdato: LocalDate,
    val type: PersonType,
    val overstyrteVilkårresultater: List<MinimertVilkårResultat>,
    val andreVurderinger: List<MinimertAnnenVurdering>,
    val endredeUtbetalinger: List<EndretUtbetalingAndelPåPersonForLogging>,
    val utbetalinger: List<UtbetalingPåPersonForLogging>,
    val harReduksjonFraForrigeBehandling: Boolean,
)

data class UtbetalingPåPersonForLogging(
    val ytelseType: YtelseType,
    val utbetaltPerMnd: Int,
    val erPåvirketAvEndring: Boolean,
    val prosent: BigDecimal,
)

data class EndretUtbetalingAndelPåPersonForLogging(
    val periode: MånedPeriode,
    val årsak: Årsak,
)

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    defaultImpl = BegrunnelseDataForLogging::class,
)
@JsonSubTypes(value = [JsonSubTypes.Type(value = FritekstBegrunnelseTestForLogging::class, name = "fritekst")])
interface TestBegrunnelse

data class FritekstBegrunnelseTestForLogging(val fritekst: String) : TestBegrunnelse

data class BegrunnelseDataForLogging(
    val gjelderSoker: Boolean,
    val barnasFodselsdatoer: String,
    val antallBarn: Int,
    val maanedOgAarBegrunnelsenGjelderFor: String?,
    val maalform: String,
    val apiNavn: String,
    val belop: Int,
) : TestBegrunnelse

data class BrevBegrunnelseGrunnlagForLogging(
    val standardbegrunnelse: IVedtakBegrunnelse,
)
