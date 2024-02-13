package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.ba.sak.kjerne.tilbakekreving.domene.TilbakekrevingRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties

data class IverksettMotFamilieTilbakeData(
    val metadata: Properties,
)

@Service
class IverksettMotFamilieTilbake(
    private val vedtakService: VedtakService,
    private val tilbakekrevingService: TilbakekrevingService,
    private val taskRepository: TaskRepositoryWrapper,
    private val tilbakekrevingRepository: TilbakekrevingRepository,
) : BehandlingSteg<IverksettMotFamilieTilbakeData> {

    override fun utførStegOgAngiNeste(behandling: Behandling, data: IverksettMotFamilieTilbakeData): StegType {
        val vedtak = vedtakService.hentAktivForBehandling(behandling.id) ?: throw Feil(
            "Fant ikke vedtak for behandling ${behandling.id} ved iverksetting mot familie-tilbake.",
        )

        val tilbakekreving = tilbakekrevingRepository.findByBehandlingId(behandling.id)

        if (tilbakekreving != null &&
            tilbakekreving.valg != Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING &&
            !tilbakekrevingService.søkerHarÅpenTilbakekreving(behandling.fagsak.id)
        ) {
            val tilbakekrevingId = tilbakekrevingService.opprettTilbakekreving(behandling)
            tilbakekreving.tilbakekrevingsbehandlingId = tilbakekrevingId

            logger.info("Opprettet tilbakekreving for behandling ${behandling.id} og tilbakekrevingsid $tilbakekrevingId")
            tilbakekrevingRepository.save(tilbakekreving)
        }

        if (!behandling.erBehandlingMedVedtaksbrevutsending()) {
            throw Feil("Neste steg på behandling $behandling er journalføring, men denne behandlingen skal ikke sende ut vedtaksbrev")
        }

        opprettTaskJournalførVedtaksbrev(vedtakId = vedtak.id, data.metadata)

        return hentNesteStegForNormalFlyt(behandling)
    }

    private fun opprettTaskJournalførVedtaksbrev(vedtakId: Long, metadata: Properties) {
        val task = Task(
            type = JournalførVedtaksbrevTask.TASK_STEP_TYPE,
            payload = "$vedtakId",
            properties = metadata,
        )
        taskRepository.save(task)
    }

    override fun stegType(): StegType {
        return StegType.IVERKSETT_MOT_FAMILIE_TILBAKE
    }

    companion object {

        private val logger = LoggerFactory.getLogger(StatusFraOppdrag::class.java)
    }
}
