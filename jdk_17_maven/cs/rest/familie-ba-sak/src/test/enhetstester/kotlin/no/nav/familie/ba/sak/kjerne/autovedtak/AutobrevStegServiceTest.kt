package no.nav.familie.ba.sak.kjerne.autovedtak

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.AutovedtakFødselshendelseService
import no.nav.familie.ba.sak.kjerne.autovedtak.omregning.AutovedtakBrevService
import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.AutovedtakSmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.prosessering.error.RekjørSenereException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AutobrevStegServiceTest {
    private val fagsakService = mockk<FagsakService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val oppgaveService = mockk<OppgaveService>()
    private val autovedtakFødselshendelseService = mockk<AutovedtakFødselshendelseService>()
    private val autovedtakBrevService = mockk<AutovedtakBrevService>()
    private val autovedtakSmåbarnstilleggService = mockk<AutovedtakSmåbarnstilleggService>()

    val autovedtakStegService = AutovedtakStegService(
        fagsakService = fagsakService,
        behandlingHentOgPersisterService = behandlingHentOgPersisterService,
        oppgaveService = oppgaveService,
        autovedtakFødselshendelseService = autovedtakFødselshendelseService,
        autovedtakBrevService = autovedtakBrevService,
        autovedtakSmåbarnstilleggService = autovedtakSmåbarnstilleggService,
    )

    @Test
    fun `Skal stoppe autovedtak og opprette oppgave ved åpen behandling som utredes`() {
        val aktør = randomAktør()
        val fagsak = defaultFagsak(aktør)
        val behandling = lagBehandling(fagsak).also {
            it.status = BehandlingStatus.UTREDES
        }

        every { autovedtakSmåbarnstilleggService.skalAutovedtakBehandles(SmåbarnstilleggData(aktør)) } returns true
        every { fagsakService.hentNormalFagsak(aktør) } returns fagsak
        every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsakId = fagsak.id) } returns behandling
        every { oppgaveService.opprettOppgaveForManuellBehandling(any(), any(), any(), any()) } returns ""

        autovedtakStegService.kjørBehandlingSmåbarnstillegg(
            mottakersAktør = aktør,
            aktør = aktør,
        )

        verify(exactly = 1) { oppgaveService.opprettOppgaveForManuellBehandling(any(), any(), any(), any()) }
    }

    @Test
    fun `Skal stoppe autovedtak og opprette oppgave ved åpen behandling med status Fatter vedtak`() {
        val aktør = randomAktør()
        val fagsak = defaultFagsak(aktør)
        val behandling = lagBehandling(fagsak).also {
            it.status = BehandlingStatus.FATTER_VEDTAK
        }

        every { autovedtakSmåbarnstilleggService.skalAutovedtakBehandles(SmåbarnstilleggData(aktør)) } returns true
        every { fagsakService.hentNormalFagsak(aktør) } returns fagsak
        every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsakId = fagsak.id) } returns behandling
        every { oppgaveService.opprettOppgaveForManuellBehandling(any(), any(), any(), any()) } returns ""

        autovedtakStegService.kjørBehandlingSmåbarnstillegg(
            mottakersAktør = aktør,
            aktør = aktør,
        )

        verify(exactly = 1) { oppgaveService.opprettOppgaveForManuellBehandling(any(), any(), any(), any()) }
    }

    @Test
    fun `Skal stoppe autovedtak ved å kaste feil ved åpen behandling som iverksettes`() {
        val aktør = randomAktør()
        val fagsak = defaultFagsak(aktør)
        val behandling = lagBehandling(fagsak).also {
            it.status = BehandlingStatus.IVERKSETTER_VEDTAK
        }

        every { autovedtakSmåbarnstilleggService.skalAutovedtakBehandles(SmåbarnstilleggData(aktør)) } returns true
        every { fagsakService.hentNormalFagsak(aktør) } returns fagsak
        every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsakId = fagsak.id) } returns behandling
        every { oppgaveService.opprettOppgaveForManuellBehandling(any(), any(), any(), any()) } returns ""

        assertThrows<RekjørSenereException> {
            autovedtakStegService.kjørBehandlingSmåbarnstillegg(
                mottakersAktør = aktør,
                aktør = aktør,
            )
        }
    }
}
