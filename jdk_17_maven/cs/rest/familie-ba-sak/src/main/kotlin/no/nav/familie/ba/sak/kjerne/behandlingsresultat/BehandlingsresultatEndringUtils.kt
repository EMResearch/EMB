package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatOpphørUtils.utledOpphørsdatoForNåværendeBehandlingMedFallback
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseTidslinje
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIEndretUtbetalingAndelUtil
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIKompetanseUtil
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIVilkårsvurderingUtil
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilMånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.beskjær
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import java.time.YearMonth

internal enum class Endringsresultat {
    ENDRING,
    INGEN_ENDRING,
}
object BehandlingsresultatEndringUtils {

    internal fun utledEndringsresultat(
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        forrigeAndeler: List<AndelTilkjentYtelse>,
        personerFremstiltKravFor: List<Aktør>,
        nåværendeKompetanser: List<Kompetanse>,
        forrigeKompetanser: List<Kompetanse>,
        nåværendePersonResultat: Set<PersonResultat>,
        forrigePersonResultat: Set<PersonResultat>,
        nåværendeEndretAndeler: List<EndretUtbetalingAndel>,
        forrigeEndretAndeler: List<EndretUtbetalingAndel>,
        personerIBehandling: Set<Person>,
        personerIForrigeBehandling: Set<Person>,
    ): Endringsresultat {
        val erEndringIBeløp = erEndringIBeløp(
            nåværendeAndeler = nåværendeAndeler,
            forrigeAndeler = forrigeAndeler,
            nåværendeEndretAndeler = nåværendeEndretAndeler,
            personerFremstiltKravFor = personerFremstiltKravFor,
        )

        val erEndringIKompetanse = erEndringIKompetanse(
            nåværendeKompetanser = nåværendeKompetanser,
            forrigeKompetanser = forrigeKompetanser,
        )

        val erEndringIVilkårsvurdering = erEndringIVilkårsvurdering(
            nåværendePersonResultat = nåværendePersonResultat,
            forrigePersonResultat = forrigePersonResultat,
            personerIBehandling = personerIBehandling,
            personerIForrigeBehandling = personerIForrigeBehandling,
        )

        val erEndringIEndretUtbetalingAndeler = erEndringIEndretUtbetalingAndeler(
            nåværendeEndretAndeler = nåværendeEndretAndeler,
            forrigeEndretAndeler = forrigeEndretAndeler,
        )

        val erMinstEnEndring = erEndringIBeløp || erEndringIKompetanse || erEndringIVilkårsvurdering || erEndringIEndretUtbetalingAndeler

        return if (erMinstEnEndring) Endringsresultat.ENDRING else Endringsresultat.INGEN_ENDRING
    }

    // NB: For personer fremstilt krav for tar vi ikke hensyn til alle endringer i beløp i denne funksjonen
    internal fun erEndringIBeløp(
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        nåværendeEndretAndeler: List<EndretUtbetalingAndel>,
        forrigeAndeler: List<AndelTilkjentYtelse>,
        personerFremstiltKravFor: List<Aktør>,
    ): Boolean {
        val allePersonerMedAndeler = (nåværendeAndeler.map { it.aktør } + forrigeAndeler.map { it.aktør }).distinct()
        val opphørstidspunkt = nåværendeAndeler.utledOpphørsdatoForNåværendeBehandlingMedFallback(
            forrigeAndeler = forrigeAndeler,
            nåværendeEndretAndeler = nåværendeEndretAndeler,
        ) ?: return false // Returnerer false hvis verken forrige eller nåværende behandling har andeler

        val erEndringIBeløpForMinstEnPerson = allePersonerMedAndeler.any { aktør ->
            val ytelseTyperForPerson = (nåværendeAndeler.map { it.type } + forrigeAndeler.map { it.type }).distinct()

            ytelseTyperForPerson.any { ytelseType ->
                erEndringIBeløpForPersonOgType(
                    nåværendeAndeler = nåværendeAndeler.filter { it.aktør == aktør && it.type == ytelseType },
                    forrigeAndeler = forrigeAndeler.filter { it.aktør == aktør && it.type == ytelseType },
                    opphørstidspunkt = opphørstidspunkt,
                    erFremstiltKravForPerson = personerFremstiltKravFor.contains(aktør),
                )
            }
        }

        return erEndringIBeløpForMinstEnPerson
    }

    // Kun interessert i endringer i beløp FØR opphørstidspunkt
    private fun erEndringIBeløpForPersonOgType(
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        forrigeAndeler: List<AndelTilkjentYtelse>,
        opphørstidspunkt: YearMonth,
        erFremstiltKravForPerson: Boolean,
    ): Boolean {
        val nåværendeTidslinje = AndelTilkjentYtelseTidslinje(nåværendeAndeler)
        val forrigeTidslinje = AndelTilkjentYtelseTidslinje(forrigeAndeler)

        val endringIBeløpTidslinje = nåværendeTidslinje.kombinerMed(forrigeTidslinje) { nåværende, forrige ->
            val nåværendeBeløp = nåværende?.kalkulertUtbetalingsbeløp ?: 0
            val forrigeBeløp = forrige?.kalkulertUtbetalingsbeløp ?: 0

            if (erFremstiltKravForPerson) {
                // Hvis det er søkt for person vil vi kun ha med endringer som går fra beløp > 0 til 0/null
                when {
                    forrigeBeløp > 0 && nåværendeBeløp == 0 -> true
                    else -> false
                }
            } else {
                // Hvis det ikke er søkt for person vil vi ha med alle endringer i beløp
                when {
                    forrigeBeløp != nåværendeBeløp -> true
                    else -> false
                }
            }
        }.fjernPerioderEtterOpphørsdato(opphørstidspunkt)

        return endringIBeløpTidslinje.perioder().any { it.innhold == true }
    }

    private fun Tidslinje<Boolean, Måned>.fjernPerioderEtterOpphørsdato(opphørstidspunkt: YearMonth) =
        this.beskjær(fraOgMed = TIDENES_MORGEN.tilMånedTidspunkt(), tilOgMed = opphørstidspunkt.forrigeMåned().tilTidspunkt())

    internal fun erEndringIKompetanse(
        nåværendeKompetanser: List<Kompetanse>,
        forrigeKompetanser: List<Kompetanse>,
    ): Boolean {
        val endringIKompetanseTidslinje = EndringIKompetanseUtil.lagEndringIKompetanseTidslinje(
            nåværendeKompetanser = nåværendeKompetanser,
            forrigeKompetanser = forrigeKompetanser,
        )

        return endringIKompetanseTidslinje.perioder().any { it.innhold == true }
    }

    internal fun erEndringIVilkårsvurdering(
        nåværendePersonResultat: Set<PersonResultat>,
        forrigePersonResultat: Set<PersonResultat>,
        personerIBehandling: Set<Person>,
        personerIForrigeBehandling: Set<Person>,
    ): Boolean {
        val endringIVilkårsvurderingTidslinje = EndringIVilkårsvurderingUtil.lagEndringIVilkårsvurderingTidslinje(
            nåværendePersonResultater = nåværendePersonResultat,
            forrigePersonResultater = forrigePersonResultat,
            personerIBehandling = personerIBehandling,
            personerIForrigeBehandling = personerIForrigeBehandling,
        )
        return endringIVilkårsvurderingTidslinje.perioder().any { it.innhold == true }
    }

    internal fun erEndringIEndretUtbetalingAndeler(
        nåværendeEndretAndeler: List<EndretUtbetalingAndel>,
        forrigeEndretAndeler: List<EndretUtbetalingAndel>,
    ): Boolean {
        val endringIEndretUtbetalingAndelTidslinje = EndringIEndretUtbetalingAndelUtil.lagEndringIEndretUtbetalingAndelTidslinje(
            nåværendeEndretAndeler = nåværendeEndretAndeler,
            forrigeEndretAndeler = forrigeEndretAndeler,
        )

        return endringIEndretUtbetalingAndelTidslinje.perioder().any { it.innhold == true }
    }
}
