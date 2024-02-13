package no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom

/**
 * Extension-funksjon som slår sammen påfølgende perioder der innholdet er likt
 * Benytter tidslinjeFraTidspunkt, som bygger sammenslåtte perioder som default
 */
fun <I, T : Tidsenhet> Tidslinje<I, T>.slåSammenLike(): Tidslinje<I, T> =
    tidsrom().tidslinjeFraTidspunkt { tidspunkt -> innholdForTidspunkt(tidspunkt) }
