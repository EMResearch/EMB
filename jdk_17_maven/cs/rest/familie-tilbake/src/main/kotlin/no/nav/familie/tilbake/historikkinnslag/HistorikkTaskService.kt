package no.nav.familie.tilbake.historikkinnslag

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.behandling.FagsakService
import no.nav.familie.tilbake.config.PropertyName
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
class HistorikkTaskService(
    private val taskService: TaskService,
    private val fagsakService: FagsakService,
) {

    fun lagHistorikkTask(
        behandlingId: UUID,
        historikkinnslagstype: TilbakekrevingHistorikkinnslagstype,
        aktør: Aktør,
        triggerTid: LocalDateTime? = null,
        beskrivelse: String? = null,
        brevtype: Brevtype? = null,
        beslutter: String? = null,
    ) {
        val fagsystem = fagsakService.finnFagsystemForBehandlingId(behandlingId)
        val properties = Properties().apply {
            setProperty("historikkinnslagstype", historikkinnslagstype.name)
            setProperty("aktør", aktør.name)
            setProperty(PropertyName.FAGSYSTEM, fagsystem.name)
            setProperty("opprettetTidspunkt", LocalDateTime.now().toString())
            beslutter?.let { setProperty(PropertyName.BESLUTTER, beslutter) }
            beskrivelse?.let { setProperty("beskrivelse", fjernNewlinesFraString(it)) }
            brevtype?.let { setProperty("brevtype", brevtype.name) }
        }

        val task = Task(
            type = LagHistorikkinnslagTask.TYPE,
            payload = behandlingId.toString(),
            properties = properties,
        )
        triggerTid?.let { taskService.save(task.medTriggerTid(triggerTid)) } ?: taskService.save(task)
    }

    private fun fjernNewlinesFraString(tekst: String): String {
        return tekst
            .replace("\r", "")
            .replace("\n", " ")
    }
}
