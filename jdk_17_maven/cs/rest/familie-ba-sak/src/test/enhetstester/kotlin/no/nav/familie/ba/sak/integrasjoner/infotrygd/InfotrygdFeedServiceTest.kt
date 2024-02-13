package no.nav.familie.ba.sak.integrasjoner.infotrygd

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.task.OpprettTaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class InfotrygdFeedServiceTest {
    private val opprettTaskServiceMock = mockk<OpprettTaskService>()

    @Test
    fun `Skal send riktig start behandling feed`() {
        val ident = "12345678900"
        val identSlot = slot<Aktør>()
        every { opprettTaskServiceMock.opprettSendStartBehandlingTilInfotrygdTask(capture(identSlot)) } just runs

        val infotrygdFeedService = InfotrygdFeedService(opprettTaskServiceMock)
        infotrygdFeedService.sendStartBehandlingTilInfotrygdFeed(tilAktør(ident))
        verify(exactly = 1) {
            opprettTaskServiceMock.opprettSendStartBehandlingTilInfotrygdTask(any())
        }
        assertThat(identSlot.captured.aktivFødselsnummer()).isEqualTo(ident)
    }
}
