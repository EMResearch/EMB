package no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingMigreringsinfoRepository
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.erAlleredeBegrunnetMedBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.leader.LeaderClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth

@Service
class RestartAvSmåbarnstilleggService(
    private val fagsakRepository: FagsakRepository,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val opprettTaskService: OpprettTaskService,
    private val vedtakService: VedtakService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val behandlingMigreringsinfoRepository: BehandlingMigreringsinfoRepository,
    private val andelerTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) {

    /**
     * Første dag hver måned sjekkes det om noen fagsaker har oppstart av småbarnstillegg inneværende måned, etter å ha
     * hatt et opphold. Hvis perioden ikke allerede er begrunnet, skal det opprettes en "vurder livshendelse"-oppgave
     * med mindre forrige behandling var en migrering fra Infotrygd.
     */
    @Scheduled(cron = "0 0 7 1 * *")
    @Transactional
    fun scheduledFinnRestartetSmåbarnstilleggOgOpprettOppgave() {
        if (LeaderClient.isLeader() == true) {
            finnOgOpprettetOppgaveForSmåbarnstilleggSomSkalRestartesIDenneMåned(true)
        }
    }

    fun finnOgOpprettetOppgaveForSmåbarnstilleggSomSkalRestartesIDenneMåned(skalOppretteOppgaver: Boolean) {
        logger.info("Starter jobb for å finne småbarnstillegg som skal restartes, men som ikke allerede begrunnet. skalOppretteOppgaver=$skalOppretteOppgaver")
        finnAlleFagsakerMedRestartetSmåbarnstilleggIMåned().forEach { fagsakId ->
            logger.info("Oppretter 'vurder livshendelse'-oppgave på fagsak $fagsakId fordi småbarnstillegg har startet opp igjen denne måneden")

            val sisteIverksatteBehandling =
                behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId = fagsakId)

            if (sisteIverksatteBehandling != null) {
                if (skalOppretteOppgaver) {
                    opprettTaskService.opprettOppgaveTask(
                        behandlingId = sisteIverksatteBehandling.id,
                        oppgavetype = Oppgavetype.VurderLivshendelse,
                        beskrivelse = "Småbarnstillegg: endring i overgangsstønad må behandles manuelt",
                    )
                } else {
                    logger.info("DryRun av RestartAvSmåbarnstilleggService. Ville ha opprettet en VurderLivshendelse for behandling=${sisteIverksatteBehandling.id}, fagsakId=$fagsakId")
                }
            }
        }
        logger.info("Avslutter jobb for å finne småbarnstillegg som skal restartes, men som ikke allerede begrunnet")
    }

    fun finnAlleFagsakerMedRestartetSmåbarnstilleggIMåned(måned: YearMonth = YearMonth.now()): List<Long> {
        return behandlingHentOgPersisterService.partitionByIverksatteBehandlinger {
            finnAlleFagsakerMedOppstartSmåbarnstilleggIMåned(iverksatteLøpendeBehandlinger = it, måned = måned)
        }.filter { fagsakId ->
            val migreringsdato = behandlingMigreringsinfoRepository.finnSisteMigreringsdatoPåFagsak(fagsakId)
            migreringsdato?.month != LocalDate.now().minusMonths(1).month
        }.filter { fagsakId ->
            !periodeMedRestartetSmåbarnstilleggErAlleredeBegrunnet(fagsakId = fagsakId, måned = måned)
        }
    }

    private fun finnAlleFagsakerMedOppstartSmåbarnstilleggIMåned(
        iverksatteLøpendeBehandlinger: List<Long>,
        måned: YearMonth,
    ): List<Long> {
        val fagsaker = fagsakRepository.finnAlleFagsakerMedOppstartSmåbarnstilleggIMåned(
            iverksatteLøpendeBehandlinger = iverksatteLøpendeBehandlinger,
            stønadFom = måned,
        )
        if (SatsService.finnSisteSatsFor(SatsType.SMA).gyldigFom.toYearMonth() == måned) {
            return fagsaker.mapNotNull { fagsakId ->
                val sisteVedtatteBehandling =
                    behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId)

                if (sisteVedtatteBehandling != null) {
                    val atySmåbarnstillegg =
                        andelerTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteVedtatteBehandling.id)
                            .filter { it.erSmåbarnstillegg() }
                    val harSmåbarnstilleggForrigeMåned = atySmåbarnstillegg.any { it.stønadTom == måned.minusMonths(1) }
                    if (harSmåbarnstilleggForrigeMåned) {
                        null
                    } else {
                        fagsakId
                    }
                } else {
                    null
                }
            }
        } else {
            return fagsaker
        }
    }

    internal fun periodeMedRestartetSmåbarnstilleggErAlleredeBegrunnet(fagsakId: Long, måned: YearMonth): Boolean {
        val vedtaksperioderForVedtatteBehandlinger =
            behandlingHentOgPersisterService.hentBehandlinger(fagsakId = fagsakId)
                .filter { behandling ->
                    behandling.erVedtatt()
                }
                .flatMap { behandling ->
                    val vedtak = vedtakService.hentAktivForBehandlingThrows(behandling.id)
                    vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)
                }

        val standardbegrunnelser = listOf(Standardbegrunnelse.INNVILGET_SMÅBARNSTILLEGG)

        return vedtaksperioderForVedtatteBehandlinger.erAlleredeBegrunnetMedBegrunnelse(
            standardbegrunnelser = standardbegrunnelser,
            måned = måned,
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RestartAvSmåbarnstilleggService::class.java)
    }
}
