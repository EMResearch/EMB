package no.nav.familie.ba.sak.kjerne.verge

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.common.lagBehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VergeServiceTest {

    private val vergeRepositoryMock: VergeRepository = mockk()

    @Test
    fun `RegistrerVerge() skal lagre verge og oppdater behandling`() {
        val behandling = lagBehandling()
        val vergeSlot = slot<Verge>()
        every { vergeRepositoryMock.findByBehandling(any()) } returns null
        every { vergeRepositoryMock.save(capture(vergeSlot)) } returns Verge(1L, "", behandling)
        val vergeService = VergeService(vergeRepositoryMock)
        val verge = Verge(1L, "verge 1", behandling)
        vergeService.oppdaterVergeForBehandling(behandling, verge)
        val vergeCaptured = vergeSlot.captured
        assertThat(vergeCaptured.id).isEqualTo(verge.id)
        assertThat(vergeCaptured.ident).isEqualTo(verge.ident)
        assertThat(vergeCaptured.behandling.id).isEqualTo(behandling.id)
    }
}
