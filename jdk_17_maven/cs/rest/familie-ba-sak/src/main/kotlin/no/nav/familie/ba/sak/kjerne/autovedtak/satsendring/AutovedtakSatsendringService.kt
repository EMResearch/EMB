package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.UtbetalingsikkerhetFeil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.FeatureToggleConfig.Companion.SATSENDRING_SNIKE_I_KØEN
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.Satskjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.SettPåMaskinellVentÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.SnikeIKøenService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.task.SatsendringTaskDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AutovedtakSatsendringService(
    private val taskRepository: TaskRepositoryWrapper,
    private val behandlingRepository: BehandlingRepository,
    private val autovedtakService: AutovedtakService,
    private val satskjøringRepository: SatskjøringRepository,
    private val behandlingService: BehandlingService,
    private val satsendringService: SatsendringService,
    private val loggService: LoggService,
    private val featureToggleService: FeatureToggleService,
    private val snikeIKøenService: SnikeIKøenService,
    private val tilkjentYtelseValideringService: TilkjentYtelseValideringService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) {

    private val satsendringAlleredeUtført = Metrics.counter("satsendring.allerede.utfort")
    private val satsendringIverksatt = Metrics.counter("satsendring.iverksatt")
    private val satsendringIgnorertÅpenBehandling = Metrics.counter("satsendring.ignorert.aapenbehandling")

    /**
     * Gjennomfører og commiter revurderingsbehandling
     * med årsak satsendring og uten endring i vilkår.
     *
     */
    @Transactional
    fun kjørBehandling(behandlingsdata: SatsendringTaskDto): SatsendringSvar {
        val fagsakId = behandlingsdata.fagsakId

        val satskjøringForFagsak =
            satskjøringRepository.findByFagsakIdAndSatsTidspunkt(fagsakId, behandlingsdata.satstidspunkt)
                ?: satskjøringRepository.save(Satskjøring(fagsakId = fagsakId, satsTidspunkt = behandlingsdata.satstidspunkt))

        val sisteVedtatteBehandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId) ?: error("Fant ikke siste vedtatte behandling for $fagsakId")

        if (satsendringService.erFagsakOppdatertMedSisteSatser(fagsakId)) {
            satskjøringForFagsak.ferdigTidspunkt = LocalDateTime.now()
            satskjøringRepository.save(satskjøringForFagsak)
            logger.info("Satsendring allerede utført for fagsak=$fagsakId")
            satsendringAlleredeUtført.increment()
            return SatsendringSvar.SATSENDRING_ER_ALLEREDE_UTFØRT
        }

        val aktivOgÅpenBehandling =
            behandlingRepository.findByFagsakAndAktivAndOpen(fagsakId = sisteVedtatteBehandling.fagsak.id)
        val søkerAktør = sisteVedtatteBehandling.fagsak.aktør

        logger.info("Kjører satsendring på $sisteVedtatteBehandling")
        secureLogger.info("Kjører satsendring på $sisteVedtatteBehandling for ${søkerAktør.aktivFødselsnummer()}")
        if (sisteVedtatteBehandling.fagsak.status != FagsakStatus.LØPENDE) throw Feil("Forsøker å utføre satsendring på ikke løpende fagsak ${sisteVedtatteBehandling.fagsak.id}")

        if (aktivOgÅpenBehandling != null) {
            val brukerHarÅpenBehandlingSvar = hentBrukerHarÅpenBehandlingSvar(aktivOgÅpenBehandling)
            if (brukerHarÅpenBehandlingSvar == SatsendringSvar.BEHANDLING_KAN_SNIKES_FORBI &&
                featureToggleService.isEnabled(SATSENDRING_SNIKE_I_KØEN)
            ) {
                snikeIKøenService.settAktivBehandlingTilPåMaskinellVent(
                    aktivOgÅpenBehandling.id,
                    SettPåMaskinellVentÅrsak.SATSENDRING,
                )
            } else {
                satskjøringForFagsak.feiltype = brukerHarÅpenBehandlingSvar.name
                satskjøringRepository.save(satskjøringForFagsak)

                logger.info(brukerHarÅpenBehandlingSvar.melding)
                satsendringIgnorertÅpenBehandling.increment()

                return brukerHarÅpenBehandlingSvar
            }
        }

        if (harUtbetalingerSomOverstiger100Prosent(sisteVedtatteBehandling)) {
            logger.warn("Det løper over 100 prosent utbetaling på fagsak=${sisteVedtatteBehandling.fagsak.id}")
        }

        val behandlingEtterBehandlingsresultat =
            autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                aktør = søkerAktør,
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = BehandlingÅrsak.SATSENDRING,
                fagsakId = sisteVedtatteBehandling.fagsak.id,
            )

        val opprettetVedtak =
            autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(
                behandlingEtterBehandlingsresultat,
            )

        val task = when (behandlingEtterBehandlingsresultat.steg) {
            StegType.IVERKSETT_MOT_OPPDRAG -> {
                IverksettMotOppdragTask.opprettTask(
                    behandlingEtterBehandlingsresultat,
                    opprettetVedtak,
                    SikkerhetContext.hentSaksbehandler(),
                )
            }

            StegType.FERDIGSTILLE_BEHANDLING -> {
                behandlingService.oppdaterStatusPåBehandling(
                    behandlingEtterBehandlingsresultat.id,
                    BehandlingStatus.IVERKSETTER_VEDTAK,
                )
                FerdigstillBehandlingTask.opprettTask(
                    søkerAktør.aktivFødselsnummer(),
                    behandlingEtterBehandlingsresultat.id,
                )
            }

            else -> throw Feil("Ugyldig neste steg ${behandlingEtterBehandlingsresultat.steg} ved satsendring for fagsak=$fagsakId")
        }

        satskjøringForFagsak.ferdigTidspunkt = LocalDateTime.now()
        satskjøringRepository.save(satskjøringForFagsak)
        taskRepository.save(task)

        satsendringIverksatt.increment()

        return SatsendringSvar.SATSENDRING_KJØRT_OK
    }

    private fun hentBrukerHarÅpenBehandlingSvar(
        aktivOgÅpenBehandling: Behandling,
    ): SatsendringSvar {
        val status = aktivOgÅpenBehandling.status
        return when {
            status != BehandlingStatus.UTREDES && status != BehandlingStatus.SATT_PÅ_VENT ->
                SatsendringSvar.BEHANDLING_ER_LÅST_SATSENDRING_TRIGGES_NESTE_VIRKEDAG
            kanSnikeIKøen(aktivOgÅpenBehandling) -> SatsendringSvar.BEHANDLING_KAN_SNIKES_FORBI
            else -> SatsendringSvar.BEHANDLING_KAN_IKKE_SETTES_PÅ_VENT
        }
    }

    private fun kanSnikeIKøen(aktivOgÅpenBehandling: Behandling): Boolean {
        val behandlingId = aktivOgÅpenBehandling.id
        val loggSuffix = "endrer status på behandling til på vent"
        if (aktivOgÅpenBehandling.status == BehandlingStatus.SATT_PÅ_VENT) {
            logger.info("Behandling=$behandlingId er satt på vent av saksbehandler, $loggSuffix")
            return true
        }
        val sisteLogghendelse = loggService.hentLoggForBehandling(behandlingId).maxBy { it.opprettetTidspunkt }
        val tid4TimerSiden = LocalDateTime.now().minusHours(4)
        if (aktivOgÅpenBehandling.endretTidspunkt.isAfter(tid4TimerSiden)) {
            logger.info(
                "Behandling=$behandlingId har endretTid=${aktivOgÅpenBehandling.endretTidspunkt} " +
                    "kan ikke sette behandlingen på maskinell vent",
            )
            return false
        }
        if (sisteLogghendelse.opprettetTidspunkt.isAfter(tid4TimerSiden)) {
            logger.info(
                "Behandling=$behandlingId siste logginslag er " +
                    "type=${sisteLogghendelse.type} tid=${sisteLogghendelse.opprettetTidspunkt}, $loggSuffix",
            )
            return false
        }
        return true
    }

    private fun harUtbetalingerSomOverstiger100Prosent(sisteIverksatteBehandling: Behandling): Boolean {
        try {
            tilkjentYtelseValideringService.validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(sisteIverksatteBehandling)
        } catch (e: UtbetalingsikkerhetFeil) {
            secureLogger.info("fagsakId=${sisteIverksatteBehandling.fagsak.id} har UtbetalingsikkerhetFeil. Skipper satsendring: ${e.frontendFeilmelding}")
            return true
        }
        return false
    }

    companion object {
        val logger = LoggerFactory.getLogger(AutovedtakSatsendringService::class.java)
    }
}

enum class SatsendringSvar(val melding: String) {
    SATSENDRING_KJØRT_OK(melding = "Satsendring kjørt OK"),
    SATSENDRING_ER_ALLEREDE_UTFØRT(melding = "Satsendring allerede utført for fagsak"),
    BEHANDLING_ER_LÅST_SATSENDRING_TRIGGES_NESTE_VIRKEDAG(
        melding = "Behandlingen er låst for endringer og satsendring vil bli trigget neste virkedag.",
    ),
    BEHANDLING_KAN_SNIKES_FORBI("Behandling kan snikes forbi (toggle er slått av)"),
    BEHANDLING_KAN_IKKE_SETTES_PÅ_VENT("Behandlingen kan ikke settes på vent"),
}
