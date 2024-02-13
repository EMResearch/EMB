package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.ekstern.pensjon.HentAlleIdenterTilPsysResponseDTO
import no.nav.familie.ba.sak.ekstern.pensjon.Meldingstype
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.statistikk.producer.KafkaProducer
import no.nav.familie.ba.sak.task.HentAlleIdenterTilPsysTask.Companion.TASK_STEP_TYPE
import no.nav.familie.ba.sak.task.dto.HentAlleIdenterTilPsysRequestDTO
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = TASK_STEP_TYPE,
    beskrivelse = "Henter alle identer som har barnetrygd for gjeldende år til psys",
    maxAntallFeil = 1,
)
class HentAlleIdenterTilPsysTask(
    private val kafkaProducer: KafkaProducer,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) : AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(HentAlleIdenterTilPsysTask::class.java)

    override fun doTask(task: Task) {
        val hentAlleIdenterDto = objectMapper.readValue(task.payload, HentAlleIdenterTilPsysRequestDTO::class.java)
        logger.info("Starter med å hente alle identer fra DB for request ${hentAlleIdenterDto.requestId}")
        val identer = andelTilkjentYtelseRepository.finnIdenterMedLøpendeBarnetrygdForGittÅr(hentAlleIdenterDto.år)
        logger.info("Ferdig med å hente alle identer fra DB for request ${hentAlleIdenterDto.requestId}")
        logger.info("Starter på å sende alle identer til kafka for request ${hentAlleIdenterDto.requestId}")

        kafkaProducer.sendIdentTilPSys(
            HentAlleIdenterTilPsysResponseDTO(meldingstype = Meldingstype.START, requestId = hentAlleIdenterDto.requestId, personident = null),
        )
        identer.forEach { kafkaProducer.sendIdentTilPSys(HentAlleIdenterTilPsysResponseDTO(meldingstype = Meldingstype.DATA, personident = it, requestId = hentAlleIdenterDto.requestId)) }
        kafkaProducer.sendIdentTilPSys(
            HentAlleIdenterTilPsysResponseDTO(meldingstype = Meldingstype.SLUTT, requestId = hentAlleIdenterDto.requestId, personident = null),
        )
        logger.info("Ferdig med å sende alle identer til kafka for request ${hentAlleIdenterDto.requestId}")
    }

    override fun onCompletion(task: Task) {
    }

    companion object {
        fun lagTask(år: Int, uuid: UUID): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(HentAlleIdenterTilPsysRequestDTO(år = år, requestId = uuid)),
                properties = Properties().apply {
                    this["år"] = år.toString()
                    this["requestId"] = uuid.toString()
                    this["callId"] = uuid.toString()
                },
            )
        }
        const val TASK_STEP_TYPE = "hentAlleIdenterTilPsys"
    }
}
