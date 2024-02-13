package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.kravgrunnlag.task.BehandleKravgrunnlagTask
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class KravgrunnlagMottakerTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var taskService: TaskService

    @Test
    fun `verifiser at det er mulig å lagre en task`() {
        taskService.save(
            Task(
                type = BehandleKravgrunnlagTask.TYPE,
                payload = "kravgrunnlagFraOppdrag",
            ),
        )
        taskService.findAll().filter { it.type == BehandleKravgrunnlagTask.TYPE }.isNotEmpty()
    }
}
