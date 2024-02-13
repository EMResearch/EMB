package no.nav.familie.tilbake.dokumentbestilling.felles.task

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstidspunkt
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.historikkinnslag.LagHistorikkinnslagTask
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.Properties

internal class DistribuerDokumentVedDødsfallTaskTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var distribuerDokumentVedDødsfallTask: DistribuerDokumentVedDødsfallTask

    private val behandling = Testdata.behandling
    private val behandlingId = behandling.id

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(behandling)
    }

    @Test
    fun `skal kjøre ferdig når adressen er blitt oppdatert`() {
        distribuerDokumentVedDødsfallTask.doTask(opprettTask("jp1"))

        assertHistorikkTask(
            TilbakekrevingHistorikkinnslagstype.DISTRIBUSJON_BREV_DØDSBO_SUKSESS,
        )
    }

    @Test
    fun `skal feile når adressen ikke har blitt oppdatert`() {
        val exception = shouldThrow<java.lang.RuntimeException> {
            distribuerDokumentVedDødsfallTask.doTask(opprettTask("jpUkjentDødsbo"))
        }

        exception.message shouldBe "org.springframework.web.client.RestClientResponseException: Ukjent adresse dødsbo"
    }

    @Test
    fun `skal opprette historikkinnslag når tasken er for gammel`() {
        distribuerDokumentVedDødsfallTask.doTask(
            opprettTask("jpUkjentDødsbo").copy(
                opprettetTid = LocalDateTime.now()
                    .minusMonths(7),
            ),
        )

        assertHistorikkTask(
            TilbakekrevingHistorikkinnslagstype.DISTRIBUSJON_BREV_DØDSBO_FEILET_6_MND,
        )
    }

    private fun opprettTask(journalpostId: String): Task {
        return Task(
            type = DistribuerDokumentVedDødsfallTask.TYPE,
            payload = behandling.id.toString(),
            properties = Properties().apply {
                this["journalpostId"] = journalpostId
                this["fagsystem"] = Fagsystem.BA.name
                this["distribusjonstype"] = Distribusjonstype.VIKTIG.name
                this["distribusjonstidspunkt"] = Distribusjonstidspunkt.KJERNETID.name
                this["mottager"] = Brevmottager.BRUKER.name
                this["brevtype"] = Brevtype.VEDTAK.name
                this["ansvarligSaksbehandler"] = Constants.BRUKER_ID_VEDTAKSLØSNINGEN
            },
        )
    }

    private fun assertHistorikkTask(
        historikkinnslagstype: TilbakekrevingHistorikkinnslagstype,
    ) {
        taskService.findAll().shouldHaveSingleElement {
            LagHistorikkinnslagTask.TYPE == it.type &&
                historikkinnslagstype.name == it.metadata["historikkinnslagstype"] &&
                behandlingId.toString() == it.payload
        }
    }
}
