package no.nav.familie.tilbake.iverksettvedtak.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.dokumentbestilling.vedtak.SendVedtaksbrevTask
import no.nav.familie.tilbake.iverksettvedtak.IverksettelseService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = SendØkonomiTilbakekrevingsvedtakTask.TYPE,
    maxAntallFeil = 3,
    beskrivelse = "Sender tilbakekrevingsvedtak til økonomi",
    triggerTidVedFeilISekunder = 300L,
)
class SendØkonomiTilbakekrevingsvedtakTask(
    private val iverksettelseService: IverksettelseService,
    private val taskService: TaskService,
    private val behandlingskontrollService: BehandlingskontrollService,
) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("SendØkonomiTilbakekrevingsvedtakTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        val behandlingId = UUID.fromString(task.payload)
        iverksettelseService.sendIverksettVedtak(behandlingId)

        behandlingskontrollService
            .oppdaterBehandlingsstegStatus(
                behandlingId,
                Behandlingsstegsinfo(
                    behandlingssteg = Behandlingssteg.IVERKSETT_VEDTAK,
                    behandlingsstegstatus = Behandlingsstegstatus.UTFØRT,
                ),
            )
        behandlingskontrollService.fortsettBehandling(behandlingId)
    }

    @Transactional
    override fun onCompletion(task: Task) {
        taskService.save(
            Task(
                type = SendVedtaksbrevTask.TYPE,
                payload = task.payload,
                properties = task.metadata,
            ),
        )
    }

    companion object {

        const val TYPE = "sendØkonomiVedtak"
    }
}
