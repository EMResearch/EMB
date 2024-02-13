package no.nav.familie.tilbake.kravgrunnlag

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Faktainfo
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingRequestSendtRepository
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingService
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottatt
import no.nav.familie.tilbake.kravgrunnlag.event.EndretKravgrunnlagEventPublisher
import no.nav.familie.tilbake.kravgrunnlag.task.FinnKravgrunnlagTask
import no.nav.familie.tilbake.micrometer.TellerService
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigInteger
import java.time.LocalDate
import java.util.UUID

internal class FinnKravgrunnlagTaskTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var økonomiXmlMottattRepository: ØkonomiXmlMottattRepository

    @Autowired
    private lateinit var økonomiXmlMottattArkivRepository: ØkonomiXmlMottattArkivRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var requestSendtRepository: HentFagsystemsbehandlingRequestSendtRepository

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var behandlingskontrollService: BehandlingskontrollService

    @Autowired
    private lateinit var stegService: StegService

    @Autowired
    private lateinit var oppgaveTaskService: OppgaveTaskService

    @Autowired
    private lateinit var mottattXmlService: ØkonomiXmlMottattService

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var historikkTaskService: HistorikkTaskService

    @Autowired
    private lateinit var kravvedtakstatusService: KravvedtakstatusService

    @Autowired
    private lateinit var tellerService: TellerService

    @Autowired
    private lateinit var endretKravgrunnlagEventPublisher: EndretKravgrunnlagEventPublisher

    private val kafkaProducer: KafkaProducer = mockk()

    private lateinit var kravgrunnlagService: KravgrunnlagService
    private lateinit var hentFagsystemsbehandlingService: HentFagsystemsbehandlingService
    private lateinit var finnKravgrunnlagTask: FinnKravgrunnlagTask

    private lateinit var behandling: Behandling
    private lateinit var behandlingId: UUID

    private val eksternFagsakId = "testverdi"

    @BeforeEach
    fun init() {
        hentFagsystemsbehandlingService = HentFagsystemsbehandlingService(requestSendtRepository, kafkaProducer)
        kravgrunnlagService = KravgrunnlagService(
            kravgrunnlagRepository,
            behandlingRepository,
            mottattXmlService,
            stegService,
            behandlingskontrollService,
            taskService,
            tellerService,
            oppgaveTaskService,
            historikkTaskService,
            hentFagsystemsbehandlingService,
            endretKravgrunnlagEventPublisher,
        )

        finnKravgrunnlagTask = FinnKravgrunnlagTask(
            behandlingRepository,
            fagsakRepository,
            økonomiXmlMottattRepository,
            kravgrunnlagRepository,
            kravgrunnlagService,
            kravvedtakstatusService,
        )

        every { kafkaProducer.sendHentFagsystemsbehandlingRequest(any(), any()) } returns Unit
    }

    @Test
    fun `doTask skal finne og koble grunnlag med behandling`() {
        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml")
        lagreMottattKravgrunnlag(kravgrunnlagXml)

        behandling = opprettBehandling(finnesVerge = true)
        behandlingId = behandling.id

        finnKravgrunnlagTask.doTask(Task(type = FinnKravgrunnlagTask.TYPE, payload = behandlingId.toString()))

        val arkivXmlene = økonomiXmlMottattArkivRepository.findByEksternFagsakIdAndYtelsestype(
            eksternFagsakId,
            Ytelsestype.BARNETRYGD,
        )
        arkivXmlene.shouldNotBeEmpty()

        (økonomiXmlMottattRepository.findAll() as List<*>).shouldBeEmpty()

        kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandlingId).shouldBeTrue()

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.VERGE, Behandlingsstegstatus.AUTOUTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
    }

    @Test
    fun `doTask skal finne og koble grunnlag med behandling når grunnlag er sperret`() {
        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml")
        lagreMottattKravgrunnlag(kravgrunnlagXml, true)

        behandling = opprettBehandling(finnesVerge = true)
        behandlingId = behandling.id

        finnKravgrunnlagTask.doTask(Task(type = FinnKravgrunnlagTask.TYPE, payload = behandlingId.toString()))

        val arkivXmlene = økonomiXmlMottattArkivRepository.findByEksternFagsakIdAndYtelsestype(
            eksternFagsakId,
            Ytelsestype.BARNETRYGD,
        )
        arkivXmlene.shouldNotBeEmpty()

        (økonomiXmlMottattRepository.findAll() as List<*>).shouldBeEmpty()

        kravgrunnlagRepository.existsByBehandlingIdAndAktivTrueAndSperretTrue(behandlingId).shouldBeTrue()

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.VENTER)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.VERGE, Behandlingsstegstatus.AUTOUTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.AVBRUTT)
    }

    @Test
    fun `doTask skal finne og koble grunnlag med behandling når det finnes et NY og et ENDR grunnlag`() {
        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml")
        lagreMottattKravgrunnlag(kravgrunnlagXml, true)

        behandling = opprettBehandling(finnesVerge = false)
        behandlingId = behandling.id

        val endretKravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_BA_ENDR.xml")
        lagreMottattKravgrunnlag(endretKravgrunnlagXml)

        finnKravgrunnlagTask.doTask(Task(type = FinnKravgrunnlagTask.TYPE, payload = behandlingId.toString()))

        val arkivXmlene = økonomiXmlMottattArkivRepository.findByEksternFagsakIdAndYtelsestype(
            eksternFagsakId,
            Ytelsestype.BARNETRYGD,
        )
        arkivXmlene.shouldNotBeEmpty()
        arkivXmlene.size shouldBe 2

        (økonomiXmlMottattRepository.findAll() as List<*>).shouldBeEmpty()

        kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandlingId).shouldBeTrue()
        val kravgrunnlagene = kravgrunnlagRepository.findByBehandlingId(behandlingId)
        kravgrunnlagene.any { it.aktiv && it.kravstatuskode == Kravstatuskode.ENDRET }.shouldBeTrue()
        kravgrunnlagene.any { !it.aktiv && it.sperret && it.kravstatuskode == Kravstatuskode.NYTT }.shouldBeTrue()

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
        behandlingsstegstilstand.any { it.behandlingssteg == Behandlingssteg.VERGE }.shouldBeFalse()
    }

    private fun opprettBehandling(finnesVerge: Boolean): Behandling {
        val faktainfo = Faktainfo(
            revurderingsårsak = "testverdi",
            revurderingsresultat = "testresultat",
            tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
        )

        val verge = if (finnesVerge) {
            no.nav.familie.kontrakter.felles.tilbakekreving.Verge(
                vergetype = Vergetype.VERGE_FOR_BARN,
                navn = "Andy",
                personIdent = "321321321",
            )
        } else {
            null
        }

        val request = OpprettTilbakekrevingRequest(
            ytelsestype = Ytelsestype.BARNETRYGD,
            fagsystem = Fagsystem.BA,
            eksternFagsakId = eksternFagsakId,
            personIdent = "321321322",
            eksternId = "0",
            manueltOpprettet = false,
            språkkode = Språkkode.NB,
            enhetId = "8020",
            enhetsnavn = "Oslo",
            varsel = null,
            verge = verge,
            revurderingsvedtaksdato = LocalDate.now(),
            faktainfo = faktainfo,
            saksbehandlerIdent = "Z0000",
        )
        return behandlingService.opprettBehandling(request)
    }

    private fun lagreMottattKravgrunnlag(
        kravgrunnlagXml: String,
        sperret: Boolean = false,
    ) {
        økonomiXmlMottattRepository.insert(
            ØkonomiXmlMottatt(
                melding = kravgrunnlagXml,
                kravstatuskode = Kravstatuskode.NYTT,
                eksternFagsakId = eksternFagsakId,
                ytelsestype = Ytelsestype.BARNETRYGD,
                referanse = "0",
                eksternKravgrunnlagId = BigInteger.ZERO,
                vedtakId = BigInteger.ZERO,
                kontrollfelt = "2021-03-02-18.50.15.236315",
                sperret = sperret,
            ),
        )
    }

    private fun assertBehandlingsstegstilstand(
        behandlingsstegstilstand: List<Behandlingsstegstilstand>,
        behandlingssteg: Behandlingssteg,
        behandlingsstegstatus: Behandlingsstegstatus,
    ) {
        behandlingsstegstilstand.any {
            it.behandlingssteg == behandlingssteg &&
                it.behandlingsstegsstatus == behandlingsstegstatus
        }.shouldBeTrue()
    }
}
