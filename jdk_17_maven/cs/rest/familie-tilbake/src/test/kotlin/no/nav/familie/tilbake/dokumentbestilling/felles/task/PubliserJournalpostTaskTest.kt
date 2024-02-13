package no.nav.familie.tilbake.dokumentbestilling.felles.task

import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstidspunkt
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstype
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.Properties
import java.util.UUID

internal class PubliserJournalpostTaskTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var publiserJournalpostTask: PubliserJournalpostTask

    @Test
    fun `skal kjøre OK`() {
        val opprettetTask = opprettTask("jp1")
        publiserJournalpostTask.doTask(opprettetTask)

        opprettetTask.metadata["ukjentAdresse"] shouldBe null
        opprettetTask.metadata["dødsboUkjentAdresse"] shouldBe null
    }

    @Test
    fun `skal merke task med ukjentadresse når bruker har ukjent adresse`() {
        val opprettetTask = opprettTask("jpUkjentAdresse")
        publiserJournalpostTask.doTask(opprettetTask)

        opprettetTask.metadata["ukjentAdresse"] shouldBe "true"
        opprettetTask.metadata["dødsboUkjentAdresse"] shouldBe null
    }

    @Test
    fun `skal opprette DistribuerDokumentVedDødsfallTask ved ukjent adresse dødsbø`() {
        val opprettetTask = opprettTask("jpUkjentDødsbo")
        publiserJournalpostTask.doTask(opprettetTask)

        assertDistribuerDokumentVedDødsfallTask()

        opprettetTask.metadata["ukjentAdresse"] shouldBe null
        opprettetTask.metadata["dødsboUkjentAdresse"] shouldBe "true"
    }

    @Test
    fun `skal kjøre OK når dokdist sender kode 409`() {
        val opprettetTask = opprettTask("jpDuplikatDistribusjon")
        publiserJournalpostTask.doTask(opprettetTask)

        opprettetTask.metadata["ukjentAdresse"] shouldBe null
        opprettetTask.metadata["dødsboUkjentAdresse"] shouldBe null
    }

    private fun opprettTask(journalpostId: String): Task {
        return Task(
            type = PubliserJournalpostTask.TYPE,
            payload = objectMapper.writeValueAsString(PubliserJournalpostTaskData(UUID.randomUUID(), manuellAdresse = null)),
            properties = Properties().apply {
                this["journalpostId"] = journalpostId
                this["fagsystem"] = Fagsystem.BA.name
                this["distribusjonstype"] = Distribusjonstype.VIKTIG.name
                this["distribusjonstidspunkt"] = Distribusjonstidspunkt.KJERNETID.name
            },
        )
    }

    private fun assertDistribuerDokumentVedDødsfallTask() {
        taskService.finnTasksMedStatus(listOf(Status.UBEHANDLET)).shouldHaveSingleElement {
            DistribuerDokumentVedDødsfallTask.TYPE == it.type
        }
    }
}
