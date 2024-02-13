package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.økonomi.AndelTilkjentYtelseForSimuleringFactory
import no.nav.familie.ba.sak.integrasjoner.økonomi.AndelTilkjentYtelseForUtbetalingsoppdrag
import no.nav.familie.ba.sak.integrasjoner.økonomi.IdentOgYtelse
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIUtbetalingUtil
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.barn
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.steg.EndringerIUtbetalingForBehandlingSteg
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class BeregningService(
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val fagsakService: FagsakService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val behandlingRepository: BehandlingRepository,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val småbarnstilleggService: SmåbarnstilleggService,
    private val tilkjentYtelseEndretAbonnenter: List<TilkjentYtelseEndretAbonnent> = emptyList(),
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val featureToggleService: FeatureToggleService,
) {
    fun slettTilkjentYtelseForBehandling(behandlingId: Long) =
        tilkjentYtelseRepository.findByBehandlingOptional(behandlingId)
            ?.let { tilkjentYtelseRepository.delete(it) }

    fun hentLøpendeAndelerTilkjentYtelseMedUtbetalingerForBehandlinger(
        behandlingIder: List<Long>,
        avstemmingstidspunkt: LocalDateTime,
    ): List<AndelTilkjentYtelse> =
        andelTilkjentYtelseRepository.finnLøpendeAndelerTilkjentYtelseForBehandlinger(
            behandlingIder,
            avstemmingstidspunkt.toLocalDate().toYearMonth(),
        )
            .filter { it.erAndelSomSkalSendesTilOppdrag() }

    fun hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(behandlingId: Long): List<AndelTilkjentYtelse> =
        andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)
            .filter { it.erAndelSomSkalSendesTilOppdrag() }

    fun hentAndelerTilkjentYtelseForBehandling(behandlingId: Long): List<AndelTilkjentYtelse> =
        andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)

    fun lagreTilkjentYtelseMedOppdaterteAndeler(tilkjentYtelse: TilkjentYtelse) =
        tilkjentYtelseRepository.save(tilkjentYtelse)

    fun hentTilkjentYtelseForBehandling(behandlingId: Long) =
        tilkjentYtelseRepository.findByBehandling(behandlingId)

    fun hentOptionalTilkjentYtelseForBehandling(behandlingId: Long) =
        tilkjentYtelseRepository.findByBehandlingOptional(behandlingId)

    fun hentSisteAndelPerIdent(fagsakId: Long): Map<IdentOgYtelse, AndelTilkjentYtelseForUtbetalingsoppdrag> {
        return andelTilkjentYtelseRepository.hentSisteAndelPerIdentOgType(fagsakId)
            .groupBy { IdentOgYtelse(it.aktør.aktivFødselsnummer(), it.type) }
            .mapValues { AndelTilkjentYtelseForSimuleringFactory().pakkInnForUtbetaling(it.value).single() }
    }

    /**
     * Denne metoden henter alle relaterte behandlinger på en person.
     * Per fagsak henter man tilkjent ytelse fra:
     * 1. Behandling som er til godkjenning
     * 2. Siste behandling som er vedtatt
     * 3. Filtrer bort behandlinger der barnet ikke lenger finnes
     */
    fun hentRelevanteTilkjentYtelserForBarn(
        barnAktør: Aktør,
        fagsakId: Long,
    ): List<TilkjentYtelse> {
        val andreFagsaker = fagsakService.hentFagsakerPåPerson(barnAktør)
            .filter { it.id != fagsakId }

        return andreFagsaker.mapNotNull { fagsak ->
            val behandlingSomLiggerTilGodkjenning = behandlingRepository.finnBehandlingerSomLiggerTilGodkjenning(
                fagsakId = fagsak.id,
            ).singleOrNull()

            if (behandlingSomLiggerTilGodkjenning != null) {
                behandlingSomLiggerTilGodkjenning
            } else {
                val godkjenteBehandlingerSomIkkeErIverksattEnda =
                    behandlingRepository.finnBehandlingerSomHolderPåÅIverksettes(fagsakId = fagsak.id).singleOrNull()
                if (godkjenteBehandlingerSomIkkeErIverksattEnda != null) {
                    godkjenteBehandlingerSomIkkeErIverksattEnda
                } else {
                    val sisteVedtatteBehandling =
                        behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = fagsak.id)
                    sisteVedtatteBehandling
                }
            }
        }.map {
            hentTilkjentYtelseForBehandling(behandlingId = it.id)
        }.filter {
            personopplysningGrunnlagRepository
                .finnSøkerOgBarnAktørerTilAktiv(behandlingId = it.behandling.id)
                .barn().map { barn -> barn.aktør }
                .contains(barnAktør)
        }.map { it }
    }

    fun erEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(behandling: Behandling): Boolean =
        hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(behandling) == EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING

    fun hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomi(behandling: Behandling): EndringerIUtbetalingForBehandlingSteg {
        val endringerIUtbetaling =
            hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomiTidslinje(behandling)
                .perioder()
                .any { it.innhold == true }

        return if (endringerIUtbetaling) EndringerIUtbetalingForBehandlingSteg.ENDRING_I_UTBETALING else EndringerIUtbetalingForBehandlingSteg.INGEN_ENDRING_I_UTBETALING
    }

    fun hentEndringerIUtbetalingFraForrigeBehandlingSendtTilØkonomiTidslinje(behandling: Behandling): Tidslinje<Boolean, Måned> {
        val nåværendeAndeler = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id)
        val forrigeAndeler = hentAndelerFraForrigeIverksattebehandling(behandling)

        if (nåværendeAndeler.isEmpty() && forrigeAndeler.isEmpty()) return TomTidslinje()

        return EndringIUtbetalingUtil.lagEndringIUtbetalingTidslinje(
            nåværendeAndeler = nåværendeAndeler,
            forrigeAndeler = forrigeAndeler,
        )
    }

    fun hentAndelerFraForrigeIverksattebehandling(behandling: Behandling): List<AndelTilkjentYtelse> {
        val forrigeBehandling = behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling)
        return forrigeBehandling?.let { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(it.id) }
            ?: emptyList()
    }

    @Transactional
    fun oppdaterBehandlingMedBeregning(
        behandling: Behandling,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        nyEndretUtbetalingAndel: EndretUtbetalingAndel? = null,
    ): TilkjentYtelse {
        val endreteUtbetalingAndeler = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandling.id).filter {
                // Ved automatiske behandlinger ønsker vi alltid å ta vare på de gamle endrede andelene
                if (behandling.skalBehandlesAutomatisk) {
                    true
                } else if (nyEndretUtbetalingAndel != null) {
                    it.id == nyEndretUtbetalingAndel.id || it.andelerTilkjentYtelse.isNotEmpty()
                } else {
                    it.andelerTilkjentYtelse.isNotEmpty()
                }
            }

        return genererOgLagreTilkjentYtelse(
            behandling = behandling,
            personopplysningGrunnlag = personopplysningGrunnlag,
            endreteUtbetalingAndeler = endreteUtbetalingAndeler,
        )
    }

    private fun genererOgLagreTilkjentYtelse(
        behandling: Behandling,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        endreteUtbetalingAndeler: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>,
    ): TilkjentYtelse {
        tilkjentYtelseRepository.slettTilkjentYtelseFor(behandling)
        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandling.id)
            ?: throw IllegalStateException("Kunne ikke hente vilkårsvurdering for behandling med id ${behandling.id}")

        val tilkjentYtelse =
            TilkjentYtelseUtils.beregnTilkjentYtelse(
                vilkårsvurdering = vilkårsvurdering,
                personopplysningGrunnlag = personopplysningGrunnlag,
                endretUtbetalingAndeler = endreteUtbetalingAndeler,
                fagsakType = behandling.fagsak.type,
            ) { søkerAktør ->
                småbarnstilleggService.hentOgLagrePerioderMedOvergangsstønadForBehandling(
                    søkerAktør = søkerAktør,
                    behandling = behandling,
                )

                småbarnstilleggService.hentPerioderMedFullOvergangsstønad(behandling)
            }

        val lagretTilkjentYtelse = tilkjentYtelseRepository.save(tilkjentYtelse)
        tilkjentYtelseEndretAbonnenter.forEach { it.endretTilkjentYtelse(lagretTilkjentYtelse) }
        return lagretTilkjentYtelse
    }

    // For at endret utbetaling andeler skal fungere så må man generere andeler før man kobler endringene på andelene
// Dette er fordi en endring regnes som gyldig når den overlapper med en andel og har gyldig årsak
// Hvis man ikke genererer andeler før man kobler på endringene så vil ingen av endringene ses på som gyldige, altså ikke oppdatere noen andeler
    fun genererTilkjentYtelseFraVilkårsvurdering(
        behandling: Behandling,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
    ): TilkjentYtelse {
        // 1: Genererer andeler fra vilkårsvurderingen uten å ta hensyn til endret utbetaling andeler
        genererOgLagreTilkjentYtelse(
            behandling = behandling,
            personopplysningGrunnlag = personopplysningGrunnlag,
            endreteUtbetalingAndeler = emptyList(),
        )

        // 2: Genererer andeler som også tar hensyn til endret utbetaling andeler
        return oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)
    }

    fun oppdaterTilkjentYtelseMedUtbetalingsoppdrag(
        behandling: Behandling,
        utbetalingsoppdrag: Utbetalingsoppdrag,
    ): TilkjentYtelse {
        val nyTilkjentYtelse = populerTilkjentYtelse(behandling, utbetalingsoppdrag)
        return tilkjentYtelseRepository.save(nyTilkjentYtelse)
    }

    fun kanAutomatiskIverksetteSmåbarnstilleggEndring(
        behandling: Behandling,
        sistIverksatteBehandling: Behandling?,
    ): Boolean {
        if (!behandling.skalBehandlesAutomatisk || !behandling.erSmåbarnstillegg()) return false

        val forrigeSmåbarnstilleggAndeler =
            if (sistIverksatteBehandling == null) {
                emptyList()
            } else {
                hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(
                    behandlingId = sistIverksatteBehandling.id,
                ).filter { it.erSmåbarnstillegg() }
            }

        val nyeSmåbarnstilleggAndeler =
            if (sistIverksatteBehandling == null) {
                emptyList()
            } else {
                hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(
                    behandlingId = behandling.id,
                ).filter { it.erSmåbarnstillegg() }
            }

        val (innvilgedeMånedPerioder, reduserteMånedPerioder) = hentInnvilgedeOgReduserteAndelerSmåbarnstillegg(
            forrigeSmåbarnstilleggAndeler = forrigeSmåbarnstilleggAndeler,
            nyeSmåbarnstilleggAndeler = nyeSmåbarnstilleggAndeler,
        )

        return kanAutomatiskIverksetteSmåbarnstillegg(
            innvilgedeMånedPerioder = innvilgedeMånedPerioder,
            reduserteMånedPerioder = reduserteMånedPerioder,
        )
    }

    /**
     * Henter alle barn på behandlingen som har minst en periode med tilkjentytelse.
     */
    fun finnBarnFraBehandlingMedTilkjentYtelse(behandlingId: Long): List<Aktør> {
        val andelerTilkjentYtelse = andelTilkjentYtelseRepository
            .finnAndelerTilkjentYtelseForBehandling(behandlingId)

        return personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)?.barna?.map { it.aktør }
            ?.filter {
                andelerTilkjentYtelse.any { aty -> aty.aktør == it }
            } ?: emptyList()
    }

    fun populerTilkjentYtelse(
        behandling: Behandling,
        utbetalingsoppdrag: Utbetalingsoppdrag,
    ): TilkjentYtelse {
        val erRentOpphør =
            utbetalingsoppdrag.utbetalingsperiode.isNotEmpty() && utbetalingsoppdrag.utbetalingsperiode.all { it.opphør != null }
        var opphørsdato: LocalDate? = null
        if (erRentOpphør) {
            opphørsdato = utbetalingsoppdrag.utbetalingsperiode.minOf { it.opphør!!.opphørDatoFom }
        }

        if (behandling.type == BehandlingType.REVURDERING) {
            val opphørPåRevurdering = utbetalingsoppdrag.utbetalingsperiode.filter { it.opphør != null }
            if (opphørPåRevurdering.isNotEmpty()) {
                opphørsdato = opphørPåRevurdering.maxByOrNull { it.opphør!!.opphørDatoFom }!!.opphør!!.opphørDatoFom
            }
        }

        val tilkjentYtelse =
            tilkjentYtelseRepository.findByBehandling(behandling.id)

        return tilkjentYtelse.apply {
            this.utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdrag)
            this.stønadTom = tilkjentYtelse.andelerTilkjentYtelse.maxOfOrNull { it.stønadTom }
            this.stønadFom =
                if (erRentOpphør) null else tilkjentYtelse.andelerTilkjentYtelse.minOfOrNull { it.stønadFom }
            this.endretDato = LocalDate.now()
            this.opphørFom = opphørsdato?.toYearMonth()
        }
    }
}

interface TilkjentYtelseEndretAbonnent {
    fun endretTilkjentYtelse(tilkjentYtelse: TilkjentYtelse)
}
