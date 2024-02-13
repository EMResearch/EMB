package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.behandling.AutomatiskBeslutningService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.sikkerhet.SaksbehandlerContext
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.FerdigstillOppgaver
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BeslutteVedtak(
    private val totrinnskontrollService: TotrinnskontrollService,
    private val vedtakService: VedtakService,
    private val behandlingService: BehandlingService,
    private val beregningService: BeregningService,
    private val taskRepository: TaskRepositoryWrapper,
    private val loggService: LoggService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val featureToggleService: FeatureToggleService,
    private val tilkjentYtelseValideringService: TilkjentYtelseValideringService,
    private val saksbehandlerContext: SaksbehandlerContext,
    private val automatiskBeslutningService: AutomatiskBeslutningService,
) : BehandlingSteg<RestBeslutningPåVedtak> {

    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: RestBeslutningPåVedtak,
    ): StegType {
        if (behandling.status == BehandlingStatus.IVERKSETTER_VEDTAK) {
            throw FunksjonellFeil("Behandlingen er allerede sendt til oppdrag og venter på kvittering")
        } else if (behandling.status == BehandlingStatus.AVSLUTTET) {
            throw FunksjonellFeil("Behandlingen er allerede avsluttet")
        } else if (behandling.opprettetÅrsak == BehandlingÅrsak.KORREKSJON_VEDTAKSBREV &&
            !featureToggleService.isEnabled(FeatureToggleConfig.KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV)
        ) {
            throw FunksjonellFeil(
                melding = "Årsak ${BehandlingÅrsak.KORREKSJON_VEDTAKSBREV.visningsnavn} og toggle ${FeatureToggleConfig.KAN_MANUELT_KORRIGERE_MED_VEDTAKSBREV} false",
                frontendFeilmelding = "Du har ikke tilgang til å beslutte for denne behandlingen. Ta kontakt med teamet dersom dette ikke stemmer.",
            )
        } else if (behandling.erTekniskBehandling() && !featureToggleService.isEnabled(FeatureToggleConfig.TEKNISK_ENDRING)) {
            throw FunksjonellFeil(
                "Du har ikke tilgang til å beslutte en behandling med årsak=${behandling.opprettetÅrsak.visningsnavn}. Ta kontakt med teamet dersom dette ikke stemmer.",
            )
        }

        val behandlingSkalAutomatiskBesluttes =
            automatiskBeslutningService.behandlingSkalAutomatiskBesluttes(behandling)

        val beslutter =
            if (behandlingSkalAutomatiskBesluttes) SikkerhetContext.SYSTEM_NAVN else saksbehandlerContext.hentSaksbehandlerSignaturTilBrev()
        val beslutterId =
            if (behandlingSkalAutomatiskBesluttes) SikkerhetContext.SYSTEM_FORKORTELSE else SikkerhetContext.hentSaksbehandler()

        val totrinnskontroll = totrinnskontrollService.besluttTotrinnskontroll(
            behandling = behandling,
            beslutter = beslutter,
            beslutterId = beslutterId,
            beslutning = data.beslutning,
            kontrollerteSider = data.kontrollerteSider,
        )

        opprettTaskFerdigstillGodkjenneVedtak(
            behandling = behandling,
            beslutning = data,
            behandlingErAutomatiskBesluttet = behandlingSkalAutomatiskBesluttes,
        )

        return if (data.beslutning.erGodkjent()) {
            val vedtak = vedtakService.hentAktivForBehandling(behandlingId = behandling.id)
                ?: error("Fant ikke aktivt vedtak på behandling ${behandling.id}")

            vedtakService.oppdaterVedtaksdatoOgBrev(vedtak)

            val nesteSteg = sjekkOmBehandlingSkalIverksettesOgHentNesteSteg(behandling)

            when (nesteSteg) {
                StegType.IVERKSETT_MOT_OPPDRAG -> {
                    opprettTaskIverksettMotOppdrag(behandling, vedtak)
                }

                StegType.JOURNALFØR_VEDTAKSBREV -> {
                    if (!behandling.erBehandlingMedVedtaksbrevutsending()) {
                        throw Feil("Prøvde å opprette vedtaksbrev for behandling som ikke skal sende ut vedtaksbrev.")
                    }

                    opprettJournalførVedtaksbrevTask(behandling, vedtak)
                }

                StegType.FERDIGSTILLE_BEHANDLING -> {
                    if (behandling.type == BehandlingType.TEKNISK_ENDRING || behandling.erManuellMigreringForEndreMigreringsdato()) {
                        opprettFerdigstillBehandlingTask(behandling)
                    } else {
                        throw Feil("Neste steg 'ferdigstille behandling' er ikke implementert på 'beslutte vedtak'-steg")
                    }
                }

                else -> throw Feil("Neste steg '$nesteSteg' er ikke implementert på 'beslutte vedtak'-steg")
            }
            nesteSteg
        } else {
            val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)
                ?: throw Feil("Fant ikke vilkårsvurdering på behandling")
            val kopiertVilkårsVurdering = vilkårsvurdering.kopier(inkluderAndreVurderinger = true)
            vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering = kopiertVilkårsVurdering)

            behandlingService.opprettOgInitierNyttVedtakForBehandling(
                behandling = behandling,
                kopierVedtakBegrunnelser = true,
                begrunnelseVilkårPekere =
                VilkårsvurderingService.matchVilkårResultater(
                    vilkårsvurdering,
                    kopiertVilkårsVurdering,
                ),
            )

            val behandleUnderkjentVedtakTask = OpprettOppgaveTask.opprettTask(
                behandlingId = behandling.id,
                oppgavetype = Oppgavetype.BehandleUnderkjentVedtak,
                tilordnetRessurs = totrinnskontroll.saksbehandlerId,
                fristForFerdigstillelse = LocalDate.now(),
            )
            taskRepository.save(behandleUnderkjentVedtakTask)
            StegType.SEND_TIL_BESLUTTER
        }
    }

    override fun postValiderSteg(behandling: Behandling) {
        tilkjentYtelseValideringService.validerAtIngenUtbetalingerOverstiger100Prosent(behandling)
    }

    override fun stegType(): StegType {
        return StegType.BESLUTTE_VEDTAK
    }

    private fun sjekkOmBehandlingSkalIverksettesOgHentNesteSteg(behandling: Behandling): StegType {
        val endringerIUtbetaling =
            beregningService.hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(behandling)

        return hentNesteStegGittEndringerIUtbetaling(behandling, endringerIUtbetaling)
    }

    private fun opprettFerdigstillBehandlingTask(behandling: Behandling) {
        val ferdigstillBehandlingTask = FerdigstillBehandlingTask.opprettTask(
            søkerIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
            behandlingsId = behandling.id,
        )
        taskRepository.save(ferdigstillBehandlingTask)
    }

    private fun opprettTaskFerdigstillGodkjenneVedtak(
        behandling: Behandling,
        beslutning: RestBeslutningPåVedtak,
        behandlingErAutomatiskBesluttet: Boolean,
    ) {
        loggService.opprettBeslutningOmVedtakLogg(
            behandling = behandling,
            beslutning = beslutning.beslutning,
            begrunnelse = beslutning.begrunnelse,
            behandlingErAutomatiskBesluttet = behandlingErAutomatiskBesluttet,
        )

        if (!behandling.erManuellMigrering() || !behandlingErAutomatiskBesluttet) {
            val ferdigstillGodkjenneVedtakTask =
                FerdigstillOppgaver.opprettTask(behandling.id, Oppgavetype.GodkjenneVedtak)
            taskRepository.save(ferdigstillGodkjenneVedtakTask)
        }
    }

    private fun opprettTaskIverksettMotOppdrag(behandling: Behandling, vedtak: Vedtak) {
        val task = IverksettMotOppdragTask.opprettTask(behandling, vedtak, SikkerhetContext.hentSaksbehandler())
        taskRepository.save(task)
    }

    private fun opprettJournalførVedtaksbrevTask(behandling: Behandling, vedtak: Vedtak) {
        val task = JournalførVedtaksbrevTask.opprettTaskJournalførVedtaksbrev(
            vedtakId = vedtak.id,
            personIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
            behandlingId = behandling.id,
        )
        taskRepository.save(task)
    }
}
