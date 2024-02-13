package no.nav.familie.ba.sak.statistikk.saksstatistikk

import no.nav.familie.ba.sak.common.Utils.hentPropertyFraMaven
import no.nav.familie.ba.sak.common.nyOrdinærBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakController
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRequest
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.statistikk.producer.MockKafkaProducer
import no.nav.familie.ba.sak.statistikk.producer.MockKafkaProducer.Companion.sendteMeldinger
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringType.BEHANDLING
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringType.SAK
import no.nav.familie.ba.sak.util.BrukerContextUtil
import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SakDVH
import no.nav.familie.log.mdc.MDCConstants
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

class SaksstatistikkTest(
    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val fagsakController: FagsakController,

    @Autowired
    private val behandlingService: BehandlingService,

    @Autowired
    private val databaseCleanupService: DatabaseCleanupService,

    @Autowired
    private val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository,
) : AbstractSpringIntegrationTest() {

    private lateinit var saksstatistikkScheduler: SaksstatistikkScheduler

    @BeforeEach
    fun init() {
        MDC.put(MDCConstants.MDC_CALL_ID, "00001111")
        BrukerContextUtil.mockBrukerContext(SikkerhetContext.SYSTEM_FORKORTELSE)

        val kafkaProducer = MockKafkaProducer(saksstatistikkMellomlagringRepository)
        saksstatistikkScheduler = SaksstatistikkScheduler(saksstatistikkMellomlagringRepository, kafkaProducer)
        databaseCleanupService.truncate()
    }

    @AfterEach
    fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    @Tag("integration")
    fun `Skal lagre saksstatistikk sak til repository og sende meldinger`() {
        val fnr = randomFnr()
        val fagsakId = fagsakController.hentEllerOpprettFagsak(FagsakRequest(personIdent = fnr)).body!!.data!!.id

        val mellomlagredeStatistikkHendelser = saksstatistikkMellomlagringRepository.findByTypeAndTypeId(SAK, fagsakId)

        assertEquals(1, mellomlagredeStatistikkHendelser.size)
        assertEquals(SAK, mellomlagredeStatistikkHendelser.first().type)
        assertNull(mellomlagredeStatistikkHendelser.first().konvertertTidspunkt)
        assertNull(mellomlagredeStatistikkHendelser.first().sendtTidspunkt)
        assertEquals(
            hentPropertyFraMaven("familie.kontrakter.saksstatistikk"),
            mellomlagredeStatistikkHendelser.first().kontraktVersjon,
        )

        val lagretJsonSomSakDVH: SakDVH =
            sakstatistikkObjectMapper.readValue(mellomlagredeStatistikkHendelser.first().json, SakDVH::class.java)

        saksstatistikkScheduler.sendSaksstatistikk()
        val oppdatertMellomlagretSaksstatistikkHendelse =
            saksstatistikkMellomlagringRepository.findByIdOrNull(mellomlagredeStatistikkHendelser.first().id)

        assertNotNull(oppdatertMellomlagretSaksstatistikkHendelse!!.sendtTidspunkt)
        assertEquals(lagretJsonSomSakDVH, sendteMeldinger["sak-$fagsakId"] as SakDVH)
    }

    @Test
    @Tag("integration")
    fun `Skal lagre saksstatistikk behandling til repository og sende meldinger`() {
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr, false)
        val behandling = behandlingService.opprettBehandling(
            nyOrdinærBehandling(
                søkersIdent = fnr,
                fagsakId = fagsak.id,
            ),
        )

        behandlingService.oppdaterStatusPåBehandling(behandlingId = behandling.id, BehandlingStatus.AVSLUTTET)

        val mellomlagretBehandling =
            saksstatistikkMellomlagringRepository.findByTypeAndTypeId(BEHANDLING, behandling.id)
        assertEquals(2, mellomlagretBehandling.size)
        assertNull(mellomlagretBehandling.first().konvertertTidspunkt)
        assertNull(mellomlagretBehandling.first().sendtTidspunkt)
        assertEquals(
            hentPropertyFraMaven("familie.kontrakter.saksstatistikk"),
            mellomlagretBehandling.first().kontraktVersjon,
        )
        assertEquals("UTREDES", mellomlagretBehandling.first().jsonToBehandlingDVH().behandlingStatus)
        assertEquals("AVSLUTTET", mellomlagretBehandling.last().jsonToBehandlingDVH().behandlingStatus)

        val lagretJsonSomSakDVH: BehandlingDVH =
            sakstatistikkObjectMapper.readValue(mellomlagretBehandling.last().json, BehandlingDVH::class.java)

        saksstatistikkScheduler.sendSaksstatistikk()
        val oppdatertMellomlagretSaksstatistikkHendelse =
            saksstatistikkMellomlagringRepository.findByIdOrNull(mellomlagretBehandling.first().id)

        assertNotNull(oppdatertMellomlagretSaksstatistikkHendelse!!.sendtTidspunkt)
        assertEquals(lagretJsonSomSakDVH, sendteMeldinger["behandling-${behandling.id}"] as BehandlingDVH)
    }
}
