package no.nav.familie.tilbake.behandling.task

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.tilbakekreving.Faktainfo
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandling
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRespons
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingRequestSendtRepository
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingService
import no.nav.familie.tilbake.behandling.domain.HentFagsystemsbehandlingRequestSendt
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.Properties

internal class OppdaterFaktainfoTaskTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var requestSendtRepository: HentFagsystemsbehandlingRequestSendtRepository

    @Autowired
    private lateinit var behandlingService: BehandlingService

    private val mockKafkaProducer: KafkaProducer = mockk()
    private lateinit var hentFagsystemsbehandlingService: HentFagsystemsbehandlingService
    private lateinit var oppdaterFaktainfoTask: OppdaterFaktainfoTask

    private val fagsak = Testdata.fagsak
    private val behandling = Testdata.behandling

    @BeforeEach
    fun init() {
        hentFagsystemsbehandlingService = HentFagsystemsbehandlingService(requestSendtRepository, mockKafkaProducer)
        oppdaterFaktainfoTask = OppdaterFaktainfoTask(hentFagsystemsbehandlingService, behandlingService)

        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(behandling)
    }

    @AfterEach
    fun tearDown() {
        requestSendtRepository.deleteAll()
    }

    @Test
    fun `doTask skal oppdatere fakta info når respons-en har mottatt fra fagsystem`() {
        requestSendtRepository
            .insert(
                HentFagsystemsbehandlingRequestSendt(
                    eksternFagsakId = fagsak.eksternFagsakId,
                    eksternId = "1",
                    ytelsestype = fagsak.ytelsestype,
                    respons = objectMapper.writeValueAsString(lagRespons(eksternId = "1")),
                ),
            )

        oppdaterFaktainfoTask.doTask(lagTask())

        val oppdatertBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        oppdatertBehandling.aktivFagsystemsbehandling.eksternId shouldBe "1"
        oppdatertBehandling.aktivFagsystemsbehandling.resultat shouldBe "testresultat"
        oppdatertBehandling.aktivFagsystemsbehandling.årsak shouldBe "testårsak"
        oppdatertBehandling.aktivFagsystemsbehandling.tilbakekrevingsvalg shouldBe Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING
    }

    @Test
    fun `doTask skal ikke oppdatere fakta info når respons-en ikke har mottatt fra fagsystem`() {
        requestSendtRepository.insert(
            HentFagsystemsbehandlingRequestSendt(
                eksternFagsakId = fagsak.eksternFagsakId,
                eksternId = "1",
                ytelsestype = fagsak.ytelsestype,
            ),
        )
        val exception = shouldThrow<RuntimeException> { oppdaterFaktainfoTask.doTask(lagTask()) }
        exception.message shouldBe "HentFagsystemsbehandlingRespons er ikke mottatt fra fagsystem for " +
            "eksternFagsakId=${fagsak.eksternFagsakId},ytelsestype=${fagsak.ytelsestype},eksternId=1." +
            "Task kan kjøre på nytt manuelt når respons er mottatt."
    }

    @Test
    fun `doTask skal ikke oppdatere fakta info når tilbakekrevingsbehandling allerede er tilkoblet med riktig fagsak`() {
        requestSendtRepository.insert(
            HentFagsystemsbehandlingRequestSendt(
                eksternFagsakId = fagsak.eksternFagsakId,
                eksternId = behandling.aktivFagsystemsbehandling.eksternId,
                ytelsestype = fagsak.ytelsestype,
                respons = objectMapper.writeValueAsString(lagRespons(eksternId = behandling.aktivFagsystemsbehandling.eksternId)),
            ),
        )
        shouldNotThrowAny { oppdaterFaktainfoTask.doTask(lagTask(eksternId = behandling.aktivFagsystemsbehandling.eksternId)) }
        val oppdatertBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        oppdatertBehandling.endretTidspunkt.isEqual(behandling.endretTidspunkt)
        oppdatertBehandling.aktivFagsystemsbehandling.sporbar.endret.endretTid.isEqual(behandling.aktivFagsystemsbehandling.sporbar.endret.endretTid)
    }

    private fun lagRespons(eksternId: String): HentFagsystemsbehandlingRespons {
        val hentFagsystemsbehandling = HentFagsystemsbehandling(
            eksternFagsakId = fagsak.eksternFagsakId,
            eksternId = eksternId,
            ytelsestype = fagsak.ytelsestype,
            personIdent = fagsak.bruker.ident,
            språkkode = fagsak.bruker.språkkode,
            enhetId = behandling.behandlendeEnhet,
            enhetsnavn = behandling.behandlendeEnhetsNavn,
            revurderingsvedtaksdato = LocalDate.now(),
            faktainfo = Faktainfo(
                revurderingsårsak = "testårsak",
                revurderingsresultat = "testresultat",
                tilbakekrevingsvalg = Tilbakekrevingsvalg
                    .IGNORER_TILBAKEKREVING,
            ),
        )
        return HentFagsystemsbehandlingRespons(hentFagsystemsbehandling = hentFagsystemsbehandling)
    }

    private fun lagTask(eksternId: String = "1"): Task {
        return Task(
            type = OppdaterFaktainfoTask.TYPE,
            payload = "",
            properties = Properties().apply {
                setProperty("eksternFagsakId", fagsak.eksternFagsakId)
                setProperty("ytelsestype", fagsak.ytelsestype.name)
                setProperty("eksternId", eksternId)
            },
        )
    }
}
