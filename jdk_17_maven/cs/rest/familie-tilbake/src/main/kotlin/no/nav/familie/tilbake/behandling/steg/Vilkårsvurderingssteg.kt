package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.tilbake.api.dto.BehandlingsstegDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg.VILKÅRSVURDERING
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.AUTOUTFØRT
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.UTFØRT
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.foreldelse.ForeldelseService
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kravgrunnlag.event.EndretKravgrunnlagEvent
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class Vilkårsvurderingssteg(
    private val behandlingskontrollService: BehandlingskontrollService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val foreldelseService: ForeldelseService,
    private val historikkTaskService: HistorikkTaskService,
    private val oppgaveTaskService: OppgaveTaskService,
) : IBehandlingssteg {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    override fun utførSteg(behandlingId: UUID) {
        logger.info("Behandling $behandlingId er på $VILKÅRSVURDERING steg")
        if (harAllePerioderForeldet(behandlingId)) {
            // hvis det finnes noen periode som ble vurdert før i vilkårsvurdering, må slettes
            vilkårsvurderingService.deaktiverEksisterendeVilkårsvurdering(behandlingId)

            lagHistorikkinnslag(behandlingId, Aktør.VEDTAKSLØSNING)

            behandlingskontrollService.oppdaterBehandlingsstegStatus(
                behandlingId,
                Behandlingsstegsinfo(VILKÅRSVURDERING, AUTOUTFØRT),
            )
            behandlingskontrollService.fortsettBehandling(behandlingId)
        }
    }

    @Transactional
    override fun utførSteg(behandlingId: UUID, behandlingsstegDto: BehandlingsstegDto) {
        logger.info("Behandling $behandlingId er på $VILKÅRSVURDERING steg")
        if (harAllePerioderForeldet(behandlingId)) {
            throw Feil(
                message = "Alle perioder er foreldet for $behandlingId,kan ikke behandle vilkårsvurdering",
                frontendFeilmelding = "Alle perioder er foreldet for $behandlingId,kan ikke behandle vilkårsvurdering",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
        vilkårsvurderingService.lagreVilkårsvurdering(behandlingId, behandlingsstegDto as BehandlingsstegVilkårsvurderingDto)

        oppgaveTaskService.oppdaterAnsvarligSaksbehandlerOppgaveTask(behandlingId)

        lagHistorikkinnslag(behandlingId, Aktør.SAKSBEHANDLER)

        behandlingskontrollService.oppdaterBehandlingsstegStatus(behandlingId, Behandlingsstegsinfo(VILKÅRSVURDERING, UTFØRT))
        behandlingskontrollService.fortsettBehandling(behandlingId)
    }

    @Transactional
    override fun utførStegAutomatisk(behandlingId: UUID) {
        logger.info("Behandling $behandlingId er på $VILKÅRSVURDERING steg og behandler automatisk..")
        if (harAllePerioderForeldet(behandlingId)) {
            utførSteg(behandlingId)
            return
        }

        vilkårsvurderingService.lagreFastVilkårForAutomatiskSaksbehandling(behandlingId)
        lagHistorikkinnslag(behandlingId, Aktør.VEDTAKSLØSNING)

        behandlingskontrollService.oppdaterBehandlingsstegStatus(
            behandlingId,
            Behandlingsstegsinfo(VILKÅRSVURDERING, UTFØRT),
        )
        behandlingskontrollService.fortsettBehandling(behandlingId)
    }

    @Transactional
    override fun gjenopptaSteg(behandlingId: UUID) {
        logger.info("Behandling $behandlingId gjenopptar på $VILKÅRSVURDERING steg")
        behandlingskontrollService.oppdaterBehandlingsstegStatus(
            behandlingId,
            Behandlingsstegsinfo(
                VILKÅRSVURDERING,
                Behandlingsstegstatus.KLAR,
            ),
        )
    }

    override fun getBehandlingssteg(): Behandlingssteg {
        return VILKÅRSVURDERING
    }

    @EventListener
    fun deaktiverEksisterendeVilkårsvurdering(endretKravgrunnlagEvent: EndretKravgrunnlagEvent) {
        vilkårsvurderingService.deaktiverEksisterendeVilkårsvurdering(endretKravgrunnlagEvent.behandlingId)
    }

    private fun harAllePerioderForeldet(behandlingId: UUID): Boolean {
        return foreldelseService.hentAktivVurdertForeldelse(behandlingId)
            ?.foreldelsesperioder?.all { it.erForeldet() } ?: false
    }

    private fun lagHistorikkinnslag(behandlingId: UUID, aktør: Aktør) {
        historikkTaskService.lagHistorikkTask(
            behandlingId,
            TilbakekrevingHistorikkinnslagstype.VILKÅRSVURDERING_VURDERT,
            aktør,
        )
    }
}
