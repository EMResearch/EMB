package no.nav.familie.tilbake.datavarehus.saksstatistikk

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.datavarehus.saksstatistikk.sakshendelse.Behandlingstilstand
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = SendSakshendelseTilDvhTask.TASK_TYPE,
    beskrivelse = "Sending av sakshendelser til datavarehus",
)
class SendSakshendelseTilDvhTask(private val kafkaProducer: KafkaProducer) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("SendSakshendelseTilDvhTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        val behandlingId = UUID.fromString(task.payload)
        val behandlingstilstand: Behandlingstilstand = objectMapper.readValue(task.metadata.getProperty("behandlingstilstand"))
        kafkaProducer.sendSaksdata(
            behandlingId,
            behandlingstilstand.copy(tekniskTidspunkt = OffsetDateTime.now(ZoneOffset.UTC)),
        )
    }

    companion object {

        const val TASK_TYPE = "dvh.send.sakshendelse"
    }
}
