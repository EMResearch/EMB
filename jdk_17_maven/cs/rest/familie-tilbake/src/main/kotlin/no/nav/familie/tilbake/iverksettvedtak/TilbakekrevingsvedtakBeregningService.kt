package no.nav.familie.tilbake.iverksettvedtak

import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.beregning.TilbakekrevingsberegningService
import no.nav.familie.tilbake.beregning.modell.Beregningsresultatsperiode
import no.nav.familie.tilbake.common.isGreaterThanZero
import no.nav.familie.tilbake.common.isLessThanZero
import no.nav.familie.tilbake.common.isZero
import no.nav.familie.tilbake.iverksettvedtak.domain.KodeResultat
import no.nav.familie.tilbake.iverksettvedtak.domain.Tilbakekrevingsbeløp
import no.nav.familie.tilbake.iverksettvedtak.domain.Tilbakekrevingsperiode
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsperiode432
import no.nav.familie.tilbake.vilkårsvurdering.domain.AnnenVurdering
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Service
class TilbakekrevingsvedtakBeregningService(private val tilbakekrevingsberegningService: TilbakekrevingsberegningService) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun beregnVedtaksperioder(
        behandlingId: UUID,
        kravgrunnlag431: Kravgrunnlag431,
    ): List<Tilbakekrevingsperiode> {
        val beregningsresultat = tilbakekrevingsberegningService.beregn(behandlingId)

        val kravgrunnlagsperioder = kravgrunnlag431.perioder.toList().sortedBy { it.periode.fom }
        val beregnetePerioder = beregningsresultat.beregningsresultatsperioder.sortedBy { it.periode.fom }

        // oppretter kravgrunnlagsperioderMedSkatt basert på månedligSkattebeløp
        var kravgrunnlagsperioderMedSkatt = kravgrunnlagsperioder.associate { it.periode to it.månedligSkattebeløp }

        return beregnetePerioder.map { beregnetPeriode ->
            var perioder = lagTilbakekrevingsperioder(kravgrunnlagsperioder, beregnetPeriode)

            // avrunding tilbakekrevesbeløp og uinnkrevd beløp
            perioder = justerAvrunding(beregnetPeriode, perioder)

            // skatt
            // oppdaterer kravgrunnlagsperioderMedSkatt med gjenstående skatt
            // (ved å trekke totalSkattBeløp fra månedligeSkattebeløp)
            kravgrunnlagsperioderMedSkatt = oppdaterGjenståendeSkattetrekk(perioder, kravgrunnlagsperioderMedSkatt)
            perioder = justerAvrundingSkatt(beregnetPeriode, perioder, kravgrunnlagsperioderMedSkatt)

            // renter
            perioder = beregnRenter(beregnetPeriode, perioder)
            justerAvrundingRenter(beregnetPeriode, perioder)
        }.flatten()
    }

    private fun lagTilbakekrevingsperioder(
        kravgrunnlagsperioder: List<Kravgrunnlagsperiode432>,
        beregnetPeriode: Beregningsresultatsperiode,
    ): List<Tilbakekrevingsperiode> {
        return kravgrunnlagsperioder.filter { it.periode.snitt(beregnetPeriode.periode) != null }
            .map { Tilbakekrevingsperiode(it.periode, BigDecimal.ZERO, lagTilbakekrevingsbeløp(it.beløp, beregnetPeriode)) }
    }

    private fun lagTilbakekrevingsbeløp(
        kravgrunnlagsbeløp: Set<Kravgrunnlagsbeløp433>,
        beregnetPeriode: Beregningsresultatsperiode,
    ): List<Tilbakekrevingsbeløp> {
        return kravgrunnlagsbeløp.mapNotNull {
            when (it.klassetype) {
                Klassetype.FEIL -> Tilbakekrevingsbeløp(
                    klassetype = it.klassetype,
                    klassekode = it.klassekode,
                    nyttBeløp = it.nyttBeløp.setScale(0, RoundingMode.HALF_UP),
                    utbetaltBeløp = BigDecimal.ZERO,
                    tilbakekrevesBeløp = BigDecimal.ZERO,
                    uinnkrevdBeløp = BigDecimal.ZERO,
                    skattBeløp = BigDecimal.ZERO,
                    kodeResultat = utledKodeResulat(beregnetPeriode),
                )
                Klassetype.YTEL -> {
                    val beregnetTilbakrevesbeløp = beregnTilbakekrevesbeløp(beregnetPeriode, it)
                    val opprinneligTilbakekrevesbeløp: BigDecimal = it.tilbakekrevesBeløp

                    Tilbakekrevingsbeløp(
                        klassetype = it.klassetype,
                        klassekode = it.klassekode,
                        nyttBeløp = it.nyttBeløp.setScale(0, RoundingMode.HALF_UP),
                        utbetaltBeløp = it.opprinneligUtbetalingsbeløp.setScale(0, RoundingMode.HALF_UP),
                        tilbakekrevesBeløp = beregnetTilbakrevesbeløp,
                        uinnkrevdBeløp = opprinneligTilbakekrevesbeløp
                            .subtract(beregnetTilbakrevesbeløp).setScale(0, RoundingMode.HALF_UP),
                        skattBeløp = beregnSkattBeløp(
                            beregnetTilbakrevesbeløp,
                            it.skatteprosent,
                        ),
                        kodeResultat = utledKodeResulat(beregnetPeriode),
                    )
                }

                else -> null
            }
        }
    }

    private fun beregnSkattBeløp(
        bruttoTilbakekrevesBeløp: BigDecimal,
        skattProsent: BigDecimal,
    ): BigDecimal {
        return bruttoTilbakekrevesBeløp.multiply(skattProsent).divide(BigDecimal(100), 0, RoundingMode.DOWN)
    }

    private fun utledKodeResulat(beregnetPeriode: Beregningsresultatsperiode): KodeResultat {
        return when {
            beregnetPeriode.vurdering == AnnenVurdering.FORELDET -> {
                KodeResultat.FORELDET
            }
            beregnetPeriode.tilbakekrevingsbeløpUtenRenter.isZero() -> {
                KodeResultat.INGEN_TILBAKEKREVING
            }
            beregnetPeriode.feilutbetaltBeløp == beregnetPeriode.tilbakekrevingsbeløpUtenRenter -> {
                KodeResultat.FULL_TILBAKEKREVING
            }
            else -> KodeResultat.DELVIS_TILBAKEKREVING
        }
    }

    private fun justerAvrunding(
        beregnetPeriode: Beregningsresultatsperiode,
        perioder: List<Tilbakekrevingsperiode>,
    ): List<Tilbakekrevingsperiode> {
        val tilbakekrevingsbeløpUtenRenter = beregnetPeriode.tilbakekrevingsbeløpUtenRenter
        val totalTilbakekrevingsbeløp = beregnTotalTilbakekrevesbeløp(perioder)
        val differanse = totalTilbakekrevingsbeløp.subtract(tilbakekrevingsbeløpUtenRenter)

        return when {
            differanse.isGreaterThanZero() -> justerNed(differanse, perioder)
            differanse.isLessThanZero() -> justerOpp(differanse, perioder)
            else -> perioder
        }
    }

    private fun justerNed(differanse: BigDecimal, perioder: List<Tilbakekrevingsperiode>): List<Tilbakekrevingsperiode> {
        var diff = differanse
        return perioder.map { periode ->
            var justertebeløp = periode.beløp
            while (diff.isGreaterThanZero()) {
                justertebeløp = justertebeløp.map { beløp ->
                    if (Klassetype.FEIL == beløp.klassetype) {
                        beløp
                    } else {
                        diff = diff.subtract(BigDecimal.ONE)
                        beløp.copy(
                            tilbakekrevesBeløp = beløp.tilbakekrevesBeløp.subtract(BigDecimal.ONE),
                            uinnkrevdBeløp = beløp.uinnkrevdBeløp.add(BigDecimal.ONE),
                        )
                    }
                }
            }
            periode.copy(beløp = justertebeløp)
        }
    }

    private fun justerOpp(differanse: BigDecimal, perioder: List<Tilbakekrevingsperiode>): List<Tilbakekrevingsperiode> {
        var diff = differanse
        return perioder.map { periode ->
            var justertebeløp = periode.beløp
            while (diff.isLessThanZero()) {
                justertebeløp = justertebeløp.map { beløp ->
                    if (Klassetype.FEIL == beløp.klassetype) {
                        beløp
                    } else {
                        diff = diff.add(BigDecimal.ONE)
                        beløp.copy(
                            tilbakekrevesBeløp = beløp.tilbakekrevesBeløp.add(BigDecimal.ONE),
                            uinnkrevdBeløp = beløp.uinnkrevdBeløp.subtract(BigDecimal.ONE),
                        )
                    }
                }
            }
            periode.copy(beløp = justertebeløp)
        }
    }

    private fun oppdaterGjenståendeSkattetrekk(
        perioder: List<Tilbakekrevingsperiode>,
        kravgrunnlagsperioderMedSkatt: Map<Månedsperiode, BigDecimal>,
    ): Map<Månedsperiode, BigDecimal> {
        val grunnlagsperioderMedSkatt = kravgrunnlagsperioderMedSkatt.toMutableMap()
        perioder.forEach {
            val skattBeløp = it.beløp
                .filter { beløp -> Klassetype.YTEL == beløp.klassetype }
                .sumOf { ytelsebeløp -> ytelsebeløp.skattBeløp }
            val gjenståendeSkattBeløp = kravgrunnlagsperioderMedSkatt.getNotNull(it.periode).subtract(skattBeløp)
            grunnlagsperioderMedSkatt[it.periode] = gjenståendeSkattBeløp
        }
        return grunnlagsperioderMedSkatt
    }

    private fun justerAvrundingSkatt(
        beregnetPeriode: Beregningsresultatsperiode,
        perioder: List<Tilbakekrevingsperiode>,
        kravgrunnlagsperioderMedSkatt: Map<Månedsperiode, BigDecimal>,
    ): List<Tilbakekrevingsperiode> {
        val grunnlagsperioderMedSkatt = kravgrunnlagsperioderMedSkatt.toMutableMap()
        val totalSkattBeløp = perioder.sumOf { it.beløp.sumOf { beløp -> beløp.skattBeløp } }
        val beregnetSkattBeløp = beregnetPeriode.skattebeløp
        var differanse = totalSkattBeløp.subtract(beregnetSkattBeløp)

        return perioder.map {
            val periode = it.periode
            var justertebeløp = it.beløp
            justertebeløp = justertebeløp.map { beløp ->
                if (Klassetype.FEIL == beløp.klassetype) {
                    beløp
                } else {
                    val justerSkattOpp = differanse.isLessThanZero() &&
                        grunnlagsperioderMedSkatt.getNotNull(periode) >= BigDecimal.ONE
                    val justerSkattNed = differanse.isGreaterThanZero() &&
                        beløp.skattBeløp.compareTo(BigDecimal.ONE) >= 1
                    if (justerSkattOpp || justerSkattNed) {
                        val justering = BigDecimal(differanse.signum()).negate()
                        grunnlagsperioderMedSkatt[periode] = grunnlagsperioderMedSkatt.getNotNull(periode).add(justering)
                        differanse = differanse.add(justering)
                        beløp.copy(skattBeløp = beløp.skattBeløp.add(justering))
                    } else {
                        beløp
                    }
                }
            }
            it.copy(beløp = justertebeløp)
        }
    }

    private fun beregnTilbakekrevesbeløp(
        beregnetPeriode: Beregningsresultatsperiode,
        kravgrunnlagsbeløp: Kravgrunnlagsbeløp433,
    ): BigDecimal {
        return kravgrunnlagsbeløp.tilbakekrevesBeløp.multiply(beregnetPeriode.tilbakekrevingsbeløpUtenRenter)
            .divide(beregnetPeriode.feilutbetaltBeløp, 0, RoundingMode.HALF_UP)
    }

    private fun beregnTotalTilbakekrevesbeløp(perioder: List<Tilbakekrevingsperiode>): BigDecimal {
        return perioder.sumOf { it.beløp.sumOf { beløp -> beløp.tilbakekrevesBeløp } }
    }

    private fun summerTilbakekrevesbeløp(periode: Tilbakekrevingsperiode): BigDecimal {
        return periode.beløp.sumOf { it.tilbakekrevesBeløp }
    }

    private fun Map<Månedsperiode, BigDecimal>.getNotNull(key: Månedsperiode) = requireNotNull(this[key])

    private fun beregnRenter(
        beregnetPeriode: Beregningsresultatsperiode,
        perioder: List<Tilbakekrevingsperiode>,
    ): List<Tilbakekrevingsperiode> {
        return perioder.map {
            val tilbakekrevesbeløp = summerTilbakekrevesbeløp(it)
            var renteBeløp = BigDecimal.ZERO
            if (beregnetPeriode.tilbakekrevingsbeløpUtenRenter != BigDecimal.ZERO) {
                renteBeløp = beregnetPeriode.rentebeløp.multiply(tilbakekrevesbeløp)
                    .divide(beregnetPeriode.tilbakekrevingsbeløpUtenRenter, 0, RoundingMode.HALF_UP)
            }
            it.copy(renter = renteBeløp)
        }
    }

    private fun justerAvrundingRenter(
        beregnetPeriode: Beregningsresultatsperiode,
        perioder: List<Tilbakekrevingsperiode>,
    ): List<Tilbakekrevingsperiode> {
        val totalBeregnetRenteBeløp = beregnetPeriode.rentebeløp
        val totalBeregnetRenterIIverksettelse = perioder.sumOf { it.renter }
        logger.info(
            "Total beregnet renteBeløp som sendes i vedtaksbrev er $totalBeregnetRenteBeløp " +
                "mens total beregnet renteBeløp under iverksettelse er $totalBeregnetRenterIIverksettelse ",
        )
        var differanse = totalBeregnetRenteBeløp.minus(totalBeregnetRenterIIverksettelse)

        return when {
            differanse.isGreaterThanZero() -> {
                perioder.map { periode ->
                    var renteBeløp = periode.renter
                    while (differanse.isGreaterThanZero()) {
                        renteBeløp = renteBeløp.add(BigDecimal.ONE)
                        differanse = differanse.minus(BigDecimal.ONE)
                    }
                    periode.copy(renter = renteBeløp)
                }
            }
            differanse.isLessThanZero() -> {
                perioder.map { periode ->
                    var renteBeløp = periode.renter
                    while (differanse.isLessThanZero()) {
                        renteBeløp = renteBeløp.minus(BigDecimal.ONE)
                        differanse = differanse.plus(BigDecimal.ONE)
                    }
                    periode.copy(renter = renteBeløp)
                }
            }
            else -> perioder
        }
    }
}
