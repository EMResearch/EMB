package no.nav.familie.tilbake.kravgrunnlag

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg.FAKTA
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg.FORELDELSE
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg.FORESLÅ_VEDTAK
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg.GRUNNLAG
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg.VARSEL
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg.VILKÅRSVURDERING
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.exceptionhandler.UgyldigStatusmeldingFeil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.historikkinnslag.LagHistorikkinnslagTask
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.familie.tilbake.kravgrunnlag.task.BehandleKravgrunnlagTask
import no.nav.familie.tilbake.kravgrunnlag.task.BehandleStatusmeldingTask
import no.nav.familie.tilbake.oppgave.OppdaterOppgaveTask
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import java.time.LocalDate

internal class BehandleStatusmeldingTaskTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var mottattXmlRepository: ØkonomiXmlMottattRepository

    @Autowired
    private lateinit var mottattXmlArkivRepository: ØkonomiXmlMottattArkivRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var behandleStatusmeldingTask: BehandleStatusmeldingTask

    @Autowired
    private lateinit var behandleKravgrunnlagTask: BehandleKravgrunnlagTask

    private val fagsak = Testdata.fagsak
    private val behandling = Testdata.behandling

    @BeforeEach
    fun init() {
        fagsakRepository.insert(fagsak)
    }

    @Test
    fun `doTask skal ikke prosessere SPER melding når det ikke finnes et kravgrunnlag`() {
        val statusmeldingXml = readXml("/kravvedtakstatusxml/statusmelding_SPER_BA.xml")
        val task = opprettTask(statusmeldingXml, BehandleStatusmeldingTask.TYPE)

        val exception = shouldThrow<Feil> { behandleStatusmeldingTask.doTask(task) }
        exception.message shouldBe "Det finnes intet kravgrunnlag for fagsystemId=${fagsak.eksternFagsakId} " +
            "og ytelsestype=${fagsak.ytelsestype}"
    }

    @Test
    fun `doTask skal ikke prosessere ugyldig SPER melding`() {
        val statusmeldingXml = readXml("/kravvedtakstatusxml/statusmelding_SPER_ugyldig.xml")
        val task = opprettTask(statusmeldingXml, BehandleStatusmeldingTask.TYPE)

        val exception = shouldThrow<UgyldigStatusmeldingFeil> { behandleStatusmeldingTask.doTask(task) }
        exception.message shouldBe "Ugyldig statusmelding for vedtakId=0, Mangler referanse."
    }

    @Test
    fun `doTask skal prosessere SPER melding uten behandling`() {
        opprettGrunnlag()

        val statusmeldingXml = readXml("/kravvedtakstatusxml/statusmelding_SPER_BA.xml")
        val task = opprettTask(statusmeldingXml, BehandleStatusmeldingTask.TYPE)

        behandleStatusmeldingTask.doTask(task)
        kravgrunnlagRepository.findAll().toList().shouldBeEmpty()

        val mottattXmlListe = mottattXmlRepository.findByEksternFagsakIdAndYtelsestype(fagsak.eksternFagsakId, fagsak.ytelsestype)
        mottattXmlListe.size shouldBe 1
        val mottattXml = mottattXmlListe[0]
        mottattXml.melding.contains(Constants.kravgrunnlagXmlRootElement).shouldBeTrue()
        mottattXml.sperret.shouldBeTrue()

        assertArkivertXml(1, false, Kravstatuskode.SPERRET)
    }

    @Test
    fun `doTask skal prosessere ENDR melding uten behandling`() {
        opprettGrunnlag()

        var statusmeldingXml = readXml("/kravvedtakstatusxml/statusmelding_SPER_BA.xml")
        var task = opprettTask(statusmeldingXml, BehandleStatusmeldingTask.TYPE)
        behandleStatusmeldingTask.doTask(task)

        statusmeldingXml = readXml("/kravvedtakstatusxml/statusmelding_ENDR_BA.xml")
        task = opprettTask(statusmeldingXml, BehandleStatusmeldingTask.TYPE)

        behandleStatusmeldingTask.doTask(task)
        kravgrunnlagRepository.findAll().toList().shouldBeEmpty()

        val mottattXmlListe = mottattXmlRepository.findByEksternFagsakIdAndYtelsestype(fagsak.eksternFagsakId, fagsak.ytelsestype)
        mottattXmlListe.size shouldBe 1
        val mottattXml = mottattXmlListe[0]
        mottattXml.melding.contains(Constants.kravgrunnlagXmlRootElement).shouldBeTrue()
        mottattXml.sperret.shouldBeFalse()

        assertArkivertXml(2, false, Kravstatuskode.SPERRET, Kravstatuskode.ENDRET)
    }

    @Test
    fun `doTask skal prosessere AVSL melding uten behandling`() {
        opprettGrunnlag()

        val statusmeldingXml = readXml("/kravvedtakstatusxml/statusmelding_AVSL_BA.xml")
        val task = opprettTask(statusmeldingXml, BehandleStatusmeldingTask.TYPE)

        behandleStatusmeldingTask.doTask(task)
        kravgrunnlagRepository.findAll().toList().shouldBeEmpty()
        mottattXmlRepository.findByEksternFagsakIdAndYtelsestype(fagsak.eksternFagsakId, fagsak.ytelsestype).shouldBeEmpty()

        assertArkivertXml(2, true, Kravstatuskode.AVSLUTTET)
    }

    @Test
    fun `doTask skal prosessere SPER melding med behandling på VARSEL steg`() {
        behandlingRepository.insert(behandling)
        lagBehandlingsstegstilstand(VARSEL, Behandlingsstegstatus.VENTER)

        opprettGrunnlag()

        val statusmeldingXml = readXml("/kravvedtakstatusxml/statusmelding_SPER_BA.xml")
        val task = opprettTask(statusmeldingXml, BehandleStatusmeldingTask.TYPE)

        behandleStatusmeldingTask.doTask(task)
        kravgrunnlagRepository.findByBehandlingId(behandling.id).shouldNotBeEmpty()
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        kravgrunnlag.sperret.shouldBeTrue()

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingstegstilstand(behandlingsstegstilstand, VARSEL, Behandlingsstegstatus.VENTER)

        val venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_MOTTATT)
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT, venteårsak.beskrivelse)
        taskService.findAll().none {
            it.type == OppdaterOppgaveTask.TYPE &&
                it.payload == behandling.id.toString() &&
                it.metadata["beskrivelse"] == venteårsak.beskrivelse &&
                it.metadata["frist"] == LocalDate.now().plusWeeks(venteårsak.defaultVenteTidIUker).toString()
        }.shouldBeTrue()
    }

    @Test
    fun `doTask skal prosessere SPER melding med behandling på FAKTA steg`() {
        behandlingRepository.insert(behandling)
        lagBehandlingsstegstilstand(VARSEL, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(GRUNNLAG, Behandlingsstegstatus.VENTER)

        opprettGrunnlag()
        var behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingstegstilstand(behandlingsstegstilstand, FAKTA, Behandlingsstegstatus.KLAR)

        val statusmeldingXml = readXml("/kravvedtakstatusxml/statusmelding_SPER_BA.xml")
        val task = opprettTask(statusmeldingXml, BehandleStatusmeldingTask.TYPE)

        behandleStatusmeldingTask.doTask(task)
        kravgrunnlagRepository.findByBehandlingId(behandling.id).shouldNotBeEmpty()
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        kravgrunnlag.sperret.shouldBeTrue()

        behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingstegstilstand(behandlingsstegstilstand, FAKTA, Behandlingsstegstatus.AVBRUTT)
        assertBehandlingstegstilstand(behandlingsstegstilstand, GRUNNLAG, Behandlingsstegstatus.VENTER)
        assertBehandlingstegstilstand(behandlingsstegstilstand, VARSEL, Behandlingsstegstatus.UTFØRT)

        assertArkivertXml(2, true, Kravstatuskode.SPERRET)
        mottattXmlRepository.findByEksternFagsakIdAndYtelsestype(fagsak.eksternFagsakId, fagsak.ytelsestype).shouldBeEmpty()

        val venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_MOTTATT)
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT, venteårsak.beskrivelse)
        assertOppgaveTask(venteårsak.beskrivelse, LocalDate.now().plusWeeks(venteårsak.defaultVenteTidIUker))
    }

    @Test
    fun `doTask skal prosessere SPER melding med behandling på FORESLÅ VEDTAK steg`() {
        behandlingRepository.insert(behandling)
        settBehandlingTilForeslåVedtakSteg()

        val statusmeldingXml = readXml("/kravvedtakstatusxml/statusmelding_SPER_BA.xml")
        val task = opprettTask(statusmeldingXml, BehandleStatusmeldingTask.TYPE)

        behandleStatusmeldingTask.doTask(task)
        kravgrunnlagRepository.findByBehandlingId(behandling.id).shouldNotBeEmpty()
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        kravgrunnlag.sperret.shouldBeTrue()

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingstegstilstand(behandlingsstegstilstand, FAKTA, Behandlingsstegstatus.UTFØRT)
        assertBehandlingstegstilstand(behandlingsstegstilstand, GRUNNLAG, Behandlingsstegstatus.VENTER)
        assertBehandlingstegstilstand(behandlingsstegstilstand, VARSEL, Behandlingsstegstatus.UTFØRT)
        assertBehandlingstegstilstand(behandlingsstegstilstand, FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        assertBehandlingstegstilstand(behandlingsstegstilstand, VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        assertBehandlingstegstilstand(behandlingsstegstilstand, FORESLÅ_VEDTAK, Behandlingsstegstatus.AVBRUTT)

        assertArkivertXml(2, true, Kravstatuskode.SPERRET)
        mottattXmlRepository.findByEksternFagsakIdAndYtelsestype(fagsak.eksternFagsakId, fagsak.ytelsestype).shouldBeEmpty()

        val venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_MOTTATT)
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT, venteårsak.beskrivelse)
        assertOppgaveTask(venteårsak.beskrivelse, LocalDate.now().plusWeeks(venteårsak.defaultVenteTidIUker))
    }

    @Test
    fun `doTask skal prosessere ENDR melding med behandling på FAKTA steg`() {
        behandlingRepository.insert(behandling)
        lagBehandlingsstegstilstand(VARSEL, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(GRUNNLAG, Behandlingsstegstatus.VENTER)
        opprettGrunnlag()

        var behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingstegstilstand(behandlingsstegstilstand, GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingstegstilstand(behandlingsstegstilstand, FAKTA, Behandlingsstegstatus.KLAR)

        var statusmeldingXml = readXml("/kravvedtakstatusxml/statusmelding_SPER_BA.xml")
        var task = opprettTask(statusmeldingXml, BehandleStatusmeldingTask.TYPE)
        behandleStatusmeldingTask.doTask(task)

        behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingstegstilstand(behandlingsstegstilstand, FAKTA, Behandlingsstegstatus.AVBRUTT)
        assertBehandlingstegstilstand(behandlingsstegstilstand, GRUNNLAG, Behandlingsstegstatus.VENTER)

        statusmeldingXml = readXml("/kravvedtakstatusxml/statusmelding_ENDR_BA.xml")
        task = opprettTask(statusmeldingXml, BehandleStatusmeldingTask.TYPE)

        behandleStatusmeldingTask.doTask(task)
        kravgrunnlagRepository.findByBehandlingId(behandling.id).shouldNotBeEmpty()
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        kravgrunnlag.sperret.shouldBeFalse()

        behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingstegstilstand(behandlingsstegstilstand, FAKTA, Behandlingsstegstatus.KLAR)
        assertBehandlingstegstilstand(behandlingsstegstilstand, GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingstegstilstand(behandlingsstegstilstand, VARSEL, Behandlingsstegstatus.UTFØRT)

        assertArkivertXml(3, true, Kravstatuskode.SPERRET, Kravstatuskode.ENDRET)

        mottattXmlRepository.findByEksternFagsakIdAndYtelsestype(fagsak.eksternFagsakId, fagsak.ytelsestype).shouldBeEmpty()

        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_MOTTATT)
        assertHistorikkTask(
            TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT,
            Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG.beskrivelse,
        )
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.BEHANDLING_GJENOPPTATT)
        assertOppgaveTask("Behandling er tatt av vent, pga mottatt ENDR melding", LocalDate.now())
    }

    @Test
    fun `doTask skal prosessere ENDR melding med behandling på FORESLÅ VEDTAK steg`() {
        behandlingRepository.insert(behandling)
        settBehandlingTilForeslåVedtakSteg()

        var statusmeldingXml = readXml("/kravvedtakstatusxml/statusmelding_SPER_BA.xml")
        var task = opprettTask(statusmeldingXml, BehandleStatusmeldingTask.TYPE)
        behandleStatusmeldingTask.doTask(task)

        statusmeldingXml = readXml("/kravvedtakstatusxml/statusmelding_ENDR_BA.xml")
        task = opprettTask(statusmeldingXml, BehandleStatusmeldingTask.TYPE)

        behandleStatusmeldingTask.doTask(task)
        kravgrunnlagRepository.findByBehandlingId(behandling.id).shouldNotBeEmpty()
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        kravgrunnlag.sperret.shouldBeFalse()

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingstegstilstand(behandlingsstegstilstand, FAKTA, Behandlingsstegstatus.UTFØRT)
        assertBehandlingstegstilstand(behandlingsstegstilstand, GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingstegstilstand(behandlingsstegstilstand, VARSEL, Behandlingsstegstatus.UTFØRT)
        assertBehandlingstegstilstand(behandlingsstegstilstand, FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        assertBehandlingstegstilstand(behandlingsstegstilstand, VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        assertBehandlingstegstilstand(behandlingsstegstilstand, FORESLÅ_VEDTAK, Behandlingsstegstatus.KLAR)

        assertArkivertXml(3, true, Kravstatuskode.SPERRET, Kravstatuskode.ENDRET)

        mottattXmlRepository.findByEksternFagsakIdAndYtelsestype(fagsak.eksternFagsakId, fagsak.ytelsestype).shouldBeEmpty()

        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_MOTTATT)
        assertHistorikkTask(
            TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT,
            Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG.beskrivelse,
        )
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.BEHANDLING_GJENOPPTATT)
        assertOppgaveTask("Behandling er tatt av vent, pga mottatt ENDR melding", LocalDate.now())
    }

    @Test
    fun `doTask skal prosessere AVSL melding med behandling på FORESLÅ VEDTAK steg`() {
        behandlingRepository.insert(behandling)
        settBehandlingTilForeslåVedtakSteg()

        val statusmeldingXml = readXml("/kravvedtakstatusxml/statusmelding_AVSL_BA.xml")
        val task = opprettTask(statusmeldingXml, BehandleStatusmeldingTask.TYPE)

        behandleStatusmeldingTask.doTask(task)
        kravgrunnlagRepository.findByBehandlingId(behandling.id).shouldNotBeEmpty()
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        kravgrunnlag.sperret.shouldBeFalse()
        kravgrunnlag.avsluttet.shouldBeTrue()

        assertArkivertXml(2, true, Kravstatuskode.AVSLUTTET)

        mottattXmlRepository.findByEksternFagsakIdAndYtelsestype(fagsak.eksternFagsakId, fagsak.ytelsestype).shouldBeEmpty()

        val behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandling.status shouldBe Behandlingsstatus.AVSLUTTET

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.shouldHaveSingleElement { Behandlingsstegstatus.AVBRUTT != it.behandlingsstegsstatus }
        behandlingsstegstilstand.shouldHaveSingleElement {
            Behandlingsstegstatus.AVBRUTT != it.behandlingsstegsstatus &&
                it.behandlingssteg == VARSEL
        }
        assertBehandlingstegstilstand(behandlingsstegstilstand, VARSEL, Behandlingsstegstatus.UTFØRT)

        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_MOTTATT)
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT, "")
    }

    private fun settBehandlingTilForeslåVedtakSteg() {
        lagBehandlingsstegstilstand(VARSEL, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(GRUNNLAG, Behandlingsstegstatus.VENTER)

        opprettGrunnlag()

        // oppdater FAKTA steg manuelt til UTFØRT
        val aktivtBehandlingsstegstilstand = behandlingsstegstilstandRepository
            .findByBehandlingIdAndBehandlingssteg(behandling.id, FAKTA)
        aktivtBehandlingsstegstilstand.shouldNotBeNull()
        aktivtBehandlingsstegstilstand.let {
            behandlingsstegstilstandRepository.update(it.copy(behandlingsstegsstatus = Behandlingsstegstatus.UTFØRT))
        }
        // sett aktivt behandlingssteg til FORESLÅ_VEDTAK
        lagBehandlingsstegstilstand(FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        lagBehandlingsstegstilstand(VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(FORESLÅ_VEDTAK, Behandlingsstegstatus.KLAR)
    }

    private fun opprettGrunnlag() {
        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml")
        val task = opprettTask(kravgrunnlagXml, BehandleKravgrunnlagTask.TYPE)
        behandleKravgrunnlagTask.doTask(task)

        kravgrunnlagRepository.findByBehandlingId(behandling.id) // skrevet for å fikse Optimistic Lock Exception
    }

    private fun lagBehandlingsstegstilstand(
        behandlingssteg: Behandlingssteg,
        behandlingsstegstatus: Behandlingsstegstatus,
        venteårsak: Venteårsak? = null,
        tidsfrist: LocalDate? = null,
    ) {
        behandlingsstegstilstandRepository.insert(
            Behandlingsstegstilstand(
                behandlingId = behandling.id,
                behandlingssteg = behandlingssteg,
                behandlingsstegsstatus = behandlingsstegstatus,
                venteårsak = venteårsak,
                tidsfrist = tidsfrist,
            ),
        )
    }

    private fun assertBehandlingstegstilstand(
        behandlingsstegstilstand: List<Behandlingsstegstilstand>,
        behandlingssteg: Behandlingssteg,
        behandlingsstegstatus: Behandlingsstegstatus,
    ) {
        behandlingsstegstilstand.shouldHaveSingleElement {
            behandlingssteg == it.behandlingssteg &&
                behandlingsstegstatus == it.behandlingsstegsstatus
        }
    }

    private fun assertArkivertXml(
        size: Int,
        finnesKravgrunnlag: Boolean,
        vararg statusmeldingKravstatuskode: Kravstatuskode,
    ) {
        val arkivertXmlListe = mottattXmlArkivRepository.findByEksternFagsakIdAndYtelsestype(
            fagsak.eksternFagsakId,
            fagsak.ytelsestype,
        )
        arkivertXmlListe.size shouldBe size

        if (finnesKravgrunnlag) {
            arkivertXmlListe.any { it.melding.contains(Constants.kravgrunnlagXmlRootElement) }.shouldBeTrue()
        }
        statusmeldingKravstatuskode.forEach { kravstatuskode ->
            arkivertXmlListe.shouldHaveSingleElement {
                it.melding.contains(Constants.statusmeldingXmlRootElement) &&
                    it.melding.contains(kravstatuskode.kode)
            }
        }
    }

    private fun opprettTask(xml: String, taskType: String): Task {
        return taskService.save(
            Task(
                type = taskType,
                payload = xml,
            ),
        )
    }

    private fun assertHistorikkTask(
        historikkinnslagstype: TilbakekrevingHistorikkinnslagstype,
        beskrivelse: String? = null,
    ) {
        taskService.finnTasksMedStatus(
            listOf(
                Status.KLAR_TIL_PLUKK,
                Status.UBEHANDLET,
                Status.BEHANDLER,
                Status.FERDIG,
            ),
            page = Pageable.unpaged(),
        ).any {
            LagHistorikkinnslagTask.TYPE == it.type &&
                historikkinnslagstype.name == it.metadata["historikkinnslagstype"] &&
                Aktør.VEDTAKSLØSNING.name == it.metadata["aktør"] &&
                beskrivelse == it.metadata["beskrivelse"] &&
                behandling.id.toString() == it.payload
        }.shouldBeTrue()
    }

    private fun assertOppgaveTask(beskrivelse: String, fristTid: LocalDate) {
        taskService.findAll().any {
            it.type == OppdaterOppgaveTask.TYPE &&
                it.payload == behandling.id.toString() &&
                it.metadata["beskrivelse"] == beskrivelse &&
                it.metadata["frist"] == fristTid.toString()
        }.shouldBeTrue()
    }
}
