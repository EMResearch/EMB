package no.nav.familie.tilbake.kravgrunnlag

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.avstemming.task.AvstemmingTask
import no.nav.familie.tilbake.common.repository.Sporbar
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kravgrunnlag.batch.HentFagsystemsbehandlingTask
import no.nav.familie.tilbake.kravgrunnlag.batch.HåndterGamleKravgrunnlagBatch
import no.nav.familie.tilbake.kravgrunnlag.batch.HåndterGammelKravgrunnlagTask
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID

internal class HåndterGamleKravgrunnlagBatchTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var mottattXmlRepository: ØkonomiXmlMottattRepository

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var håndterGamleKravgrunnlagBatch: HåndterGamleKravgrunnlagBatch

    @Test
    fun `utfør skal ikke opprette tasker når det ikke finnes noen kravgrunnlag som er gamle enn bestemte uker`() {
        mottattXmlRepository.insert(Testdata.økonomiXmlMottatt)

        håndterGamleKravgrunnlagBatch.utfør()
        (taskService.findAll().filter { it.type != AvstemmingTask.TYPE }).shouldBeEmpty()
    }

    @Test
    fun `utfør skal ikke opprette tasker når det allerede finnes en feilet task på det samme kravgrunnlag`() {
        val mottattXml = mottattXmlRepository.insert(Testdata.økonomiXmlMottatt)
        val task = taskService.save(Task(type = HåndterGammelKravgrunnlagTask.TYPE, payload = mottattXml.id.toString()))
        taskService.save(taskService.findById(task.id).copy(status = Status.FEILET))

        håndterGamleKravgrunnlagBatch.utfør()
        taskService.findAll().any { it.type == HentFagsystemsbehandlingTask.TYPE }.shouldBeFalse()
    }

    @Test
    fun `utfør skal opprette tasker når det finnes noen kravgrunnlag som er gamle enn bestemte uker`() {
        val førsteXml = Testdata.økonomiXmlMottatt.copy(
            id = UUID.randomUUID(),
            sporbar = Sporbar(opprettetTid = LocalDateTime.now().minusWeeks(9)),
        )
        mottattXmlRepository.insert(førsteXml)

        val andreXml = Testdata.økonomiXmlMottatt.copy(
            id = UUID.randomUUID(),
            sporbar = Sporbar(opprettetTid = LocalDateTime.now().minusWeeks(9)),
            ytelsestype = Ytelsestype.SKOLEPENGER,
        )
        mottattXmlRepository.insert(andreXml)

        val tredjeXml = Testdata.økonomiXmlMottatt
        mottattXmlRepository.insert(tredjeXml)

        håndterGamleKravgrunnlagBatch.utfør()
        (taskService.findAll() as List<*>).shouldNotBeEmpty()
        taskService.findAll().count { it.type == HentFagsystemsbehandlingTask.TYPE } shouldBe 2
    }
}
