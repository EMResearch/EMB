package no.nav.familie.tilbake.kravgrunnlag

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingRequestSendtRepository
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingService
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.common.exceptionhandler.UgyldigKravgrunnlagFeil
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import no.nav.familie.tilbake.kravgrunnlag.batch.HentFagsystemsbehandlingTask
import no.nav.familie.tilbake.kravgrunnlag.batch.HåndterGamleKravgrunnlagService
import no.nav.familie.tilbake.kravgrunnlag.batch.HåndterGammelKravgrunnlagTask
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottatt
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

internal class HentFagsystemsbehandlingTaskTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var xmlMottattRepository: ØkonomiXmlMottattRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var requestSendtRepository: HentFagsystemsbehandlingRequestSendtRepository

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var økonomiXmlMottattService: ØkonomiXmlMottattService

    @Autowired
    private lateinit var stegService: StegService

    @Autowired
    private lateinit var historikkService: HistorikkService

    @Autowired
    private lateinit var behandlingskontrollService: BehandlingskontrollService

    private val mockHentKravgrunnlagService: HentKravgrunnlagService = mockk()

    private lateinit var håndterGamleKravgrunnlagService: HåndterGamleKravgrunnlagService
    private lateinit var hentFagsystemsbehandlingService: HentFagsystemsbehandlingService
    private lateinit var hentFagsystemsbehandlingTask: HentFagsystemsbehandlingTask

    private var xmlMottatt: ØkonomiXmlMottatt = Testdata.økonomiXmlMottatt
    private lateinit var mottattXMl: String
    private lateinit var mottattXmlId: UUID

    private val eksternFagsakIdSlot = slot<String>()
    private val ytelsestypeSlot = slot<Ytelsestype>()
    private val eksternIdSlot = slot<String>()

    @BeforeEach
    fun init() {
        mottattXMl = readXml("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml")
        xmlMottatt = xmlMottattRepository.insert(Testdata.økonomiXmlMottatt.copy(melding = mottattXMl))
        mottattXmlId = xmlMottatt.id

        håndterGamleKravgrunnlagService = HåndterGamleKravgrunnlagService(
            behandlingRepository,
            kravgrunnlagRepository,
            behandlingService,
            behandlingskontrollService,
            økonomiXmlMottattService,
            mockHentKravgrunnlagService,
            stegService,
            historikkService,
        )
        val kafkaProducer: KafkaProducer = mockk()
        hentFagsystemsbehandlingService = spyk(HentFagsystemsbehandlingService(requestSendtRepository, kafkaProducer))
        hentFagsystemsbehandlingTask =
            HentFagsystemsbehandlingTask(håndterGamleKravgrunnlagService, hentFagsystemsbehandlingService, taskService)

        every { kafkaProducer.sendHentFagsystemsbehandlingRequest(any(), any()) } returns Unit
    }

    @AfterEach
    fun tearDown() {
        requestSendtRepository.deleteAll()
    }

    @Test
    fun `doTask skal kaste exception når det allerede finnes en behandling på samme fagsak`() {
        fagsakRepository.insert(Testdata.fagsak.copy(eksternFagsakId = xmlMottatt.eksternFagsakId))
        behandlingRepository.insert(Testdata.behandling)

        val exception = shouldThrow<UgyldigKravgrunnlagFeil> { hentFagsystemsbehandlingTask.doTask(lagTask()) }
        exception.message shouldBe "Kravgrunnlag med $mottattXmlId er ugyldig." +
            "Det finnes allerede en åpen behandling for " +
            "fagsak=${xmlMottatt.eksternFagsakId} og ytelsestype=${xmlMottatt.ytelsestype}. " +
            "Kravgrunnlaget skulle være koblet. Kravgrunnlaget arkiveres manuelt" +
            "ved å bruke forvaltningsrutine etter feilundersøkelse."
    }

    @Test
    fun `doTask skal sende hentFagsystemsbehandling request når det ikke finnes en behandling på samme fagsak`() {
        hentFagsystemsbehandlingTask.doTask(lagTask())

        verify {
            hentFagsystemsbehandlingService.sendHentFagsystemsbehandlingRequest(
                capture(eksternFagsakIdSlot),
                capture(ytelsestypeSlot),
                capture(eksternIdSlot),
            )
        }
        eksternFagsakIdSlot.captured shouldBe xmlMottatt.eksternFagsakId
        ytelsestypeSlot.captured shouldBe xmlMottatt.ytelsestype
        eksternIdSlot.captured shouldBe xmlMottatt.referanse

        requestSendtRepository.findByEksternFagsakIdAndYtelsestypeAndEksternId(
            xmlMottatt.eksternFagsakId,
            xmlMottatt.ytelsestype,
            xmlMottatt.referanse,
        ).shouldNotBeNull()
    }

    @Test
    fun `onCompletion skal opprette task for å håndtere gammel kravgrunnlag`() {
        hentFagsystemsbehandlingTask.onCompletion(lagTask())

        taskService.finnTasksMedStatus(listOf(Status.UBEHANDLET)).shouldHaveSingleElement {
            it.type == HåndterGammelKravgrunnlagTask.TYPE &&
                it.payload == xmlMottatt.id.toString()
        }
    }

    private fun lagTask(): Task {
        return taskService.save(Task(type = HentFagsystemsbehandlingTask.TYPE, payload = mottattXmlId.toString()))
    }
}
