package no.nav.familie.tilbake.iverksettvedtak.task

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = AvsluttBehandlingTask.TYPE,
    beskrivelse = "Avslutter behandling",
    triggerTidVedFeilISekunder = 60 * 5L,
)
class AvsluttBehandlingTask(
    private val behandlingRepository: BehandlingRepository,
    private val behandlingskontrollService: BehandlingskontrollService,
    private val historikkTaskService: HistorikkTaskService,
) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional
    override fun doTask(task: Task) {
        log.info("AvsluttBehandlingTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        val behandlingId = UUID.fromString(task.payload)

        var behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (!behandling.erUnderIverksettelse) {
            throw Feil(message = "Behandling med id=$behandlingId kan ikke avsluttes")
        }

        behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingRepository.update(
            behandling.copy(
                status = Behandlingsstatus.AVSLUTTET,
                avsluttetDato = LocalDate.now(),
            ),
        )

        behandlingskontrollService
            .oppdaterBehandlingsstegStatus(
                behandlingId,
                Behandlingsstegsinfo(
                    behandlingssteg = Behandlingssteg.AVSLUTTET,
                    behandlingsstegstatus = Behandlingsstegstatus.UTFØRT,
                ),
            )

        historikkTaskService.lagHistorikkTask(
            behandlingId = behandlingId,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BEHANDLING_AVSLUTTET,
            aktør = Aktør.VEDTAKSLØSNING,
        )
    }

    companion object {

        const val TYPE = "avsluttBehandling"
    }
}
