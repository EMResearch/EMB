package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.common.exceptionhandler.UgyldigKravgrunnlagFeil
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagBelopDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagPeriodeDto
import no.nav.tilbakekreving.typer.v1.PeriodeDto
import no.nav.tilbakekreving.typer.v1.TypeKlasseDto
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.time.YearMonth

object KravgrunnlagValidator {

    @Throws(UgyldigKravgrunnlagFeil::class)
    fun validerGrunnlag(kravgrunnlag: DetaljertKravgrunnlagDto) {
        validerReferanse(kravgrunnlag)
        validerPeriodeInnenforMåned(kravgrunnlag)
        validerPeriodeStarterFørsteDagIMåned(kravgrunnlag)
        validerPeriodeSlutterSisteDagIMåned(kravgrunnlag)
        validerOverlappendePerioder(kravgrunnlag)
        validerSkatt(kravgrunnlag)
        validerPerioderHarFeilutbetalingspostering(kravgrunnlag)
        validerPerioderHarYtelsespostering(kravgrunnlag)
        validerPerioderHarFeilPosteringMedNegativFeilutbetaltBeløp(kravgrunnlag)
        validerYtelseMotFeilutbetaling(kravgrunnlag)
        validerYtelsesPosteringTilbakekrevesMotNyttOgOpprinneligUtbetalt(kravgrunnlag)
    }

    private fun validerReferanse(kravgrunnlag: DetaljertKravgrunnlagDto) {
        kravgrunnlag.referanse ?: throw UgyldigKravgrunnlagFeil(
            melding = "Ugyldig kravgrunnlag for kravgrunnlagId " +
                "${kravgrunnlag.kravgrunnlagId}. Mangler referanse.",
        )
    }

    private fun validerPeriodeInnenforMåned(kravgrunnlag: DetaljertKravgrunnlagDto) {
        kravgrunnlag.tilbakekrevingsPeriode.forEach {
            val periode = it.periode
            val fomMåned = YearMonth.of(periode.fom.year, periode.fom.month)
            val tomMåned = YearMonth.of(periode.tom.year, periode.tom.month)
            if (fomMåned != tomMåned) {
                throw UgyldigKravgrunnlagFeil(
                    "Ugyldig kravgrunnlag for kravgrunnlagId ${kravgrunnlag.kravgrunnlagId}." +
                        " Perioden ${periode.fom}-${periode.tom} er ikke innenfor en kalendermåned.",
                )
            }
        }
    }

    private fun validerPeriodeStarterFørsteDagIMåned(kravgrunnlag: DetaljertKravgrunnlagDto) {
        kravgrunnlag.tilbakekrevingsPeriode.forEach {
            if (it.periode.fom.dayOfMonth != 1) {
                throw UgyldigKravgrunnlagFeil(
                    "Ugyldig kravgrunnlag for kravgrunnlagId ${kravgrunnlag.kravgrunnlagId}." +
                        " Perioden ${it.periode.fom}-${it.periode.tom} starter ikke første dag i måned.",
                )
            }
        }
    }

    private fun validerPeriodeSlutterSisteDagIMåned(kravgrunnlag: DetaljertKravgrunnlagDto) {
        kravgrunnlag.tilbakekrevingsPeriode.forEach {
            if (it.periode.tom.dayOfMonth != YearMonth.from(it.periode.tom).lengthOfMonth()) {
                throw UgyldigKravgrunnlagFeil(
                    "Ugyldig kravgrunnlag for kravgrunnlagId ${kravgrunnlag.kravgrunnlagId}." +
                        " Perioden ${it.periode.fom}-${it.periode.tom} slutter ikke siste dag i måned.",
                )
            }
        }
    }

    private fun validerPerioderHarFeilutbetalingspostering(kravgrunnlag: DetaljertKravgrunnlagDto) {
        kravgrunnlag.tilbakekrevingsPeriode.forEach {
            if (it.tilbakekrevingsBelop.none { beløp -> finnesFeilutbetalingspostering(beløp.typeKlasse) }) {
                throw UgyldigKravgrunnlagFeil(
                    "Ugyldig kravgrunnlag for kravgrunnlagId ${kravgrunnlag.kravgrunnlagId}. " +
                        "Perioden ${it.periode.fom}-${it.periode.tom} " +
                        "mangler postering med klassetype=FEIL.",
                )
            }
        }
    }

    private fun validerPerioderHarYtelsespostering(kravgrunnlag: DetaljertKravgrunnlagDto) {
        kravgrunnlag.tilbakekrevingsPeriode.forEach {
            if (it.tilbakekrevingsBelop.none { beløp -> finnesYtelsespostering(beløp.typeKlasse) }) {
                throw UgyldigKravgrunnlagFeil(
                    "Ugyldig kravgrunnlag for kravgrunnlagId ${kravgrunnlag.kravgrunnlagId}. " +
                        "Perioden ${it.periode.fom}-${it.periode.tom} " +
                        "mangler postering med klassetype=YTEL.",
                )
            }
        }
    }

    private fun validerOverlappendePerioder(kravgrunnlag: DetaljertKravgrunnlagDto) {
        val sortertePerioder: List<Månedsperiode> = kravgrunnlag.tilbakekrevingsPeriode
            .map { p -> Månedsperiode(p.periode.fom, p.periode.tom) }
            .sorted()
        for (i in 1 until sortertePerioder.size) {
            val forrigePeriode = sortertePerioder[i - 1]
            val nåværendePeriode = sortertePerioder[i]
            if (nåværendePeriode.fom <= forrigePeriode.tom) {
                throw UgyldigKravgrunnlagFeil(
                    "Ugyldig kravgrunnlag for kravgrunnlagId ${kravgrunnlag.kravgrunnlagId}." +
                        " Overlappende perioder $forrigePeriode og $nåværendePeriode.",
                )
            }
        }
    }

    private fun validerSkatt(kravgrunnlag: DetaljertKravgrunnlagDto) {
        val grupppertPåMåned: Map<YearMonth, List<DetaljertKravgrunnlagPeriodeDto>> = kravgrunnlag.tilbakekrevingsPeriode
            .groupBy { tilMåned(it.periode) }.toMap()

        for ((key, value) in grupppertPåMåned) {
            validerSkattForPeriode(key, value, kravgrunnlag.kravgrunnlagId)
        }
    }

    private fun validerSkattForPeriode(
        måned: YearMonth,
        perioder: List<DetaljertKravgrunnlagPeriodeDto>,
        kravgrunnlagId: BigInteger,
    ) {
        var månedligSkattBeløp: BigDecimal? = null
        var totalSkatt = BigDecimal.ZERO
        for (periode in perioder) {
            if (månedligSkattBeløp == null) {
                månedligSkattBeløp = periode.belopSkattMnd
            } else {
                if (månedligSkattBeløp.compareTo(periode.belopSkattMnd) != 0) {
                    throw UgyldigKravgrunnlagFeil(
                        "Ugyldig kravgrunnlag for kravgrunnlagId $kravgrunnlagId. " +
                            "For måned $måned er opplyses ulike verdier maks skatt i ulike perioder",
                    )
                }
            }
            for (postering in periode.tilbakekrevingsBelop) {
                totalSkatt += postering.belopTilbakekreves.multiply(postering.skattProsent)
            }
        }
        totalSkatt = totalSkatt.divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
        if (månedligSkattBeløp == null) {
            throw UgyldigKravgrunnlagFeil(
                "Ugyldig kravgrunnlag for kravgrunnlagId $kravgrunnlagId. " +
                    "Mangler max skatt for måned $måned",
            )
        }
        if (totalSkatt > månedligSkattBeløp) {
            throw UgyldigKravgrunnlagFeil(
                "Ugyldig kravgrunnlag for kravgrunnlagId $kravgrunnlagId. " +
                    "For måned $måned er maks skatt $månedligSkattBeløp, " +
                    "men maks tilbakekreving ganget med skattesats blir $totalSkatt",
            )
        }
    }

    private fun validerPerioderHarFeilPosteringMedNegativFeilutbetaltBeløp(kravgrunnlag: DetaljertKravgrunnlagDto) {
        for (kravgrunnlagsperiode in kravgrunnlag.tilbakekrevingsPeriode) {
            for (beløp in kravgrunnlagsperiode.tilbakekrevingsBelop) {
                if (finnesFeilutbetalingspostering(beløp.typeKlasse) && beløp.belopNy < BigDecimal.ZERO) {
                    throw UgyldigKravgrunnlagFeil(
                        "Ugyldig kravgrunnlag for kravgrunnlagId ${kravgrunnlag.kravgrunnlagId}. " +
                            "Perioden ${kravgrunnlagsperiode.periode.fom}-" +
                            "${kravgrunnlagsperiode.periode.tom} " +
                            "har FEIL postering med negativ beløp",
                    )
                }
            }
        }
    }

    private fun validerYtelseMotFeilutbetaling(kravgrunnlag: DetaljertKravgrunnlagDto) {
        for (kravgrunnlagsperiode in kravgrunnlag.tilbakekrevingsPeriode) {
            val sumTilbakekrevesFraYtelsePosteringer = kravgrunnlagsperiode.tilbakekrevingsBelop
                .filter { finnesYtelsespostering(it.typeKlasse) }
                .sumOf(DetaljertKravgrunnlagBelopDto::getBelopTilbakekreves)
            val sumNyttBelopFraFeilposteringer = kravgrunnlagsperiode.tilbakekrevingsBelop
                .filter { finnesFeilutbetalingspostering(it.typeKlasse) }
                .sumOf(DetaljertKravgrunnlagBelopDto::getBelopNy)
            if (sumNyttBelopFraFeilposteringer.compareTo(sumTilbakekrevesFraYtelsePosteringer) != 0) {
                throw UgyldigKravgrunnlagFeil(
                    "Ugyldig kravgrunnlag for kravgrunnlagId ${kravgrunnlag.kravgrunnlagId}. " +
                        "For perioden ${kravgrunnlagsperiode.periode.fom}" +
                        "-${kravgrunnlagsperiode.periode.tom} total tilkakekrevesBeløp i YTEL " +
                        "posteringer er $sumTilbakekrevesFraYtelsePosteringer, mens total nytt beløp i " +
                        "FEIL posteringer er $sumNyttBelopFraFeilposteringer. " +
                        "Det er forventet at disse er like.",
                )
            }
        }
    }

    private fun validerYtelsesPosteringTilbakekrevesMotNyttOgOpprinneligUtbetalt(kravgrunnlag: DetaljertKravgrunnlagDto) {
        var harPeriodeMedBeløpMindreEnnDiff = false
        var harPeriodeMedBeløpStørreEnnDiff = false

        for (kravgrunnlagsperiode in kravgrunnlag.tilbakekrevingsPeriode) {
            for (kgBeløp in kravgrunnlagsperiode.tilbakekrevingsBelop) {
                if (finnesYtelsespostering(kgBeløp.typeKlasse)) {
                    val diff: BigDecimal = kgBeløp.belopOpprUtbet.subtract(kgBeløp.belopNy)
                    if (kgBeløp.belopTilbakekreves > diff) {
                        harPeriodeMedBeløpStørreEnnDiff = true
                    } else {
                        harPeriodeMedBeløpMindreEnnDiff = true
                    }
                }
            }
        }

        // Hvis vi kun har YTEL-posteringer som er sørre enn diferansen mellom nyttBeløp og opprinneligBeløp
        // vil vi kaste en valideringsfeil
        if (harPeriodeMedBeløpStørreEnnDiff && !harPeriodeMedBeløpMindreEnnDiff) {
            throw UgyldigKravgrunnlagFeil(
                "Ugyldig kravgrunnlag for kravgrunnlagId ${kravgrunnlag.kravgrunnlagId}. " +
                    "Har en eller flere perioder med YTEL-postering " +
                    "med tilbakekrevesBeløp som er større enn differanse mellom " +
                    "nyttBeløp og opprinneligBeløp",
            )
        }
    }

    private fun tilMåned(periode: PeriodeDto): YearMonth {
        return YearMonth.of(periode.fom.year, periode.fom.month)
    }

    private fun finnesFeilutbetalingspostering(typeKlasse: TypeKlasseDto): Boolean {
        return Klassetype.FEIL.name == typeKlasse.value()
    }

    private fun finnesYtelsespostering(typeKlasse: TypeKlasseDto): Boolean {
        return Klassetype.YTEL.name == typeKlasse.value()
    }
}
