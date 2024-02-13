package no.nav.familie.ba.sak.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.UtbetalingsikkerhetFeil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.integrasjoner.økonomi.AndelTilkjentYtelseForIverksettingFactory
import no.nav.familie.ba.sak.integrasjoner.økonomi.AndelTilkjentYtelseForSimuleringFactory
import no.nav.familie.ba.sak.integrasjoner.økonomi.pakkInnForUtbetaling
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiKlient
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.grupperAndeler
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.utbetalingsoppdrag
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.YearMonth

@Service
class ForvalterService(
    private val økonomiService: ØkonomiService,
    private val økonomiKlient: ØkonomiKlient,
    private val vedtakService: VedtakService,
    private val beregningService: BeregningService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val endretUtbetalingAndelService: EndretUtbetalingAndelService,
    private val stegService: StegService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val taskRepository: TaskRepositoryWrapper,
    private val autovedtakService: AutovedtakService,
    private val fagsakRepository: FagsakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val tilkjentYtelseValideringService: TilkjentYtelseValideringService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val infotrygdService: InfotrygdService,
) {
    private val logger = LoggerFactory.getLogger(ForvalterService::class.java)

    @Transactional
    fun lagOgSendUtbetalingsoppdragTilØkonomiForBehandling(behandlingId: Long) {
        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId)
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)

        val forrigeBehandlingSendtTilØkonomi =
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling)
        val erBehandlingOpprettetEtterDenneSomErSendtTilØkonomi = forrigeBehandlingSendtTilØkonomi != null &&
            forrigeBehandlingSendtTilØkonomi.aktivertTidspunkt.isAfter(behandling.aktivertTidspunkt)

        if (tilkjentYtelse.utbetalingsoppdrag != null) {
            throw Feil("Behandling $behandlingId har allerede opprettet utbetalingsoppdrag")
        }
        if (erBehandlingOpprettetEtterDenneSomErSendtTilØkonomi) {
            throw Feil("Det finnes en behandling opprettet etter $behandlingId som er sendt til økonomi")
        }

        økonomiService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(
            vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId),
            saksbehandlerId = "VL",
            andelTilkjentYtelseForUtbetalingsoppdragFactory = AndelTilkjentYtelseForIverksettingFactory(),
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun kopierEndretUtbetalingFraForrigeBehandling(
        sisteVedtatteBehandling: Behandling,
        nestSisteVedtatteBehandling: Behandling,
    ) {
        endretUtbetalingAndelService.kopierEndretUtbetalingAndelFraForrigeBehandling(
            behandling = sisteVedtatteBehandling,
            forrigeBehandling = nestSisteVedtatteBehandling,
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun kjørForenkletSatsendringFor(fagsakId: Long) {
        val fagsak = fagsakService.hentPåFagsakId(fagsakId)

        val nyBehandling = stegService.håndterNyBehandling(
            NyBehandling(
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = BehandlingÅrsak.SATSENDRING,
                søkersIdent = fagsak.aktør.aktivFødselsnummer(),
                skalBehandlesAutomatisk = true,
                fagsakId = fagsakId,
            ),
        )

        val behandlingEtterVilkårsvurdering =
            stegService.håndterVilkårsvurdering(nyBehandling)

        val opprettetVedtak =
            autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(
                behandlingEtterVilkårsvurdering,
            )
        behandlingService.oppdaterStatusPåBehandling(nyBehandling.id, BehandlingStatus.IVERKSETTER_VEDTAK)
        val task =
            IverksettMotOppdragTask.opprettTask(nyBehandling, opprettetVedtak, SikkerhetContext.hentSaksbehandler())
        taskRepository.save(task)
    }

    fun identifiserUtbetalingerOver100Prosent(callId: String) {
        MDC.put(MDCConstants.MDC_CALL_ID, callId)

        runBlocking {
            finnOgLoggUtbetalingerOver100Prosent(callId)
        }

        logger.info("Ferdig med å kjøre identifiserUtbetalingerOver100Prosent")
    }

    @OptIn(InternalCoroutinesApi::class) // for å få lov til å hente CancellationException
    suspend fun finnOgLoggUtbetalingerOver100Prosent(callId: String) {
        var slice = fagsakRepository.finnLøpendeFagsaker(PageRequest.of(0, 10000))
        val scope = CoroutineScope(Dispatchers.Default.limitedParallelism(10))
        val deffereds = mutableListOf<Deferred<Unit>>()

        // coroutineScope {
        while (slice.pageable.isPaged) {
            val sideNr = slice.number
            val fagsaker = slice.get().toList()
            logger.info("Starter kjøring av identifiserUtbetalingerOver100Prosent side=$sideNr")
            deffereds.add(
                scope.async {
                    MDC.put(MDCConstants.MDC_CALL_ID, callId)
                    sjekkChunkMedFagsakerOmDeHarUtbetalingerOver100Prosent(fagsaker)
                    logger.info("Avslutter kjøring av identifiserUtbetalingerOver100Prosent side=$sideNr")
                },
            )

            slice = fagsakRepository.finnLøpendeFagsaker(slice.nextPageable())
        }
        deffereds.forEach {
            if (it.isCancelled) {
                logger.warn("Async jobb med status kansellert. Se securelog")
                secureLogger.warn(
                    "Async jobb kansellert med: ${it.getCancellationException().message} ${
                        it.getCancellationException().stackTraceToString()
                    }",
                )
            }

            it.await()
        }

        logger.info("Alle async jobber er kjørt. Totalt antall sider=${deffereds.size}")
    }

    private fun sjekkChunkMedFagsakerOmDeHarUtbetalingerOver100Prosent(fagsaker: List<Long>) {
        fagsaker.forEach { fagsakId ->
            val sisteIverksatteBehandling =
                behandlingRepository.finnSisteIverksatteBehandling(fagsakId = fagsakId)
            if (sisteIverksatteBehandling != null) {
                try {
                    tilkjentYtelseValideringService.validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(
                        sisteIverksatteBehandling,
                    )
                } catch (e: UtbetalingsikkerhetFeil) {
                    val arbeidsfordelingService =
                        arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId = sisteIverksatteBehandling.id)
                    secureLogger.warn("Over 100% utbetaling for fagsak=$fagsakId, enhet=${arbeidsfordelingService.behandlendeEnhetId}, melding=${e.message}")
                }
            } else {
                logger.warn("Skipper sjekk 100% for fagsak $fagsakId pga manglende sisteIverksettBehandling")
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun lagKorrigertUtbetalingsoppdragOgIverksettMotØkonomi(behandlingId: Long, versjon: Int = 1) {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandlingId)
        if (tilkjentYtelse.behandling.aktiv == false) throw Exception("Behandling $behandlingId er ikke den aktive behandlingen på fagsaken")
        val validertUtbetalingsoppdrag = validerOpphørsdatoIUtbetalingsoppdrag(tilkjentYtelse)
        if (!validertUtbetalingsoppdrag.harKorrekteOpphørsdatoer && validertUtbetalingsoppdrag.nyttUtbetalingsoppdrag != null && detFinnesUtbetalingsperioderMedFeilOgTilhørendeKorrigerteUtbetalingsperioder(
                validertUtbetalingsoppdrag,
            )
        ) {
            secureLogger.info("Iverksetter korrigert utbetalingsoppdrag ${validertUtbetalingsoppdrag.nyttUtbetalingsoppdrag} for behandling $behandlingId")
            økonomiKlient.iverksettOppdragPåNytt(validertUtbetalingsoppdrag.nyttUtbetalingsoppdrag, versjon)
            secureLogger.info("Oppdaterer TilkjentYtelse med korrigert utbetalingsoppdrag ${validertUtbetalingsoppdrag.nyttUtbetalingsoppdrag} for behandling $behandlingId")
            beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(
                tilkjentYtelse.behandling,
                validertUtbetalingsoppdrag.nyttUtbetalingsoppdrag,
            )
        } else {
            throw Exception("Nytt utbetalingsoppdrag ikke sendt for behandling $behandlingId. HarKorrekteOpphørsdatoer: ${validertUtbetalingsoppdrag.harKorrekteOpphørsdatoer}, Nytt utbetalingsoppdrag: ${validertUtbetalingsoppdrag.nyttUtbetalingsoppdrag}, ")
        }
    }

    private fun detFinnesUtbetalingsperioderMedFeilOgTilhørendeKorrigerteUtbetalingsperioder(validertUtbetalingsoppdrag: ValidertUtbetalingsoppdrag): Boolean {
        if (
            !validertUtbetalingsoppdrag.korrigerteUtbetalingsperioder.isNullOrEmpty() && !validertUtbetalingsoppdrag.utbetalingsperioderMedFeilOpphørsdato.isNullOrEmpty() &&
            // De korrigerte utbetalingsperiodene matcher utbetalingsperiodene med feil
            validertUtbetalingsoppdrag.korrigerteUtbetalingsperioder.size == validertUtbetalingsoppdrag.utbetalingsperioderMedFeilOpphørsdato.size &&
            validertUtbetalingsoppdrag.utbetalingsperioderMedFeilOpphørsdato.all { utbetalingsperiodeMedFeil ->
                validertUtbetalingsoppdrag.korrigerteUtbetalingsperioder.any { korrigertUtbetalingsperiode ->
                    korrigertUtbetalingsperiode.periodeId == utbetalingsperiodeMedFeil.periodeId
                }
            }

        ) {
            return true
        } else {
            throw Exception("Korrigerte utbetalingsperioder matcher ikke utbetalingsperiodene med feil. UtbetalingsperioderMedFeil: ${validertUtbetalingsoppdrag.utbetalingsperioderMedFeilOpphørsdato}, KorrigerteUtbetalingsperioder: ${validertUtbetalingsoppdrag.korrigerteUtbetalingsperioder}")
        }
    }

    fun validerOpphørsdatoIUtbetalingsoppdragForBehandling(behandlingId: Long): ValidertUtbetalingsoppdrag {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandlingId)
        return validerOpphørsdatoIUtbetalingsoppdrag(tilkjentYtelse)
    }

    fun identifiserPåvirkedeBehandlingerOgValiderOpphørsdatoIUtbetalingsoppdrag(): BehandlingerMedFeilIUtbetalingsoppdrag {
        val tilkjenteYtelserMedOpphørSomKanVæreFeil =
            tilkjentYtelseRepository.findTilkjentYtelseMedFeilUtbetalingsoppdrag()
        logger.info("Behandlinger som potensielt har feil: ${tilkjenteYtelserMedOpphørSomKanVæreFeil.map { it.behandling.id }}")

        val validerteUtbetalingsoppdragMedFeil: Set<ValidertUtbetalingsoppdrag> =
            tilkjenteYtelserMedOpphørSomKanVæreFeil
                .map { validerOpphørsdatoIUtbetalingsoppdrag(it) }
                .filter { !it.harKorrekteOpphørsdatoer }.toSet()

        return BehandlingerMedFeilIUtbetalingsoppdrag(
            behandlinger = validerteUtbetalingsoppdragMedFeil.map { it.behandlingId },
            validerteUtbetalingsoppdrag = validerteUtbetalingsoppdragMedFeil,
        )
    }

    private fun validerOpphørsdatoIUtbetalingsoppdrag(tilkjentYtelse: TilkjentYtelse): ValidertUtbetalingsoppdrag {
        val utbetalingsoppdrag = tilkjentYtelse.utbetalingsoppdrag() ?: return ValidertUtbetalingsoppdrag(
            harKorrekteOpphørsdatoer = true,
            behandlingId = tilkjentYtelse.behandling.id,
        )
        logger.info("Sjekker behandling for korrekt opphørsdato ${tilkjentYtelse.behandling.id}")
        try {
            val grupperteNyeAndeler = grupperAndeler(
                beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(behandlingId = tilkjentYtelse.behandling.id)
                    .pakkInnForUtbetaling(AndelTilkjentYtelseForSimuleringFactory()),
            )

            val forrigeIverksatteBehandling =
                behandlingRepository.finnIverksatteBehandlinger(tilkjentYtelse.behandling.fagsak.id)
                    .filter { it.id != tilkjentYtelse.behandling.id && it.aktivertTidspunkt < tilkjentYtelse.behandling.aktivertTidspunkt }
                    .maxByOrNull { it.aktivertTidspunkt }!!

            val grupperteForrigeAndeler = grupperAndeler(
                beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(behandlingId = forrigeIverksatteBehandling.id)
                    .pakkInnForUtbetaling(AndelTilkjentYtelseForSimuleringFactory()),
            )

            val sisteBeståendeAndelPerKjede =
                ØkonomiUtils.sisteBeståendeAndelPerKjede(grupperteForrigeAndeler, grupperteNyeAndeler)

            val endretMigreringsdato = beregnOmMigreringsDatoErEndret(
                tilkjentYtelse.behandling,
                grupperteForrigeAndeler.values.flatten().minByOrNull { it.stønadFom }?.stønadFom,
            )

            // Finner andeler som skal opphøres slik vi gjorde før
            val andelerTilOpphør = grupperteForrigeAndeler
                .mapValues { (person, forrigeAndeler) ->
                    forrigeAndeler.filter {
                        sisteBeståendeAndelPerKjede[person] == null ||
                            it.stønadFom > sisteBeståendeAndelPerKjede[person]!!.stønadTom
                    }
                }
                .filter { (_, andelerSomOpphøres) -> andelerSomOpphøres.isNotEmpty() }
                .mapValues { andelForKjede -> andelForKjede.value.sortedBy { it.stønadFom } }
                .map { (_, kjedeEtterFørsteEndring) ->
                    kjedeEtterFørsteEndring.last() to (
                        endretMigreringsdato
                            ?: kjedeEtterFørsteEndring.minOf { it.stønadFom }
                        )
                }

            secureLogger.info("Andeler som som skal opphøres: ${andelerTilOpphør.map { "PeriodeId: ${it.first.periodeOffset} ForrigePeriodeId: ${it.first.forrigePeriodeOffset} Opphørsdato: ${it.second}" }} for behandling ${tilkjentYtelse.behandling.id}")
            val utbetalingsperioderMedOpphør = utbetalingsoppdrag.utbetalingsperiode.filter { it.opphør != null }
            secureLogger.info("Utbetalingsperioder med opphør: $utbetalingsperioderMedOpphør for behandling ${tilkjentYtelse.behandling.id}")

            val utbetalingsperioderMedFeilOpphørsdato = mutableListOf<Utbetalingsperiode>()
            val korrigerteUtbetalingsperioder = mutableListOf<Utbetalingsperiode>()

            // Finner ut hvilken opphørsAndel som tilhører hvilken utbetalingsperiodeMedOpphør
            for (periodeMedOpphør in utbetalingsperioderMedOpphør) {
                val andelerTilPersonMedOpphør =
                    andelerTilOpphør.filter { andelForPerson -> andelForPerson.first.periodeOffset == periodeMedOpphør.periodeId }
                if (andelerTilPersonMedOpphør.size != 1) {
                    secureLogger.info("Mer enn 1 eller ingen andeler med samme periodeOffsett som opphørsperioden $periodeMedOpphør for behandling ${tilkjentYtelse.behandling.id}")
                    utbetalingsperioderMedFeilOpphørsdato.add(periodeMedOpphør)
                    // Nullstiller korrigerteUtbetalingsperioder slik at validering før iverksettelse feiler.
                    korrigerteUtbetalingsperioder.clear()
                    break
                } else {
                    secureLogger.info("Andel fra forrige med korrekt opphørsdato: ${andelerTilPersonMedOpphør.first().second.førsteDagIInneværendeMåned()}. Opphørsperiode sendt til økonomi med opphørsdato: ${periodeMedOpphør.opphør!!.opphørDatoFom} for behandling ${tilkjentYtelse.behandling.id}")
                    if (andelerTilPersonMedOpphør.first().second
                            .førsteDagIInneværendeMåned() != periodeMedOpphør.opphør!!.opphørDatoFom
                    ) {
                        utbetalingsperioderMedFeilOpphørsdato.add(periodeMedOpphør)
                        korrigerteUtbetalingsperioder.add(
                            periodeMedOpphør.copy(
                                opphør = periodeMedOpphør.opphør!!.copy(
                                    opphørDatoFom = andelerTilPersonMedOpphør.first().second
                                        .førsteDagIInneværendeMåned(),
                                ),
                            ),
                        )
                    }
                }
            }

            if (utbetalingsperioderMedFeilOpphørsdato.isEmpty()) {
                return ValidertUtbetalingsoppdrag(
                    harKorrekteOpphørsdatoer = true,
                    behandlingId = tilkjentYtelse.behandling.id,
                )
            }
            return ValidertUtbetalingsoppdrag(
                harKorrekteOpphørsdatoer = false,
                behandlingId = tilkjentYtelse.behandling.id,
                utbetalingsperioderMedFeilOpphørsdato = utbetalingsperioderMedFeilOpphørsdato,
                korrigerteUtbetalingsperioder = korrigerteUtbetalingsperioder,
                gammeltUtbetalingsoppdrag = utbetalingsoppdrag,
                nyttUtbetalingsoppdrag = utbetalingsoppdrag.copy(
                    avstemmingTidspunkt = LocalDateTime.now(),
                    utbetalingsperiode = korrigerteUtbetalingsperioder
                        .map { it.copy(erEndringPåEksisterendePeriode = true) },
                ),
            )
        } catch (e: Exception) {
            secureLogger.warn(
                "opphørsdatoErKorrekt kaster feil: ${e.message} for behandling ${tilkjentYtelse.behandling.id}",
                e,
            )
            return ValidertUtbetalingsoppdrag(
                harKorrekteOpphørsdatoer = false,
                behandlingId = tilkjentYtelse.behandling.id,
            )
        }
    }

    private fun beregnOmMigreringsDatoErEndret(behandling: Behandling, forrigeTilstandFraDato: YearMonth?): YearMonth? {
        val erMigrertSak =
            behandlingHentOgPersisterService.hentBehandlinger(behandling.fagsak.id)
                .any { it.type == BehandlingType.MIGRERING_FRA_INFOTRYGD }

        if (!erMigrertSak) {
            return null
        }

        val nyttTilstandFraDato = behandlingService.hentMigreringsdatoPåFagsak(fagsakId = behandling.fagsak.id)
            ?.toYearMonth()
            ?.plusMonths(1)

        return if (forrigeTilstandFraDato != null &&
            nyttTilstandFraDato != null &&
            forrigeTilstandFraDato.isAfter(nyttTilstandFraDato)
        ) {
            nyttTilstandFraDato
        } else {
            null
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun oppdaterStønadFomTomForBehandling(behandlingId: Long): Boolean {
        tilkjentYtelseRepository.findByBehandling(behandlingId).apply {
            if (this.stønadFom == null && this.stønadTom == null && this.utbetalingsoppdrag == null && this.andelerTilkjentYtelse.isNotEmpty()) {
                this.stønadTom = this.andelerTilkjentYtelse.maxOfOrNull { it.stønadTom }
                this.stønadFom = this.andelerTilkjentYtelse.minOfOrNull { it.stønadFom }
                tilkjentYtelseRepository.save(this)
                return true
            } else if (this.stønadFom == null && this.stønadTom == null && this.utbetalingsoppdrag == null && this.andelerTilkjentYtelse.isEmpty()) {
                logger.info("Skipper oppdatering av tilkjent ytelse for behandlingId=$behandlingId fordi aty er tom, så får ikke satt tom/fom")
                return false
            }
        }
        return false
    }

    fun finnÅpneFagsakerMedFlereMigreringsbehandlingerOgLøpendeSakIInfotrygd(): List<Pair<Long, String>> {
        val løpendeFagsakerMedFlereMigreringsbehandlinger =
            fagsakRepository.finnFagsakerMedFlereMigreringsbehandlinger()
        return løpendeFagsakerMedFlereMigreringsbehandlinger.filter { infotrygdService.harLøpendeSakIInfotrygd(listOf(it.aktør.aktivFødselsnummer())) }
            .map { Pair(it.id, it.aktør.aktivFødselsnummer()) }
    }

    fun finnÅpneFagsakerMedFlereMigreringsbehandlinger(): List<Pair<Long, String>> {
        return fagsakRepository.finnFagsakerMedFlereMigreringsbehandlinger()
            .map { Pair(it.id, it.aktør.aktivFødselsnummer()) }
    }
}

data class ValidertUtbetalingsoppdrag(
    val harKorrekteOpphørsdatoer: Boolean,
    val behandlingId: Long,
    val utbetalingsperioderMedFeilOpphørsdato: List<Utbetalingsperiode>? = null,
    val korrigerteUtbetalingsperioder: List<Utbetalingsperiode>? = null,
    val gammeltUtbetalingsoppdrag: Utbetalingsoppdrag? = null,
    val nyttUtbetalingsoppdrag: Utbetalingsoppdrag? = null,
)

data class BehandlingerMedFeilIUtbetalingsoppdrag(
    val behandlinger: List<Long>,
    val validerteUtbetalingsoppdrag: Set<ValidertUtbetalingsoppdrag>,
)
