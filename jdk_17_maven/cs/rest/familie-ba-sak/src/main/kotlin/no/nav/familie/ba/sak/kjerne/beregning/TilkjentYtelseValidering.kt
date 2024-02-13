package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.KONTAKT_TEAMET_SUFFIX
import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.common.UtbetalingsikkerhetFeil
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValidering.maksBeløp
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjeMedAndeler
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerPersonOgType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIUtbetalingUtil
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringUtil.tilFørsteEndringstidspunkt
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonEnkel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.barn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.søker
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombiner
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.outerJoin
import no.nav.familie.ba.sak.kjerne.tidslinje.månedPeriodeAv
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

// 3 år (krav i loven)
fun hentGyldigEtterbetalingFom(kravDato: LocalDate) =
    kravDato.minusYears(3)
        .toYearMonth()

fun hentSøkersAndeler(
    andeler: List<AndelTilkjentYtelse>,
    søker: PersonEnkel,
) = andeler.filter { it.aktør == søker.aktør }

fun hentBarnasAndeler(andeler: List<AndelTilkjentYtelse>, barna: List<PersonEnkel>) = barna.map { barn ->
    barn to andeler.filter { it.aktør == barn.aktør }
}

/**
 * Ekstra sikkerhet rundt hva som utbetales som på sikt vil legges inn i
 * de respektive stegene SB håndterer slik at det er lettere for SB å rette feilene.
 */
object TilkjentYtelseValidering {

    internal fun validerAtSatsendringKunOppdatererSatsPåEksisterendePerioder(
        andelerFraForrigeBehandling: List<AndelTilkjentYtelse>,
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    ) {
        val andelerGruppert = andelerTilkjentYtelse.tilTidslinjerPerPersonOgType()
        val forrigeAndelerGruppert = andelerFraForrigeBehandling.tilTidslinjerPerPersonOgType()

        andelerGruppert.outerJoin(forrigeAndelerGruppert) { nåværendeAndel, forrigeAndel ->
            when {
                forrigeAndel == null && nåværendeAndel != null ->
                    throw Feil("Satsendring kan ikke legge til en andel som ikke var der i forrige behandling")

                forrigeAndel != null && nåværendeAndel == null ->
                    throw Feil("Satsendring kan ikke fjerne en andel som fantes i forrige behandling")

                forrigeAndel != null && forrigeAndel.prosent != nåværendeAndel?.prosent ->
                    throw Feil("Satsendring kan ikke endre på prosenten til en andel")

                forrigeAndel != null && forrigeAndel.type != nåværendeAndel?.type ->
                    throw Feil("Satsendring kan ikke endre YtelseType til en andel")

                else -> false
            }
        }.values.map { it.perioder() } // Må kalle på .perioder() for at feilene over skal bli kastet
    }

    fun finnAktørIderMedUgyldigEtterbetalingsperiode(
        forrigeAndelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
        andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
        kravDato: LocalDateTime,
    ): List<Aktør> {
        val gyldigEtterbetalingFom = hentGyldigEtterbetalingFom(kravDato.toLocalDate())

        val aktører = unikeAntører(andelerTilkjentYtelse, forrigeAndelerTilkjentYtelse)

        val personerMedUgyldigEtterbetaling =
            aktører.mapNotNull { aktør ->
                val andelerTilkjentYtelseForPerson = andelerTilkjentYtelse.filter { it.aktør == aktør }
                val forrigeAndelerTilkjentYtelseForPerson = forrigeAndelerTilkjentYtelse.filter { it.aktør == aktør }

                aktør.takeIf {
                    erUgyldigEtterbetalingPåPerson(
                        forrigeAndelerTilkjentYtelseForPerson,
                        andelerTilkjentYtelseForPerson,
                        gyldigEtterbetalingFom,
                    )
                }
            }

        return personerMedUgyldigEtterbetaling
    }

    private fun unikeAntører(
        andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
        forrigeAndelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
    ): Set<Aktør> {
        val aktørIderFraAndeler = andelerTilkjentYtelse.map { it.aktør }
        val aktøerIderFraForrigeAndeler = forrigeAndelerTilkjentYtelse.map { it.aktør }
        return (aktørIderFraAndeler + aktøerIderFraForrigeAndeler).toSet()
    }

    fun erUgyldigEtterbetalingPåPerson(
        forrigeAndelerForPerson: List<AndelTilkjentYtelse>,
        andelerForPerson: List<AndelTilkjentYtelse>,
        gyldigEtterbetalingFom: YearMonth,
    ): Boolean {
        return YtelseType.values().any { ytelseType ->
            val forrigeAndelerForPersonOgType = forrigeAndelerForPerson.filter { it.type == ytelseType }
            val andelerForPersonOgType = andelerForPerson.filter { it.type == ytelseType }

            val etterbetalingTidslinje = EndringIUtbetalingUtil.lagEtterbetalingstidslinjeForPersonOgType(
                nåværendeAndeler = andelerForPersonOgType,
                forrigeAndeler = forrigeAndelerForPersonOgType,
            )

            val førsteMånedMedEtterbetaling = etterbetalingTidslinje.tilFørsteEndringstidspunkt()

            førsteMånedMedEtterbetaling != null && førsteMånedMedEtterbetaling < gyldigEtterbetalingFom
        }
    }

    fun validerAtTilkjentYtelseHarFornuftigePerioderOgBeløp(
        tilkjentYtelse: TilkjentYtelse,
        søkerOgBarn: List<PersonEnkel>,
    ) {
        val søker = søkerOgBarn.søker()
        val barna = søkerOgBarn.barn()

        val tidslinjeMedAndeler = tilkjentYtelse.tilTidslinjeMedAndeler()

        val fagsakType = tilkjentYtelse.behandling.fagsak.type

        tidslinjeMedAndeler.toSegments().forEach {
            val søkersAndeler = hentSøkersAndeler(it.value, søker)
            val barnasAndeler = hentBarnasAndeler(it.value, barna)

            validerAtBeløpForPartStemmerMedSatser(person = søker, andeler = søkersAndeler, fagsakType = fagsakType)

            barnasAndeler.forEach { (barn, andeler) ->
                validerAtBeløpForPartStemmerMedSatser(person = barn, andeler = andeler, fagsakType = fagsakType)
            }
        }
    }

    fun validerAtBarnIkkeFårFlereUtbetalingerSammePeriode(
        behandlendeBehandlingTilkjentYtelse: TilkjentYtelse,
        barnMedAndreRelevanteTilkjentYtelser: List<Pair<PersonEnkel, List<TilkjentYtelse>>>,
        søkerOgBarn: List<PersonEnkel>,
    ) {
        val barna = søkerOgBarn.barn().sortedBy { it.fødselsdato }

        val barnasAndeler = hentBarnasAndeler(behandlendeBehandlingTilkjentYtelse.andelerTilkjentYtelse.toList(), barna)

        val barnMedUtbetalingsikkerhetFeil = mutableMapOf<PersonEnkel, List<MånedPeriode>>()
        barnasAndeler.forEach { (barn, andeler) ->
            val barnsAndelerFraAndreBehandlinger =
                barnMedAndreRelevanteTilkjentYtelser.filter { it.first.aktør == barn.aktør }
                    .flatMap { it.second }
                    .flatMap { it.andelerTilkjentYtelse }
                    .filter { it.aktør == barn.aktør }

            val perioderMedOverlapp = finnPeriodeMedOverlappAvAndeler(
                andeler = andeler,
                barnsAndelerFraAndreBehandlinger = barnsAndelerFraAndreBehandlinger,
            )
            if (perioderMedOverlapp.isNotEmpty()) {
                barnMedUtbetalingsikkerhetFeil.put(barn, perioderMedOverlapp)
            }
        }
        if (barnMedUtbetalingsikkerhetFeil.isNotEmpty()) {
            throw UtbetalingsikkerhetFeil(
                melding = "Vi finner utbetalinger som overstiger 100% på hvert av barna: ${
                    barnMedUtbetalingsikkerhetFeil.tilFeilmeldingTekst()
                }",
                frontendFeilmelding = "Du kan ikke godkjenne dette vedtaket fordi det vil betales ut mer enn 100% for barn født ${
                    barnMedUtbetalingsikkerhetFeil.tilFeilmeldingTekst()
                }. Reduksjonsvedtak til annen person må være sendt til godkjenning før du kan gå videre.",
            )
        }
    }

    fun MutableMap<PersonEnkel, List<MånedPeriode>>.tilFeilmeldingTekst() =
        Utils.slåSammen(this.map { "${it.key.fødselsdato.tilKortString()} i perioden ${it.value.joinToString(", ") { "${it.fom} til ${it.tom}" }}" })

    fun maksBeløp(personType: PersonType, fagsakType: FagsakType): Int {
        val satser = SatsService.hentAllesatser()
        val småbarnsTillegg = satser.filter { it.type == SatsType.SMA }
        val ordinærMedTillegg = satser.filter { it.type == SatsType.TILLEGG_ORBA }
        val utvidet = satser.filter { it.type == SatsType.UTVIDET_BARNETRYGD }
        if (småbarnsTillegg.isEmpty() || ordinærMedTillegg.isEmpty() || utvidet.isEmpty()) error("Fant ikke satser ved validering")
        val maksSmåbarnstillegg = småbarnsTillegg.maxByOrNull { it.beløp }!!.beløp
        val maksOrdinærMedTillegg = ordinærMedTillegg.maxByOrNull { it.beløp }!!.beløp
        val maksUtvidet = utvidet.maxBy { it.beløp }.beløp

        return if (fagsakType == FagsakType.BARN_ENSLIG_MINDREÅRIG) {
            maksOrdinærMedTillegg + maksUtvidet
        } else {
            when (personType) {
                PersonType.BARN -> maksOrdinærMedTillegg
                PersonType.SØKER -> maksUtvidet + maksSmåbarnstillegg
                else -> throw Feil("Ikke støtte for å utbetale til persontype ${personType.name}")
            }
        }
    }

    fun finnPeriodeMedOverlappAvAndeler(
        andeler: List<AndelTilkjentYtelse>,
        barnsAndelerFraAndreBehandlinger: List<AndelTilkjentYtelse>,
    ): List<MånedPeriode> {
        val kombinertOverlappTidslinje = YtelseType.values().map { ytelseType ->
            lagErOver100ProsentUtbetalingPåYtelseTidslinje(
                andeler = andeler.filter { it.type == ytelseType },
                barnsAndelerFraAndreBehandlinger = barnsAndelerFraAndreBehandlinger.filter { it.type == ytelseType },
            )
        }.kombiner { it.minstEnYtelseHarOverlapp() }

        return kombinertOverlappTidslinje.perioder().filter { it.innhold == true }
            .map { MånedPeriode(it.fraOgMed.tilYearMonth(), it.tilOgMed.tilYearMonth()) }
    }

    internal fun Iterable<Boolean>.minstEnYtelseHarOverlapp(): Boolean {
        return any { it }
    }

    fun lagErOver100ProsentUtbetalingPåYtelseTidslinje(
        andeler: List<AndelTilkjentYtelse>,
        barnsAndelerFraAndreBehandlinger: List<AndelTilkjentYtelse>,
    ): Tidslinje<Boolean, Måned> {
        if (barnsAndelerFraAndreBehandlinger.isEmpty()) {
            return emptyList<Periode<Boolean, Måned>>().tilTidslinje()
        }
        val prosenttidslinjerPerBehandling =
            (andeler + barnsAndelerFraAndreBehandlinger).groupBy { it.behandlingId }.values
                .map { it.tilProsentAvYtelseUtbetaltTidslinje() }

        val erOver100ProsentTidslinje =
            prosenttidslinjerPerBehandling.fold(emptyList<Periode<BigDecimal, Måned>>().tilTidslinje()) { summertProsentTidslinje, prosentTidslinje ->
                summertProsentTidslinje.kombinerMed(prosentTidslinje) { sumProsentForPeriode, prosentForAndel ->
                    (sumProsentForPeriode ?: BigDecimal.ZERO) + (prosentForAndel ?: BigDecimal.ZERO)
                }
            }.map { sumProsentForPeriode -> (sumProsentForPeriode ?: BigDecimal.ZERO) > BigDecimal.valueOf(100) }

        return erOver100ProsentTidslinje
    }
}

private fun List<AndelTilkjentYtelse>.tilProsentAvYtelseUtbetaltTidslinje() =
    this.map {
        månedPeriodeAv(
            fraOgMed = it.periode.fom,
            tilOgMed = it.periode.tom,
            innhold = it.prosent,
        )
    }.tilTidslinje()

private fun validerAtBeløpForPartStemmerMedSatser(
    person: PersonEnkel,
    andeler: List<AndelTilkjentYtelse>,
    fagsakType: FagsakType,
) {
    val maksAntallAndeler =
        if (fagsakType == FagsakType.BARN_ENSLIG_MINDREÅRIG) 2 else if (person.type == PersonType.BARN) 1 else 2
    val maksTotalBeløp = maksBeløp(personType = person.type, fagsakType = fagsakType)

    if (andeler.size > maksAntallAndeler) {
        throw UtbetalingsikkerhetFeil(
            melding = "Validering av andeler for ${person.type} i perioden (${andeler.first().stønadFom} - ${andeler.first().stønadTom}) feilet: Tillatte andeler = $maksAntallAndeler, faktiske andeler = ${andeler.size}.",
            frontendFeilmelding = "Det har skjedd en systemfeil, og beløpene stemmer ikke overens med dagens satser. $KONTAKT_TEAMET_SUFFIX",
        )
    }

    val totalbeløp = andeler.map { it.kalkulertUtbetalingsbeløp }
        .fold(0) { sum, beløp -> sum + beløp }
    if (totalbeløp > maksTotalBeløp) {
        throw UtbetalingsikkerhetFeil(
            melding = "Validering av andeler for ${person.type} i perioden (${andeler.first().stønadFom} - ${andeler.first().stønadTom}) feilet: Tillatt totalbeløp = $maksTotalBeløp, faktiske totalbeløp = $totalbeløp.",
            frontendFeilmelding = "Det har skjedd en systemfeil, og beløpene stemmer ikke overens med dagens satser. $KONTAKT_TEAMET_SUFFIX",
        )
    }
}
