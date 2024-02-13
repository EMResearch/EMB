package no.nav.familie.tilbake.dokumentbestilling.felles.task

import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager.MANUELL_TILLEGGSMOTTAKER
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager.VERGE
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.historikkinnslag.LagHistorikkinnslagTask
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.iverksettvedtak.task.AvsluttBehandlingTask
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.Properties
import java.util.UUID

internal class LagreBrevsporingTaskTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var brevsporingRepository: BrevsporingRepository

    @Autowired
    private lateinit var lagreBrevsporingTask: LagreBrevsporingTask

    private val behandling = Testdata.behandling
    private val behandlingId = behandling.id

    private val dokumentId: String = "testverdi"
    private val journalpostId: String = "testverdi"

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(behandling)
    }

    @Test
    fun `doTask skal lagre brevsporing for varselbrev`() {
        lagreBrevsporingTask.doTask(opprettTask(behandlingId, Brevtype.VARSEL))

        assertBrevsporing(Brevtype.VARSEL)
    }

    @Test
    fun `onCompletion skal lage historikk task for varselbrev`() {
        lagreBrevsporingTask.onCompletion(opprettTask(behandlingId, Brevtype.VARSEL))

        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.VARSELBREV_SENDT, Aktør.VEDTAKSLØSNING, Brevtype.VARSEL)
    }

    @Test
    fun `onCompletion skal lage historikk task for manuelt varselbrev`() {
        lagreBrevsporingTask.onCompletion(opprettTask(behandlingId, Brevtype.VARSEL, "Z0000"))

        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.VARSELBREV_SENDT, Aktør.SAKSBEHANDLER, Brevtype.VARSEL)
    }

    @Test
    fun `doTask skal lagre brevsporing for korrigert varselbrev`() {
        lagreBrevsporingTask.doTask(opprettTask(behandlingId, Brevtype.KORRIGERT_VARSEL))

        assertBrevsporing(Brevtype.KORRIGERT_VARSEL)
    }

    @Test
    fun `onCompletion skal lage historikk task for korrigert varselbrev`() {
        lagreBrevsporingTask.onCompletion(opprettTask(behandlingId, Brevtype.KORRIGERT_VARSEL))

        assertHistorikkTask(
            TilbakekrevingHistorikkinnslagstype.KORRIGERT_VARSELBREV_SENDT,
            Aktør.SAKSBEHANDLER,
            Brevtype.KORRIGERT_VARSEL,
        )
    }

    @Test
    fun `doTask skal lagre brevsporing for henleggelsesbrev`() {
        lagreBrevsporingTask.doTask(opprettTask(behandlingId, Brevtype.HENLEGGELSE))

        assertBrevsporing(Brevtype.HENLEGGELSE)
    }

    @Test
    fun `onCompletion skal lage historikk task for henleggelsesbrev`() {
        lagreBrevsporingTask.onCompletion(opprettTask(behandlingId, Brevtype.HENLEGGELSE))

        assertHistorikkTask(
            TilbakekrevingHistorikkinnslagstype.HENLEGGELSESBREV_SENDT,
            Aktør.VEDTAKSLØSNING,
            Brevtype.HENLEGGELSE,
        )
    }

    @Test
    fun `doTask skal lagre brevsporing for innhent dokumentasjon`() {
        lagreBrevsporingTask.doTask(opprettTask(behandlingId, Brevtype.INNHENT_DOKUMENTASJON))

        assertBrevsporing(Brevtype.INNHENT_DOKUMENTASJON)
    }

    @Test
    fun `onCompletion skal lage historikk task for innhent dokumentasjon`() {
        lagreBrevsporingTask.onCompletion(opprettTask(behandlingId, Brevtype.INNHENT_DOKUMENTASJON))

        assertHistorikkTask(
            TilbakekrevingHistorikkinnslagstype.INNHENT_DOKUMENTASJON_BREV_SENDT,
            Aktør.SAKSBEHANDLER,
            Brevtype.INNHENT_DOKUMENTASJON,
        )
    }

    @Test
    fun `doTask skal lagre brevsporing for vedtaksbrev`() {
        lagreBrevsporingTask.doTask(opprettTask(behandlingId, Brevtype.VEDTAK))

        assertBrevsporing(Brevtype.VEDTAK)
    }

    @Test
    fun `onCompletion skal lage historikk task for vedtaksbrev`() {
        lagreBrevsporingTask.onCompletion(opprettTask(behandlingId, Brevtype.VEDTAK))

        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.VEDTAKSBREV_SENDT, Aktør.VEDTAKSLØSNING, Brevtype.VEDTAK)
    }

    @Test
    fun `onCompletion skal lage historikk task for vedtaksbrev når mottaker adresse er ukjent`() {
        lagreBrevsporingTask.onCompletion(
            opprettTask(behandlingId, Brevtype.VEDTAK).also { task ->
                task.metadata.also { it["ukjentAdresse"] = "true" }
            },
        )

        assertHistorikkTask(
            TilbakekrevingHistorikkinnslagstype.BREV_IKKE_SENDT_UKJENT_ADRESSE,
            Aktør.VEDTAKSLØSNING,
            Brevtype.VEDTAK,
        )
        taskService.finnTasksMedStatus(listOf(Status.UBEHANDLET))
            .shouldHaveSingleElement {
                it.type == LagHistorikkinnslagTask.TYPE &&
                    TilbakekrevingHistorikkinnslagstype.VEDTAKSBREV_SENDT.tekst == it.metadata["beskrivelse"]
            }
    }

    @Test
    fun `onCompletion skal lage historikk task for vedtaksbrev når adresse til dødsbo er ukjent`() {
        lagreBrevsporingTask.onCompletion(
            opprettTask(behandlingId, Brevtype.VEDTAK).also { task ->
                task.metadata.also { it["dødsboUkjentAdresse"] = "true" }
            },
        )

        assertHistorikkTask(
            TilbakekrevingHistorikkinnslagstype.BREV_IKKE_SENDT_DØDSBO_UKJENT_ADRESSE,
            Aktør.VEDTAKSLØSNING,
            Brevtype.VEDTAK,
        )
        taskService.finnTasksMedStatus(listOf(Status.UBEHANDLET))
            .shouldHaveSingleElement {
                it.type == LagHistorikkinnslagTask.TYPE &&
                    TilbakekrevingHistorikkinnslagstype.VEDTAKSBREV_SENDT.tekst == it.metadata["beskrivelse"]
            }
    }

    @Test
    fun `onCompletion skal lage AvsluttBehandlingTask ved brevtype VEDTAK, men kun når mottakeren ikke er en tilleggsmottaker`() {
        lagreBrevsporingTask.onCompletion(opprettTask(behandlingId, Brevtype.VEDTAK))
        lagreBrevsporingTask.onCompletion(opprettTask(behandlingId, Brevtype.VEDTAK, brevmottager = MANUELL_TILLEGGSMOTTAKER))
        lagreBrevsporingTask.onCompletion(opprettTask(behandlingId, Brevtype.VEDTAK, brevmottager = VERGE))

        taskService.finnTasksMedStatus(listOf(Status.UBEHANDLET))
            .single { it.type == AvsluttBehandlingTask.TYPE }
            .also { it.metadata["mottager"] shouldBe Brevmottager.BRUKER.name }
    }

    private fun opprettTask(
        behandlingId: UUID,
        brevtype: Brevtype,
        ansvarligSaksbehandler: String? = Constants.BRUKER_ID_VEDTAKSLØSNINGEN,
        brevmottager: Brevmottager = Brevmottager.BRUKER,
    ): Task {
        return Task(
            type = LagreBrevsporingTask.TYPE,
            payload = behandlingId.toString(),
            properties = Properties().apply {
                this["dokumentId"] = dokumentId
                this["journalpostId"] = journalpostId
                this["brevtype"] = brevtype.name
                this["mottager"] = brevmottager.name
                this["ansvarligSaksbehandler"] = ansvarligSaksbehandler
            },
        )
    }

    private fun assertBrevsporing(brevtype: Brevtype) {
        val brevsporing = brevsporingRepository.findFirstByBehandlingIdAndBrevtypeOrderBySporbarOpprettetTidDesc(
            behandlingId,
            brevtype,
        )
        brevsporing.shouldNotBeNull()
        brevsporing.dokumentId shouldBe dokumentId
        brevsporing.journalpostId shouldBe journalpostId
    }

    private fun assertHistorikkTask(
        historikkinnslagstype: TilbakekrevingHistorikkinnslagstype,
        aktør: Aktør,
        brevtype: Brevtype,
    ) {
        taskService.finnTasksMedStatus(listOf(Status.UBEHANDLET)).shouldHaveSingleElement {
            LagHistorikkinnslagTask.TYPE == it.type &&
                historikkinnslagstype.name == it.metadata["historikkinnslagstype"] &&
                aktør.name == it.metadata["aktør"] &&
                behandlingId.toString() == it.payload &&
                brevtype.name == it.metadata["brevtype"]
        }
    }
}
