package no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt

import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom.rangeTo

fun <T : Tidsenhet> Tidspunkt<T>.ogSenere() = this.somUendeligLengeTil().rangeTo(this.somUendeligLengeTil())
fun <T : Tidsenhet> Tidspunkt<T>.ogTidligere() = this.somUendeligLengeSiden().rangeTo(this.somUendeligLengeSiden())
