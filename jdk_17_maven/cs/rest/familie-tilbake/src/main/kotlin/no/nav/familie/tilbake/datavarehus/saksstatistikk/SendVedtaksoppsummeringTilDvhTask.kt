package no.nav.familie.tilbake.datavarehus.saksstatistikk

import jakarta.validation.Validation
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak.Vedtaksoppsummering
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = SendVedtaksoppsummeringTilDvhTask.TYPE,
    beskrivelse = "Sender oppsummering av vedtak til datavarehus.",
)
class SendVedtaksoppsummeringTilDvhTask(
    private val vedtaksoppsummeringService: VedtaksoppsummeringService,
    private val kafkaProducer: KafkaProducer,
) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")
    private val validator = Validation.buildDefaultValidatorFactory().validator

    override fun doTask(task: Task) {
        log.info("SendVedtaksoppsummeringTilDvhTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        val behandlingId = UUID.fromString(task.payload)
        val vedtaksoppsummering: Vedtaksoppsummering = vedtaksoppsummeringService.hentVedtaksoppsummering(behandlingId)
        validate(vedtaksoppsummering)

        secureLogger.info(
            "Sender Vedtaksoppsummering=${objectMapper.writeValueAsString(vedtaksoppsummering)} til Dvh " +
                "for behandling $behandlingId",
        )
        kafkaProducer.sendVedtaksdata(behandlingId, vedtaksoppsummering)
    }

    private fun validate(vedtaksoppsummering: Vedtaksoppsummering) {
        val valideringsfeil = validator.validate(vedtaksoppsummering)
        require(valideringsfeil.isEmpty()) {
            "Valideringsfeil for ${vedtaksoppsummering::class.simpleName}: Valideringsfeil:$valideringsfeil"
        }
    }

    companion object {

        const val TYPE = "dvh.send.vedtak"
    }
}
