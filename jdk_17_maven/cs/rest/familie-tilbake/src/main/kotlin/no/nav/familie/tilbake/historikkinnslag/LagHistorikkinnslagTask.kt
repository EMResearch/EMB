package no.nav.familie.tilbake.historikkinnslag

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.config.PropertyName
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = LagHistorikkinnslagTask.TYPE,
    maxAntallFeil = 3,
    beskrivelse = "Lag historikkinnslag og sender det til kafka",
    triggerTidVedFeilISekunder = 60 * 5L,
)
class LagHistorikkinnslagTask(private val historikkService: HistorikkService) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("LagHistorikkinnslagTask prosesserer med id=${task.id} og metadata ${task.metadata}")

        val behandlingId: UUID = UUID.fromString(task.payload)
        val historikkinnslagstype =
            TilbakekrevingHistorikkinnslagstype.valueOf(task.metadata.getProperty("historikkinnslagstype"))
        val aktør = Aktør.valueOf(task.metadata.getProperty("aktør"))
        val opprettetTidspunkt = LocalDateTime.parse(task.metadata.getProperty("opprettetTidspunkt"))
        val beskrivelse = task.metadata.getProperty("beskrivelse")
        val brevtype = task.metadata.getProperty("brevtype")
        val beslutter = task.metadata.getProperty(PropertyName.BESLUTTER)

        historikkService.lagHistorikkinnslag(
            behandlingId,
            historikkinnslagstype,
            aktør,
            opprettetTidspunkt,
            beskrivelse,
            brevtype,
            beslutter,
        )
    }

    companion object {

        const val TYPE = "lagHistorikkinnslag"
    }
}
