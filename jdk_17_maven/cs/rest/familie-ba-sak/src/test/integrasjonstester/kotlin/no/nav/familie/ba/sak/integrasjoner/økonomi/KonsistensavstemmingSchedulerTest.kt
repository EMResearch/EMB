package no.nav.familie.ba.sak.integrasjoner.økonomi

import io.mockk.called
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.config.AbstractMockkSpringRunner
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.prosessering.domene.Status
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-brev-klient", "integrasjonstest")
@Tag("integration")
class KonsistensavstemmingSchedulerTest : AbstractMockkSpringRunner() {

    @Autowired
    lateinit var taskRepository: TaskRepositoryWrapper

    @Autowired
    lateinit var batchService: BatchService

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var fagsakService: FagsakService

    @Autowired
    lateinit var konsistensavstemmingScheduler: KonsistensavstemmingScheduler

    @Autowired
    private lateinit var databaseCleanupService: DatabaseCleanupService

    @Autowired
    private lateinit var featureToggleService: FeatureToggleService

    @BeforeEach
    fun setUp() {
        databaseCleanupService.truncate()
        konsistensavstemmingScheduler =
            KonsistensavstemmingScheduler(
                batchService,
                behandlingService,
                fagsakService,
                taskRepository,
                featureToggleService,
            )
        taskRepository = spyk(taskRepository)
    }

    @Test
    fun `Skal ikke trigge avstemming når det ikke er noen ledige batchkjøringer for dato`() {
        val dagensDato = LocalDate.now()
        val nyBatch = Batch(kjøreDato = dagensDato, status = KjøreStatus.TATT)
        batchService.lagreNyStatus(nyBatch, KjøreStatus.TATT)

        konsistensavstemmingScheduler.utførKonsistensavstemming()

        verify { taskRepository wasNot called }
    }

    @Test
    fun `Skal ikke trigge avstemming når det ikke finnes batchkjøringer for dato`() {
        val imorgen = LocalDate.now().plusDays(1)
        val nyBatch = Batch(kjøreDato = imorgen)
        batchService.lagreNyStatus(nyBatch, KjøreStatus.LEDIG)

        konsistensavstemmingScheduler.utførKonsistensavstemming()

        verify { taskRepository wasNot called }
    }

    @Test
    fun `Skal trigge en avstemming når det er ledig batchkjøring for dato`() {
        val dagensDato = LocalDate.now()
        val nyBatch = Batch(kjøreDato = dagensDato)
        batchService.lagreNyStatus(nyBatch, KjøreStatus.LEDIG)
        fagsakService.hentLøpendeFagsaker().forEach { fagsakService.oppdaterStatus(it, FagsakStatus.AVSLUTTET) }

        konsistensavstemmingScheduler.utførKonsistensavstemming()

        val tasks = taskRepository.findByStatus(Status.UBEHANDLET)
        Assertions.assertEquals(1, tasks.size)

        // Setter task til Ferdig for å unngå at den kjøres fra andre tester.
        taskRepository.save(tasks[0].copy(status = Status.FERDIG))
    }
}
