package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.UtvidetBarnetrygdUtil.beregnTilkjentYtelseUtvidet
import no.nav.familie.ba.sak.kjerne.beregning.UtvidetBarnetrygdUtil.validerUtvidetVilkårsresultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth

class UtvidetBarnetrygdUtilTest {

    @Test
    fun `Valider utvidet vilkår - skal kaste feil hvis fom og tom er i samme kalendermåned uten etterfølgende periode`() {
        val vilkårResultat = lagVilkårResultat(
            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
            periodeFom = LocalDate.of(2022, 7, 1),
            periodeTom = LocalDate.of(2022, 7, 31),
            resultat = Resultat.OPPFYLT,
        )

        assertThrows<FunksjonellFeil> {
            validerUtvidetVilkårsresultat(
                vilkårResultat = vilkårResultat,
                utvidetVilkårResultater = listOf(vilkårResultat),
            )
        }
    }

    @Test
    fun `Valider utvidet vilkår - skal ikke kaste feil hvis fom og tom er i samme kalendermåned og har etterfølgende periode`() {
        val vilkårResultat = lagVilkårResultat(
            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
            periodeFom = LocalDate.of(2022, 7, 1),
            periodeTom = LocalDate.of(2022, 7, 31),
            resultat = Resultat.OPPFYLT,
        )

        val etterfølgendeVilkårResultat = lagVilkårResultat(
            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
            periodeFom = LocalDate.of(2022, 8, 1),
            periodeTom = LocalDate.of(2022, 12, 10),
            resultat = Resultat.OPPFYLT,
        )

        assertDoesNotThrow {
            validerUtvidetVilkårsresultat(
                vilkårResultat = vilkårResultat,
                utvidetVilkårResultater = listOf(
                    vilkårResultat,
                    etterfølgendeVilkårResultat,
                ),
            )
        }
    }

    @Test
    fun `Beregn utvidet - skal ikke gi rett til utvidet for perioder der barnet ikke bor med søker`() {
        val testBeregnUtvidet = TestBeregnTilkjentYtelseUtvidet()

        val feilmeldinger = assertThrows<FunksjonellFeil> {
            testBeregnUtvidet.med(UtdypendeVilkårsvurdering.BARN_BOR_I_EØS_MED_ANNEN_FORELDER)
        }.melding to assertThrows<FunksjonellFeil> {
            testBeregnUtvidet.med(UtdypendeVilkårsvurdering.BARN_BOR_I_STORBRITANNIA_MED_ANNEN_FORELDER)
        }.melding

        val forventetFeilmelding = "Du har lagt til utvidet barnetrygd for en periode der det ikke er rett til barnetrygd"

        assertTrue(forventetFeilmelding in feilmeldinger.first && forventetFeilmelding in feilmeldinger.second)

        assertDoesNotThrow {
            testBeregnUtvidet.med(UtdypendeVilkårsvurdering.BARN_BOR_I_EØS_MED_SØKER)
        }
    }

    private class TestBeregnTilkjentYtelseUtvidet {
        val tilkjentYtelse = lagInitiellTilkjentYtelse()

        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn = tilfeldigPerson()

        val personResultatSøker = lagPersonResultat(
            vilkårsvurdering = Vilkårsvurdering(behandling = tilkjentYtelse.behandling),
            person = søker,
            resultat = Resultat.OPPFYLT,
            periodeFom = LocalDate.of(2021, 10, 1),
            periodeTom = LocalDate.of(2022, 2, 28),
        )

        val personResultatBarn = lagPersonResultat(
            vilkårsvurdering = Vilkårsvurdering(behandling = tilkjentYtelse.behandling),
            person = barn,
            resultat = Resultat.OPPFYLT,
            periodeFom = LocalDate.of(2021, 10, 1),
            periodeTom = LocalDate.of(2022, 2, 28),
            vilkårType = Vilkår.BOR_MED_SØKER,
        )

        val utvidetVilkår = lagVilkårResultat(
            vilkårType = Vilkår.UTVIDET_BARNETRYGD,
            periodeFom = LocalDate.of(2021, 10, 1),
            periodeTom = LocalDate.of(2022, 2, 28),
            resultat = Resultat.OPPFYLT,
            personResultat = personResultatSøker,
        )

        fun med(utdypendeVilkårsvurdering: UtdypendeVilkårsvurdering) = utdypendeVilkårsvurdering.let {
            personResultatBarn.vilkårResultater.first().utdypendeVilkårsvurderinger = listOf(utdypendeVilkårsvurdering)

            beregnTilkjentYtelseUtvidet(
                utvidetVilkår = listOf(utvidetVilkår),
                tilkjentYtelse = tilkjentYtelse,
                andelerTilkjentYtelseBarnaMedEtterbetaling3ÅrEndringer = listOf(
                    lagAndelTilkjentYtelseMedEndreteUtbetalinger(
                        person = barn,
                        fom = YearMonth.of(2021, 10),
                        tom = YearMonth.of(2022, 2),
                    ),
                ),
                endretUtbetalingAndelerSøker = emptyList(),
                personResultater = setOf(personResultatBarn),
            )
        }
    }
}
