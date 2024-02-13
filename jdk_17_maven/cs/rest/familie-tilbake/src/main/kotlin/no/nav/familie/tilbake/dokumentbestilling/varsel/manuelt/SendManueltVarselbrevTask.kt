package no.nav.familie.tilbake.dokumentbestilling.varsel.manuelt

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.config.FeatureToggleService
import no.nav.familie.tilbake.config.PropertyName
import no.nav.familie.tilbake.dokumentbestilling.brevmaler.Dokumentmalstype
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = SendManueltVarselbrevTask.TYPE,
    maxAntallFeil = 3,
    beskrivelse = "Sender manuelt varselbrev",
    triggerTidVedFeilISekunder = 60 * 5L,
)
class SendManueltVarselbrevTask(
    private val behandlingRepository: BehandlingRepository,
    private val manueltVarselBrevService: ManueltVarselbrevService,
    private val behandlingskontrollService: BehandlingskontrollService,
    private val oppgaveTaskService: OppgaveTaskService,
    private val fagsakRepository: FagsakRepository,
    private val featureToggleService: FeatureToggleService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val taskdata: SendManueltVarselbrevTaskdata = objectMapper.readValue(task.payload)
        val behandling = behandlingRepository.findByIdOrThrow(taskdata.behandlingId)
        val maltype = taskdata.maltype
        val fritekst = taskdata.fritekst

        manueltVarselBrevService.sendVarselbrev(
            behandling = behandling,
            fritekst = fritekst,
            erKorrigert = maltype.erKorrigert,
        )

        val fristTid = Constants.saksbehandlersTidsfrist()
        oppgaveTaskService.oppdaterOppgaveTask(
            behandlingId = behandling.id,
            beskrivelse = "Frist er oppdatert. Saksbehandler ${
                behandling
                    .ansvarligSaksbehandler
            } har sendt varselbrev til bruker",
            frist = fristTid,
            saksbehandler = behandling.ansvarligSaksbehandler,
        )
        // Oppdaterer fristen dersom tasken har tidligere feilet. Behandling ble satt på vent i DokumentBehandlingService.
        if (task.opprettetTid.toLocalDate() < LocalDate.now()) {
            behandlingskontrollService.settBehandlingPåVent(
                behandling.id,
                Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                fristTid,
            )
        }
    }

    companion object {

        fun opprettTask(behandlingId: UUID, fagsystem: Fagsystem, maltype: Dokumentmalstype, fritekst: String): Task =
            Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(
                    SendManueltVarselbrevTaskdata(
                        behandlingId = behandlingId,
                        maltype = maltype,
                        fritekst = fritekst,
                    ),
                ),
                properties = Properties().apply { setProperty(PropertyName.FAGSYSTEM, fagsystem.name) },
            )

        const val TYPE = "brev.sendManueltVarsel"
    }
}

data class SendManueltVarselbrevTaskdata(
    val behandlingId: UUID,
    val maltype: Dokumentmalstype,
    val fritekst: String,
)

private val Dokumentmalstype.erKorrigert: Boolean
    get() = when (this) {
        Dokumentmalstype.KORRIGERT_VARSEL -> true
        Dokumentmalstype.VARSEL -> false
        else -> throw IllegalArgumentException("SendManueltVarselbrevTask kan ikke sende Dokumentmalstype.$this")
    }
