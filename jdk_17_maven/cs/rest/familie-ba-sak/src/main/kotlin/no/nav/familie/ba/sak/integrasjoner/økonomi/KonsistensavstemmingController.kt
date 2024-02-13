package no.nav.familie.ba.sak.integrasjoner.økonomi

import jakarta.transaction.Transactional
import no.nav.familie.ba.sak.task.KonsistensavstemMotOppdragStartTask
import no.nav.familie.ba.sak.task.dto.KonsistensavstemmingStartTaskDTO
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/konsistensavstemming")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class KonsistensavstemmingController(
    private val taskService: TaskService,
    private val batchRepository: BatchRepository,
) {

    @PostMapping(path = ["/dryrun"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Transactional
    fun kjørKonsistensavstemmingUtenSendingTilØkonomi(): ResponseEntity<Ressurs<String>> {
        val (transaksjonsId, task) = opprettKonsistensavstemMotOppdragStartTask(false, LocalDateTime.now())

        return ResponseEntity.ok(Ressurs.success("Testkjører konsistensavstemming uten å sende til økonomi. transaksjonsId=$transaksjonsId callId=${task.callId}"))
    }

    @PostMapping(path = ["/run"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Transactional
    fun kjørKonsistensavstemming(@RequestBody request: StartKonsistensavstemming): ResponseEntity<Ressurs<String>> {
        val (transaksjonsId, task) = opprettKonsistensavstemMotOppdragStartTask(true, request.triggerTid)

        return ResponseEntity.ok(Ressurs.success("Kjører konsistensavstemming. transaksjonsId=$transaksjonsId callId=${task.callId}"))
    }

    data class StartKonsistensavstemming(val triggerTid: LocalDateTime)

    private fun opprettKonsistensavstemMotOppdragStartTask(
        sendTilØkonomi: Boolean,
        triggerTid: LocalDateTime,
    ): Pair<UUID, Task> {
        val transaksjonsId = UUID.randomUUID()
        val batch = batchRepository.saveAndFlush(Batch(kjøreDato = LocalDate.now(), status = KjøreStatus.MANUELL))
        val task = taskService.save(
            Task(
                type = KonsistensavstemMotOppdragStartTask.TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(
                    KonsistensavstemmingStartTaskDTO(
                        batchId = batch.id,
                        avstemmingdato = triggerTid,
                        transaksjonsId = transaksjonsId,
                        sendTilØkonomi = sendTilØkonomi,
                    ),
                ),
                triggerTid = triggerTid,
            ),
        )
        return Pair(transaksjonsId, task)
    }
}
