package no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement

import no.nav.familie.ba.sak.config.FeatureToggleConfig.Companion.ENDRET_EØS_REGELVERKFILTER_FOR_BARN
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelerOppdatertAbonnent
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEndringAbonnent
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaService
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSkjemaer
import no.nav.familie.ba.sak.kjerne.eøs.felles.medBehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.replaceLast
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.EndretUtbetalingAndelTidslinjeService
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.KombinertRegelverkResultat
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat.OPPFYLT_BLANDET_REGELVERK
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjeService
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.tilBarnasSkalIkkeUtbetalesTidslinjer
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrer
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.fraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.leftJoin
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.outerJoin
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.somUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.tidslinje.tilOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.unleash.UnleashService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TilpassKompetanserTilRegelverkService(
    private val vilkårsvurderingTidslinjeService: VilkårsvurderingTidslinjeService,
    private val endretUtbetalingAndelTidslinjeService: EndretUtbetalingAndelTidslinjeService,
    private val unleashNext: UnleashService,
    kompetanseRepository: PeriodeOgBarnSkjemaRepository<Kompetanse>,
    endringsabonnenter: Collection<PeriodeOgBarnSkjemaEndringAbonnent<Kompetanse>>,
) {
    val skjemaService = PeriodeOgBarnSkjemaService(
        kompetanseRepository,
        endringsabonnenter,
    )

    @Transactional
    fun tilpassKompetanserTilRegelverk(behandlingId: BehandlingId) {
        val gjeldendeKompetanser = skjemaService.hentMedBehandlingId(behandlingId)
        val barnasRegelverkResultatTidslinjer =
            vilkårsvurderingTidslinjeService.hentBarnasRegelverkResultatTidslinjer(behandlingId)

        val barnasSkalIkkeUtbetalesTidslinjer =
            endretUtbetalingAndelTidslinjeService.hentBarnasSkalIkkeUtbetalesTidslinjer(behandlingId)

        val annenForelderOmfattetAvNorskLovgivningTidslinje =
            vilkårsvurderingTidslinjeService.hentAnnenForelderOmfattetAvNorskLovgivningTidslinje(behandlingId = behandlingId)

        val oppdaterteKompetanser = tilpassKompetanserTilRegelverk(
            gjeldendeKompetanser,
            barnasRegelverkResultatTidslinjer,
            barnasSkalIkkeUtbetalesTidslinjer,
            annenForelderOmfattetAvNorskLovgivningTidslinje,
            brukBarnetsRegelverkVedBlandetResultat = unleashNext.isEnabled(ENDRET_EØS_REGELVERKFILTER_FOR_BARN),
        ).medBehandlingId(behandlingId)

        skjemaService.lagreDifferanseOgVarsleAbonnenter(behandlingId, gjeldendeKompetanser, oppdaterteKompetanser)
    }
}

@Service
class TilpassKompetanserTilEndretUtebetalingAndelerService(
    private val vilkårsvurderingTidslinjeService: VilkårsvurderingTidslinjeService,
    private val unleashNext: UnleashService,
    kompetanseRepository: PeriodeOgBarnSkjemaRepository<Kompetanse>,
    endringsabonnenter: Collection<PeriodeOgBarnSkjemaEndringAbonnent<Kompetanse>>,
) : EndretUtbetalingAndelerOppdatertAbonnent {
    val skjemaService = PeriodeOgBarnSkjemaService(
        kompetanseRepository,
        endringsabonnenter,
    )

    @Transactional
    override fun endretUtbetalingAndelerOppdatert(
        behandlingIdLong: Long,
        endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    ) {
        val behandlingId = BehandlingId(behandlingIdLong)
        val gjeldendeKompetanser = skjemaService.hentMedBehandlingId(behandlingId)
        val barnasRegelverkResultatTidslinjer =
            vilkårsvurderingTidslinjeService.hentBarnasRegelverkResultatTidslinjer(behandlingId)

        val barnasSkalIkkeUtbetalesTidslinjer = endretUtbetalingAndeler
            .tilBarnasSkalIkkeUtbetalesTidslinjer()

        val annenForelderOmfattetAvNorskLovgivningTidslinje =
            vilkårsvurderingTidslinjeService.hentAnnenForelderOmfattetAvNorskLovgivningTidslinje(behandlingId = behandlingId)

        val oppdaterteKompetanser = tilpassKompetanserTilRegelverk(
            gjeldendeKompetanser,
            barnasRegelverkResultatTidslinjer,
            barnasSkalIkkeUtbetalesTidslinjer,
            annenForelderOmfattetAvNorskLovgivningTidslinje,
            brukBarnetsRegelverkVedBlandetResultat = unleashNext.isEnabled(ENDRET_EØS_REGELVERKFILTER_FOR_BARN),
        ).medBehandlingId(behandlingId)

        skjemaService.lagreDifferanseOgVarsleAbonnenter(behandlingId, gjeldendeKompetanser, oppdaterteKompetanser)
    }
}

fun tilpassKompetanserTilRegelverk(
    gjeldendeKompetanser: Collection<Kompetanse>,
    barnaRegelverkTidslinjer: Map<Aktør, Tidslinje<KombinertRegelverkResultat, Måned>>,
    barnasSkalIkkeUtbetalesTidslinjer: Map<Aktør, Tidslinje<Boolean, Måned>>,
    annenForelderOmfattetAvNorskLovgivningTidslinje: Tidslinje<Boolean, Måned> = TomTidslinje<Boolean, Måned>(),
    brukBarnetsRegelverkVedBlandetResultat: Boolean = true,
): Collection<Kompetanse> {
    val barnasEøsRegelverkTidslinjer = barnaRegelverkTidslinjer.tilBarnasEøsRegelverkTidslinjer(
        brukBarnetsRegelverkVedBlandetResultat,
    )
        .leftJoin(barnasSkalIkkeUtbetalesTidslinjer) { regelverk, harEtterbetaling3År ->
            when (harEtterbetaling3År) {
                true -> null // ta bort regelverk hvis barnet har etterbetaling 3 år
                else -> regelverk
            }
        }

    return gjeldendeKompetanser.tilSeparateTidslinjerForBarna()
        .outerJoin(barnasEøsRegelverkTidslinjer) { kompetanse, regelverk ->
            regelverk?.let { kompetanse ?: Kompetanse.NULL }
        }
        .mapValues { (_, value) ->
            value.kombinerMed(annenForelderOmfattetAvNorskLovgivningTidslinje) { kompetanse, annenForelderOmfattet ->
                kompetanse?.copy(erAnnenForelderOmfattetAvNorskLovgivning = annenForelderOmfattet ?: false)
            }
        }
        .tilSkjemaer()
}

fun VilkårsvurderingTidslinjeService.hentBarnasRegelverkResultatTidslinjer(behandlingId: BehandlingId) =
    this.hentTidslinjerThrows(behandlingId).barnasTidslinjer()
        .mapValues { (_, tidslinjer) ->
            tidslinjer.regelverkResultatTidslinje
        }

private fun Map<Aktør, Tidslinje<KombinertRegelverkResultat, Måned>>.tilBarnasEøsRegelverkTidslinjer(
    brukBarnetsRegelverkVedBlandetResultat: Boolean,
) =
    this.mapValues { (_, tidslinjer) ->
        tidslinjer.mapTilRegelverk(brukBarnetsRegelverkVedBlandetResultat)
            .filtrer { it == Regelverk.EØS_FORORDNINGEN }
            .filtrerIkkeNull()
            .forlengFremtidTilUendelig(MånedTidspunkt.nå())
    }

private fun Tidslinje<KombinertRegelverkResultat, Måned>.mapTilRegelverk(brukBarnetsRegelverkVedBlandetResultat: Boolean) =
    map {
        if (it?.kombinertResultat == OPPFYLT_BLANDET_REGELVERK && brukBarnetsRegelverkVedBlandetResultat) {
            it.barnetsResultat?.regelverk
        } else {
            it?.kombinertResultat?.regelverk
        }
    }

private fun <I, T : Tidsenhet> Tidslinje<I, T>.forlengFremtidTilUendelig(nå: Tidspunkt<T>): Tidslinje<I, T> {
    val tilOgMed = this.tilOgMed()
    return if (tilOgMed != null && tilOgMed > nå) {
        this.flyttTilOgMed(tilOgMed.somUendeligLengeTil())
    } else {
        this
    }
}

private fun <I, T : Tidsenhet> Tidslinje<I, T>.flyttTilOgMed(tilTidspunkt: Tidspunkt<T>): Tidslinje<I, T> {
    val tidslinje = this
    val fraOgMed = tidslinje.fraOgMed()

    return if (fraOgMed == null || tilTidspunkt < fraOgMed) {
        TomTidslinje()
    } else {
        object : Tidslinje<I, T>() {
            override fun lagPerioder(): Collection<Periode<I, T>> = tidslinje.perioder()
                .filter { it.fraOgMed <= tilTidspunkt }
                .replaceLast { Periode(it.fraOgMed, tilTidspunkt, it.innhold) }
        }
    }
}
