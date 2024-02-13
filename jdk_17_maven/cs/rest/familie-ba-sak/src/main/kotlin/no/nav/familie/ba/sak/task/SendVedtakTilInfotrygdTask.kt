package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdFeedClient
import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.InfotrygdVedtakFeedDto
import no.nav.familie.ba.sak.integrasjoner.infotrygd.domene.InfotrygdVedtakFeedTaskDto
import no.nav.familie.ba.sak.kjerne.beregning.beregnUtbetalingsperioderUtenKlassifisering
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.fpsak.tidsserie.LocalDateSegment
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = SendVedtakTilInfotrygdTask.TASK_STEP_TYPE,
    beskrivelse = "Send vedtaksmelding til Infotrygd feed.",
)
class SendVedtakTilInfotrygdTask(
    private val infotrygdFeedClient: InfotrygdFeedClient,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val infotrygdVedtakFeedTaskDto = objectMapper.readValue(task.payload, InfotrygdVedtakFeedTaskDto::class.java)

        infotrygdFeedClient.sendVedtakFeedTilInfotrygd(
            InfotrygdVedtakFeedDto(
                infotrygdVedtakFeedTaskDto.fnrStoenadsmottaker,
                finnFørsteUtbetalingsperiode(infotrygdVedtakFeedTaskDto.behandlingId),
            ),
        )
    }

    private fun finnFørsteUtbetalingsperiode(behandlingId: Long): LocalDate {
        val andelerMedEndringer = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId)

        return if (andelerMedEndringer.isNotEmpty()) {
            val førsteUtbetalingsperiode = beregnUtbetalingsperioderUtenKlassifisering(andelerMedEndringer)
                .sortedWith(compareBy<LocalDateSegment<Int>>({ it.fom }, { it.value }, { it.tom }))
                .first()
            førsteUtbetalingsperiode.fom
        } else {
            error("Finner ikke første utbetalingsperiode")
        }
    }

    companion object {

        const val TASK_STEP_TYPE = "sendVedtakFeedTilInfotrygd"

        fun opprettTask(fnrStoenadsmottaker: String, behandlingId: Long): Task {
            secureLogger.info("Oppretter task for å sende vedtaksmelding for $fnrStoenadsmottaker til Infotrygd.")

            val metadata = Properties().apply {
                this["fnrStoenadsmottaker"] = fnrStoenadsmottaker
                if (!MDC.get(MDCConstants.MDC_CALL_ID).isNullOrEmpty()) {
                    this["callId"] = MDC.get(MDCConstants.MDC_CALL_ID) ?: IdUtils.generateId()
                }
            }

            return Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(
                    InfotrygdVedtakFeedTaskDto(
                        fnrStoenadsmottaker = fnrStoenadsmottaker,
                        behandlingId = behandlingId,
                    ),
                ),
                properties = metadata,
            )
        }
    }
}
