package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.rest

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.tilPersonEnkelSøkerOgBarn
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjer
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilInneværendeMåned
import no.nav.familie.ba.sak.kjerne.tidslinje.util.VilkårsvurderingBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.des
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.nov
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RestTidslinjerTest {

    @Test
    fun `når barnet har løpende vilkår, skal likevel rest-tidslinjene for regelverk og oppfylt vilkår være avsluttet ved 18 år`() {
        val barnsFødselsdato = 13.jan(2020)
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barnsFødselsdato.tilLocalDate())

        val behandling = lagBehandling()
        val startMåned = barnsFødselsdato.tilInneværendeMåned()

        val vilkårsvurderingBygger = VilkårsvurderingBuilder<Måned>(behandling)
            .forPerson(søker, startMåned)
            .medVilkår("EEEEEEEEEEEEEEEEEEEEEEEEE", Vilkår.BOSATT_I_RIKET)
            .medVilkår("EEEEEEEEEEEEEEEEEEEEEEEEE", Vilkår.LOVLIG_OPPHOLD)
            .forPerson(barn1, startMåned)
            .medVilkår("+++++++++>", Vilkår.UNDER_18_ÅR)
            .medVilkår(" EEEE++EE>", Vilkår.BOSATT_I_RIKET)
            .medVilkår("EEEEEEEEE>", Vilkår.LOVLIG_OPPHOLD)
            .medVilkår("EEEENNEEE>", Vilkår.BOR_MED_SØKER)
            .medVilkår("+++++++++>", Vilkår.GIFT_PARTNERSKAP)

        val vilkårsvurderingTidslinjer = VilkårsvurderingTidslinjer(
            vilkårsvurdering = vilkårsvurderingBygger.byggVilkårsvurdering(),
            søkerOgBarn = lagTestPersonopplysningGrunnlag(behandling.id, søker, barn1)
                .tilPersonEnkelSøkerOgBarn(),
        )

        val restTidslinjer = vilkårsvurderingTidslinjer.tilRestTidslinjer()
        val barnetsTidslinjer = restTidslinjer.barnasTidslinjer[barn1.aktør.aktivFødselsnummer()]!!

        // Stopper ved søkers siste til-og-med-dato fordi Regelverk er <null> etter det, som filtreres bort
        assertEquals(
            31.jan(2022).tilLocalDate(),
            barnetsTidslinjer.regelverkTidslinje.last().tilOgMed,
        )
        assertEquals(
            31.jan(2022).tilLocalDate(),
            barnetsTidslinjer.oppfyllerEgneVilkårIKombinasjonMedSøkerTidslinje.last().tilOgMed,
        )

        // Alle vilkårene til barnet kuttes ved siste dag i måneden før barnet fyller 18 år
        barnetsTidslinjer.vilkårTidslinjer.forEach {
            assertEquals(
                31.des(2037).tilLocalDate(),
                it.last().tilOgMed,
            )
        }
    }

    @Test
    fun `søkers rest-tidslinjene for oppfylt vilkår skal begrenses av barnas 18-års-perioder`() {
        val søkersFødselsdato = 3.feb(1995)
        val barn1Fødselsdato = 13.jan(2020)
        val barn2Fødselsdato = 27.des(2021)
        val søker = tilfeldigPerson(personType = PersonType.SØKER, fødselsdato = søkersFødselsdato.tilLocalDate())
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barn1Fødselsdato.tilLocalDate())
        val barn2 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barn2Fødselsdato.tilLocalDate())

        val behandling = lagBehandling()

        val vilkårsvurderingBygger = VilkårsvurderingBuilder<Måned>(behandling)
            .forPerson(søker, søkersFødselsdato.tilInneværendeMåned())
            .medVilkår("E>", Vilkår.BOSATT_I_RIKET)
            .medVilkår("E>", Vilkår.LOVLIG_OPPHOLD)
            .forPerson(barn1, barn1Fødselsdato.tilInneværendeMåned())
            .medVilkår("+>", Vilkår.UNDER_18_ÅR)
            .medVilkår("E>", Vilkår.BOSATT_I_RIKET)
            .medVilkår("E>", Vilkår.LOVLIG_OPPHOLD)
            .medVilkår("E>", Vilkår.BOR_MED_SØKER)
            .medVilkår("+>", Vilkår.GIFT_PARTNERSKAP)
            .forPerson(barn2, barn2Fødselsdato.tilInneværendeMåned())
            .medVilkår("+>", Vilkår.UNDER_18_ÅR)
            .medVilkår("E>", Vilkår.BOSATT_I_RIKET)
            .medVilkår("E>", Vilkår.LOVLIG_OPPHOLD)
            .medVilkår("E>", Vilkår.BOR_MED_SØKER)
            .medVilkår("+>", Vilkår.GIFT_PARTNERSKAP)

        val vilkårsvurderingTidslinjer = VilkårsvurderingTidslinjer(
            vilkårsvurdering = vilkårsvurderingBygger.byggVilkårsvurdering(),
            søkerOgBarn = lagTestPersonopplysningGrunnlag(behandling.id, søker, barn1, barn2)
                .tilPersonEnkelSøkerOgBarn(),
        )

        val restTidslinjer = vilkårsvurderingTidslinjer.tilRestTidslinjer()
        val søkersTidslinjer = restTidslinjer.søkersTidslinjer

        // Stopper ved siste dag i måneden før yngste barn fyller 18 år
        søkersTidslinjer.vilkårTidslinjer.forEach {
            assertEquals(
                30.nov(2039).tilLocalDate(),
                it.last().tilOgMed,
            )
        }

        assertEquals(
            30.nov(2039).tilLocalDate(),
            søkersTidslinjer.oppfyllerEgneVilkårTidslinje.last().tilOgMed,
        )
    }
}
