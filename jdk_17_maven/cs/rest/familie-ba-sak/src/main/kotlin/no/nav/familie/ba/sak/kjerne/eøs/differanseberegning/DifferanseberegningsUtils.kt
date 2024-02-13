package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.common.del
import no.nav.familie.ba.sak.common.multipliser
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.medPeriode
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrer
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.mapIkkeNull
import java.math.BigDecimal

fun Intervall.konverterBeløpTilMånedlig(beløp: BigDecimal): BigDecimal =
    when (this) {
        Intervall.ÅRLIG -> beløp.del(12.toBigDecimal(), 10)
        Intervall.KVARTALSVIS -> beløp.del(3.toBigDecimal(), 10)
        Intervall.MÅNEDLIG -> beløp
        Intervall.UKENTLIG -> beløp.multipliser(4.35.toBigDecimal(), 10)
    }.stripTrailingZeros().toPlainString().toBigDecimal()

/**
 * Kalkulerer nytt utbetalingsbeløp fra [utenlandskPeriodebeløpINorskeKroner]
 * Beløpet konverteres fra desimaltall til heltall ved å strippe desimalene, og dermed øke den norske ytelsen med inntil én krone
 * Må håndtere tilfellet der [kalkulertUtebetalngsbeløp] blir modifisert andre steder i koden, men antar at det aldri vil være negativt
 * [nasjonaltPeriodebeløp] settes til den originale, nasjonale beregningen (aldri negativt)
 * [differanseberegnetBeløp] er differansen mellom [nasjonaltPeriodebeløp] og (avrundet) [utenlandskPeriodebeløpINorskeKroner] (kan bli negativt)
 * [kalkulertUtebetalngsbeløp] blir satt til [differanseberegnetBeløp], med mindre det er negativt. Da blir det 0 (null)
 * Hvis [utenlandskPeriodebeløpINorskeKroner] er <null>, så skal utbetalingsbeløpet reverteres til det originale nasjonale beløpet
 */
fun AndelTilkjentYtelse?.oppdaterDifferanseberegning(
    utenlandskPeriodebeløpINorskeKroner: BigDecimal?,
): AndelTilkjentYtelse? {
    val nyAndelTilkjentYtelse = when {
        this == null -> null
        utenlandskPeriodebeløpINorskeKroner == null -> this.utenDifferanseberegning()
        else -> this.medDifferanseberegning(utenlandskPeriodebeløpINorskeKroner)
    }

    return nyAndelTilkjentYtelse
}

fun AndelTilkjentYtelse.medDifferanseberegning(
    utenlandskPeriodebeløpINorskeKroner: BigDecimal,
): AndelTilkjentYtelse {
    val avrundetUtenlandskPeriodebeløp = utenlandskPeriodebeløpINorskeKroner
        .toBigInteger().intValueExact() // Fjern desimaler for å gi fordel til søker

    val nyttDifferanseberegnetBeløp = (
        nasjonaltPeriodebeløp
            ?: kalkulertUtbetalingsbeløp
        ) - avrundetUtenlandskPeriodebeløp

    return copy(
        id = 0,
        kalkulertUtbetalingsbeløp = maxOf(nyttDifferanseberegnetBeløp, 0),
        differanseberegnetPeriodebeløp = nyttDifferanseberegnetBeløp,
    )
}

private fun AndelTilkjentYtelse.utenDifferanseberegning(): AndelTilkjentYtelse {
    return copy(
        id = 0,
        kalkulertUtbetalingsbeløp = nasjonaltPeriodebeløp ?: this.kalkulertUtbetalingsbeløp,
        differanseberegnetPeriodebeløp = null,
    )
}

/**
 * Gjør et forsøk på fjerne differanseberegning på andelen, samtidig som tidligere, funksjonelle splitter bevares
 * Det kan være en funksjonell grunn til at en splitt finnes, selv om nabo-andelene ellers like, f.eks
 * endring i overgangsstønad, som ikke fører til endring i småbarnstillegget. Splitten er nødvendig for riktige vedtaksperioder
 * Splitten opprettholdes når fom og tom er satt på andelen og er forskjellig fra fom og tom på nabo-andelen. Det vil gjelde søkers ytelser
 * Ved å sette fom og tom til <null> på alle andeler som har differanseberegning, vil naboer slås sammen hvis de ellers er like
 * Det er en potensiell bug her: Det er en mulighet for at en funksjonell splitt i andeler
 * sammenfaller med splitt pga differanseberegning, og blir fjernet
 * Det beste hadde vært om andelene IKKE inneholdt splitter av denne typen, men at ekstra splitter ble utledet der de trengs
 */
fun <T : Tidsenhet> Tidslinje<AndelTilkjentYtelse, T>.utenDifferanseberegning() =
    mapIkkeNull {
        when {
            it.differanseberegnetPeriodebeløp != null -> it.medPeriode(null, null)
            else -> it
        }
    }.mapIkkeNull { it.utenDifferanseberegning() }

fun Tidslinje<AndelTilkjentYtelse, Måned>.oppdaterDifferanseberegning(
    utenlandskBeløpINorskeKronerTidslinje: Tidslinje<BigDecimal, Måned>,
): Tidslinje<AndelTilkjentYtelse, Måned> {
    return this.kombinerMed(utenlandskBeløpINorskeKronerTidslinje) { andel, utenlandskBeløpINorskeKroner ->
        andel.oppdaterDifferanseberegning(utenlandskBeløpINorskeKroner)
    }
}

/**
 * Konverterer negativt differanseberegnet periodebeløp på andelene til underskudd som positiv BigDecimal
 * Altså:
 * AndelTilkjentYtelse{ differanseberegnetPeriodebeløp: -700 } => BigDecimal{ 700 }
 * AndelTilkjentYtelse{ differanseberegnetPeriodebeløp: 200 } => null
 * AndelTilkjentYtelse{ differanseberegnetPeriodebeløp: null } => null
 */
fun <K, T : Tidsenhet> Map<K, Tidslinje<AndelTilkjentYtelse, T>>.tilUnderskuddPåDifferanseberegningen(): Map<K, Tidslinje<BigDecimal, T>> =
    mapValues { (_, tidslinje) ->
        tidslinje
            .mapIkkeNull { innhold -> innhold.differanseberegnetPeriodebeløp }
            .mapIkkeNull { maxOf(-it, 0) }
            .filtrer { it != null && it > 0 }
            .mapIkkeNull { it.toBigDecimal() }
    }
