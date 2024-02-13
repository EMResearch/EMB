package no.nav.familie.tilbake.behandling.task

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.tilbakekreving.Faktainfo
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandling
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRespons
import no.nav.familie.kontrakter.felles.tilbakekreving.Institusjon
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingManuellOpprettelseService
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.FagsystemUtil
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingRequestSendtRepository
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingService
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.integration.kafka.DefaultKafkaProducer
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import no.nav.familie.tilbake.kravgrunnlag.task.FinnKravgrunnlagTask
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.time.LocalDate
import java.util.Properties
import java.util.UUID

internal class OpprettBehandlingManuellTaskTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var requestSendtRepository: HentFagsystemsbehandlingRequestSendtRepository

    @Autowired
    private lateinit var økonomiXmlMottattRepository: ØkonomiXmlMottattRepository

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingService: BehandlingService

    private val mockKafkaTemplate: KafkaTemplate<String, String> = mockk()
    private lateinit var spyKafkaProducer: KafkaProducer

    private lateinit var hentFagsystemsbehandlingService: HentFagsystemsbehandlingService
    private lateinit var behandlingManuellOpprettelseService: BehandlingManuellOpprettelseService
    private lateinit var opprettBehandlingManueltTask: OpprettBehandlingManueltTask

    private val requestIdSlot = slot<UUID>()
    private val hentFagsystemsbehandlingRequestSlot = slot<HentFagsystemsbehandlingRequest>()

    private val eksternFagsakId = "testverdi"
    private val ytelsestype = Ytelsestype.BARNETRYGD
    private val eksternId = "testverdi"
    private val ansvarligSaksbehandler = "Z0000"

    @BeforeEach
    fun init() {
        spyKafkaProducer = spyk(DefaultKafkaProducer(mockKafkaTemplate))
        hentFagsystemsbehandlingService = HentFagsystemsbehandlingService(requestSendtRepository, spyKafkaProducer)
        behandlingManuellOpprettelseService = BehandlingManuellOpprettelseService(behandlingService)
        opprettBehandlingManueltTask = OpprettBehandlingManueltTask(
            hentFagsystemsbehandlingService,
            behandlingManuellOpprettelseService,
        )

        val recordMetadata = mockk<RecordMetadata>()
        every { recordMetadata.offset() } returns 1
        val result = SendResult<String, String>(mockk(), recordMetadata)
        every { mockKafkaTemplate.send(any<ProducerRecord<String, String>>()).get() } returns result
    }

    @AfterEach
    fun tearDown() {
        requestSendtRepository.deleteAll()
    }

    @Test
    fun `preCondition skal sende hentFagsystemsbehandling request`() {
        opprettBehandlingManueltTask.preCondition(lagTask())

        verify {
            spyKafkaProducer.sendHentFagsystemsbehandlingRequest(
                capture(requestIdSlot),
                capture(hentFagsystemsbehandlingRequestSlot),
            )
        }
        val requestId = requestIdSlot.captured
        val requestSendt = requestSendtRepository
            .findByEksternFagsakIdAndYtelsestypeAndEksternId(
                eksternFagsakId,
                ytelsestype,
                eksternId,
            )
        requestSendt.shouldNotBeNull()
        requestSendt.id shouldBe requestId
        requestSendt.eksternFagsakId shouldBe eksternFagsakId
        requestSendt.ytelsestype shouldBe ytelsestype
        requestSendt.eksternId shouldBe eksternId
        requestSendt.respons.shouldBeNull()
    }

    @Test
    fun `doTask skal ikke opprette behandling når responsen ikke har mottatt fra fagsystem`() {
        opprettBehandlingManueltTask.preCondition(lagTask())

        val exception = shouldThrow<RuntimeException> { opprettBehandlingManueltTask.doTask(lagTask()) }
        exception.message shouldBe "HentFagsystemsbehandling respons-en har ikke mottatt fra fagsystem for " +
            "eksternFagsakId=$eksternFagsakId,ytelsestype=$ytelsestype," +
            "eksternId=$eksternId." +
            "Task-en kan kjøre på nytt manuelt når respons-en er mottatt"

        val requestSendt = requestSendtRepository
            .findByEksternFagsakIdAndYtelsestypeAndEksternId(
                eksternFagsakId,
                ytelsestype,
                eksternId,
            )
        requestSendt.shouldNotBeNull()
    }

    @Test
    fun `doTask skal ikke opprette behandling når responsen har mottatt fra fagsystem men finnes ikke kravgrunnlag`() {
        opprettBehandlingManueltTask.preCondition(lagTask())

        val requestSendt = requestSendtRepository
            .findByEksternFagsakIdAndYtelsestypeAndEksternId(
                eksternFagsakId,
                ytelsestype,
                eksternId,
            )
        val respons = lagHentFagsystemsbehandlingRespons()
        requestSendt?.let { requestSendtRepository.update(it.copy(respons = objectMapper.writeValueAsString(respons))) }

        val exception = shouldThrow<RuntimeException> { opprettBehandlingManueltTask.doTask(lagTask()) }
        exception.message shouldBe "Det finnes intet kravgrunnlag for ytelsestype=$ytelsestype,eksternFagsakId=$eksternFagsakId " +
            "og eksternId=$eksternId. Tilbakekrevingsbehandling kan ikke opprettes manuelt."
    }

    @Test
    fun `doTask skal opprette behandling når responsen har mottatt fra fagsystem og finnes kravgrunnlag`() {
        opprettBehandlingManueltTask.preCondition(lagTask())

        val requestSendt = requestSendtRepository
            .findByEksternFagsakIdAndYtelsestypeAndEksternId(
                eksternFagsakId,
                ytelsestype,
                eksternId,
            )
        val respons = lagHentFagsystemsbehandlingRespons()
        requestSendt?.let { requestSendtRepository.update(it.copy(respons = objectMapper.writeValueAsString(respons))) }

        val økonomiXmlMottatt = Testdata.økonomiXmlMottatt
        økonomiXmlMottattRepository.insert(økonomiXmlMottatt.copy(eksternFagsakId = eksternFagsakId, referanse = eksternId))

        opprettBehandlingManueltTask.doTask(lagTask())

        taskService.findAll().any { FinnKravgrunnlagTask.TYPE == it.type }.shouldBeTrue()

        val behandling = behandlingRepository.finnÅpenTilbakekrevingsbehandling(ytelsestype, eksternFagsakId)
        behandling.shouldNotBeNull()
        behandling.manueltOpprettet.shouldBeTrue()
        behandling.aktivtVarsel.shouldBeNull()
        behandling.aktivVerge.shouldBeNull()
        behandling.aktivFagsystemsbehandling.eksternId shouldBe eksternId

        val fagsystemsbehandling = respons.hentFagsystemsbehandling
        fagsystemsbehandling.shouldNotBeNull()
        behandling.aktivFagsystemsbehandling.resultat shouldBe fagsystemsbehandling.faktainfo.revurderingsresultat
        behandling.aktivFagsystemsbehandling.årsak shouldBe fagsystemsbehandling.faktainfo.revurderingsårsak
        behandling.behandlendeEnhet shouldBe fagsystemsbehandling.enhetId
        behandling.behandlendeEnhetsNavn shouldBe fagsystemsbehandling.enhetsnavn
        behandling.ansvarligSaksbehandler shouldBe "bb1234"
        behandling.ansvarligBeslutter.shouldBeNull()
        behandling.status shouldBe Behandlingsstatus.UTREDES

        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        fagsak.bruker.språkkode shouldBe fagsystemsbehandling.språkkode
        fagsak.fagsystem shouldBe FagsystemUtil.hentFagsystemFraYtelsestype(fagsystemsbehandling.ytelsestype)
        fagsak.institusjon shouldBe null
    }

    @Test
    fun `doTask skal opprette behandling for institusjon når responsen har mottatt fra fagsystem og finnes kravgrunnlag`() {
        opprettBehandlingManueltTask.preCondition(lagTask())

        val requestSendt = requestSendtRepository
            .findByEksternFagsakIdAndYtelsestypeAndEksternId(
                eksternFagsakId,
                ytelsestype,
                eksternId,
            )
        val respons = lagHentFagsystemsbehandlingRespons(erInstitusjon = true)
        requestSendt?.let { requestSendtRepository.update(it.copy(respons = objectMapper.writeValueAsString(respons))) }

        val økonomiXmlMottatt = Testdata.økonomiXmlMottatt
        økonomiXmlMottattRepository.insert(økonomiXmlMottatt.copy(eksternFagsakId = eksternFagsakId, referanse = eksternId))

        opprettBehandlingManueltTask.doTask(lagTask())

        taskService.findAll().any { FinnKravgrunnlagTask.TYPE == it.type }.shouldBeTrue()

        val behandling = behandlingRepository.finnÅpenTilbakekrevingsbehandling(ytelsestype, eksternFagsakId)
        behandling.shouldNotBeNull()
        behandling.manueltOpprettet.shouldBeTrue()
        behandling.aktivtVarsel.shouldBeNull()
        behandling.aktivVerge.shouldBeNull()
        behandling.aktivFagsystemsbehandling.eksternId shouldBe eksternId

        val fagsystemsbehandling = respons.hentFagsystemsbehandling
        fagsystemsbehandling.shouldNotBeNull()
        behandling.aktivFagsystemsbehandling.resultat shouldBe fagsystemsbehandling.faktainfo.revurderingsresultat
        behandling.aktivFagsystemsbehandling.årsak shouldBe fagsystemsbehandling.faktainfo.revurderingsårsak
        behandling.behandlendeEnhet shouldBe fagsystemsbehandling.enhetId
        behandling.behandlendeEnhetsNavn shouldBe fagsystemsbehandling.enhetsnavn
        behandling.ansvarligSaksbehandler shouldBe "bb1234"
        behandling.ansvarligBeslutter.shouldBeNull()
        behandling.status shouldBe Behandlingsstatus.UTREDES

        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        fagsak.bruker.språkkode shouldBe fagsystemsbehandling.språkkode
        fagsak.fagsystem shouldBe FagsystemUtil.hentFagsystemFraYtelsestype(fagsystemsbehandling.ytelsestype)
        fagsak.institusjon shouldNotBe null
        fagsak.institusjon!!.organisasjonsnummer shouldBe "987654321"
    }

    private fun lagTask(): Task {
        return Task(
            type = OpprettBehandlingManueltTask.TYPE,
            payload = "",
            properties = Properties().apply {
                setProperty("eksternFagsakId", eksternFagsakId)
                setProperty("ytelsestype", ytelsestype.name)
                setProperty("eksternId", eksternId)
                setProperty("ansvarligSaksbehandler", ansvarligSaksbehandler)
            },
        )
    }

    private fun lagHentFagsystemsbehandlingRespons(
        erInstitusjon: Boolean = false,
        feilmelding: String? = null,
    ): HentFagsystemsbehandlingRespons {
        var institusjon = if (erInstitusjon) Institusjon(organisasjonsnummer = "987654321") else null
        val fagsystemsbehandling = HentFagsystemsbehandling(
            eksternFagsakId = eksternFagsakId,
            ytelsestype = ytelsestype,
            eksternId = eksternId,
            personIdent = "testverdi",
            språkkode = Språkkode.NB,
            enhetId = "8020",
            enhetsnavn = "testverdi",
            revurderingsvedtaksdato = LocalDate.now(),
            faktainfo = Faktainfo(
                revurderingsårsak = "testverdi",
                revurderingsresultat = "OPPHØR",
                tilbakekrevingsvalg = Tilbakekrevingsvalg
                    .IGNORER_TILBAKEKREVING,
            ),
            institusjon = institusjon,
        )
        return HentFagsystemsbehandlingRespons(hentFagsystemsbehandling = fagsystemsbehandling, feilMelding = feilmelding)
    }
}
