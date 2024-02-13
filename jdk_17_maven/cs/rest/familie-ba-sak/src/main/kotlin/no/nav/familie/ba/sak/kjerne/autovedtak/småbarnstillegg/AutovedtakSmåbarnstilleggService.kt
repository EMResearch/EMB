package no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakBehandlingService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.SmåbarnstilleggData
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.SmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.beregning.VedtaksperiodefinnerSmåbarnstilleggFeil
import no.nav.familie.ba.sak.kjerne.beregning.finnAktuellVedtaksperiodeOgLeggTilSmåbarnstilleggbegrunnelse
import no.nav.familie.ba.sak.kjerne.beregning.hentInnvilgedeOgReduserteAndelerSmåbarnstillegg
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.task.dto.ManuellOppgaveType
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AutovedtakSmåbarnstilleggService(
    private val fagsakService: FagsakService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val vedtakService: VedtakService,
    private val behandlingService: BehandlingService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val småbarnstilleggService: SmåbarnstilleggService,
    private val taskService: TaskService,
    private val beregningService: BeregningService,
    private val autovedtakService: AutovedtakService,
    private val oppgaveService: OppgaveService,
    private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService,
) : AutovedtakBehandlingService<SmåbarnstilleggData> {

    private val antallVedtakOmOvergangsstønad: Counter =
        Metrics.counter("behandling", "saksbehandling", "hendelse", "smaabarnstillegg", "antall")
    private val antallVedtakOmOvergangsstønadPåvirkerFagsak: Counter =
        Metrics.counter("behandling", "saksbehandling", "hendelse", "smaabarnstillegg", "paavirker_fagsak")
    private val antallVedtakOmOvergangsstønadPåvirkerIkkeFagsak: Counter =
        Metrics.counter("behandling", "saksbehandling", "hendelse", "smaabarnstillegg", "paavirker_ikke_fagsak")

    enum class TilManuellBehandlingÅrsak(val beskrivelse: String) {
        NYE_UTBETALINGSPERIODER_FØRER_TIL_MANUELL_BEHANDLING("Endring i OS gir etterbetaling, feilutbetaling eller endring mer enn 1 måned frem i tid"),
        KLARER_IKKE_BEGRUNNE("Klarer ikke å begrunne"),
    }

    private val antallVedtakOmOvergangsstønadTilManuellBehandling: Map<TilManuellBehandlingÅrsak, Counter> =
        TilManuellBehandlingÅrsak.values().associateWith {
            Metrics.counter(
                "behandling",
                "saksbehandling",
                "hendelse",
                "smaabarnstillegg",
                "til_manuell_behandling",
                "aarsak",
                it.name,
                "beskrivelse",
                it.beskrivelse,
            )
        }

    override fun skalAutovedtakBehandles(behandlingsdata: SmåbarnstilleggData): Boolean {
        val fagsak = fagsakService.hentNormalFagsak(aktør = behandlingsdata.aktør) ?: return false
        val påvirkerFagsak = småbarnstilleggService.vedtakOmOvergangsstønadPåvirkerFagsak(fagsak)
        return if (!påvirkerFagsak) {
            antallVedtakOmOvergangsstønadPåvirkerIkkeFagsak.increment()

            logger.info("Påvirker ikke fagsak")
            false
        } else {
            antallVedtakOmOvergangsstønadPåvirkerFagsak.increment()
            true
        }
    }

    @Transactional
    override fun kjørBehandling(behandlingsdata: SmåbarnstilleggData): String {
        antallVedtakOmOvergangsstønad.increment()
        val aktør = behandlingsdata.aktør
        val fagsak = fagsakService.hentNormalFagsak(aktør)
            ?: throw Feil(message = "Fant ikke fagsak av typen NORMAL for aktør ${aktør.aktørId}")
        val behandlingEtterBehandlingsresultat =
            autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                aktør = aktør,
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = BehandlingÅrsak.SMÅBARNSTILLEGG,
                fagsakId = fagsak.id,
            )

        if (behandlingEtterBehandlingsresultat.status != BehandlingStatus.IVERKSETTER_VEDTAK) {
            return kanIkkeBehandleAutomatisk(
                behandling = behandlingEtterBehandlingsresultat,
                metric = antallVedtakOmOvergangsstønadTilManuellBehandling[TilManuellBehandlingÅrsak.NYE_UTBETALINGSPERIODER_FØRER_TIL_MANUELL_BEHANDLING]!!,
                meldingIOppgave = "Småbarnstillegg: endring i overgangsstønad må behandles manuelt",
            )
        }

        try {
            begrunnAutovedtakForSmåbarnstillegg(behandlingEtterBehandlingsresultat)
        } catch (e: VedtaksperiodefinnerSmåbarnstilleggFeil) {
            logger.warn(e.message, e)

            val behandlingSomSkalManueltBehandles = behandlingService.oppdaterStatusPåBehandling(
                behandlingEtterBehandlingsresultat.id,
                BehandlingStatus.UTREDES,
            )
            return kanIkkeBehandleAutomatisk(
                behandling = behandlingSomSkalManueltBehandles,
                metric = antallVedtakOmOvergangsstønadTilManuellBehandling[TilManuellBehandlingÅrsak.KLARER_IKKE_BEGRUNNE]!!,
                meldingIOppgave = "Småbarnstillegg: klarer ikke bestemme vedtaksperiode som skal begrunnes, må behandles manuelt",
            )
        }

        val vedtakEtterTotrinn = autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(
            behandlingEtterBehandlingsresultat,
        )

        val task = IverksettMotOppdragTask.opprettTask(
            behandlingEtterBehandlingsresultat,
            vedtakEtterTotrinn,
            SikkerhetContext.hentSaksbehandler(),
        )
        taskService.save(task)

        return AutovedtakStegService.BEHANDLING_FERDIG
    }

    private fun begrunnAutovedtakForSmåbarnstillegg(
        behandlingEtterBehandlingsresultat: Behandling,
    ) {
        val sistIverksatteBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId = behandlingEtterBehandlingsresultat.fagsak.id)
        val forrigeSmåbarnstilleggAndeler =
            if (sistIverksatteBehandling == null) {
                emptyList()
            } else {
                beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(
                    behandlingId = sistIverksatteBehandling.id,
                ).filter { it.erSmåbarnstillegg() }
            }

        val nyeSmåbarnstilleggAndeler =
            if (sistIverksatteBehandling == null) {
                emptyList()
            } else {
                beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(
                    behandlingId = behandlingEtterBehandlingsresultat.id,
                ).filter { it.erSmåbarnstillegg() }
            }

        val (innvilgedeMånedPerioder, reduserteMånedPerioder) = hentInnvilgedeOgReduserteAndelerSmåbarnstillegg(
            forrigeSmåbarnstilleggAndeler = forrigeSmåbarnstilleggAndeler,
            nyeSmåbarnstilleggAndeler = nyeSmåbarnstilleggAndeler,
        )

        vedtaksperiodeHentOgPersisterService.lagre(
            finnAktuellVedtaksperiodeOgLeggTilSmåbarnstilleggbegrunnelse(
                innvilgetMånedPeriode = innvilgedeMånedPerioder.singleOrNull(),
                redusertMånedPeriode = reduserteMånedPerioder.singleOrNull(),
                vedtaksperioderMedBegrunnelser = vedtaksperiodeService.hentPersisterteVedtaksperioder(
                    vedtak = vedtakService.hentAktivForBehandlingThrows(
                        behandlingId = behandlingEtterBehandlingsresultat.id,
                    ),
                ),
            ),
        )
    }

    private fun kanIkkeBehandleAutomatisk(
        behandling: Behandling,
        metric: Counter,
        meldingIOppgave: String,
    ): String {
        metric.increment()
        val omgjortBehandling = autovedtakService.omgjørBehandlingTilManuellOgKjørSteg(
            behandling = behandling,
            steg = StegType.VILKÅRSVURDERING,
        )
        return oppgaveService.opprettOppgaveForManuellBehandling(
            behandling = omgjortBehandling,
            begrunnelse = meldingIOppgave,
            opprettLogginnslag = true,
            manuellOppgaveType = ManuellOppgaveType.SMÅBARNSTILLEGG,
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(AutovedtakSmåbarnstilleggService::class.java)
    }
}
