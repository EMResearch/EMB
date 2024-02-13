package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.AutomatiskBeslutningService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.ValiderBrevmottakerService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatSteg
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.validerPerioderInneholderBegrunnelser
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.task.FerdigstillOppgaver
import no.nav.familie.ba.sak.task.OpprettOppgaveTask
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class SendTilBeslutter(
    private val behandlingService: BehandlingService,
    private val taskRepository: TaskRepositoryWrapper,
    private val oppgaveService: OppgaveService,
    private val loggService: LoggService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val vedtakService: VedtakService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val automatiskBeslutningService: AutomatiskBeslutningService,
    private val validerBrevmottakerService: ValiderBrevmottakerService,
) : BehandlingSteg<String> {

    override fun preValiderSteg(
        behandling: Behandling,
        stegService: StegService?,
    ) {
        validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(behandlingId = behandling.id)
        vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)
            ?.validerAtAlleAnndreVurderingerErVurdert()

        val behandlingsresultatSteg: BehandlingsresultatSteg =
            stegService?.hentBehandlingSteg(StegType.BEHANDLINGSRESULTAT) as BehandlingsresultatSteg
        behandlingsresultatSteg.preValiderSteg(behandling)

        behandling.validerRekkefølgeOgUnikhetPåSteg()
        behandling.validerMaksimaltEtStegIkkeUtført()

        if (behandling.resultat != Behandlingsresultat.FORTSATT_INNVILGET) {
            val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = behandling.id)
            val utvidetVedtaksperioder = vedtaksperiodeService.hentUtvidetVedtaksperiodeMedBegrunnelser(vedtak)
            utvidetVedtaksperioder.validerPerioderInneholderBegrunnelser(
                behandlingId = behandling.id,
                fagsakId = behandling.fagsak.id,
            )
        }
    }

    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: String,
    ): StegType {
        totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(behandling)

        if (!automatiskBeslutningService.behandlingSkalAutomatiskBesluttes(behandling)) {
            val godkjenneVedtakTask = OpprettOppgaveTask.opprettTask(
                behandlingId = behandling.id,
                oppgavetype = Oppgavetype.GodkjenneVedtak,
                fristForFerdigstillelse = LocalDate.now(),
            )
            loggService.opprettSendTilBeslutterLogg(behandling = behandling, skalAutomatiskBesluttes = false)
            taskRepository.save(godkjenneVedtakTask)
        } else {
            loggService.opprettSendTilBeslutterLogg(behandling = behandling, skalAutomatiskBesluttes = true)
        }

        opprettFerdigstillOppgaveTasker(behandling)

        behandlingService.sendBehandlingTilBeslutter(behandling)

        return hentNesteStegForNormalFlyt(behandling)
    }

    private fun opprettFerdigstillOppgaveTasker(behandling: Behandling) {
        listOf(
            Oppgavetype.BehandleSak,
            Oppgavetype.BehandleUnderkjentVedtak,
            Oppgavetype.VurderLivshendelse,
        ).forEach { oppgavetype ->
            oppgaveService.hentOppgaverSomIkkeErFerdigstilt(oppgavetype, behandling).also {
                if (it.isNotEmpty()) {
                    val ferdigstillOppgaverTask = FerdigstillOppgaver.opprettTask(behandling.id, oppgavetype)
                    taskRepository.save(ferdigstillOppgaverTask)
                }
            }
        }
    }

    override fun stegType(): StegType {
        return StegType.SEND_TIL_BESLUTTER
    }
}

fun Behandling.validerRekkefølgeOgUnikhetPåSteg(): Boolean {
    if (erHenlagt()) {
        throw Feil("Valideringen kan ikke kjøres for henlagte behandlinger.")
    }

    var forrigeBehandlingStegTilstand: BehandlingStegTilstand? = null
    behandlingStegTilstand.forEach {
        if (forrigeBehandlingStegTilstand != null &&
            forrigeBehandlingStegTilstand!!.behandlingSteg >= it.behandlingSteg &&
            (
                forrigeBehandlingStegTilstand!!.behandlingSteg.rekkefølge != it.behandlingSteg.rekkefølge ||
                    forrigeBehandlingStegTilstand!!.behandlingSteg == it.behandlingSteg
                )
        ) {
            throw Feil("Rekkefølge på steg registrert på behandling $id er feil eller redundante.")
        }
        forrigeBehandlingStegTilstand = it
    }
    return true
}

fun Behandling.validerMaksimaltEtStegIkkeUtført() {
    if (erHenlagt()) {
        throw Feil("Valideringen kan ikke kjøres for henlagte behandlinger.")
    }

    if (behandlingStegTilstand.filter { it.behandlingStegStatus == BehandlingStegStatus.IKKE_UTFØRT }.size > 1) {
        throw Feil("Behandling $id har mer enn ett ikke fullført steg.")
    }
}

fun Vilkårsvurdering.validerAtAlleAnndreVurderingerErVurdert() {
    personResultater.flatMap { it.andreVurderinger }
        .takeIf { it.any { annenVurdering -> annenVurdering.resultat == Resultat.IKKE_VURDERT } }
        ?.let {
            throw FunksjonellFeil(
                melding = "Forsøker å ferdigstille uten å ha fylt ut påkrevde vurderinger",
                frontendFeilmelding = "Andre vurderinger må tas stilling til før behandling kan sendes til beslutter.",
            )
        }
}
