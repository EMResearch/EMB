package no.nav.familie.tilbake.beregning

import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.beregning.modell.Beregningsresultatsperiode
import no.nav.familie.tilbake.beregning.modell.FordeltKravgrunnlagsbeløp
import no.nav.familie.tilbake.beregning.modell.GrunnlagsperiodeMedSkatteprosent
import no.nav.familie.tilbake.common.isZero
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.AnnenVurdering
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingAktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingGodTro
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsperiode
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vurdering
import java.math.BigDecimal
import java.math.RoundingMode

internal object TilbakekrevingsberegningVilkår {

    private val HUNDRE_PROSENT: BigDecimal = BigDecimal.valueOf(100)
    private val RENTESATS: BigDecimal = BigDecimal.valueOf(10)
    private val RENTEFAKTOR: BigDecimal = RENTESATS.divide(HUNDRE_PROSENT, 2, RoundingMode.UNNECESSARY)

    fun beregn(
        vilkårVurdering: Vilkårsvurderingsperiode,
        delresultat: FordeltKravgrunnlagsbeløp,
        perioderMedSkatteprosent: List<GrunnlagsperiodeMedSkatteprosent>,
        beregnRenter: Boolean,
        bruk6desimalerISkatteberegning: Boolean = false,
    ): Beregningsresultatsperiode {
        val periode: Månedsperiode = vilkårVurdering.periode
        val vurdering: Vurdering = finnVurdering(vilkårVurdering)
        val renter = beregnRenter && finnRenter(vilkårVurdering)
        val andel: BigDecimal? = finnAndelAvBeløp(vilkårVurdering)
        val manueltBeløp: BigDecimal? = finnManueltSattBeløp(vilkårVurdering)
        val ignoreresPgaLavtBeløp = false == vilkårVurdering.aktsomhet?.tilbakekrevSmåbeløp
        val beløpUtenRenter: BigDecimal =
            if (ignoreresPgaLavtBeløp) {
                BigDecimal.ZERO
            } else {
                finnBeløpUtenRenter(
                    delresultat.feilutbetaltBeløp,
                    andel,
                    manueltBeløp,
                )
            }
        val rentebeløp: BigDecimal = beregnRentebeløp(beløpUtenRenter, renter)
        val tilbakekrevingBeløp: BigDecimal = beløpUtenRenter.add(rentebeløp)
        val skattBeløp: BigDecimal =
            beregnSkattBeløp(
                periode,
                beløpUtenRenter,
                perioderMedSkatteprosent,
                bruk6desimalerISkatteberegning,
            )
        val nettoBeløp: BigDecimal = tilbakekrevingBeløp.subtract(skattBeløp)
        return Beregningsresultatsperiode(
            periode = periode,
            vurdering = vurdering,
            renteprosent = if (renter) RENTESATS else null,
            feilutbetaltBeløp = delresultat.feilutbetaltBeløp,
            riktigYtelsesbeløp = delresultat.riktigYtelsesbeløp,
            utbetaltYtelsesbeløp = delresultat.utbetaltYtelsesbeløp,
            andelAvBeløp = andel,
            manueltSattTilbakekrevingsbeløp = manueltBeløp,
            tilbakekrevingsbeløpUtenRenter = beløpUtenRenter,
            rentebeløp = rentebeløp,
            tilbakekrevingsbeløpEtterSkatt = nettoBeløp,
            skattebeløp = skattBeløp,
            tilbakekrevingsbeløp = tilbakekrevingBeløp,
        )
    }

    private fun beregnRentebeløp(beløp: BigDecimal, renter: Boolean): BigDecimal {
        return if (renter) beløp.multiply(RENTEFAKTOR).setScale(0, RoundingMode.DOWN) else BigDecimal.ZERO
    }

    private fun beregnSkattBeløp(
        periode: Månedsperiode,
        bruttoTilbakekrevesBeløp: BigDecimal,
        perioderMedSkatteprosent: List<GrunnlagsperiodeMedSkatteprosent>,
        bruk6desimalerISkatteberegning: Boolean,
    ): BigDecimal {
        val totalKgTilbakekrevesBeløp: BigDecimal = perioderMedSkatteprosent.sumOf { it.tilbakekrevingsbeløp }
        val andel: BigDecimal = if (totalKgTilbakekrevesBeløp.isZero()) {
            BigDecimal.ZERO
        } else {
            bruttoTilbakekrevesBeløp.divide(totalKgTilbakekrevesBeløp, 4, RoundingMode.HALF_UP)
        }
        var skattBeløp: BigDecimal = BigDecimal.ZERO
        for (grunnlagPeriodeMedSkattProsent in perioderMedSkatteprosent) {
            if (periode.overlapper(grunnlagPeriodeMedSkattProsent.periode)) {
                val scale = if (bruk6desimalerISkatteberegning) 6 else 4
                val delTilbakekrevesBeløp: BigDecimal = grunnlagPeriodeMedSkattProsent.tilbakekrevingsbeløp.multiply(andel)
                val beregnetSkattBeløp = delTilbakekrevesBeløp.multiply(grunnlagPeriodeMedSkattProsent.skatteprosent)
                    .divide(BigDecimal.valueOf(100), scale, RoundingMode.HALF_UP)
                skattBeløp = skattBeløp.add(beregnetSkattBeløp).setScale(0, RoundingMode.DOWN)
            }
        }
        return skattBeløp
    }

    private fun finnBeløpUtenRenter(kravgrunnlagBeløp: BigDecimal, andel: BigDecimal?, manueltBeløp: BigDecimal?): BigDecimal {
        if (manueltBeløp != null) {
            return manueltBeløp
        }
        if (andel != null) {
            return kravgrunnlagBeløp.multiply(andel).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
        }
        throw IllegalArgumentException("Utvikler-feil: Forventer at utledet andel eller manuelt beløp er satt begge manglet")
    }

    private fun finnRenter(vurdering: Vilkårsvurderingsperiode): Boolean {
        val aktsomhet: VilkårsvurderingAktsomhet? = vurdering.aktsomhet
        if (aktsomhet != null) {
            val erForsett: Boolean = Aktsomhet.FORSETT == aktsomhet.aktsomhet
            return erForsett && (aktsomhet.ileggRenter == null || aktsomhet.ileggRenter) ||
                aktsomhet.ileggRenter != null && aktsomhet.ileggRenter
        }
        return false
    }

    private fun finnAndelAvBeløp(vurdering: Vilkårsvurderingsperiode): BigDecimal? {
        val aktsomhet: VilkårsvurderingAktsomhet? = vurdering.aktsomhet
        val godTro: VilkårsvurderingGodTro? = vurdering.godTro
        if (aktsomhet != null) {
            return finnAndelForAktsomhet(aktsomhet)
        } else if (godTro != null && !godTro.beløpErIBehold) {
            return BigDecimal.ZERO
        }
        return null
    }

    private fun finnAndelForAktsomhet(aktsomhet: VilkårsvurderingAktsomhet): BigDecimal? {
        return if (Aktsomhet.SIMPEL_UAKTSOMHET == aktsomhet.aktsomhet && !aktsomhet.tilbakekrevSmåbeløp) {
            BigDecimal.ZERO
        } else if (Aktsomhet.FORSETT == aktsomhet.aktsomhet || !aktsomhet.særligeGrunnerTilReduksjon) {
            HUNDRE_PROSENT
        } else {
            aktsomhet.andelTilbakekreves
        }
    }

    private fun finnManueltSattBeløp(vurdering: Vilkårsvurderingsperiode): BigDecimal? {
        val aktsomhet: VilkårsvurderingAktsomhet? = vurdering.aktsomhet
        val godTro: VilkårsvurderingGodTro? = vurdering.godTro
        if (aktsomhet != null) {
            return aktsomhet.manueltSattBeløp
        } else if (godTro != null) {
            return godTro.beløpTilbakekreves
        }
        throw IllegalArgumentException("VVurdering skal peke til GodTro-entiet eller Aktsomhet-entitet")
    }

    private fun finnVurdering(vurdering: Vilkårsvurderingsperiode): Vurdering {
        if (vurdering.aktsomhet != null) {
            return vurdering.aktsomhet.aktsomhet
        }
        if (vurdering.godTro != null) {
            return AnnenVurdering.GOD_TRO
        }
        throw IllegalArgumentException("VVurdering skal peke til GodTro-entiet eller Aktsomhet-entitet")
    }
}
