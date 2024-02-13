package no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingMigreringsinfoRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RestartAvSmåbarnstilleggServiceTest {

    private val fagsakRepository = mockk<FagsakRepository>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val behandlingMigreringsinfoRepository = mockk<BehandlingMigreringsinfoRepository>()
    private val restartAvSmåbarnstilleggService = spyk(
        RestartAvSmåbarnstilleggService(
            fagsakRepository = fagsakRepository,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            opprettTaskService = mockk(),
            vedtakService = mockk(),
            vedtaksperiodeService = mockk(),
            behandlingMigreringsinfoRepository = behandlingMigreringsinfoRepository,
            andelerTilkjentYtelseRepository = mockk(),
        ),
    )

    @Test
    fun `Skal ikke inkludere saker som er migrert forrige måned ved opprettelse av restartet småbarnstillegg oppgave`() {
        every { behandlingHentOgPersisterService.partitionByIverksatteBehandlinger<Long>(any()) } returns
            listOf(0L, 1L, 2L)

        every { behandlingMigreringsinfoRepository.finnSisteMigreringsdatoPåFagsak(0L) } returns LocalDate.now()

        every { behandlingMigreringsinfoRepository.finnSisteMigreringsdatoPåFagsak(1L) } returns LocalDate.now()
            .minusMonths(1)
        every { behandlingMigreringsinfoRepository.finnSisteMigreringsdatoPåFagsak(2L) } returns LocalDate.now()
            .minusMonths(2)

        every {
            restartAvSmåbarnstilleggService.periodeMedRestartetSmåbarnstilleggErAlleredeBegrunnet(any(), any())
        } returns false

        Assertions.assertEquals(
            listOf(0L, 2L),
            restartAvSmåbarnstilleggService.finnAlleFagsakerMedRestartetSmåbarnstilleggIMåned(),
        )
    }
}
