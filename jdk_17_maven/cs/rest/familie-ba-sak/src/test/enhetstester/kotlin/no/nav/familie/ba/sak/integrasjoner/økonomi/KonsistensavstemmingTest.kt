package no.nav.familie.ba.sak.integrasjoner.økonomi

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.task.KonsistensavstemMotOppdragAvsluttTask
import no.nav.familie.ba.sak.task.KonsistensavstemMotOppdragDataTask
import no.nav.familie.ba.sak.task.KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask
import no.nav.familie.ba.sak.task.KonsistensavstemMotOppdragStartTask
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingAvsluttTaskDTO
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingDataTaskDTO
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingStartTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KonsistensavstemmingTest {
    private val økonomiKlient = mockk<ØkonomiKlient>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val beregningService = mockk<BeregningService>()
    private val taskService = mockk<TaskService>(relaxed = true)
    private val batchRepository = mockk<BatchRepository>()
    private val dataChunkRepository = mockk<DataChunkRepository>(relaxed = true)

    private val avstemmingService = AvstemmingService(
        behandlingHentOgPersisterService,
        økonomiKlient,
        beregningService,
        taskService,
        batchRepository,
        dataChunkRepository,
    )

    private val batchId = 1000000L
    private val behandlingId = BigInteger.ONE
    private val avstemmingsdato = LocalDateTime.now()

    private lateinit var konistensavstemmingStartTask: KonsistensavstemMotOppdragStartTask
    private lateinit var konsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask: KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask
    private lateinit var konsistensavstemMotOppdragDataTask: KonsistensavstemMotOppdragDataTask
    private lateinit var konsistensavstemMotOppdragAvsluttTask: KonsistensavstemMotOppdragAvsluttTask

    @BeforeEach
    fun setUp() {
        every { taskService.save(any()) } returns Task(type = "dummy", payload = "")
        konistensavstemmingStartTask = KonsistensavstemMotOppdragStartTask(avstemmingService)
        konsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask =
            KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask(avstemmingService, taskService)
        konsistensavstemMotOppdragDataTask = KonsistensavstemMotOppdragDataTask(avstemmingService)
        konsistensavstemMotOppdragAvsluttTask =
            KonsistensavstemMotOppdragAvsluttTask(avstemmingService, dataChunkRepository, BatchService(batchRepository))
    }

    @Test
    fun `Første gangs kjøring av start task - Verifiser at konsistensavstemOppdragStart oppretter finn perioder for relevante behandlinger task- og avslutt task og sender start melding hvis transaksjon ikke allerede kjørt`() {
        val transaksjonsId = UUID.randomUUID()
        val avstemmingsdatoSlot = lagMockForStartTaskHappCase(transaksjonsId)
        konistensavstemmingStartTask.doTask(
            Task(
                payload = objectMapper.writeValueAsString(
                    KonsistensavstemmingStartTaskDTO(
                        batchId,
                        avstemmingsdato,
                        transaksjonsId,
                    ),
                ),
                type = KonsistensavstemMotOppdragStartTask.TASK_STEP_TYPE,
            ),
        )

        val taskSlots = mutableListOf<Task>()
        verify(atLeast = 1) { taskService.save(capture(taskSlots)) }
        // sjekk at KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask er opprettet
        val finnPerioderForRelevanteBehandlingerTask =
            finnFørsteTaskAvTypePåTransaksjonsId(
                taskSlots,
                transaksjonsId,
                KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask.TASK_STEP_TYPE,
            )
        assertThat(finnPerioderForRelevanteBehandlingerTask).isNotNull
        val finnPerioderForRelevanteBehandlingerDto =
            objectMapper.readValue(
                finnPerioderForRelevanteBehandlingerTask!!.payload,
                KonsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO::class.java,
            )
        assertEquals(batchId, finnPerioderForRelevanteBehandlingerDto.batchId)
        assertEquals(transaksjonsId, finnPerioderForRelevanteBehandlingerDto.transaksjonsId)
        assertEquals(1, finnPerioderForRelevanteBehandlingerDto.chunkNr)
        assertThat(finnPerioderForRelevanteBehandlingerDto.relevanteBehandlinger).hasSize(1).containsExactly(1)

        // sjekk at KonsistensavstemMotOppdragAvsluttTask er opprettet
        val finnAvsluttTask = finnFørsteTaskAvTypePåTransaksjonsId(
            taskSlots,
            transaksjonsId,
            KonsistensavstemMotOppdragAvsluttTask.TASK_STEP_TYPE,
        )
        val finnAvsluttTaskDto =
            objectMapper.readValue(
                finnAvsluttTask!!.payload,
                KonsistensavstemmingAvsluttTaskDTO::class.java,
            )
        assertThat(finnAvsluttTask).isNotNull
        assertEquals(batchId, finnAvsluttTaskDto.batchId)
        assertEquals(transaksjonsId, finnAvsluttTaskDto.transaksjonsId)

        // sjekk at datachunk er opprettet
        val dataChunkSlot = mutableListOf<DataChunk>()
        verify(atLeast = 1) { dataChunkRepository.save(capture(dataChunkSlot)) }
        assertThat(dataChunkSlot.filter { it.transaksjonsId == transaksjonsId }).hasSize(1)
        assertEquals(1, dataChunkSlot.find { it.transaksjonsId == transaksjonsId }?.chunkNr)

        // sjekk at det har blitt sendt startmelding
        verify(exactly = 1) {
            økonomiKlient.konsistensavstemOppdragStart(
                avstemmingsdato = avstemmingsdatoSlot.captured,
                transaksjonsId = transaksjonsId,
            )
        }
    }

    private fun finnFørsteTaskAvTypePåTransaksjonsId(
        tasker: List<Task>,
        transaksjonsId: UUID,
        type: String,
    ): Task? {
        return tasker.find { it.payload.contains(transaksjonsId.toString()) && it.type == type }
    }

    @Test
    fun `Rekjøring av start task - Verifiser at konsistensavstemming ikke kjører hvis alle datachunker allerede er sendt til økonomi for transaksjonId`() {
        every { batchRepository.getReferenceById(batchId) } returns Batch(
            kjøreDato = LocalDate.now(),
            status = KjøreStatus.FERDIG,
        )
        val transaksjonsId = UUID.randomUUID()

        konistensavstemmingStartTask.doTask(
            Task(
                payload = objectMapper.writeValueAsString(
                    KonsistensavstemmingStartTaskDTO(
                        batchId,
                        avstemmingsdato,
                        transaksjonsId,
                    ),
                ),
                type = KonsistensavstemMotOppdragStartTask.TASK_STEP_TYPE,
            ),
        )

        verify(exactly = 0) { avstemmingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker() }
    }

    @Test
    fun `Rekjøring av start task - Verifiser at konsistensavstemming kun rekjører chunker som ikke allerede er kjørt`() {
        val transaksjonsId = UUID.randomUUID()
        lagMockForStartTaskHappCase(transaksjonsId)
        val datachunks = listOf(
            DataChunk(
                batch = Batch(kjøreDato = LocalDate.now()),
                transaksjonsId = transaksjonsId,
                erSendt = true,
                chunkNr = 1,
            ),
            DataChunk(
                batch = Batch(kjøreDato = LocalDate.now()),
                transaksjonsId = transaksjonsId,
                erSendt = false,
                chunkNr = 2,
            ),
        )
        every { dataChunkRepository.findByTransaksjonsIdAndChunkNr(transaksjonsId, 1) } returns datachunks[0]
        every { dataChunkRepository.findByTransaksjonsIdAndChunkNr(transaksjonsId, 2) } returns datachunks[1]
        every { dataChunkRepository.findByTransaksjonsIdAndChunkNr(transaksjonsId, 3) } returns null

        every { behandlingHentOgPersisterService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker() } returns (1..1450).toList()
            .map { it.toLong() }

        konistensavstemmingStartTask.doTask(
            Task(
                payload = objectMapper.writeValueAsString(
                    KonsistensavstemmingStartTaskDTO(
                        batchId,
                        avstemmingsdato,
                        transaksjonsId,
                    ),
                ),
                type = KonsistensavstemMotOppdragStartTask.TASK_STEP_TYPE,
            ),
        )

        verify(exactly = 1) { avstemmingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker() }
        val taskSlots = mutableListOf<Task>()
        verify(exactly = 2) { taskService.save(capture(taskSlots)) }

        assertEquals(
            KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask.TASK_STEP_TYPE,
            taskSlots[0].type,
        )
        val finnPerioderForRelevanteBehandlingerDto =
            objectMapper.readValue(
                taskSlots[0].payload,
                KonsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO::class.java,
            )
        assertEquals(batchId, finnPerioderForRelevanteBehandlingerDto.batchId)
        assertEquals(transaksjonsId, finnPerioderForRelevanteBehandlingerDto.transaksjonsId)
        assertEquals(3, finnPerioderForRelevanteBehandlingerDto.chunkNr)
        assertThat(finnPerioderForRelevanteBehandlingerDto.relevanteBehandlinger).hasSize(450)

        assertEquals(KonsistensavstemMotOppdragAvsluttTask.TASK_STEP_TYPE, taskSlots[1].type)
    }

    @Test
    fun `Verifiser at konsistensavstemPeriodeFinnPerioderForRelevanteBehandlingerTask finner perioder for behandlinger og oppretter data task`() {
        val transaksjonsId = UUID.randomUUID()
        lagMockFinnPerioderForRelevanteBehandlingerHappeCase()

        konsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask.doTask(
            Task(
                payload = objectMapper.writeValueAsString(
                    KonsistensavstemmingFinnPerioderForRelevanteBehandlingerDTO(
                        batchId,
                        transaksjonsId,
                        avstemmingsdato,
                        1,
                        listOf(behandlingId.toLong()),
                    ),
                ),
                type = KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask.TASK_STEP_TYPE,
            ),
        )

        val taskSlots = mutableListOf<Task>()
        verify(atLeast = 1) { taskService.save(capture(taskSlots)) }
        val konsistensavstemmingDataDto =
            objectMapper.readValue(taskSlots.last().payload, KonsistensavstemmingDataTaskDTO::class.java)
        assertEquals(konsistensavstemmingDataDto.chunkNr, 1)
        assertEquals(konsistensavstemmingDataDto.transaksjonsId, transaksjonsId)
        assertThat(konsistensavstemmingDataDto.perioderForBehandling)
            .hasSize(1).extracting("behandlingId").containsExactly(behandlingId.toString())
    }

    @Test
    fun `Verifiser at konsistensavstemOppdragData sender data og oppdatere datachunk tabellen`() {
        val transaksjonsId = UUID.randomUUID()
        lagMockOppdragDataHappeCase(transaksjonsId)
        every { dataChunkRepository.findByTransaksjonsIdAndChunkNr(transaksjonsId, 1) } returns
            DataChunk(
                batch = Batch(id = batchId, kjøreDato = LocalDate.now()),
                transaksjonsId = transaksjonsId,
                chunkNr = 1,
            )
        every {
            økonomiKlient.konsistensavstemOppdragData(
                avstemmingsdato,
                emptyList(),
                transaksjonsId,
            )
        } returns ""

        konsistensavstemMotOppdragDataTask.doTask(
            Task(
                payload = objectMapper.writeValueAsString(
                    KonsistensavstemmingDataTaskDTO(
                        transaksjonsId = transaksjonsId,
                        chunkNr = 1,
                        avstemmingdato = avstemmingsdato,
                        perioderForBehandling = emptyList(),
                        sendTilØkonomi = true,
                    ),
                ),
                type = KonsistensavstemMotOppdragDataTask.TASK_STEP_TYPE,
            ),

        )

        val dataChunkSlot = slot<DataChunk>()
        verify(exactly = 1) { dataChunkRepository.save(capture(dataChunkSlot)) }
        assertThat(dataChunkSlot.captured.erSendt).isTrue()

        verify(exactly = 1) {
            økonomiKlient.konsistensavstemOppdragData(
                avstemmingsdato = avstemmingsdato,
                perioderTilAvstemming = emptyList(),
                transaksjonsId = transaksjonsId,
            )
        }

        assertEquals(1, dataChunkSlot.captured.chunkNr)
        assertEquals(transaksjonsId, dataChunkSlot.captured.transaksjonsId)
        assertEquals(true, dataChunkSlot.captured.erSendt)
    }

    @Test
    fun `Kjør alle tasker med input generert fra task som oppretter tasken`() {
        val transaksjonsId = UUID.randomUUID()
        lagMockForStartTaskHappCase(transaksjonsId)
        konistensavstemmingStartTask.doTask(
            Task(
                payload = objectMapper.writeValueAsString(
                    KonsistensavstemmingStartTaskDTO(
                        batchId,
                        avstemmingsdato,
                        transaksjonsId,
                    ),
                ),
                type = KonsistensavstemMotOppdragStartTask.TASK_STEP_TYPE,
            ),
        )
        val taskSlots = mutableListOf<Task>()
        verify(atLeast = 2) { taskService.save(capture(taskSlots)) }
        val finnPerioderTask = finnFørsteTaskAvTypePåTransaksjonsId(
            taskSlots,
            transaksjonsId,
            KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask.TASK_STEP_TYPE,
        )!!

        lagMockFinnPerioderForRelevanteBehandlingerHappeCase()
        konsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask.doTask(
            Task(
                payload = finnPerioderTask.payload,
                type = KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask.TASK_STEP_TYPE,
            ),
        )
        verify(atLeast = 3) { taskService.save(capture(taskSlots)) }

        lagMockOppdragDataHappeCase(transaksjonsId)
        val finnDataTask = finnFørsteTaskAvTypePåTransaksjonsId(
            taskSlots,
            transaksjonsId,
            KonsistensavstemMotOppdragDataTask.TASK_STEP_TYPE,
        )!!
        konsistensavstemMotOppdragDataTask.doTask(
            Task(
                payload = finnDataTask.payload,
                type = KonsistensavstemMotOppdragDataTask.TASK_STEP_TYPE,
            ),
        )
        verify(atLeast = 3) { taskService.save(capture(taskSlots)) }
        val dataTask = finnFørsteTaskAvTypePåTransaksjonsId(
            taskSlots,
            transaksjonsId,
            KonsistensavstemMotOppdragDataTask.TASK_STEP_TYPE,
        )!!
        val datachunksSlot = mutableListOf<DataChunk>()
        verify(atLeast = 2) { dataChunkRepository.save(capture(datachunksSlot)) }
        assertThat(datachunksSlot.last { it.transaksjonsId == transaksjonsId }.erSendt).isTrue()

        val dataTaskDto = objectMapper.readValue(dataTask.payload, KonsistensavstemmingDataTaskDTO::class.java)
        assertThat(dataTaskDto.chunkNr).isEqualTo(1)
        assertThat(dataTaskDto.transaksjonsId).isEqualTo(transaksjonsId)
        assertThat(dataTaskDto.perioderForBehandling).hasSize(1)
        assertThat(dataTaskDto.sendTilØkonomi).isTrue()

        lagMockAvsluttHappyCase(transaksjonsId)
        val avsluttTask = finnFørsteTaskAvTypePåTransaksjonsId(
            taskSlots,
            transaksjonsId,
            KonsistensavstemMotOppdragAvsluttTask.TASK_STEP_TYPE,
        )!!
        konsistensavstemMotOppdragAvsluttTask.doTask(
            Task(
                payload = avsluttTask.payload,
                type = KonsistensavstemMotOppdragAvsluttTask.TASK_STEP_TYPE,
            ),
        )

        verify(exactly = 1) { økonomiKlient.konsistensavstemOppdragStart(any(), transaksjonsId) }
        verify(exactly = 1) { økonomiKlient.konsistensavstemOppdragData(any(), any(), transaksjonsId) }
        verify(exactly = 1) { økonomiKlient.konsistensavstemOppdragAvslutt(any(), transaksjonsId) }
    }

    @Test
    fun `Kjør alle tasker med input generert fra task som oppretter tasken og send til økonomi skrudd av`() {
        val transaksjonsId = UUID.randomUUID()
        lagMockForStartTaskHappCase(transaksjonsId)
        konistensavstemmingStartTask.doTask(
            Task(
                payload = objectMapper.writeValueAsString(
                    KonsistensavstemmingStartTaskDTO(
                        batchId,
                        avstemmingsdato,
                        transaksjonsId,
                        false,
                    ),
                ),
                type = KonsistensavstemMotOppdragStartTask.TASK_STEP_TYPE,
            ),
        )
        val taskSlots = mutableListOf<Task>()
        verify(atLeast = 2) { taskService.save(capture(taskSlots)) }

        lagMockFinnPerioderForRelevanteBehandlingerHappeCase()
        val finnPerioderForRelevanteBehandlingerTask = finnFørsteTaskAvTypePåTransaksjonsId(
            taskSlots,
            transaksjonsId,
            KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask.TASK_STEP_TYPE,
        )!!
        konsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask.doTask(
            Task(
                payload = finnPerioderForRelevanteBehandlingerTask.payload,
                type = KonsistensavstemMotOppdragFinnPerioderForRelevanteBehandlingerTask.TASK_STEP_TYPE,
            ),
        )
        verify(atLeast = 3) { taskService.save(capture(taskSlots)) }

        lagMockOppdragDataHappeCase(transaksjonsId)
        konsistensavstemMotOppdragDataTask.doTask(
            Task(
                payload = finnFørsteTaskAvTypePåTransaksjonsId(
                    taskSlots,
                    transaksjonsId,
                    KonsistensavstemMotOppdragDataTask.TASK_STEP_TYPE,
                )!!.payload,
                type = KonsistensavstemMotOppdragDataTask.TASK_STEP_TYPE,
            ),
        )
        verify(atLeast = 3) { taskService.save(capture(taskSlots)) }

        lagMockAvsluttHappyCase(transaksjonsId)
        val avsluttTask = finnFørsteTaskAvTypePåTransaksjonsId(
            taskSlots,
            transaksjonsId,
            KonsistensavstemMotOppdragAvsluttTask.TASK_STEP_TYPE,
        )!!
        konsistensavstemMotOppdragAvsluttTask.doTask(
            Task(
                payload = avsluttTask.payload,
                type = KonsistensavstemMotOppdragAvsluttTask.TASK_STEP_TYPE,
            ),
        )

        verify(exactly = 0) { økonomiKlient.konsistensavstemOppdragStart(any(), transaksjonsId) }
        verify(exactly = 0) { økonomiKlient.konsistensavstemOppdragData(any(), any(), transaksjonsId) }
        verify(exactly = 0) { økonomiKlient.konsistensavstemOppdragAvslutt(any(), transaksjonsId) }
    }

    private fun lagMockForStartTaskHappCase(transaksjonsId: UUID): CapturingSlot<LocalDateTime> {
        val behandlingId = 1L
        every { behandlingHentOgPersisterService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker() } returns listOf(
            behandlingId,
        )

        every { batchRepository.getReferenceById(batchId) } returns Batch(id = batchId, kjøreDato = LocalDate.now())
        every { dataChunkRepository.save(any()) } returns DataChunk(
            batch = Batch(kjøreDato = LocalDate.now()),
            chunkNr = 1,
            transaksjonsId = transaksjonsId,
        )

        val avstemmingsdatoSlot = slot<LocalDateTime>()
        every {
            økonomiKlient.konsistensavstemOppdragStart(
                capture(avstemmingsdatoSlot),
                transaksjonsId,
            )
        } returns ""

        every { dataChunkRepository.findByTransaksjonsIdAndChunkNr(transaksjonsId, any()) } returns null
        return avstemmingsdatoSlot
    }

    private fun lagMockFinnPerioderForRelevanteBehandlingerHappeCase() {
        every {
            beregningService.hentLøpendeAndelerTilkjentYtelseMedUtbetalingerForBehandlinger(
                any(),
                any(),
            )
        } returns listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.now().minusMonths(4),
                tom = YearMonth.now(),
                periodeIdOffset = 0,
            ).also { it.kildeBehandlingId = behandlingId.toLong() },
        )
        val aktivFødselsnummere = mapOf(behandlingId.toLong() to "test")
        every { behandlingHentOgPersisterService.hentAktivtFødselsnummerForBehandlinger(any()) } returns aktivFødselsnummere
        every { behandlingHentOgPersisterService.hentTssEksternIdForBehandlinger(any()) } returns emptyMap()
    }

    private fun lagMockOppdragDataHappeCase(transaksjonsId: UUID) {
        every {
            økonomiKlient.konsistensavstemOppdragData(
                any(),
                any(),
                transaksjonsId,
            )
        } returns ""

        every { dataChunkRepository.findByTransaksjonsIdAndChunkNr(transaksjonsId, 1) } returns DataChunk(
            batch = Batch(kjøreDato = LocalDate.now()),
            chunkNr = 1,
            transaksjonsId = transaksjonsId,
        )
        every { dataChunkRepository.save(any()) } returns DataChunk(
            batch = Batch(kjøreDato = LocalDate.now()),
            chunkNr = 1,
            transaksjonsId = transaksjonsId,
        )
    }

    private fun lagMockAvsluttHappyCase(transaksjonsId: UUID) {
        every {
            økonomiKlient.konsistensavstemOppdragAvslutt(
                any(),
                transaksjonsId,
            )
        } returns ""

        every { batchRepository.saveAndFlush(any()) } returns Batch(kjøreDato = LocalDate.now())
    }
}
