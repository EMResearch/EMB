package no.nav.familie.tilbake.kravgrunnlag.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagService
import no.nav.familie.tilbake.kravgrunnlag.KravvedtakstatusService
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = FinnKravgrunnlagTask.TYPE,
    beskrivelse = "Finner frakoblet grunnlag og statusmeldinger for samme fagsak " +
        "og kobler dem til behandling",
    triggerTidVedFeilISekunder = 60 * 5L,
)
class FinnKravgrunnlagTask(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val økonomiXmlMottattRepository: ØkonomiXmlMottattRepository,
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val kravgrunnlagService: KravgrunnlagService,
    private val kravvedtakstatusService: KravvedtakstatusService,
) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("FinnKravgrunnlagTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        val behandlingId = UUID.fromString(task.payload)
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)

        val mottattKravgrunnlagene = økonomiXmlMottattRepository
            .findByEksternFagsakIdAndYtelsestype(fagsak.eksternFagsakId, fagsak.ytelsestype)
            .sortedBy { it.sporbar.opprettetTid }
        mottattKravgrunnlagene.forEach { mottattKravgrunnlag ->
            kravgrunnlagService.håndterMottattKravgrunnlag(mottattKravgrunnlag.melding)
            if (mottattKravgrunnlag.sperret) {
                val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
                kravvedtakstatusService.håndterSperMeldingMedBehandling(behandlingId, kravgrunnlag)
            }
            // Fjern mottatt xml om det koblet med behandlingen
            if (kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandlingId)) {
                økonomiXmlMottattRepository.deleteById(mottattKravgrunnlag.id)
            }
        }
    }

    companion object {

        const val TYPE = "finnKravgrunnlag"
    }
}
