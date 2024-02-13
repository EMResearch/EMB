package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomFnr
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PersongrunnlagTest {

    val persongrunnlagService = mockk<PersongrunnlagService>()

    @Test
    fun `Returnerer nytt barn fra personopplysningsgrunnlag`() {
        val søker = randomFnr()
        val barn = randomFnr()
        val nyttbarn = randomFnr()

        val forrigeBehandling = lagBehandling()
        val forrigeGrunnlag = lagTestPersonopplysningGrunnlag(
            behandlingId = forrigeBehandling.id,
            søkerPersonIdent = søker,
            barnasIdenter = listOf(barn),
        )

        val behandling = lagBehandling()
        val grunnlag = lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søker,
            barnasIdenter = listOf(barn, nyttbarn),
        )

        every { persongrunnlagService.hentAktiv(forrigeBehandling.id) } returns forrigeGrunnlag
        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns grunnlag
        every { persongrunnlagService.finnNyeBarn(any(), any()) } answers { callOriginal() }

        val nye = persongrunnlagService.finnNyeBarn(forrigeBehandling = forrigeBehandling, behandling = behandling)
        Assertions.assertEquals(nyttbarn, nye.singleOrNull()!!.aktør.aktivFødselsnummer())
    }

    @Test
    fun `Returnerer barnet som 'søker' når grunnlag kun består av ett barn (enslig mindreårig eller institusjonsbarn)`() {
        val barnet = lagPerson(type = PersonType.BARN)
        val persongrunnlag = lagTestPersonopplysningGrunnlag(1L, barnet)
        Assertions.assertEquals(barnet, persongrunnlag.søker)
    }
}
