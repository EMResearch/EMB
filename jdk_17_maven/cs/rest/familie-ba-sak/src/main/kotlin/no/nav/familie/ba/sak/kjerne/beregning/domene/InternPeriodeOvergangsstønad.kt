package no.nav.familie.ba.sak.kjerne.beregning.domene

import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.grunnlag.småbarnstillegg.PeriodeOvergangsstønadGrunnlag
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.kontrakter.felles.ef.EksternPeriode
import java.time.LocalDate

data class InternPeriodeOvergangsstønad(
    val personIdent: String,
    val fomDato: LocalDate,
    val tomDato: LocalDate,
) {
    constructor(periodeOvergangsstønadGrunnlag: PeriodeOvergangsstønadGrunnlag) : this(
        personIdent = periodeOvergangsstønadGrunnlag.aktør.aktivFødselsnummer(),
        fomDato = periodeOvergangsstønadGrunnlag.fom,
        tomDato = periodeOvergangsstønadGrunnlag.tom,
    )
}

fun EksternPeriode.tilInternPeriodeOvergangsstønad() = InternPeriodeOvergangsstønad(
    personIdent = this.personIdent,
    fomDato = this.fomDato,
    tomDato = this.tomDato,
)

fun List<InternPeriodeOvergangsstønad>.slåSammenTidligerePerioder(
    dagensDato: LocalDate,
): List<InternPeriodeOvergangsstønad> {
    val tidligerePerioder = this.filter { it.fomDato.isSameOrBefore(dagensDato) }

    val nyePerioder = this.minus(tidligerePerioder)
    return tidligerePerioder.slåSammenSammenhengendePerioder() + nyePerioder
}

fun List<InternPeriodeOvergangsstønad>.slåSammenSammenhengendePerioder(): List<InternPeriodeOvergangsstønad> {
    return this.sortedBy { it.fomDato }
        .fold(mutableListOf()) { sammenslåttePerioder, nestePeriode ->
            if (sammenslåttePerioder.lastOrNull()?.tomDato?.toYearMonth() == nestePeriode.fomDato.forrigeMåned()
            ) {
                sammenslåttePerioder.apply { add(removeLast().copy(tomDato = nestePeriode.tomDato)) }
            } else {
                sammenslåttePerioder.apply { add(nestePeriode) }
            }
        }
}

/***
 * Dersom vi i en behandling har overgangsstønad i tre måneder:
 * |OOO-----|
 * som fører til småbarnstillegg.
 * Og så utvides overgangsstønadsperioden til fem måneder:
 * |OOOOO---|
 * som fører til småbarnstillegg i alle månedene.
 * Ønsker vi å kunne begrunne de to siste månedene med småbarnstillegg.
 * Splitter derfor opp overgangsstønads-perioden slik at vi kan begrunne endringen for søker i riktig periode.
 * |OOO-----|
 * |---OO---|
 *
 ***/
fun List<InternPeriodeOvergangsstønad>.splitFramtidigePerioderFraForrigeBehandling(
    overgangsstønadPerioderFraForrigeBehandling: List<InternPeriodeOvergangsstønad>,
    dagensDato: LocalDate,
): List<InternPeriodeOvergangsstønad> {
    val tidligerePerioder = this.filter { it.tomDato.isSameOrBefore(dagensDato) }
    val framtidigePerioder = this.minus(tidligerePerioder)
    val nyeOvergangsstønadTidslinje = InternPeriodeOvergangsstønadTidslinje(framtidigePerioder)

    val gammelOvergangsstønadTidslinje =
        InternPeriodeOvergangsstønadTidslinje(overgangsstønadPerioderFraForrigeBehandling)

    val oppsplittedeFramtigigePerioder = gammelOvergangsstønadTidslinje
        .kombinerMed(nyeOvergangsstønadTidslinje) { gammelOvergangsstønadPeriode, nyOvergangsstønadPeriode ->
            if (nyOvergangsstønadPeriode == null) {
                null
            } else {
                gammelOvergangsstønadPeriode ?: nyOvergangsstønadPeriode
            }
        }
        .lagInternePerioderOvergangsstønad()

    return tidligerePerioder + oppsplittedeFramtigigePerioder
}
