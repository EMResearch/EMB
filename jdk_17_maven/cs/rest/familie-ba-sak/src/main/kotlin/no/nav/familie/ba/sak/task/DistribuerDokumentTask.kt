package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.brev.DokumentDistribueringService
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.domene.ManuellAdresseInfo
import no.nav.familie.ba.sak.task.DistribuerDokumentTask.Companion.TASK_STEP_TYPE
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(taskStepType = TASK_STEP_TYPE, beskrivelse = "Send dokument til Dokdist", maxAntallFeil = 3)
class DistribuerDokumentTask(
    private val stegService: StegService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val dokumentDistribueringService: DokumentDistribueringService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val distribuerDokumentDTO = objectMapper.readValue(task.payload, DistribuerDokumentDTO::class.java)

        val erManueltSendtOgIkkeVedtaksbrev =
            distribuerDokumentDTO.erManueltSendt && !distribuerDokumentDTO.brevmal.erVedtaksbrev
        val erVedtaksbrevOgIkkeManueltSent =
            !distribuerDokumentDTO.erManueltSendt && distribuerDokumentDTO.brevmal.erVedtaksbrev

        if (erManueltSendtOgIkkeVedtaksbrev && distribuerDokumentDTO.behandlingId == null) {
            dokumentDistribueringService.prøvDistribuerBrevOgLoggHendelse(
                distribuerDokumentDTO = distribuerDokumentDTO,
                loggBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            )
        } else if (erManueltSendtOgIkkeVedtaksbrev && distribuerDokumentDTO.behandlingId != null) {
            dokumentDistribueringService.prøvDistribuerBrevOgLoggHendelseFraBehandling(
                distribuerDokumentDTO = distribuerDokumentDTO,
                loggBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            )
        } else if (erVedtaksbrevOgIkkeManueltSent && distribuerDokumentDTO.behandlingId != null) {
            stegService.håndterDistribuerVedtaksbrev(
                behandling = behandlingHentOgPersisterService.hent(distribuerDokumentDTO.behandlingId),
                distribuerDokumentDTO = distribuerDokumentDTO,
            )
        } else {
            throw Feil("erManueltSendt=${distribuerDokumentDTO.erManueltSendt} ikke støttet for brev=${distribuerDokumentDTO.brevmal.visningsTekst}")
        }
    }

    companion object {

        fun opprettDistribuerDokumentTask(
            distribuerDokumentDTO: DistribuerDokumentDTO,
            properties: Properties,
        ): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(distribuerDokumentDTO),
                properties = properties,
            ).copy(
                triggerTid = nesteGyldigeTriggertidForBehandlingIHverdager(),
            )
        }

        const val TASK_STEP_TYPE = "distribuerDokument"
    }
}

data class DistribuerDokumentDTO(
    val behandlingId: Long?,
    val journalpostId: String,
    val personEllerInstitusjonIdent: String,
    val brevmal: Brevmal,
    val erManueltSendt: Boolean,
    val manuellAdresseInfo: ManuellAdresseInfo? = null,
)
