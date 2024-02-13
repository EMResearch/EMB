package no.nav.familie.tilbake.api.dto

import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.SærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import java.math.BigDecimal

data class VurdertVilkårsvurderingDto(
    val perioder: List<VurdertVilkårsvurderingsperiodeDto>,
    val rettsgebyr: Long,
)

data class VurdertVilkårsvurderingsperiodeDto(
    val periode: Datoperiode,
    val feilutbetaltBeløp: BigDecimal,
    val hendelsestype: Hendelsestype,
    val reduserteBeløper: List<RedusertBeløpDto> = listOf(),
    val aktiviteter: List<AktivitetDto> = listOf(),
    val vilkårsvurderingsresultatInfo: VurdertVilkårsvurderingsresultatDto? = null,
    val begrunnelse: String? = null,
    val foreldet: Boolean,
)

data class VurdertVilkårsvurderingsresultatDto(
    val vilkårsvurderingsresultat: Vilkårsvurderingsresultat? = null,
    val godTro: VurdertGodTroDto? = null,
    val aktsomhet: VurdertAktsomhetDto? = null,
)

data class VurdertGodTroDto(
    val beløpErIBehold: Boolean,
    val beløpTilbakekreves: BigDecimal? = null,
    val begrunnelse: String,
)

data class VurdertAktsomhetDto(
    val aktsomhet: Aktsomhet,
    val ileggRenter: Boolean? = null,
    val andelTilbakekreves: BigDecimal? = null,
    val beløpTilbakekreves: BigDecimal? = null,
    val begrunnelse: String,
    val særligeGrunner: List<VurdertSærligGrunnDto>? = null,
    val særligeGrunnerTilReduksjon: Boolean = false,
    val tilbakekrevSmåbeløp: Boolean = true,
    val særligeGrunnerBegrunnelse: String? = null,
)

data class VurdertSærligGrunnDto(
    val særligGrunn: SærligGrunn,
    val begrunnelse: String? = null,
)

data class RedusertBeløpDto(val trekk: Boolean, val beløp: BigDecimal)

data class AktivitetDto(val aktivitet: String, val beløp: BigDecimal)
