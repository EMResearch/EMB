package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.FiltreringsreglerService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.domene.erOppfylt
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FiltreringFødselshendelserSteg(
    private val filtreringsreglerService: FiltreringsreglerService,
) : BehandlingSteg<NyBehandlingHendelse> {

    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: NyBehandlingHendelse,
    ): StegType {
        logger.info("Kjører filtreringsregler for behandling ${behandling.id}")

        val fødselshendelsefiltreringResultat = filtreringsreglerService.kjørFiltreringsregler(
            data,
            behandling,
        )

        return if (!fødselshendelsefiltreringResultat.erOppfylt()) {
            StegType.HENLEGG_BEHANDLING
        } else {
            hentNesteStegForNormalFlyt(behandling)
        }
    }

    override fun stegType(): StegType {
        return StegType.FILTRERING_FØDSELSHENDELSER
    }

    companion object {

        private val logger = LoggerFactory.getLogger(FiltreringFødselshendelserSteg::class.java)
    }
}
