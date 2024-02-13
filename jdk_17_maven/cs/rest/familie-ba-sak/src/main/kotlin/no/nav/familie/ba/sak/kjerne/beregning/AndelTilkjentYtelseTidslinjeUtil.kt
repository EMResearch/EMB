package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.erTilogMed3ÅrTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MAX_MÅNED
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MIN_MÅNED
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.joinIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
import java.math.BigDecimal
import java.time.YearMonth

fun Iterable<AndelTilkjentYtelse>.tilSeparateTidslinjerForBarna(): Map<Aktør, Tidslinje<AndelTilkjentYtelse, Måned>> {
    return this
        .filter { !it.erSøkersAndel() }
        .groupBy { it.aktør }
        .mapValues { (_, andeler) -> tidslinje { andeler.map { it.tilPeriode() } } }
}

fun Map<Aktør, Tidslinje<AndelTilkjentYtelse, Måned>>.tilAndelerTilkjentYtelse(): List<AndelTilkjentYtelse> {
    return this.values.flatMap { it.tilAndelTilkjentYtelse() }
}

fun Iterable<Tidslinje<AndelTilkjentYtelse, Måned>>.tilAndelerTilkjentYtelse(): List<AndelTilkjentYtelse> {
    return this.flatMap { it.tilAndelTilkjentYtelse() }
}

fun Tidslinje<AndelTilkjentYtelse, Måned>.tilAndelTilkjentYtelse(): List<AndelTilkjentYtelse> {
    return this
        .perioder().map {
            it.innhold?.medPeriode(
                it.fraOgMed.tilYearMonth(),
                it.tilOgMed.tilYearMonth(),
            )
        }.filterNotNull()
}

fun AndelTilkjentYtelse.tilPeriode() = Periode(
    this.stønadFom.tilTidspunkt(),
    this.stønadTom.tilTidspunkt(),
    // Ta bort periode, slik at det ikke blir med på innholdet som vurderes for likhet
    this.medPeriode(null, null),
)

fun AndelTilkjentYtelse.medPeriode(fraOgMed: YearMonth?, tilOgMed: YearMonth?) =
    copy(
        id = 0,
        stønadFom = fraOgMed ?: MIN_MÅNED,
        stønadTom = tilOgMed ?: MAX_MÅNED,
    ).also { versjon = this.versjon }

/**
 * Ivaretar fom og tom, slik at eventuelle splitter blir med videre.
 */
fun Iterable<AndelTilkjentYtelse>.tilTidslinjeForSøkersYtelse(ytelseType: YtelseType) = this
    .filter { it.erSøkersAndel() }
    .filter { it.type == ytelseType }
    .let {
        tidslinje {
            it.map { Periode(it.stønadFom.tilTidspunkt(), it.stønadTom.tilTidspunkt(), it) }
        }
    }

fun Map<Aktør, Tidslinje<AndelTilkjentYtelse, Måned>>.kunAndelerTilOgMed3År(barna: List<Person>): Map<Aktør, Tidslinje<AndelTilkjentYtelse, Måned>> {
    val barnasErInntil3ÅrTidslinjer = barna.associate { it.aktør to erTilogMed3ÅrTidslinje(it.fødselsdato) }

    // For hvert barn kombiner andel-tidslinjen med 3-års-tidslinjen. Resultatet er andelene når barna er inntil 3 år
    return this.joinIkkeNull(barnasErInntil3ÅrTidslinjer) { andel, _ -> andel }
}

data class AndelTilkjentYtelseForTidslinje(
    val aktør: Aktør,
    val beløp: Int,
    val sats: Int,
    val ytelseType: YtelseType,
    val prosent: BigDecimal,
    val nasjonaltPeriodebeløp: Int = beløp,
    val differanseberegnetPeriodebeløp: Int? = null,
)

fun AndelTilkjentYtelse.tilpassTilTidslinje() =
    AndelTilkjentYtelseForTidslinje(
        aktør = this.aktør,
        beløp = this.kalkulertUtbetalingsbeløp,
        ytelseType = this.type,
        sats = this.sats,
        prosent = this.prosent,
        nasjonaltPeriodebeløp = this.nasjonaltPeriodebeløp ?: this.kalkulertUtbetalingsbeløp,
        differanseberegnetPeriodebeløp = this.differanseberegnetPeriodebeløp,
    )

fun Tidslinje<AndelTilkjentYtelseForTidslinje, Måned>.tilAndelerTilkjentYtelse(tilkjentYtelse: TilkjentYtelse) =
    perioder()
        .filter { it.innhold != null }
        .map {
            AndelTilkjentYtelse(
                behandlingId = tilkjentYtelse.behandling.id,
                tilkjentYtelse = tilkjentYtelse,
                aktør = it.innhold!!.aktør,
                type = it.innhold.ytelseType,
                kalkulertUtbetalingsbeløp = it.innhold.beløp,
                nasjonaltPeriodebeløp = it.innhold.nasjonaltPeriodebeløp,
                differanseberegnetPeriodebeløp = it.innhold.differanseberegnetPeriodebeløp,
                sats = it.innhold.sats,
                prosent = it.innhold.prosent,
                stønadFom = it.fraOgMed.tilYearMonth(),
                stønadTom = it.tilOgMed.tilYearMonth(),
            )
        }

/**
 * Lager tidslinje med AndelTilkjentYtelseForTidslinje-objekter, som derfor er "trygg" mtp DB-endringer
 */
fun Iterable<AndelTilkjentYtelse>.tilTryggTidslinjeForSøkersYtelse(ytelseType: YtelseType) = this
    .filter { it.erSøkersAndel() }
    .filter { it.type == ytelseType }
    .let {
        tidslinje {
            it.map {
                Periode(
                    it.stønadFom.tilTidspunkt(),
                    it.stønadTom.tilTidspunkt(),
                    it.tilpassTilTidslinje(),
                )
            }
        }
    }
