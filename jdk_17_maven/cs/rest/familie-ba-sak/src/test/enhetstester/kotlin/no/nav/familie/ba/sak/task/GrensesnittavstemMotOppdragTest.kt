package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.økonomi.AvstemmingService
import no.nav.familie.ba.sak.task.dto.GrensesnittavstemmingTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate
import java.util.Properties

class GrensesnittavstemMotOppdragTest {

    private lateinit var grensesnittavstemMotOppdrag: GrensesnittavstemMotOppdrag
    private lateinit var taskRepositoryMock: TaskRepositoryWrapper

    @BeforeEach
    fun setUp() {
        val avstemmingServiceMock = mockk<AvstemmingService>()
        taskRepositoryMock = mockk()
        grensesnittavstemMotOppdrag = GrensesnittavstemMotOppdrag(avstemmingServiceMock, taskRepositoryMock)
    }

    @ParameterizedTest
    @CsvSource(
        "2020-01-06, 2020-01-07, task som kjører en mandag og oppretter task på en tirsdag",
        "2020-01-07, 2020-01-08, task som kjører en tirsdag og oppretter task på en onsdag",
        "2020-01-08, 2020-01-09, task som kjører en onsdag og oppretter task på en torsdag",
        "2020-01-09, 2020-01-10, task som kjører en torsdag og oppretter task på en fredag",
        "2020-01-10, 2020-01-13, task som kjører en fredag og oppretter task på en mandag",
    )
    fun `Skal opprette task for neste arbeidsdag`(triggerDato: LocalDate, nesteTriggerDato: LocalDate, denneTester: String) {
        val slot = slot<Task>()
        every { taskRepositoryMock.save(capture(slot)) } answers { slot.captured }

        grensesnittavstemMotOppdrag.onCompletion(
            Task(
                payload = objectMapper.writeValueAsString(
                    GrensesnittavstemmingTaskDTO(
                        fomDato = triggerDato.minusDays(1).atStartOfDay(),
                        tomDato = triggerDato.atStartOfDay(),
                    ),
                ),
                properties = Properties(),
                type = GrensesnittavstemMotOppdrag.TASK_STEP_TYPE,

            ).medTriggerTid(triggerDato.atTime(8, 0, 0)),
        )

        val lagretTask = slot.captured
        val testDto = objectMapper.readValue(lagretTask.payload, GrensesnittavstemmingTaskDTO::class.java)

        assertEquals(triggerDato.atStartOfDay(), testDto.fomDato)
        assertEquals(nesteTriggerDato.atStartOfDay(), testDto.tomDato)
        assertEquals(nesteTriggerDato.atTime(8, 0, 0), lagretTask.triggerTid)
    }

    @Test
    fun skalBeregneNesteAvstemmingForSammenhengendeHelligdag() {
        val juledagen = LocalDate.of(2019, 12, 24)

        val testDto = grensesnittavstemMotOppdrag.nesteAvstemmingDTO(juledagen)

        assertEquals(LocalDate.of(2019, 12, 27).atStartOfDay(), testDto.tomDato)
        assertEquals(LocalDate.of(2019, 12, 24).atStartOfDay(), testDto.fomDato)
    }

    @Test
    fun skalBeregneNesteAvstemmingForEnkeltHelligdag() {
        val nyttårsdag = LocalDate.of(2019, 12, 31)

        val testDto = grensesnittavstemMotOppdrag.nesteAvstemmingDTO(nyttårsdag)

        assertEquals(LocalDate.of(2020, 1, 2).atStartOfDay(), testDto.tomDato)
        assertEquals(LocalDate.of(2019, 12, 31).atStartOfDay(), testDto.fomDato)
    }

    @Test
    fun skalBeregneNesteAvstemmingForLanghelg() {
        val valborg = LocalDate.of(2020, 4, 30)

        val testDto = grensesnittavstemMotOppdrag.nesteAvstemmingDTO(valborg)

        assertEquals(LocalDate.of(2020, 5, 4).atStartOfDay(), testDto.tomDato)
        assertEquals(LocalDate.of(2020, 4, 30).atStartOfDay(), testDto.fomDato)
    }

    @Test
    fun skalBeregneNesteAvstemmingForUkedag() {
        val enTirsdag = LocalDate.of(2020, 1, 14)

        val testDto = grensesnittavstemMotOppdrag.nesteAvstemmingDTO(enTirsdag)

        assertEquals(LocalDate.of(2020, 1, 15).atStartOfDay(), testDto.tomDato)
        assertEquals(LocalDate.of(2020, 1, 14).atStartOfDay(), testDto.fomDato)
    }

    @Test
    fun skalLageNyAvstemmingstaskEtterJobb() {
        val iDag = LocalDate.of(2020, 1, 15).atStartOfDay()
        val testTask = Task(
            type = GrensesnittavstemMotOppdrag.TASK_STEP_TYPE,
            payload = objectMapper.writeValueAsString(
                GrensesnittavstemmingTaskDTO(
                    iDag.minusDays(1),
                    iDag,
                ),
            ),
        ).medTriggerTid(
            iDag.toLocalDate().atTime(8, 0),
        )
        val slot = slot<Task>()
        every { taskRepositoryMock.save(any()) } returns testTask

        grensesnittavstemMotOppdrag.onCompletion(testTask)

        verify(exactly = 1) { taskRepositoryMock.save(capture(slot)) }
        assertEquals(GrensesnittavstemMotOppdrag.TASK_STEP_TYPE, slot.captured.type)
        assertEquals(iDag.plusDays(1).toLocalDate().atTime(8, 0), slot.captured.triggerTid)
        val taskDTO = objectMapper.readValue(slot.captured.payload, GrensesnittavstemmingTaskDTO::class.java)
        assertEquals(taskDTO.fomDato, iDag)
        assertEquals(taskDTO.tomDato, iDag.plusDays(1))
    }
}
