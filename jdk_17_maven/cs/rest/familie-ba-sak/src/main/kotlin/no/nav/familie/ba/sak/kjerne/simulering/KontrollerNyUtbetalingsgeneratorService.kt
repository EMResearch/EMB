package no.nav.familie.ba.sak.kjerne.simulering

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.økonomi.AndelTilkjentYtelseForSimuleringFactory
import no.nav.familie.ba.sak.integrasjoner.økonomi.UtbetalingsoppdragGeneratorService
import no.nav.familie.ba.sak.integrasjoner.økonomi.skalIverksettesMotOppdrag
import no.nav.familie.ba.sak.integrasjoner.økonomi.tilRestUtbetalingsoppdrag
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiKlient
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.simulering.domene.SimuleringsPeriode
import no.nav.familie.ba.sak.kjerne.simulering.domene.tilTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.fraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilForrigeMåned
import no.nav.familie.ba.sak.kjerne.tidslinje.tilOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjær
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelMedPeriodeIdLongId
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.YearMonth

@Service
class KontrollerNyUtbetalingsgeneratorService(
    private val featureToggleService: FeatureToggleService,
    private val økonomiKlient: ØkonomiKlient,
    private val utbetalingsoppdragGeneratorService: UtbetalingsoppdragGeneratorService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) {

    fun kontrollerNyUtbetalingsgenerator(
        vedtak: Vedtak,
        saksbehandlerId: String,
    ): List<DiffFeilType> {
        if (!skalKontrollereOppMotNyUtbetalingsgenerator()) return emptyList()

        val gammeltUtbetalingsoppdrag =
            utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = vedtak,
                saksbehandlerId = saksbehandlerId,
                andelTilkjentYtelseForUtbetalingsoppdragFactory = AndelTilkjentYtelseForSimuleringFactory(),
            )

        if (!gammeltUtbetalingsoppdrag.skalIverksettesMotOppdrag()) return emptyList()

        val gammeltSimuleringResultat = økonomiKlient.hentSimulering(gammeltUtbetalingsoppdrag)

        return kontrollerNyUtbetalingsgenerator(
            vedtak = vedtak,
            gammeltSimuleringResultat = gammeltSimuleringResultat,
            gammeltUtbetalingsoppdrag = gammeltUtbetalingsoppdrag,
        )
    }

    fun kontrollerNyUtbetalingsgenerator(
        vedtak: Vedtak,
        gammeltSimuleringResultat: DetaljertSimuleringResultat,
        gammeltUtbetalingsoppdrag: Utbetalingsoppdrag,
        erSimulering: Boolean = false,
    ): List<DiffFeilType> {
        try {
            if (!skalKontrollereOppMotNyUtbetalingsgenerator()) return emptyList()

            val behandling = vedtak.behandling

            if (erSimulering) {
                secureLogger.info("Simulerer utbetalingsoppdrag for simulering for behandling ${behandling.id}")
            } else {
                secureLogger.info(
                    "Simulerer utbetalingsoppdrag for iverksettelse for behandling ${behandling.id}",
                )
            }

            val diffFeilTyper = mutableListOf<DiffFeilType>()

            val beregnetUtbetalingsoppdrag =
                utbetalingsoppdragGeneratorService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                    vedtak = vedtak,
                    saksbehandlerId = SikkerhetContext.hentSaksbehandler().take(8),
                    erSimulering = erSimulering,
                )

            if (!beregnetUtbetalingsoppdrag.utbetalingsoppdrag.tilRestUtbetalingsoppdrag()
                    .skalIverksettesMotOppdrag()
            ) {
                return emptyList()
            }

            secureLogger.info("Behandling ${behandling.id} har følgende oppdaterte andeler: ${beregnetUtbetalingsoppdrag.andeler}")

            secureLogger.info("Behandling ${behandling.id} får følgende utbetalingsoppdrag med gammel generator: $gammeltUtbetalingsoppdrag")
            secureLogger.info("Behandling ${behandling.id} får følgende utbetalingsoppdrag med ny generator: ${beregnetUtbetalingsoppdrag.utbetalingsoppdrag}")

            validerAtAndelerIBeregnetUtbetalingsoppdragMatcherAndelerMedUtbetaling(
                beregnetUtbetalingsoppdrag.andeler,
                behandling,
            )?.let {
                diffFeilTyper.add(it)
            }

            val nyttSimuleringResultat =
                økonomiKlient.hentSimulering(beregnetUtbetalingsoppdrag.utbetalingsoppdrag.tilRestUtbetalingsoppdrag())

            if (nyttSimuleringResultat.simuleringMottaker.isEmpty() && gammeltSimuleringResultat.simuleringMottaker.isEmpty()) return diffFeilTyper

            if (!bådeNyOgGammelGirEtResultat(
                    nyttSimuleringResultat = nyttSimuleringResultat,
                    gammeltSimuleringResultat = gammeltSimuleringResultat,
                    behandling = behandling,
                )
            ) {
                diffFeilTyper.add(DiffFeilType.DetEneSimuleringsresultatetErTomt)
                return diffFeilTyper
            }

            val simuleringsPerioderGammel = gammeltSimuleringResultat.tilSorterteSimuleringsPerioder(behandling)

            val simuleringsPerioderNy = nyttSimuleringResultat.tilSorterteSimuleringsPerioder(behandling)

            val simuleringsPerioderGammelTidslinje: Tidslinje<SimuleringsPeriode, Måned> =
                simuleringsPerioderGammel.tilTidslinje()

            val simuleringsPerioderNyTidslinje: Tidslinje<SimuleringsPeriode, Måned> =
                simuleringsPerioderNy.tilTidslinje()

            validerAtSimuleringsPerioderGammelHarResultatLik0ForPerioderFørSimuleringsPerioderNy(
                simuleringsPerioderGammelTidslinje = simuleringsPerioderGammelTidslinje,
                simuleringsPerioderNyTidslinje = simuleringsPerioderNyTidslinje,
                behandling = behandling,
            )?.let {
                diffFeilTyper.add(it)
            }
            validerAtSimuleringsPerioderGammelHarResultatLikSimuleringsPerioderNyEtterFomTilNy(
                simuleringsPerioderGammelTidslinje = simuleringsPerioderGammelTidslinje,
                simuleringsPerioderNyTidslinje = simuleringsPerioderNyTidslinje,
                behandling = behandling,
            )?.let {
                diffFeilTyper.add(it)
            }

            if (diffFeilTyper.isNotEmpty()) {
                loggSimuleringsPerioderMedDiff(simuleringsPerioderGammel, simuleringsPerioderNy)
            }

            if (diffFeilTyper.isNotEmpty()) {
                secureLogger.info("kontrollerNyUtbetalingsgenerator for ${behandling.id} ga følgende feiltyper=$diffFeilTyper")
            }

            return diffFeilTyper
        } catch (e: Exception) {
            secureLogger.warn(
                "En uventet feil har oppstått ved kontroll av ny utbetalingsoppdrag-generator for behandling ${vedtak.behandling.id}",
                e,
            )
            return listOf(DiffFeilType.UventetFeil)
        }
    }

    private fun validerAtAndelerIBeregnetUtbetalingsoppdragMatcherAndelerMedUtbetaling(
        andeler: List<AndelMedPeriodeIdLongId>,
        behandling: Behandling,
    ): DiffFeilType? {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandlingId = behandling.id)

        val andelerMedUtbetaling = tilkjentYtelse.andelerTilkjentYtelse.filter { it.erAndelSomSkalSendesTilOppdrag() }

        if (andeler.size != andelerMedUtbetaling.size) {
            secureLogger.warn("Antallet andeler fra ny generator matcher ikke antallet andeler med utbetaling i behandling ${behandling.id}. Andeler fra ny generator: ${andeler.map { it.id }}, andeler med utbetaling: ${andelerMedUtbetaling.map { it.id }}.")
            return DiffFeilType.FeilAntallAndeler
        }

        if (!andelerMedUtbetaling.all { andelerMedUtbetaling -> andeler.any { it.id == andelerMedUtbetaling.id } }) {
            secureLogger.warn("Finner ikke match for alle andeler med utbetaling i behandling ${behandling.id} blandt andelene returnert fra ny generator. Andeler fra ny generator: ${andeler.map { it.id }}, andeler med utbetaling: ${andelerMedUtbetaling.map { it.id }}.")
            return DiffFeilType.AndelerMatcherIkke
        }

        return null
    }

    private fun bådeNyOgGammelGirEtResultat(
        nyttSimuleringResultat: DetaljertSimuleringResultat,
        gammeltSimuleringResultat: DetaljertSimuleringResultat,
        behandling: Behandling,
    ): Boolean {
        val andelerEtterDagensDato = tilkjentYtelseRepository.findByBehandling(behandlingId = behandling.id)
            .andelerTilkjentYtelse
            .filter { andelTilkjentYtelse ->
                andelTilkjentYtelse.stønadTom.isAfter(YearMonth.now())
            }
        if (!(nyttSimuleringResultat.simuleringMottaker.isNotEmpty() && gammeltSimuleringResultat.simuleringMottaker.isNotEmpty())) {
            secureLogger.warn("Behandling ${behandling.id} får tomt simuleringsresultat med ny eller gammel generator. Ny er tom: ${nyttSimuleringResultat.simuleringMottaker.isEmpty()}, Gammel er tom: ${gammeltSimuleringResultat.simuleringMottaker.isEmpty()}. antallAndeler=${andelerEtterDagensDato.size}, resultat=${behandling.resultat}")
            return false
        }
        return true
    }

    private fun skalKontrollereOppMotNyUtbetalingsgenerator(): Boolean =
        featureToggleService.isEnabled(FeatureToggleConfig.KONTROLLER_NY_UTBETALINGSGENERATOR, false)

    private fun validerAtSimuleringsPerioderGammelHarResultatLikSimuleringsPerioderNyEtterFomTilNy(
        simuleringsPerioderGammelTidslinje: Tidslinje<SimuleringsPeriode, Måned>,
        simuleringsPerioderNyTidslinje: Tidslinje<SimuleringsPeriode, Måned>,
        behandling: Behandling,
    ): DiffFeilType? {
        // Tidslinje over simuleringsperioder fra gammel simulering som starter samtidig som simulering fra ny generator
        val simuleringsPerioderTidslinjeGammelFraNy =
            simuleringsPerioderGammelTidslinje.beskjær(
                simuleringsPerioderNyTidslinje.fraOgMed()!!,
                simuleringsPerioderGammelTidslinje.tilOgMed()!!,
            )

        // Tidslinjene skal ha samme resultat for alle perioder
        val månederMedUliktResultat =
            simuleringsPerioderTidslinjeGammelFraNy
                .kombinerMed(simuleringsPerioderNyTidslinje) { gammel, ny ->
                    KombinertSimuleringsResultat(
                        erLike = gammel?.resultat == (ny?.resultat ?: BigDecimal.ZERO),
                        gammel = gammel,
                        ny = ny,
                    )
                }
                .perioder()
                .filter { !it.innhold!!.erLike }

        if (månederMedUliktResultat.isNotEmpty()) {
            secureLogger.warn("Behandling ${behandling.id}  har diff i simuleringsresultat ved bruk av ny utbetalingsgenerator - følgende måneder har ulikt resultat: [${månederMedUliktResultat.joinToString { "${it.fraOgMed} - ${it.tilOgMed}: Gammel ${it.innhold!!.gammel?.resultat} vs Ny ${it.innhold.ny?.resultat}" }}]")
            return DiffFeilType.UliktResultatISammePeriode
        }
        return null
    }

    // Tidslinje over simuleringsperioder som kommer før simuleringsperiodene til simulering fra ny generator.
    // Fordi vi opphører mer bakover i tiden med den gamle generatoren vil vi kunne få flere simuleringsperioder som kommer før simuleringsperiodene vi får fra ny generator.
    // Disse periodene skal ha et resultat som er lik 0, ellers er det noe feil.
    private fun validerAtSimuleringsPerioderGammelHarResultatLik0ForPerioderFørSimuleringsPerioderNy(
        simuleringsPerioderGammelTidslinje: Tidslinje<SimuleringsPeriode, Måned>,
        simuleringsPerioderNyTidslinje: Tidslinje<SimuleringsPeriode, Måned>,
        behandling: Behandling,
    ): DiffFeilType? {
        val perioderFraGammelFørNyMedResultatUlik0 = simuleringsPerioderGammelTidslinje
            .beskjær(
                simuleringsPerioderGammelTidslinje.fraOgMed()!!,
                simuleringsPerioderNyTidslinje.fraOgMed()!!.tilForrigeMåned(),
            ).perioder()
            // Bruker compareTo for å ignorere scale. 0 == 0.00 gir false, mens 0.compareTo(0.00) gir 0 som betyr at de er like.
            .filter { it.innhold!!.resultat.compareTo(BigDecimal.ZERO) != 0 }

        if (perioderFraGammelFørNyMedResultatUlik0.isNotEmpty()) {
            secureLogger.warn("Behandling ${behandling.id}  har diff i simuleringsresultat ved bruk av ny utbetalingsgenerator - simuleringsperioder før simuleringsperioder fra gammel generator gir resultat ulik 0. [${perioderFraGammelFørNyMedResultatUlik0.joinToString() { it.toString() }}]")
            return DiffFeilType.TidligerePerioderIGammelUlik0
        }
        return null
    }

    private fun loggSimuleringsPerioderMedDiff(
        simuleringsPerioderGammel: List<SimuleringsPeriode>,
        simuleringsPerioderNy: List<SimuleringsPeriode>,
    ) {
        secureLogger.warn("Simuleringsperioder med diff - Gammel: [${simuleringsPerioderGammel.joinToString() { "${it.fom} - ${it.tom}: ${it.resultat}" }}] Ny: [${simuleringsPerioderNy.joinToString() { "${it.fom} - ${it.tom}: ${it.resultat}" }}]")
    }

    private fun DetaljertSimuleringResultat.tilSorterteSimuleringsPerioder(behandling: Behandling): List<SimuleringsPeriode> =
        vedtakSimuleringMottakereTilSimuleringPerioder(
            this.simuleringMottaker.map {
                it.tilBehandlingSimuleringMottaker(behandling)
            },
            true,
        ).sortedBy { it.fom }

    data class KombinertSimuleringsResultat(
        val erLike: Boolean,
        val gammel: SimuleringsPeriode?,
        val ny: SimuleringsPeriode?,
    )
}

enum class DiffFeilType {
    TidligerePerioderIGammelUlik0,
    UliktResultatISammePeriode,
    DetEneSimuleringsresultatetErTomt,
    FeilAntallAndeler,
    AndelerMatcherIkke,
    UventetFeil,
}
