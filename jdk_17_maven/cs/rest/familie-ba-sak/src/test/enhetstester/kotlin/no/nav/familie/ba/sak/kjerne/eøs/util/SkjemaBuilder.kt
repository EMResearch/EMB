package no.nav.familie.ba.sak.kjerne.eøs.util

import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEntitet
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.slåSammenLike
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonthEllerNull
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilCharTidslinje

abstract class SkjemaBuilder<S, B>(
    private val startMåned: Tidspunkt<Måned> = jan(2020),
    private val behandlingId: BehandlingId,
) where S : PeriodeOgBarnSkjemaEntitet<S>, B : SkjemaBuilder<S, B> {
    private val skjemaer: MutableList<S> = mutableListOf()

    protected fun medSkjema(k: String, barn: List<Person>, mapChar: (Char?) -> S?): B {
        val tidslinje = k.tilCharTidslinje(startMåned)
            .map(mapChar)
            .slåSammenLike()

        tidslinje.perioder()
            .filter { it.innhold != null }
            .map {
                it.innhold!!.kopier(
                    fom = it.fraOgMed.tilYearMonthEllerNull(),
                    tom = it.tilOgMed.tilYearMonthEllerNull(),
                    barnAktører = barn.map { person -> person.aktør }.toSet(),
                )
            }
            .all { skjemaer.add(it) }

        @Suppress("UNCHECKED_CAST")
        return this as B
    }

    protected fun medTransformasjon(transformasjon: (S) -> S): B {
        val transformerteSkjemaer = skjemaer.map { skjema -> transformasjon(skjema) }
        skjemaer.clear()
        skjemaer.addAll(transformerteSkjemaer)

        @Suppress("UNCHECKED_CAST")
        return this as B
    }

    fun bygg(): Collection<S> = skjemaer
        .map { skjema -> skjema.also { it.behandlingId = behandlingId.id } }

    fun lagreTil(repository: PeriodeOgBarnSkjemaRepository<S>): List<S> {
        return repository.saveAll(bygg())
    }
}
