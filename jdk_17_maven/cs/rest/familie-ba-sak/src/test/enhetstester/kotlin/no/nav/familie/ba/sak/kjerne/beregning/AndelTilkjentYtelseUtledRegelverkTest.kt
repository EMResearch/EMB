package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class AndelTilkjentYtelseUtledRegelverkTest {

    val behandling = lagBehandling()
    val barnPerson = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(1))

    val andelTilkjentYtelse = lagAndelTilkjentYtelse(
        behandling = behandling,
        person = barnPerson,
        fom = YearMonth.now().minusMonths(4),
        tom = YearMonth.now().plusMonths(1),
    )

    val vilkårsvurdering = Vilkårsvurdering(
        behandling = behandling,
    )
    val personResultat = PersonResultat(
        vilkårsvurdering = vilkårsvurdering,
        aktør = barnPerson.aktør,
    )

    @Test
    fun `EØS-forordning om alle relevante vilkår er satt til regelverk EØS forordning`() {
        val regelverk = andelTilkjentYtelse.vurdertEtter(setOf(genererPersonresultat()))

        assertEquals(Regelverk.EØS_FORORDNINGEN, regelverk)
    }

    @Test
    fun `Nasjonale regler om alle relevante vilkår er satt til regelverk nasonale regler`() {
        val personResultat =
            genererPersonresultat(Regelverk.NASJONALE_REGLER, Regelverk.NASJONALE_REGLER, Regelverk.NASJONALE_REGLER)

        val regelverk = andelTilkjentYtelse.vurdertEtter(setOf(personResultat))

        assertEquals(Regelverk.NASJONALE_REGLER, regelverk)
    }

    @Test
    fun `Default til nasjonale regler om relevante vilkår er satt til forskjellig regelverk`() {
        val personResultat =
            genererPersonresultat(Regelverk.EØS_FORORDNINGEN, Regelverk.NASJONALE_REGLER, Regelverk.NASJONALE_REGLER)

        val regelverk = andelTilkjentYtelse.vurdertEtter(setOf(personResultat))

        assertEquals(Regelverk.NASJONALE_REGLER, regelverk)
    }

    private fun genererPersonresultat(
        regelVerkBosattIRiket: Regelverk = Regelverk.EØS_FORORDNINGEN,
        regelVerkLovligOpphold: Regelverk = Regelverk.EØS_FORORDNINGEN,
        regelVerkBorMedSøker: Regelverk = Regelverk.EØS_FORORDNINGEN,
    ): PersonResultat {
        val vilkårsvurdering = Vilkårsvurdering(
            behandling = behandling,
        )

        val barnPersonResultat = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = barnPerson.aktør,
        )

        val vilkårResultat = listOf(
            lagVilkårResultat(Vilkår.BOSATT_I_RIKET, regelVerkBosattIRiket, YearMonth.now().minusYears(1), null),
            lagVilkårResultat(Vilkår.LOVLIG_OPPHOLD, regelVerkLovligOpphold, YearMonth.now().minusYears(1), null),
            lagVilkårResultat(Vilkår.BOR_MED_SØKER, regelVerkBorMedSøker, YearMonth.now().minusYears(1), null),
            lagVilkårResultat(
                Vilkår.UNDER_18_ÅR,
                Regelverk.NASJONALE_REGLER,
                YearMonth.now().minusYears(1),
                YearMonth.now().plusYears(17),
            ),
        )
        barnPersonResultat.vilkårResultater.addAll(vilkårResultat)
        return barnPersonResultat
    }
}
