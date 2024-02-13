package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.oppdaterTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.eøs.util.TilkjentYtelseBuilder
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilInneværendeMåned
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TilkjentYtelseRepositoryOppdaterTilkjentYtelseTest {

    val barnsFødselsdato = 13.jan(2020)
    val startMåned = barnsFødselsdato.tilInneværendeMåned()

    val søker = tilfeldigPerson(personType = PersonType.SØKER)
    val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barnsFødselsdato.tilLocalDate())

    val tilkjentYtelseRepository: TilkjentYtelseRepository = mockk(relaxed = true)

    @Test
    fun `skal kaste exception hvis tilkjent ytelse oppdateres med overlappende andel tilkjent ytelse for et barn`() {
        val behandling = lagBehandling()

        val forrigeTilkjentYtelse = TilkjentYtelseBuilder(startMåned, behandling).bygg()

        val nyTilkjentYtelse = TilkjentYtelseBuilder(startMåned, behandling)
            .forPersoner(barn1)
            .medOrdinær(" $$$$$$")
            .medOrdinær("      $$$$$")
            .bygg()

        assertThrows<IllegalStateException> {
            tilkjentYtelseRepository.oppdaterTilkjentYtelse(
                forrigeTilkjentYtelse,
                nyTilkjentYtelse.andelerTilkjentYtelse,
            )
        }
    }

    @Test
    fun `skal kaste exception hvis tilkjent ytelse oppdateres med overlappende andel tilkjent ytelse for søker`() {
        val behandling = lagBehandling()

        val forrigeTilkjentYtelse = TilkjentYtelseBuilder(startMåned, behandling).bygg()

        val nyTilkjentYtelse = TilkjentYtelseBuilder(startMåned, behandling)
            .forPersoner(søker)
            .medUtvidet(" $$$$$$")
            .medUtvidet("      $$$$$")
            .bygg()

        assertThrows<IllegalStateException> {
            tilkjentYtelseRepository.oppdaterTilkjentYtelse(
                forrigeTilkjentYtelse,
                nyTilkjentYtelse.andelerTilkjentYtelse,
            )
        }
    }

    @Test
    fun `skal ikke kaste exception hvis tilkjent ytelse oppdateres med gyldige andeler`() {
        val behandling = lagBehandling()

        val forrigeTilkjentYtelse = TilkjentYtelseBuilder(startMåned, behandling)
            .bygg()

        val nyTilkjentYtelse = TilkjentYtelseBuilder(startMåned, behandling)
            .forPersoner(søker)
            .medUtvidet(" $$$$$$$$$$")
            .forPersoner(barn1)
            .medOrdinær("$$$$$$$$$")
            .bygg()

        every { tilkjentYtelseRepository.saveAndFlush(any()) } returns nyTilkjentYtelse

        tilkjentYtelseRepository.oppdaterTilkjentYtelse(
            forrigeTilkjentYtelse,
            nyTilkjentYtelse.andelerTilkjentYtelse,
        )

        verify(exactly = 1) { tilkjentYtelseRepository.saveAndFlush(any()) }
    }
}
