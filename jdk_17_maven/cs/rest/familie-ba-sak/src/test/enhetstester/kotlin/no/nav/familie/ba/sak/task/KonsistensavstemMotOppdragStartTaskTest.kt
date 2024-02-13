package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.integrasjoner.økonomi.AvstemmingService
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingStartTaskDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class KonsistensavstemMotOppdragStartTaskTest {
    private val avstemmingService = mockk<AvstemmingService>()
    private val startTask = KonsistensavstemMotOppdragStartTask(avstemmingService)

    @Test
    fun `Ved kjøring av task første gang, så skal den sende start til økonomi, opprette finn perioder til avstemming task og og sende avslutt til økonomi`() {
        val (transaksjonsId, task) = opprettStartTask()

        every { avstemmingService.harBatchStatusFerdig(123L) } returns false
        every { avstemmingService.erKonsistensavstemmingStartet(transaksjonsId) } returns false
        every {
            avstemmingService.skalOppretteFinnPerioderForRelevanteBehandlingerTask(
                transaksjonsId,
                any(),
            )
        } returns true
        every {
            avstemmingService.erKonsistensavstemmingKjørtForTransaksjonsidOgChunk(
                transaksjonsId,
                range(1, 3),
            )
        } returns false
        justRun { avstemmingService.sendKonsistensavstemmingStart(any(), transaksjonsId) }
        mockTreSiderMedSisteBehandlinger()
        justRun {
            avstemmingService.opprettKonsistensavstemmingFinnPerioderForRelevanteBehandlingerTask(any(), any())
        }
        justRun { avstemmingService.opprettKonsistensavstemmingAvsluttTask(any()) }

        startTask.doTask(task)

        verify(exactly = 1) { avstemmingService.sendKonsistensavstemmingStart(any(), transaksjonsId) }
        verify(exactly = 3) {
            avstemmingService.opprettKonsistensavstemmingFinnPerioderForRelevanteBehandlingerTask(
                any(),
                any(),
            )
        }
        verify(exactly = 1) { avstemmingService.opprettKonsistensavstemmingAvsluttTask(any()) }
    }

    @Test
    fun `Ved rekjøring av task som er alt kjørt, så skal den avslutte uten å sende meldinger eller generere perioder`() {
        val (transaksjonsId, task) = opprettStartTask()

        every { avstemmingService.harBatchStatusFerdig(123L) } returns true

        startTask.doTask(task)

        verify(exactly = 0) { avstemmingService.sendKonsistensavstemmingStart(any(), transaksjonsId) }
        verify(exactly = 0) {
            avstemmingService.opprettKonsistensavstemmingFinnPerioderForRelevanteBehandlingerTask(
                any(),
                any(),
            )
        }
        verify(exactly = 0) { avstemmingService.opprettKonsistensavstemmingAvsluttTask(any()) }
    }

    @Test
    fun `Ved rekjøring av task som er delvis kjørt, så skal den ikke sende start melding, men opprette finn perioder til avstemminger som det ikke er sendt og sende avslutt melding til økonomi`() {
        val (transaksjonsId, task) = opprettStartTask()

        every { avstemmingService.harBatchStatusFerdig(123L) } returns false
        every { avstemmingService.erKonsistensavstemmingStartet(transaksjonsId) } returns true
        every {
            avstemmingService.skalOppretteFinnPerioderForRelevanteBehandlingerTask(
                transaksjonsId,
                range(1, 2),
            )
        } returns false
        every {
            avstemmingService.skalOppretteFinnPerioderForRelevanteBehandlingerTask(
                transaksjonsId,
                3,
            )
        } returns true

        justRun { avstemmingService.sendKonsistensavstemmingStart(any(), transaksjonsId) }
        mockTreSiderMedSisteBehandlinger()
        justRun {
            avstemmingService.opprettKonsistensavstemmingFinnPerioderForRelevanteBehandlingerTask(any(), any())
        }
        justRun { avstemmingService.opprettKonsistensavstemmingAvsluttTask(any()) }

        startTask.doTask(task)

        verify(exactly = 0) { avstemmingService.sendKonsistensavstemmingStart(any(), transaksjonsId) }
        verify(exactly = 1) {
            avstemmingService.opprettKonsistensavstemmingFinnPerioderForRelevanteBehandlingerTask(
                any(),
                any(),
            )
        }
        verify(exactly = 1) { avstemmingService.opprettKonsistensavstemmingAvsluttTask(any()) }
    }

    private fun mockTreSiderMedSisteBehandlinger() {
        every { avstemmingService.hentSisteIverksatteBehandlingerFraLøpendeFagsaker() } returns (1L..1450).toList()
    }

    private fun opprettStartTask(): Pair<UUID, Task> {
        val avstemmingdato = LocalDateTime.of(2022, 4, 1, 0, 0)
        val batchId = 123L
        val transaksjonsId = UUID.randomUUID()
        val payload = objectMapper.writeValueAsString(
            KonsistensavstemmingStartTaskDTO(
                batchId = batchId,
                avstemmingdato = avstemmingdato,
                transaksjonsId = transaksjonsId,
            ),
        )
        val task = Task(payload = payload, type = KonsistensavstemMotOppdragStartTask.TASK_STEP_TYPE)
        return Pair(transaksjonsId, task)
    }
}
