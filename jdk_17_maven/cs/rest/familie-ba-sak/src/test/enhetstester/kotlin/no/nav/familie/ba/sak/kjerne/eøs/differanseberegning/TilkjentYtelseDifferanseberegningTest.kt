package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.eøs.assertEqualsUnordered
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.util.DeltBostedBuilder
import no.nav.familie.ba.sak.kjerne.eøs.util.TilkjentYtelseBuilder
import no.nav.familie.ba.sak.kjerne.eøs.util.UtenlandskPeriodebeløpBuilder
import no.nav.familie.ba.sak.kjerne.eøs.util.ValutakursBuilder
import no.nav.familie.ba.sak.kjerne.eøs.util.oppdaterTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilInneværendeMåned
import no.nav.familie.ba.sak.kjerne.tidslinje.util.VilkårsvurderingBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.byggTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOR_MED_SØKER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.GIFT_PARTNERSKAP
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.LOVLIG_OPPHOLD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.UNDER_18_ÅR
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Merk at operasjoner som tilsynelatende lager en ny instans av TilkjentYtelse, faktisk returner samme.
 * Det skyldes at JPA krever muterbare objekter.
 * Ikke-muterbarhet krever en omskrivning av koden. F.eks å koble vekk EndretUtbetalingPeriode fra AndelTilkjentYtelse
 */
class TilkjentYtelseDifferanseberegningTest {

    @Test
    fun `skal gjøre differanseberegning på en tilkjent ytelse med endringsperioder`() {
        val barnsFødselsdato = 13.jan(2020)
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barnsFødselsdato.tilLocalDate())
        val barn2 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barnsFødselsdato.tilLocalDate())

        val behandling = lagBehandling()
        val behandlingId = BehandlingId(behandling.id)
        val startMåned = barnsFødselsdato.tilInneværendeMåned()

        val vilkårsvurderingBygger = VilkårsvurderingBuilder<Måned>(behandling)
            .forPerson(søker, startMåned)
            .medVilkår("EEEEEEEEEEEEEEEEEEEEEEE", BOSATT_I_RIKET)
            .medVilkår("EEEEEEEEEEEEEEEEEEEEEEE", LOVLIG_OPPHOLD)
            .forPerson(barn1, startMåned)
            .medVilkår("+>", UNDER_18_ÅR, GIFT_PARTNERSKAP)
            .medVilkår("E>", BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER)
            .forPerson(barn2, startMåned)
            .medVilkår("+>", UNDER_18_ÅR, GIFT_PARTNERSKAP)
            .medVilkår("E>", BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER)
            .byggPerson()

        val tilkjentYtelse = vilkårsvurderingBygger.byggTilkjentYtelse()

        assertEquals(6, tilkjentYtelse.andelerTilkjentYtelse.size)

        DeltBostedBuilder(startMåned, tilkjentYtelse)
            .medDeltBosted(" //////000000000011111>", barn1, barn2)
            .oppdaterTilkjentYtelse()

        val forventetTilkjentYtelseMedDelt = TilkjentYtelseBuilder(startMåned, behandling)
            .forPersoner(barn1, barn2)
            .medOrdinær(" $$$$$$", prosent = 50) { it / 2 }
            .medOrdinær("       $$$$$$$$$$", prosent = 0) { 0 }
            .medOrdinær("                 $$$$$$", prosent = 100) { it }
            .bygg()

        assertEquals(8, tilkjentYtelse.andelerTilkjentYtelse.size)
        assertEqualsUnordered(
            forventetTilkjentYtelseMedDelt.andelerTilkjentYtelse,
            tilkjentYtelse.andelerTilkjentYtelse,
        )

        val utenlandskePeriodebeløp = UtenlandskPeriodebeløpBuilder(startMåned, behandlingId)
            .medBeløp(" 44555666>", "EUR", "fr", barn1, barn2)
            .bygg()

        val valutakurser = ValutakursBuilder(startMåned, behandlingId)
            .medKurs(" 888899999>", "EUR", barn1, barn2)
            .bygg()

        val forventetTilkjentYtelseMedDiff = TilkjentYtelseBuilder(startMåned, behandling)
            .forPersoner(barn1, barn2)
            .medOrdinær(" $$", 50, nasjonalt = { it / 2 }, differanse = { it / 2 - 32 }) { it / 2 - 32 }
            .medOrdinær("   $$", 50, nasjonalt = { it / 2 }, differanse = { it / 2 - 40 }) { it / 2 - 40 }
            .medOrdinær("     $", 50, nasjonalt = { it / 2 }, differanse = { it / 2 - 45 }) { it / 2 - 45 }
            .medOrdinær("      $", 50, nasjonalt = { it / 2 }, differanse = { it / 2 - 54 }) { it / 2 - 54 }
            .medOrdinær("       $$$$$$$$$$", 0, nasjonalt = { 0 }, differanse = { -54 }) { 0 }
            .medOrdinær("                 $$$$$$", 100, nasjonalt = { it }, differanse = { it - 54 }) { it - 54 }
            .bygg()

        val andelerMedDifferanse =
            beregnDifferanse(tilkjentYtelse.andelerTilkjentYtelse, utenlandskePeriodebeløp, valutakurser)

        assertEquals(14, andelerMedDifferanse.size)
        assertEqualsUnordered(
            forventetTilkjentYtelseMedDiff.andelerTilkjentYtelse,
            andelerMedDifferanse,
        )
    }

    @Test
    fun `skal fjerne differanseberegning når utenlandsk periodebeløp eller valutakurs nullstilles`() {
        val barnsFødselsdato = 13.jan(2020)
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barnsFødselsdato.tilLocalDate())

        val behandling = lagBehandling()
        val behandlingId = BehandlingId(behandling.id)
        val startMåned = barnsFødselsdato.tilInneværendeMåned()

        val vilkårsvurderingBygger = VilkårsvurderingBuilder<Måned>(behandling)
            .forPerson(søker, startMåned)
            .medVilkår("EEEEEEEEEEEEEEEEEEEEEEE", BOSATT_I_RIKET)
            .medVilkår("EEEEEEEEEEEEEEEEEEEEEEE", LOVLIG_OPPHOLD)
            .forPerson(barn1, startMåned)
            .medVilkår("+>", UNDER_18_ÅR, GIFT_PARTNERSKAP)
            .medVilkår("E>", BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER)
            .byggPerson()

        val tilkjentYtelse = vilkårsvurderingBygger.byggTilkjentYtelse()

        val forventetTilkjentYtelseKunSats = TilkjentYtelseBuilder(startMåned, behandling)
            .forPersoner(barn1)
            .medOrdinær(" $$$$$$$$$$$$$$$$$$$$$$", nasjonalt = { null }, differanse = { null })
            .bygg()

        assertEquals(3, tilkjentYtelse.andelerTilkjentYtelse.size)
        assertEqualsUnordered(
            forventetTilkjentYtelseKunSats.andelerTilkjentYtelse,
            tilkjentYtelse.andelerTilkjentYtelse,
        )

        val utenlandskePeriodebeløp = UtenlandskPeriodebeløpBuilder(startMåned, behandlingId)
            .medBeløp(" 44555666>", "EUR", "fr", barn1)
            .bygg()

        val valutakurser = ValutakursBuilder(startMåned, behandlingId)
            .medKurs(" 888899999>", "EUR", barn1)
            .bygg()

        val forventetTilkjentYtelseMedDiff = TilkjentYtelseBuilder(startMåned, behandling)
            .forPersoner(barn1)
            .medOrdinær(" $$                    ", nasjonalt = { it }, differanse = { it - 32 }) { it - 32 }
            .medOrdinær("   $$                  ", nasjonalt = { it }, differanse = { it - 40 }) { it - 40 }
            .medOrdinær("     $                 ", nasjonalt = { it }, differanse = { it - 45 }) { it - 45 }
            .medOrdinær("      $$$$$$$$$$$$$$$$$", nasjonalt = { it }, differanse = { it - 54 }) { it - 54 }
            .bygg()

        val andelerMedDiff =
            beregnDifferanse(tilkjentYtelse.andelerTilkjentYtelse, utenlandskePeriodebeløp, valutakurser)

        assertEquals(6, andelerMedDiff.size)
        assertEqualsUnordered(
            forventetTilkjentYtelseMedDiff.andelerTilkjentYtelse,
            andelerMedDiff,
        )

        val blanktUtenlandskPeridebeløp = UtenlandskPeriodebeløpBuilder(startMåned, behandlingId)
            .medBeløp(" >", null, null, barn1)
            .bygg()

        val andelerUtenDiff =
            beregnDifferanse(tilkjentYtelse.andelerTilkjentYtelse, blanktUtenlandskPeridebeløp, valutakurser)

        assertEquals(3, andelerUtenDiff.size)
        assertEqualsUnordered(
            forventetTilkjentYtelseKunSats.andelerTilkjentYtelse,
            andelerUtenDiff,
        )

        val andelerMedDiffIgjen =
            beregnDifferanse(tilkjentYtelse.andelerTilkjentYtelse, utenlandskePeriodebeløp, valutakurser)

        assertEquals(6, andelerMedDiffIgjen.size)
        assertEqualsUnordered(
            forventetTilkjentYtelseMedDiff.andelerTilkjentYtelse,
            andelerMedDiffIgjen,
        )
    }
}
