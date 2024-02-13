package no.nav.familie.ba.sak.kjerne.behandlingsresultat
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatSøknadUtils.kombinerSøknadsresultater
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatSøknadUtils.utledSøknadResultatFraAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import org.hamcrest.CoreMatchers.`is` as Is

internal class BehandlingsresultatSøknadUtilsTest {

    val søker = tilfeldigPerson()

    val des21 = LocalDate.of(2021, 12, 1)
    val jan22 = YearMonth.of(2022, 1)
    val aug22 = YearMonth.of(2022, 8)

    @Test
    fun `utledSøknadResultatFraAndelerTilkjentYtelse skal bare utlede resultater for personer det er framstilt krav for`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør

        val forrigeAndel =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør,
            )

        val søknadsResultat = utledSøknadResultatFraAndelerTilkjentYtelse(
            forrigeAndeler = listOf(forrigeAndel),
            nåværendeAndeler = listOf(forrigeAndel.copy()),
            personerFremstiltKravFor = emptyList(),
            endretUtbetalingAndeler = emptyList(),
        )

        assertThat(søknadsResultat, Is(emptyList()))
    }

    @Test
    fun `utledSøknadResultatFraAndelerTilkjentYtelse skal returnere ingen relevante endringer dersom beløpene for periodene er lik forrige behandling`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør

        val forrigeAndel =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør,
            )

        val søknadsResultat = utledSøknadResultatFraAndelerTilkjentYtelse(
            forrigeAndeler = listOf(forrigeAndel),
            nåværendeAndeler = listOf(forrigeAndel.copy()),
            personerFremstiltKravFor = listOf(barn1Aktør),
            endretUtbetalingAndeler = emptyList(),
        )

        assertThat(søknadsResultat.size, Is(1))
        assertThat(søknadsResultat[0], Is(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER))
    }

    @Test
    fun `utledSøknadResultatFraAndelerTilkjentYtelse skal returnere innvilget dersom det finnes beløp for perioder som er annerledes enn sist og større enn 0`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør

        val forrigeAndel =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 0,
                aktør = barn1Aktør,
            )

        val søknadsResultat = utledSøknadResultatFraAndelerTilkjentYtelse(
            forrigeAndeler = listOf(forrigeAndel),
            nåværendeAndeler = listOf(
                forrigeAndel.copy(kalkulertUtbetalingsbeløp = 1054),
            ),
            personerFremstiltKravFor = listOf(barn1Aktør),
            endretUtbetalingAndeler = emptyList(),
        )

        assertThat(søknadsResultat.size, Is(1))
        assertThat(søknadsResultat[0], Is(Søknadsresultat.INNVILGET))
    }

    @Test
    fun `utledSøknadResultatFraAndelerTilkjentYtelse skal returnere ingen relevante endringer dersom beløp på nåværende andel er 0 og det ikke finnes noen endringsperioder eller differanse beregning`() {
        val barn1Aktør = lagPerson(type = PersonType.BARN).aktør

        val forrigeAndel =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør,
            )

        val søknadsResultat = utledSøknadResultatFraAndelerTilkjentYtelse(
            forrigeAndeler = listOf(forrigeAndel),
            nåværendeAndeler = listOf(
                forrigeAndel.copy(kalkulertUtbetalingsbeløp = 0),
            ),
            personerFremstiltKravFor = listOf(barn1Aktør),
            endretUtbetalingAndeler = emptyList(),
        )

        assertThat(søknadsResultat.size, Is(1))
        assertThat(søknadsResultat[0], Is(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER))
    }

    @Test
    fun `utledSøknadResultatFraAndelerTilkjentYtelse skal returnere INNVILGET dersom beløp på nåværende andel er 0 og det finnes endringsperiode som DELT_BOSTED`() {
        val barn1Person = lagPerson(type = PersonType.BARN)
        val barn1Aktør = barn1Person.aktør

        val andel =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 0,
                prosent = BigDecimal.ZERO,
                aktør = barn1Aktør,
            )

        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            person = barn1Person,
            fom = jan22,
            tom = aug22,
            prosent = BigDecimal(100),
            behandlingId = 123L,
            årsak = Årsak.DELT_BOSTED,
        )

        val søknadsResultat = utledSøknadResultatFraAndelerTilkjentYtelse(
            forrigeAndeler = emptyList(),
            nåværendeAndeler = listOf(
                andel.copy(kalkulertUtbetalingsbeløp = 0),
            ),
            personerFremstiltKravFor = listOf(barn1Aktør),
            endretUtbetalingAndeler = listOf(endretUtbetalingAndel),
        )

        assertThat(søknadsResultat.size, Is(1))
        assertThat(søknadsResultat[0], Is(Søknadsresultat.INNVILGET))
    }

    @ParameterizedTest
    @EnumSource(value = Årsak::class, mode = EnumSource.Mode.EXCLUDE, names = ["DELT_BOSTED"])
    fun `utledSøknadResultatFraAndelerTilkjentYtelse skal returnere AVSLÅTT dersom beløp på nåværende andel er 0 og det finnes endringsperiode som ikke er DELT_BOSTED`(
        årsak: Årsak,
    ) {
        val barn1Person = lagPerson(type = PersonType.BARN)
        val barn1Aktør = barn1Person.aktør

        val andel =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 0,
                prosent = BigDecimal.ZERO,
                aktør = barn1Aktør,
            )

        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            person = barn1Person,
            fom = jan22,
            tom = aug22,
            prosent = BigDecimal(100),
            behandlingId = 123L,
            årsak = årsak,
        )

        val søknadsResultat = utledSøknadResultatFraAndelerTilkjentYtelse(
            forrigeAndeler = emptyList(),
            nåværendeAndeler = listOf(
                andel,
            ),
            personerFremstiltKravFor = listOf(barn1Aktør),
            endretUtbetalingAndeler = listOf(endretUtbetalingAndel),
        )

        assertThat(søknadsResultat.size, Is(1))
        assertThat(søknadsResultat[0], Is(Søknadsresultat.AVSLÅTT))
    }

    @ParameterizedTest
    @EnumSource(value = Årsak::class)
    fun `utledSøknadResultatFraAndelerTilkjentYtelse skal returnere INGEN_RELEVANTE_ENDRINGER dersom beløp på nåværende andel er 0 og andelen eksisterte forrige gang (beløp større eller lik 0)`(
        årsak: Årsak,
    ) {
        val barn1Person = lagPerson(type = PersonType.BARN)
        val barn1Aktør = barn1Person.aktør

        val forrigeAndel =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør,
            )

        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            person = barn1Person,
            fom = jan22,
            tom = aug22,
            prosent = BigDecimal(100),
            behandlingId = 123L,
            årsak = årsak,
        )

        val søknadsResultat = utledSøknadResultatFraAndelerTilkjentYtelse(
            forrigeAndeler = listOf(forrigeAndel),
            nåværendeAndeler = listOf(
                forrigeAndel.copy(kalkulertUtbetalingsbeløp = 0),
            ),
            personerFremstiltKravFor = listOf(barn1Aktør),
            endretUtbetalingAndeler = listOf(endretUtbetalingAndel),
        )

        assertThat(søknadsResultat.size, Is(1))
        assertThat(søknadsResultat[0], Is(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER))
    }

    @Test
    fun `utledSøknadResultatFraAndelerTilkjentYtelse skal returnere INNVILGET dersom beløpet på nåværende andel er 0 men er differanseberegnet`() {
        val barn1Person = lagPerson(type = PersonType.BARN)
        val barn1Aktør = barn1Person.aktør

        val andel =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 0,
                prosent = BigDecimal.ZERO,
                differanseberegnetPeriodebeløp = 0,
                aktør = barn1Aktør,
            )

        val søknadsResultat = utledSøknadResultatFraAndelerTilkjentYtelse(
            forrigeAndeler = emptyList(),
            nåværendeAndeler = listOf(
                andel.copy(
                    kalkulertUtbetalingsbeløp = 0,
                    differanseberegnetPeriodebeløp = 0,
                ),
            ),
            personerFremstiltKravFor = listOf(barn1Aktør),
            endretUtbetalingAndeler = emptyList(),
        )

        assertThat(søknadsResultat.size, Is(1))
        assertThat(søknadsResultat[0], Is(Søknadsresultat.INNVILGET))
    }

    @Test
    fun `utledSøknadResultatFraAndelerTilkjentYtelse skal returnere INNVILGET OG AVSLÅTT dersom 1 barn får innvilget og 1 barn får avslått`() {
        val barn1Person = lagPerson(type = PersonType.BARN)
        val barn1Aktør = barn1Person.aktør
        val barn2Aktør = lagPerson(type = PersonType.BARN).aktør

        val forrigeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn2Aktør,
            ),
        )
        val nåværendeAndeler = listOf(
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 0,
                aktør = barn1Aktør,
            ),
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1060,
                aktør = barn2Aktør,
            ),
        )

        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            person = barn1Person,
            fom = jan22,
            tom = aug22,
            prosent = BigDecimal(100),
            behandlingId = 123L,
            årsak = Årsak.ALLEREDE_UTBETALT,
        )

        val søknadsResultat = utledSøknadResultatFraAndelerTilkjentYtelse(
            forrigeAndeler = forrigeAndeler,
            nåværendeAndeler = nåværendeAndeler,
            personerFremstiltKravFor = listOf(barn1Aktør, barn2Aktør),
            endretUtbetalingAndeler = listOf(endretUtbetalingAndel),
        )

        assertThat(søknadsResultat.size, Is(2))
        assertThat(
            søknadsResultat,
            containsInAnyOrder(
                Søknadsresultat.AVSLÅTT,
                Søknadsresultat.INNVILGET,
            ),
        )
    }

    @Test
    fun `utledSøknadResultatFraAndelerTilkjentYtelse skal returnere INNVILGET dersom småbarnstillegg blir lagt til`() {
        val barn1Person = lagPerson(type = PersonType.BARN)
        val barn1Aktør = barn1Person.aktør
        val søker = lagPerson(type = PersonType.SØKER)

        val forrigeAndelBarn =
            lagAndelTilkjentYtelse(
                fom = jan22,
                tom = aug22,
                beløp = 1054,
                aktør = barn1Aktør,
            )

        val forrigeAndelUtvidet = lagAndelTilkjentYtelse(
            fom = jan22,
            tom = aug22,
            beløp = 1054,
            aktør = søker.aktør,
            ytelseType = YtelseType.UTVIDET_BARNETRYGD,
        )

        val søknadsResultat = utledSøknadResultatFraAndelerTilkjentYtelse(
            forrigeAndeler = listOf(forrigeAndelBarn, forrigeAndelUtvidet),
            nåværendeAndeler = listOf(
                forrigeAndelBarn,
                forrigeAndelUtvidet,
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 630,
                    aktør = søker.aktør,
                    ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ),
            ),
            personerFremstiltKravFor = listOf(søker.aktør),
            endretUtbetalingAndeler = emptyList(),
        ).filter { it != Søknadsresultat.INGEN_RELEVANTE_ENDRINGER }

        assertThat(søknadsResultat.size, Is(1))
        assertThat(søknadsResultat[0], Is(Søknadsresultat.INNVILGET))
    }

    @Test
    fun `kombinerSøknadsresultater skal kaste feil dersom lista ikke inneholder noe som helst`() {
        val listeMedIngenSøknadsresultat = listOf<Søknadsresultat>()

        val feil = assertThrows<FunksjonellFeil> { listeMedIngenSøknadsresultat.kombinerSøknadsresultater(behandlingÅrsak = BehandlingÅrsak.SØKNAD) }

        assertThat(feil.message, Is("Klarer ikke utlede søknadsresultat. Finner ingen resultater."))
    }

    @ParameterizedTest
    @EnumSource(value = Søknadsresultat::class)
    internal fun `kombinerSøknadsresultater skal alltid returnere innholdet som det er hvis det bare 1 resultat i lista`(
        søknadsresultat: Søknadsresultat,
    ) {
        val listeMedSøknadsresultat = listOf(søknadsresultat)

        val kombinertResultat = listeMedSøknadsresultat.kombinerSøknadsresultater(behandlingÅrsak = BehandlingÅrsak.SØKNAD)

        assertThat(kombinertResultat, Is(søknadsresultat))
    }

    @ParameterizedTest
    @EnumSource(value = Søknadsresultat::class, names = ["INNVILGET", "AVSLÅTT"])
    internal fun `kombinerSøknadsresultater skal ignorere INGEN_RELEVANTE_ENDRINGER dersom den er paret opp med INNVILGET eller AVSLÅTT`(
        søknadsresultat: Søknadsresultat,
    ) {
        val listeMedSøknadsresultat =
            listOf(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER, søknadsresultat)

        val kombinertResultat = listeMedSøknadsresultat.kombinerSøknadsresultater(behandlingÅrsak = BehandlingÅrsak.SØKNAD)

        assertThat(kombinertResultat, Is(søknadsresultat))
    }

    @Test
    fun `kombinerSøknadsresultater skal returnere DELVIS_INNVILGET dersom lista består av INNVILGET, AVSLÅTT OG INGEN_RELEVANTE_ENDRINGER`() {
        val listeMedSøknadsresultat = listOf(
            Søknadsresultat.INNVILGET,
            Søknadsresultat.AVSLÅTT,
            Søknadsresultat.INGEN_RELEVANTE_ENDRINGER,
        )

        val kombinertResultat = listeMedSøknadsresultat.kombinerSøknadsresultater(behandlingÅrsak = BehandlingÅrsak.SØKNAD)

        assertThat(kombinertResultat, Is(Søknadsresultat.DELVIS_INNVILGET))
    }

    @Test
    fun `utledResultatPåSøknad - skal kaste feil dersom man har endt opp med ingen resultater`() {
        assertThrows<FunksjonellFeil> {
            BehandlingsresultatSøknadUtils.utledResultatPåSøknad(
                forrigeAndeler = emptyList(),
                nåværendeAndeler = emptyList(),
                nåværendePersonResultater = emptySet(),
                personerFremstiltKravFor = emptyList(),
                endretUtbetalingAndeler = emptyList(),
                behandlingÅrsak = BehandlingÅrsak.SØKNAD,
                finnesUregistrerteBarn = false,
            )
        }
    }

    @Test
    fun `utledResultatPåSøknad - skal returnere AVSLÅTT dersom det er søkt for barn som ikke er registrert`() {
        val resultatPåSøknad = BehandlingsresultatSøknadUtils.utledResultatPåSøknad(
            forrigeAndeler = emptyList(),
            nåværendeAndeler = emptyList(),
            nåværendePersonResultater = emptySet(),
            personerFremstiltKravFor = emptyList(),
            endretUtbetalingAndeler = emptyList(),
            behandlingÅrsak = BehandlingÅrsak.SØKNAD,
            finnesUregistrerteBarn = true,
        )

        assertThat(resultatPåSøknad, Is(Søknadsresultat.AVSLÅTT))
    }

    @Test
    fun `utledResultatPåSøknad - skal returnere AVSLÅTT dersom det er eksplisitt avslag på søker (uten at det er søkt om utvidet)`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)

        val søker = lagPerson(type = PersonType.SØKER)

        val søkersPersonResultat = lagPersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            person = søker,
            resultat = Resultat.IKKE_OPPFYLT,
            periodeFom = des21,
            periodeTom = LocalDate.now(),
            personType = PersonType.SØKER,
            erEksplisittAvslagPåSøknad = true,
            lagFullstendigVilkårResultat = true,

        )
        val resultatPåSøknad = BehandlingsresultatSøknadUtils.utledResultatPåSøknad(
            forrigeAndeler = emptyList(),
            nåværendeAndeler = emptyList(),
            nåværendePersonResultater = setOf(søkersPersonResultat),
            personerFremstiltKravFor = emptyList(),
            endretUtbetalingAndeler = emptyList(),
            behandlingÅrsak = BehandlingÅrsak.SØKNAD,
            finnesUregistrerteBarn = false,
        )

        assertThat(resultatPåSøknad, Is(Søknadsresultat.AVSLÅTT))
    }

    @ParameterizedTest
    @EnumSource(value = Resultat::class, names = ["IKKE_OPPFYLT", "IKKE_VURDERT"])
    fun `utledResultatPåSøknad - skal returnere AVSLÅTT dersom behandlingen er en fødselshendelse og det finnes vilkårsvurdering som ikke er oppfylt eller vurdert`(resultat: Resultat) {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE)
        val vikårsvurdering = Vilkårsvurdering(behandling = behandling)

        val barnPersonResultat = lagPersonResultat(
            vilkårsvurdering = vikårsvurdering,
            person = lagPerson(type = PersonType.BARN, fødselsdato = des21),
            resultat = resultat,
            periodeFom = des21,
            periodeTom = LocalDate.now(),
            lagFullstendigVilkårResultat = true,
            personType = PersonType.BARN,
        )

        val resultatPåSøknad = BehandlingsresultatSøknadUtils.utledResultatPåSøknad(
            forrigeAndeler = emptyList(),
            nåværendeAndeler = emptyList(),
            nåværendePersonResultater = setOf(barnPersonResultat),
            personerFremstiltKravFor = emptyList(),
            endretUtbetalingAndeler = emptyList(),
            behandlingÅrsak = BehandlingÅrsak.FØDSELSHENDELSE,
            finnesUregistrerteBarn = false,
        )

        assertThat(resultatPåSøknad, Is(Søknadsresultat.AVSLÅTT))
    }

    @Test
    fun `utledResultatPåSøknad - skal returnere AVSLÅTT dersom er eksplisitt avslag på minst en person det er framstilt krav for`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)
        val vikårsvurdering = Vilkårsvurdering(behandling = behandling)

        val barn = lagPerson(type = PersonType.BARN, fødselsdato = des21)

        val barnPersonResultat = lagPersonResultat(
            vilkårsvurdering = vikårsvurdering,
            person = barn,
            resultat = Resultat.IKKE_OPPFYLT,
            periodeFom = des21,
            periodeTom = LocalDate.now(),
            personType = PersonType.BARN,
            erEksplisittAvslagPåSøknad = true,
            lagFullstendigVilkårResultat = true,

        )

        val resultatPåSøknad = BehandlingsresultatSøknadUtils.utledResultatPåSøknad(
            forrigeAndeler = emptyList(),
            nåværendeAndeler = emptyList(),
            nåværendePersonResultater = setOf(barnPersonResultat),
            personerFremstiltKravFor = listOf(barn.aktør),
            endretUtbetalingAndeler = emptyList(),
            behandlingÅrsak = BehandlingÅrsak.SØKNAD,
            finnesUregistrerteBarn = false,
        )

        assertThat(resultatPåSøknad, Is(Søknadsresultat.AVSLÅTT))
    }

    @Test
    fun `utledResultatPåSøknad - skal returnere INNVILGET dersom barnet det er søkt for har fått andeler med positive beløp som er annerledes enn forrige gang`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE)
        val vikårsvurdering = Vilkårsvurdering(behandling = behandling)

        val barn1Person = lagPerson(type = PersonType.BARN, fødselsdato = des21)

        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn1Person.aktør,
                ),
            )

        val barnPersonResultat = lagPersonResultat(
            vilkårsvurdering = vikårsvurdering,
            person = barn1Person,
            resultat = Resultat.OPPFYLT,
            periodeFom = des21,
            periodeTom = LocalDate.now(),
            personType = PersonType.BARN,
            lagFullstendigVilkårResultat = true,
        )

        val resultatPåSøknad = BehandlingsresultatSøknadUtils.utledResultatPåSøknad(
            forrigeAndeler = emptyList(),
            nåværendeAndeler = nåværendeAndeler,
            nåværendePersonResultater = setOf(barnPersonResultat),
            personerFremstiltKravFor = listOf(barn1Person.aktør),
            endretUtbetalingAndeler = emptyList(),
            behandlingÅrsak = BehandlingÅrsak.SØKNAD,
            finnesUregistrerteBarn = false,
        )

        assertThat(resultatPåSøknad, Is(Søknadsresultat.INNVILGET))
    }

    @Test
    fun `utledResultatPåSøknad - skal returnere DELVIS_INNVILGET dersom det finnes et barn som har fått innvilget men også et barn som ikke er registrert`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE)
        val vikårsvurdering = Vilkårsvurdering(behandling = behandling)

        val barn1Person = lagPerson(type = PersonType.BARN)

        val nåværendeAndeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn1Person.aktør,
                ),
            )

        val barnPersonResultat = lagPersonResultat(
            vilkårsvurdering = vikårsvurdering,
            person = barn1Person,
            resultat = Resultat.OPPFYLT,
            periodeFom = des21,
            periodeTom = LocalDate.now(),
            personType = PersonType.BARN,
            lagFullstendigVilkårResultat = true,
        )

        val resultatPåSøknad = BehandlingsresultatSøknadUtils.utledResultatPåSøknad(
            forrigeAndeler = emptyList(),
            nåværendeAndeler = nåværendeAndeler,
            nåværendePersonResultater = setOf(barnPersonResultat),
            personerFremstiltKravFor = listOf(barn1Person.aktør),
            endretUtbetalingAndeler = emptyList(),
            behandlingÅrsak = BehandlingÅrsak.SØKNAD,
            finnesUregistrerteBarn = true,
        )

        assertThat(resultatPåSøknad, Is(Søknadsresultat.DELVIS_INNVILGET))
    }

    @Test
    fun `utledResultatPåSøknad - skal returnere INGEN_RELEVANTE_ENDRINGER dersom barnet det er søkt for har fått helt lik andel som forrige behandling`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE)
        val vikårsvurdering = Vilkårsvurdering(behandling = behandling)

        val barn1Person = lagPerson(type = PersonType.BARN, fødselsdato = des21)

        val andeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = jan22,
                    tom = aug22,
                    beløp = 1054,
                    aktør = barn1Person.aktør,
                ),
            )

        val barnPersonResultat = lagPersonResultat(
            vilkårsvurdering = vikårsvurdering,
            person = barn1Person,
            resultat = Resultat.OPPFYLT,
            periodeFom = des21,
            periodeTom = LocalDate.now(),
            personType = PersonType.BARN,
            lagFullstendigVilkårResultat = true,
        )

        val resultatPåSøknad = BehandlingsresultatSøknadUtils.utledResultatPåSøknad(
            forrigeAndeler = andeler,
            nåværendeAndeler = andeler,
            nåværendePersonResultater = setOf(barnPersonResultat),
            personerFremstiltKravFor = listOf(barn1Person.aktør),
            endretUtbetalingAndeler = emptyList(),
            behandlingÅrsak = BehandlingÅrsak.SØKNAD,
            finnesUregistrerteBarn = false,
        )

        assertThat(resultatPåSøknad, Is(Søknadsresultat.INGEN_RELEVANTE_ENDRINGER))
    }
}
