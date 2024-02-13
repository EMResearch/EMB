package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class Grunnlagssteg(
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val behandlingskontrollService: BehandlingskontrollService,
    private val historikkTaskService: HistorikkTaskService,
) : IBehandlingssteg {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    override fun utførSteg(behandlingId: UUID) {
        logger.info("Behandling $behandlingId er på ${Behandlingssteg.GRUNNLAG} steg")
        if (kravgrunnlagRepository.existsByBehandlingIdAndAktivTrueAndSperretFalse(behandlingId)) {
            behandlingskontrollService.oppdaterBehandlingsstegStatus(
                behandlingId,
                Behandlingsstegsinfo(
                    Behandlingssteg.GRUNNLAG,
                    Behandlingsstegstatus.UTFØRT,
                ),
            )
            behandlingskontrollService.fortsettBehandling(behandlingId)
            historikkTaskService.lagHistorikkTask(
                behandlingId,
                TilbakekrevingHistorikkinnslagstype.BEHANDLING_GJENOPPTATT,
                Aktør.VEDTAKSLØSNING,
                triggerTid = LocalDateTime.now().plusSeconds(2),
            )
        }
    }

    @Transactional
    override fun gjenopptaSteg(behandlingId: UUID) {
        utførSteg(behandlingId)
        logger.info("Behandling $behandlingId gjenopptar på ${Behandlingssteg.GRUNNLAG} steg")
    }

    override fun getBehandlingssteg(): Behandlingssteg {
        return Behandlingssteg.GRUNNLAG
    }
}
