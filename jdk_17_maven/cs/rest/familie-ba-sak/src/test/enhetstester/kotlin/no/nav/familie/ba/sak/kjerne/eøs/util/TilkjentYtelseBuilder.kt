package no.nav.familie.ba.sak.kjerne.eøs.util

import no.nav.familie.ba.sak.common.erUnder18ÅrVilkårTidslinje
import no.nav.familie.ba.sak.common.erUnder6ÅrTidslinje
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.satstypeTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.tilAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MAX_MÅNED
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MIN_MÅNED
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrer
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNullMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjærEtter
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilCharTidslinje
import java.math.BigDecimal
import java.time.LocalDate

class TilkjentYtelseBuilder(
    private val startMåned: Tidspunkt<Måned>,
    private val behandling: Behandling = lagBehandling(),
) {
    private val tilkjentYtelse = TilkjentYtelse(
        behandling = behandling,
        opprettetDato = LocalDate.now(),
        endretDato = LocalDate.now(),
    )

    var gjeldendePersoner: List<Person> = emptyList()

    fun forPersoner(vararg personer: Person): TilkjentYtelseBuilder {
        gjeldendePersoner = personer.toList()
        return this
    }

    fun medSmåbarn(
        s: String,
        nasjonalt: (Int) -> Int? = { null },
        differanse: (Int) -> Int? = { null },
        kalkulert: (Int) -> Int = { it },
    ) = medYtelse(
        s = s,
        type = YtelseType.SMÅBARNSTILLEGG,
        kalkulert = kalkulert,
        differanse = differanse,
        nasjonalt = nasjonalt,
    ) {
        satstypeTidslinje(SatsType.SMA)
    }
        .also { gjeldendePersoner.single { it.type == PersonType.SØKER } }

    fun medUtvidet(
        s: String,
        nasjonalt: (Int) -> Int? = { null },
        differanse: (Int) -> Int? = { null },
        kalkulert: (Int) -> Int = { it },
    ) = medYtelse(
        s = s,
        type = YtelseType.UTVIDET_BARNETRYGD,
        kalkulert = kalkulert,
        nasjonalt = nasjonalt,
        differanse = differanse,
    ) {
        satstypeTidslinje(SatsType.UTVIDET_BARNETRYGD)
    }
        .also { gjeldendePersoner.single { it.type == PersonType.SØKER } }

    fun medOrdinær(
        s: String,
        prosent: Long = 100,
        nasjonalt: (Int) -> Int? = { null },
        differanse: (Int) -> Int? = { null },
        kalkulert: (Int) -> Int = { it },
    ) = medYtelse(
        s,
        YtelseType.ORDINÆR_BARNETRYGD,
        prosent,
        nasjonalt,
        differanse,
        kalkulert,
    ) {
        val orbaTidslinje = satstypeTidslinje(SatsType.ORBA)
        val tilleggOrbaTidslinje = satstypeTidslinje(SatsType.TILLEGG_ORBA)
            .filtrerMed(erUnder6ÅrTidslinje(it))
        orbaTidslinje.kombinerMed(tilleggOrbaTidslinje) { orba, tillegg -> tillegg ?: orba }
    }

    private fun medYtelse(
        s: String,
        type: YtelseType,
        prosent: Long = 100,
        nasjonalt: (Int) -> Int? = { null },
        differanse: (Int) -> Int? = { null },
        kalkulert: (Int) -> Int = { it },
        satsTidslinje: (Person) -> Tidslinje<Int, Måned>,
    ): TilkjentYtelseBuilder {
        val andeler = gjeldendePersoner
            .map { person ->
                val andelTilkjentYtelseTidslinje = s.tilCharTidslinje(startMåned)
                    .filtrer { char -> char?.let { !it.isWhitespace() } ?: false }
                    .map {
                        AndelTilkjentYtelse(
                            behandlingId = behandling.id,
                            tilkjentYtelse = tilkjentYtelse,
                            aktør = person.aktør,
                            stønadFom = MIN_MÅNED,
                            stønadTom = MAX_MÅNED,
                            kalkulertUtbetalingsbeløp = 0, // Overskrives under
                            nasjonaltPeriodebeløp = 0, // Overskrives under
                            differanseberegnetPeriodebeløp = null, // Overskrives under
                            prosent = BigDecimal.valueOf(prosent),
                            sats = 0, // Overskrives under
                            type = type,
                        )
                    }

                val begrensetAndelTilkjentYtelseTidslinje = when (type) {
                    YtelseType.ORDINÆR_BARNETRYGD -> andelTilkjentYtelseTidslinje.beskjærEtter(
                        erUnder18ÅrVilkårTidslinje(person.fødselsdato),
                    )
                    else -> andelTilkjentYtelseTidslinje
                }

                begrensetAndelTilkjentYtelseTidslinje.kombinerUtenNullMed(satsTidslinje(person)) { aty, sats ->
                    aty.copy(
                        sats = nasjonalt(sats) ?: kalkulert(sats),
                        kalkulertUtbetalingsbeløp = kalkulert(sats),
                        nasjonaltPeriodebeløp = nasjonalt(sats) ?: kalkulert(sats),
                        differanseberegnetPeriodebeløp = differanse(sats),
                    )
                }
            }.tilAndelerTilkjentYtelse()

        tilkjentYtelse.andelerTilkjentYtelse.addAll(andeler)
        return this
    }

    fun bygg(): TilkjentYtelse = tilkjentYtelse
}
