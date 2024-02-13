package no.nav.familie.tilbake.kravgrunnlag.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.kravgrunnlag.HentKravgrunnlagService
import no.nav.familie.tilbake.kravgrunnlag.domain.KodeAksjon
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = HentKravgrunnlagTask.TYPE,
    beskrivelse = "Henter kravgrunnlag fra økonomi",
    triggerTidVedFeilISekunder = 300L,
)
class HentKravgrunnlagTask(
    private val behandlingRepository: BehandlingRepository,
    private val hentKravgrunnlagService: HentKravgrunnlagService,
    private val stegService: StegService,
) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional
    override fun doTask(task: Task) {
        log.info("HentKravgrunnlagTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        val behandlingId = UUID.fromString(task.payload)
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.type != Behandlingstype.REVURDERING_TILBAKEKREVING) {
            throw Feil(message = "HentKravgrunnlagTask kan kjøres bare for tilbakekrevingsrevurdering.")
        }
        val originalBehandlingId = requireNotNull(behandling.sisteÅrsak?.originalBehandlingId)

        val tilbakekrevingsgrunnlag = hentKravgrunnlagService.hentTilbakekrevingskravgrunnlag(originalBehandlingId)
        val hentetKravgrunnlag = hentKravgrunnlagService.hentKravgrunnlagFraØkonomi(
            tilbakekrevingsgrunnlag.eksternKravgrunnlagId,
            KodeAksjon.HENT_GRUNNLAG_OMGJØRING,
        )
        hentKravgrunnlagService.lagreHentetKravgrunnlag(behandlingId, hentetKravgrunnlag)

        hentKravgrunnlagService.opprettHistorikkinnslag(behandlingId)

        stegService.håndterSteg(behandlingId)
    }

    companion object {

        const val TYPE = "hentKravgrunnlag"
    }
}
