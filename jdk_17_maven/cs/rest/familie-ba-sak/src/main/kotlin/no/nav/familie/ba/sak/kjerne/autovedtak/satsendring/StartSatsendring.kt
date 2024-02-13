package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.ba.sak.task.SatsendringTaskDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class StartSatsendring(
    private val fagsakRepository: FagsakRepository,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val opprettTaskService: OpprettTaskService,
    private val satskjøringRepository: SatskjøringRepository,
    private val featureToggleService: FeatureToggleService,
    private val personidentService: PersonidentService,
    private val autovedtakSatsendringService: AutovedtakSatsendringService,
    private val satsendringService: SatsendringService,
) {

    private val ignorerteFagsaker = mutableSetOf<Long>()

    @Transactional
    fun startSatsendring(
        antallFagsaker: Int,
    ) {
        if (!featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_ENABLET, false)) {
            logger.info("Skipper satsendring da toggle er skrudd av.")
            return
        }

        var antallSatsendringerStartet = 0
        var startSide = 0
        while (antallSatsendringerStartet < antallFagsaker) {
            val page = fagsakRepository.finnLøpendeFagsakerForSatsendring(
                hentAktivSatsendringstidspunkt().atDay(1),
                Pageable.ofSize(antallFagsaker + 200).withPage(startSide),
            )

            val fagsakerForSatsendring = page.toList()
            logger.info("Fant ${fagsakerForSatsendring.size} personer for satsendring på side $startSide")
            if (fagsakerForSatsendring.isNotEmpty()) {
                antallSatsendringerStartet =
                    oppretteEllerSkipSatsendring(
                        fagsakerForSatsendring,
                        antallSatsendringerStartet,
                        antallFagsaker,
                        hentAktivSatsendringstidspunkt(),
                    )
            }
            logger.info("Opprettet $antallSatsendringerStartet satsendringer (akkumulerende)")

            if (++startSide >= page.totalPages) break
        }
    }

    private fun oppretteEllerSkipSatsendring(
        fagsakForSatsendring: List<Long>,
        antallAlleredeTriggetSatsendring: Int,
        antallFagsakerTilSatsendring: Int,
        satsTidspunkt: YearMonth,
    ): Int {
        var antallFagsakerSatsendring = antallAlleredeTriggetSatsendring

        for (fagsakId in fagsakForSatsendring) {
            if (skalTriggeSatsendring(fagsakId, satsTidspunkt)) {
                antallFagsakerSatsendring++
            }

            if (antallFagsakerSatsendring == antallFagsakerTilSatsendring) {
                return antallFagsakerSatsendring
            }
        }
        return antallFagsakerSatsendring
    }

    private fun skalTriggeSatsendring(fagsakId: Long, satsTidspunkt: YearMonth): Boolean {
        if (ignorerteFagsaker.contains(fagsakId)) {
            return false
        }

        val sisteVedtatteBehandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId)
        return if (sisteVedtatteBehandling != null) {
            opprettTaskService.opprettSatsendringTask(fagsakId, satsTidspunkt)
            true
        } else {
            logger.info("Satsendring trigges ikke på fagsak=$fagsakId fordi fagsaken mangler en vedtatt behandling")
            ignorerteFagsaker.add(fagsakId)
            false
        }
    }

    fun sjekkOgOpprettSatsendringVedGammelSats(ident: String): Boolean {
        val aktør = personidentService.hentAktør(ident)
        val løpendeFagsakerForAktør = fagsakRepository.finnFagsakerForAktør(aktør)
            .filter { !it.arkivert && it.status == FagsakStatus.LØPENDE }

        var harOpprettetSatsendring = false
        løpendeFagsakerForAktør.forEach { fagsak ->
            if (opprettSatsendringTaskVedGammelSats(fagsak.id)) {
                harOpprettetSatsendring = true
            }
        }
        return harOpprettetSatsendring
    }

    fun sjekkOgOpprettSatsendringVedGammelSats(fagsakId: Long): Boolean {
        return opprettSatsendringTaskVedGammelSats(fagsakId)
    }

    private fun opprettSatsendringTaskVedGammelSats(fagsakId: Long): Boolean =
        if (kanStarteSatsendringPåFagsak(fagsakId)) {
            logger.info("Oppretter satsendringtask fagsakID=$fagsakId")
            opprettSatsendringForFagsak(fagsakId = fagsakId)
            true
        } else {
            false
        }

    fun kanStarteSatsendringPåFagsak(fagsakId: Long): Boolean {
        return satskjøringRepository.findByFagsakIdAndSatsTidspunkt(fagsakId, hentAktivSatsendringstidspunkt()) == null &&
            !satsendringService.erFagsakOppdatertMedSisteSatser(fagsakId)
    }

    fun kanGjennomføreSatsendringManuelt(fagsakId: Long): Boolean =
        !satsendringService.erFagsakOppdatertMedSisteSatser(fagsakId)

    @Transactional
    fun gjennomførSatsendringManuelt(fagsakId: Long) {
        if (!kanGjennomføreSatsendringManuelt(fagsakId)) {
            throw Feil("Kan ikke starte Satsendring på fagsak=$fagsakId")
        }

        val resultatSatsendringBehandling = autovedtakSatsendringService.kjørBehandling(
            SatsendringTaskDto(fagsakId = fagsakId, hentAktivSatsendringstidspunkt()),
        )

        when (resultatSatsendringBehandling) {
            SatsendringSvar.SATSENDRING_KJØRT_OK -> Unit

            SatsendringSvar.SATSENDRING_ER_ALLEREDE_UTFØRT ->
                throw FunksjonellFeil("Satsendring er allerede gjennomført på fagsaken. Last inn siden på nytt for å få opp siste behandling.")

            SatsendringSvar.BEHANDLING_ER_LÅST_SATSENDRING_TRIGGES_NESTE_VIRKEDAG,
            SatsendringSvar.BEHANDLING_KAN_IKKE_SETTES_PÅ_VENT,
            ->
                throw FunksjonellFeil("Det finnes en åpen behandling på fagsaken som må avsluttes før satsendring kan gjennomføres.")
            SatsendringSvar.BEHANDLING_KAN_SNIKES_FORBI ->
                throw FunksjonellFeil(resultatSatsendringBehandling.melding)
        }
    }

    fun hentAktivSatsendringstidspunkt(): YearMonth {
        return SATSENDRINGMÅNED_JULI_2023
    }

    fun opprettSatsendringForFagsak(fagsakId: Long) {
        opprettTaskService.opprettSatsendringTask(fagsakId, hentAktivSatsendringstidspunkt())
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(StartSatsendring::class.java)
        val SATSENDRINGMÅNED_MARS_2023: YearMonth = YearMonth.of(2023, 3)
        val SATSENDRINGMÅNED_JULI_2023: YearMonth = YearMonth.of(2023, 7)
    }
}
