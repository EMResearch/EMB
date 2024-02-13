package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.StartSatsendring.Companion.SATSENDRINGMÅNED_MARS_2023
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.Satskjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

internal class StartSatsendringTest {

    private val fagsakRepository: FagsakRepository = mockk()
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val satskjøringRepository: SatskjøringRepository = mockk()
    private val featureToggleService: FeatureToggleService = mockk()
    private val personidentService: PersonidentService = mockk()
    private val autovedtakSatsendringService: AutovedtakSatsendringService = mockk()
    private val satsendringService: SatsendringService = mockk()
    private val taskRepository: TaskRepositoryWrapper = mockk()

    private lateinit var startSatsendring: StartSatsendring

    @BeforeEach
    fun setUp() {
        val satsSlot = slot<Satskjøring>()
        every { satskjøringRepository.save(capture(satsSlot)) } answers { satsSlot.captured }
        val taskSlot = slot<Task>()
        every { taskRepository.save(capture(taskSlot)) } answers { taskSlot.captured }
        val opprettTaskService = OpprettTaskService(taskRepository, satskjøringRepository)

        every { satsendringService.erFagsakOppdatertMedSisteSatser(any()) } returns true

        startSatsendring = spyk(
            StartSatsendring(
                fagsakRepository = fagsakRepository,
                behandlingHentOgPersisterService = behandlingHentOgPersisterService,
                opprettTaskService = opprettTaskService,
                satskjøringRepository = satskjøringRepository,
                featureToggleService = featureToggleService,
                personidentService = personidentService,
                autovedtakSatsendringService = autovedtakSatsendringService,
                satsendringService = satsendringService,
            ),
        )
    }

    @Test
    fun `start satsendring og opprett satsendringtask på sak hvis toggler er på `() {
        every { featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_ENABLET, false) } returns true

        val behandling = lagBehandling()

        every { fagsakRepository.finnLøpendeFagsakerForSatsendring(any(), any()) } returns PageImpl(
            listOf(behandling.fagsak.id),
            Pageable.ofSize(5),
            0,
        )

        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) } returns behandling

        startSatsendring.startSatsendring(5)

        verify(exactly = 1) { taskRepository.save(any()) }
    }

    @Test
    fun `finnLøpendeFagsaker har totalt antall sider 3, så den skal kalle finnLøpendeFagsaker 3 ganger for å få 5 satsendringer`() {
        every { featureToggleService.isEnabled(any(), any()) } returns true
        every { featureToggleService.isEnabled(any()) } returns true

        val behandling = lagBehandling()

        every { fagsakRepository.finnLøpendeFagsakerForSatsendring(any(), any()) } returns PageImpl(
            listOf(behandling.fagsak.id, behandling.fagsak.id),
            Pageable.ofSize(2), // 5/2 gir totalt 3 sider, så finnLøpendeFagsakerForSatsendring skal trigges 3 ganger
            5,
        )

        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) } returns behandling

        startSatsendring.startSatsendring(5)

        verify(exactly = 5) { taskRepository.save(any()) }
        verify(exactly = 3) { fagsakRepository.finnLøpendeFagsakerForSatsendring(any(), any()) }
    }

    @Test
    fun `kanStarteSatsendringPåFagsak gir false når vi ikke har noen tidligere behandling`() {
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(1L) } returns null
        every { satskjøringRepository.findByFagsakIdAndSatsTidspunkt(1L, any()) } returns Satskjøring(
            fagsakId = 1L,
            satsTidspunkt = SATSENDRINGMÅNED_MARS_2023,
        )

        assertFalse(startSatsendring.kanStarteSatsendringPåFagsak(1L))
    }

    @Test
    fun `kanStarteSatsendringPåFagsak gir false når vi har en satskjøring for fagsaken i satskjøringsrepoet`() {
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(1L) } returns lagBehandling()
        every { satskjøringRepository.findByFagsakIdAndSatsTidspunkt(1L, any()) } returns Satskjøring(
            fagsakId = 1L,
            satsTidspunkt = SATSENDRINGMÅNED_MARS_2023,
        )

        assertFalse(startSatsendring.kanStarteSatsendringPåFagsak(1L))
    }

    @Test
    fun `kanStarteSatsendringPåFagsak gir false når harSisteSats er true`() {
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(1L) } returns lagBehandling()
        every { satskjøringRepository.findByFagsakIdAndSatsTidspunkt(1L, any()) } returns null
        every { satsendringService.erFagsakOppdatertMedSisteSatser(any()) } returns true

        assertFalse(startSatsendring.kanStarteSatsendringPåFagsak(1L))
    }

    @Test
    fun `kanStarteSatsendringPåFagsak gir true når harSisteSats er false`() {
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(1L) } returns lagBehandling()
        every { satskjøringRepository.findByFagsakIdAndSatsTidspunkt(1L, any()) } returns null
        every { satsendringService.erFagsakOppdatertMedSisteSatser(any()) } returns false

        assertTrue(startSatsendring.kanStarteSatsendringPåFagsak(1L))
    }

    @Test
    fun `kanGjennomføreSatsendringManuelt gir false når vi ikke har noen tidligere behandling`() {
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(1L) } returns null

        assertFalse(startSatsendring.kanGjennomføreSatsendringManuelt(1L))
    }

    @Test
    fun `kanGjennomføreSatsendringManuelt gir false når harSisteSats er true`() {
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(1L) } returns lagBehandling()
        every { satskjøringRepository.findByFagsakIdAndSatsTidspunkt(1L, any()) } returns null
        every { satsendringService.erFagsakOppdatertMedSisteSatser(any()) } returns true

        assertFalse(startSatsendring.kanGjennomføreSatsendringManuelt(1L))
    }

    @Test
    fun `opprettSatsendringSynkrontVedGammelSats skal kaste dersom man ikke kan starte satsendring`() {
        every { startSatsendring.kanStarteSatsendringPåFagsak(any()) } returns false

        assertThrows<Exception> {
            startSatsendring.gjennomførSatsendringManuelt(0L)
        }
    }

    @Test
    fun `kanGjennomføreSatsendringManuelt skal kaste feil for alle andre resultater enn OK`() {
        every { startSatsendring.kanGjennomføreSatsendringManuelt(any()) } returns true

        SatsendringSvar.entries.forEach {
            every { autovedtakSatsendringService.kjørBehandling(any()) } returns it

            when (it) {
                SatsendringSvar.SATSENDRING_KJØRT_OK -> assertDoesNotThrow {
                    startSatsendring.gjennomførSatsendringManuelt(0L)
                }

                else -> assertThrows<Exception> {
                    startSatsendring.gjennomførSatsendringManuelt(0L)
                }
            }
        }
    }
}
