package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.Utils.avrundetHeltallAvProsent
import no.nav.familie.ba.sak.common.erDagenFør
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inkluderer
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.beregning.UtvidetBarnetrygdUtil.finnUtvidetVilkår
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.medEndring
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

object TilkjentYtelseUtils {

    fun beregnTilkjentYtelse(
        vilkårsvurdering: Vilkårsvurdering,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        endretUtbetalingAndeler: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse> = emptyList(),
        fagsakType: FagsakType,
        hentPerioderMedFullOvergangsstønad: (aktør: Aktør) -> List<InternPeriodeOvergangsstønad> = { _ -> emptyList() },
    ): TilkjentYtelse {
        val tilkjentYtelse = TilkjentYtelse(
            behandling = vilkårsvurdering.behandling,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now(),
        )

        val (endretUtbetalingAndelerSøker, endretUtbetalingAndelerBarna) = endretUtbetalingAndeler.partition { it.person?.type == PersonType.SØKER }

        val andelerTilkjentYtelseBarnaUtenEndringer = OrdinærBarnetrygdUtil.beregnAndelerTilkjentYtelseForBarna(
            personopplysningGrunnlag = personopplysningGrunnlag,
            personResultater = vilkårsvurdering.personResultater,
            fagsakType = fagsakType,
        )
            .map {
                if (it.person.type != PersonType.BARN) throw Feil("Prøver å generere ordinær andel for person av typen ${it.person.type}")

                AndelTilkjentYtelse(
                    behandlingId = vilkårsvurdering.behandling.id,
                    tilkjentYtelse = tilkjentYtelse,
                    aktør = it.person.aktør,
                    stønadFom = it.stønadFom,
                    stønadTom = it.stønadTom,
                    kalkulertUtbetalingsbeløp = it.beløp,
                    nasjonaltPeriodebeløp = it.beløp,
                    type = YtelseType.ORDINÆR_BARNETRYGD,
                    sats = it.sats,
                    prosent = it.prosent,
                )
            }

        val barnasAndelerInkludertEtterbetaling3ÅrEndringer = oppdaterTilkjentYtelseMedEndretUtbetalingAndeler(
            andelTilkjentYtelserUtenEndringer = andelerTilkjentYtelseBarnaUtenEndringer,
            endretUtbetalingAndeler = endretUtbetalingAndelerBarna.filter { it.årsak == Årsak.ETTERBETALING_3ÅR },
        )

        val andelerTilkjentYtelseUtvidetMedAlleEndringer = UtvidetBarnetrygdUtil.beregnTilkjentYtelseUtvidet(
            utvidetVilkår = finnUtvidetVilkår(vilkårsvurdering),
            tilkjentYtelse = tilkjentYtelse,
            andelerTilkjentYtelseBarnaMedEtterbetaling3ÅrEndringer = barnasAndelerInkludertEtterbetaling3ÅrEndringer,
            endretUtbetalingAndelerSøker = endretUtbetalingAndelerSøker,
            personResultater = vilkårsvurdering.personResultater,
        )

        val småbarnstilleggErMulig = erSmåbarnstilleggMulig(
            utvidetAndeler = andelerTilkjentYtelseUtvidetMedAlleEndringer,
            barnasAndeler = barnasAndelerInkludertEtterbetaling3ÅrEndringer,
        )

        val andelerTilkjentYtelseSmåbarnstillegg = if (småbarnstilleggErMulig) {
            SmåbarnstilleggBarnetrygdGenerator(
                behandlingId = vilkårsvurdering.behandling.id,
                tilkjentYtelse = tilkjentYtelse,
            )
                .lagSmåbarnstilleggAndeler(
                    perioderMedFullOvergangsstønad = hentPerioderMedFullOvergangsstønad(
                        personopplysningGrunnlag.søker.aktør,
                    ),
                    utvidetAndeler = andelerTilkjentYtelseUtvidetMedAlleEndringer,
                    barnasAndeler = barnasAndelerInkludertEtterbetaling3ÅrEndringer,
                    barnasAktørerOgFødselsdatoer = personopplysningGrunnlag.barna.map {
                        Pair(
                            it.aktør,
                            it.fødselsdato,
                        )
                    },
                )
        } else {
            emptyList()
        }

        val andelerTilkjentYtelseBarnaMedAlleEndringer = oppdaterTilkjentYtelseMedEndretUtbetalingAndeler(
            andelTilkjentYtelserUtenEndringer = andelerTilkjentYtelseBarnaUtenEndringer,
            endretUtbetalingAndeler = endretUtbetalingAndelerBarna,
        )

        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerTilkjentYtelseBarnaMedAlleEndringer.map { it.andel } + andelerTilkjentYtelseUtvidetMedAlleEndringer.map { it.andel } + andelerTilkjentYtelseSmåbarnstillegg.map { it.andel })

        return tilkjentYtelse
    }

    private fun erSmåbarnstilleggMulig(
        utvidetAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        barnasAndeler: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
    ): Boolean = utvidetAndeler.isNotEmpty() && barnasAndeler.isNotEmpty()

    fun oppdaterTilkjentYtelseMedEndretUtbetalingAndeler(
        andelTilkjentYtelserUtenEndringer: Collection<AndelTilkjentYtelse>,
        endretUtbetalingAndeler: List<EndretUtbetalingAndelMedAndelerTilkjentYtelse>,
    ): List<AndelTilkjentYtelseMedEndreteUtbetalinger> {
        if (endretUtbetalingAndeler.isEmpty()) {
            return andelTilkjentYtelserUtenEndringer
                .map { AndelTilkjentYtelseMedEndreteUtbetalinger.utenEndringer(it.copy()) }
        }

        val (andelerUtenSmåbarnstillegg, andelerMedSmåbarnstillegg) = andelTilkjentYtelserUtenEndringer.partition { !it.erSmåbarnstillegg() }

        val nyeAndelTilkjentYtelse = mutableListOf<AndelTilkjentYtelseMedEndreteUtbetalinger>()

        andelerUtenSmåbarnstillegg.groupBy { it.aktør }.forEach { andelerForPerson ->
            val aktør = andelerForPerson.key
            val endringerForPerson =
                endretUtbetalingAndeler.filter { it.person?.aktør == aktør }

            val nyeAndelerForPerson = mutableListOf<AndelTilkjentYtelseMedEndreteUtbetalinger>()

            andelerForPerson.value.forEach { andelForPerson ->
                // Deler opp hver enkelt andel i perioder som hhv blir berørt av endringene og de som ikke berøres av de.
                val (perioderMedEndring, perioderUtenEndring) = andelForPerson.stønadsPeriode()
                    .perioderMedOgUtenOverlapp(
                        endringerForPerson.map { endringerForPerson -> endringerForPerson.periode },
                    )
                // Legger til nye AndelTilkjentYtelse for perioder som er berørt av endringer.
                nyeAndelerForPerson.addAll(
                    perioderMedEndring.map { månedPeriodeEndret ->
                        val endretUtbetalingMedAndeler =
                            endringerForPerson.single { it.overlapperMed(månedPeriodeEndret) }
                        val nyttNasjonaltPeriodebeløp = andelForPerson.sats
                            .avrundetHeltallAvProsent(endretUtbetalingMedAndeler.prosent!!)

                        val andelTilkjentYtelse = andelForPerson.copy(
                            prosent = endretUtbetalingMedAndeler.prosent!!,
                            stønadFom = månedPeriodeEndret.fom,
                            stønadTom = månedPeriodeEndret.tom,
                            kalkulertUtbetalingsbeløp = nyttNasjonaltPeriodebeløp,
                            nasjonaltPeriodebeløp = nyttNasjonaltPeriodebeløp,
                        )

                        andelTilkjentYtelse.medEndring(endretUtbetalingMedAndeler)
                    },
                )
                // Legger til nye AndelTilkjentYtelse for perioder som ikke berøres av endringer.
                nyeAndelerForPerson.addAll(
                    perioderUtenEndring.map { månedPeriodeUendret ->
                        val andelTilkjentYtelse = andelForPerson.copy(
                            stønadFom = månedPeriodeUendret.fom,
                            stønadTom = månedPeriodeUendret.tom,
                        )
                        AndelTilkjentYtelseMedEndreteUtbetalinger.utenEndringer(andelTilkjentYtelse)
                    },
                )
            }

            val nyeAndelerForPersonEtterSammenslåing =
                slåSammenPerioderSomIkkeSkulleHaVærtSplittet(
                    andelerTilkjentYtelseMedEndreteUtbetalinger = nyeAndelerForPerson,
                    skalAndelerSlåsSammen = ::skalAndelerSlåsSammen,
                )

            nyeAndelTilkjentYtelse.addAll(nyeAndelerForPersonEtterSammenslåing)
        }

        // Ettersom vi aldri ønsker å overstyre småbarnstillegg perioder fjerner vi dem og legger dem til igjen her
        nyeAndelTilkjentYtelse.addAll(
            andelerMedSmåbarnstillegg.map {
                AndelTilkjentYtelseMedEndreteUtbetalinger.utenEndringer(it)
            },
        )

        // Sorterer primært av hensyn til måten testene er implementert og kan muligens fjernes dersom dette skrives om.
        nyeAndelTilkjentYtelse.sortWith(
            compareBy(
                { it.aktør.aktivFødselsnummer() },
                { it.stønadFom },
            ),
        )
        return nyeAndelTilkjentYtelse
    }

    fun slåSammenPerioderSomIkkeSkulleHaVærtSplittet(
        andelerTilkjentYtelseMedEndreteUtbetalinger: MutableList<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        skalAndelerSlåsSammen: (førsteAndel: AndelTilkjentYtelseMedEndreteUtbetalinger, nesteAndel: AndelTilkjentYtelseMedEndreteUtbetalinger) -> Boolean,
    ): MutableList<AndelTilkjentYtelseMedEndreteUtbetalinger> {
        val sorterteAndeler = andelerTilkjentYtelseMedEndreteUtbetalinger.sortedBy { it.stønadFom }.toMutableList()
        var periodenViSerPå = sorterteAndeler.first()
        val oppdatertListeMedAndeler = mutableListOf<AndelTilkjentYtelseMedEndreteUtbetalinger>()

        for (index in 0 until sorterteAndeler.size) {
            val andel = sorterteAndeler[index]
            val nesteAndel = if (index == sorterteAndeler.size - 1) null else sorterteAndeler[index + 1]

            periodenViSerPå = if (nesteAndel != null) {
                val andelerSkalSlåsSammen =
                    skalAndelerSlåsSammen(andel, nesteAndel)

                if (andelerSkalSlåsSammen) {
                    val nyAndel = periodenViSerPå.slåSammenMed(nesteAndel)
                    nyAndel
                } else {
                    oppdatertListeMedAndeler.add(periodenViSerPå)
                    sorterteAndeler[index + 1]
                }
            } else {
                oppdatertListeMedAndeler.add(periodenViSerPå)
                break
            }
        }
        return oppdatertListeMedAndeler
    }

    /**
     * Slår sammen andeler for barn når beløpet er nedjuster til 0kr som er blitt splittet av
     * for eksempel satsendring.
     */
    fun skalAndelerSlåsSammen(
        førsteAndel: AndelTilkjentYtelseMedEndreteUtbetalinger,
        nesteAndel: AndelTilkjentYtelseMedEndreteUtbetalinger,
    ): Boolean =
        førsteAndel.stønadTom.sisteDagIInneværendeMåned()
            .erDagenFør(nesteAndel.stønadFom.førsteDagIInneværendeMåned()) && førsteAndel.prosent == BigDecimal(0) && nesteAndel.prosent == BigDecimal(
            0,
        ) && førsteAndel.endreteUtbetalinger.isNotEmpty() && førsteAndel.endreteUtbetalinger.singleOrNull() == nesteAndel.endreteUtbetalinger.singleOrNull()
}

fun MånedPeriode.perioderMedOgUtenOverlapp(perioder: List<MånedPeriode>): Pair<List<MånedPeriode>, List<MånedPeriode>> {
    if (perioder.isEmpty()) return Pair(emptyList(), listOf(this))

    val alleMånederMedOverlappstatus = mutableMapOf<YearMonth, Boolean>()
    var nesteMåned = this.fom
    while (nesteMåned <= this.tom) {
        alleMånederMedOverlappstatus[nesteMåned] =
            perioder.any { månedPeriode -> månedPeriode.inkluderer(nesteMåned) }
        nesteMåned = nesteMåned.plusMonths(1)
    }

    var periodeStart: YearMonth? = this.fom

    val perioderMedOverlapp = mutableListOf<MånedPeriode>()
    val perioderUtenOverlapp = mutableListOf<MånedPeriode>()
    while (periodeStart != null) {
        val periodeMedOverlapp = alleMånederMedOverlappstatus[periodeStart]!!

        val nesteMånedMedNyOverlappstatus = alleMånederMedOverlappstatus
            .filter { it.key > periodeStart && it.value != periodeMedOverlapp }
            .minByOrNull { it.key }
            ?.key?.minusMonths(1) ?: this.tom

        // Når tom skal utledes for en periode det eksisterer en endret periode for må den minste av følgende to datoer velges:
        // 1. tom for den aktuelle endrete perioden
        // 2. neste måned uten overlappende endret periode, eller hvis null, tom for this (som representerer en AndelTilkjentYtelse).
        // Dersom tom gjelder periode uberørt av endringer så vil alltid alt.2 være korrekt.
        val periodeSlutt = if (periodeMedOverlapp) {
            val nesteMånedUtenOverlapp = perioder.single { it.inkluderer(periodeStart!!) }.tom
            minOf(nesteMånedUtenOverlapp, nesteMånedMedNyOverlappstatus)
        } else {
            nesteMånedMedNyOverlappstatus
        }

        if (periodeMedOverlapp) {
            perioderMedOverlapp.add(MånedPeriode(periodeStart, periodeSlutt))
        } else {
            perioderUtenOverlapp.add(MånedPeriode(periodeStart, periodeSlutt))
        }

        periodeStart = alleMånederMedOverlappstatus
            .filter { it.key > periodeSlutt }
            .minByOrNull { it.key }?.key
    }
    return Pair(perioderMedOverlapp, perioderUtenOverlapp)
}

internal data class BeregnetAndel(
    val person: Person,
    val stønadFom: YearMonth,
    val stønadTom: YearMonth,
    val beløp: Int,
    val sats: Int,
    val prosent: BigDecimal,
)
