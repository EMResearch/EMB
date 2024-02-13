package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Utils.avrundetHeltallAvProsent
import no.nav.familie.ba.sak.kjerne.beregning.Prosent.alt
import no.nav.familie.ba.sak.kjerne.beregning.Prosent.halvparten
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
import no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom.TidspunktClosedRange
import java.math.BigDecimal
import java.time.YearMonth

internal infix fun Person.får(prosent: Prosent) = BeregnetAndel(
    person = this,
    prosent = when (prosent) {
        alt -> BigDecimal.valueOf(100)
        halvparten -> BigDecimal.valueOf(50)
        Prosent.ingenting -> BigDecimal.ZERO
    },
    stønadFom = YearMonth.now(),
    stønadTom = YearMonth.now(),
    beløp = 0,
    sats = 0,
)

internal infix fun Person.får(sats: Int) = BeregnetAndel(
    person = this,
    prosent = BigDecimal.valueOf(100),
    stønadFom = YearMonth.now(),
    stønadTom = YearMonth.now(),
    beløp = sats,
    sats = sats,
)

@Suppress("ktlint:standard:enum-entry-name-case")
enum class Prosent {
    alt,
    halvparten,
    ingenting,
}

internal infix fun BeregnetAndel.av(sats: Int) = this.copy(
    sats = sats,
    beløp = sats.avrundetHeltallAvProsent(prosent),
)

internal infix fun BeregnetAndel.i(tidsrom: TidspunktClosedRange<Måned>) = this.copy(
    stønadFom = tidsrom.start.tilYearMonth(),
    stønadTom = tidsrom.endInclusive.tilYearMonth(),
)
