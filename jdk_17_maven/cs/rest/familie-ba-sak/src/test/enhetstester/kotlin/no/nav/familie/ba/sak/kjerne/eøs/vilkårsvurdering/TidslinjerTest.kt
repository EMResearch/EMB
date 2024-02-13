package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.oppfyltVilkår
import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.common.tilPersonEnkelSøkerOgBarn
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.tilpassKompetanserTilRegelverk
import no.nav.familie.ba.sak.kjerne.eøs.util.tilTidslinje
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.konkatenerTidslinjer
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.ogSenere
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilInneværendeMåned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
import no.nav.familie.ba.sak.kjerne.tidslinje.tidsrom.rangeTo
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.tidslinje.util.VilkårsvurderingBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.apr
import no.nav.familie.ba.sak.kjerne.tidslinje.util.byggVilkårsvurderingTidslinjer
import no.nav.familie.ba.sak.kjerne.tidslinje.util.des
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mai
import no.nav.familie.ba.sak.kjerne.tidslinje.util.nov
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilRegelverkResultatTidslinje
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk.EØS_FORORDNINGEN
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk.NASJONALE_REGLER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOR_MED_SØKER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.GIFT_PARTNERSKAP
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.LOVLIG_OPPHOLD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.UNDER_18_ÅR
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class TidslinjerTest {

    @Test
    fun `lag en søker med to barn og mye kompleksitet i vilkårsvurderingen`() {
        val barnsFødselsdato = 13.jan(2020)
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barnsFødselsdato.tilLocalDate())
        val barn2 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barnsFødselsdato.tilLocalDate())

        val behandling = lagBehandling()
        val startMåned = barnsFødselsdato.tilInneværendeMåned()

        val vilkårsvurderingBygger = VilkårsvurderingBuilder<Måned>(behandling)
            .forPerson(søker, startMåned)
            .medVilkår("EEEEEEEENNEEEEEEEEEEE", BOSATT_I_RIKET)
            .medVilkår("EEEEEEEENNEEEEEEEEEEE", LOVLIG_OPPHOLD)
            .byggPerson()
        val søkerResult = " EEEEEEENNEEEEEEEEEEE".tilRegelverkResultatTidslinje(startMåned).filtrerIkkeNull()

        vilkårsvurderingBygger.forPerson(barn1, startMåned)
            .medVilkår("++++++++++++++++     ", UNDER_18_ÅR)
            .medVilkår("   EEE NNNN  EEEE+++ ", BOSATT_I_RIKET)
            .medVilkår("     EEENNEEEEEEEEE  ", LOVLIG_OPPHOLD)
            .medVilkår("NNNNNNNNNNEEEEEEEEEEE", BOR_MED_SØKER)
            .medVilkår("+++++++++++++++++++++", GIFT_PARTNERSKAP)
            .byggPerson()
        val barn1Result = " ???????NN!???EE?????".tilRegelverkResultatTidslinje(startMåned).filtrerIkkeNull()

        vilkårsvurderingBygger.forPerson(barn2, startMåned)
            .medVilkår("+++++++++>", UNDER_18_ÅR)
            .medVilkår(" EEEE++EE>", BOSATT_I_RIKET)
            .medVilkår("EEEEEEEEE>", LOVLIG_OPPHOLD)
            .medVilkår("EEEENNEEE>", BOR_MED_SØKER)
            .medVilkår("+++++++++>", GIFT_PARTNERSKAP)
            .byggPerson()
        val barn2Result = " ?EE!!!E!!EEEEEEEEEEE".tilRegelverkResultatTidslinje(startMåned).filtrerIkkeNull()

        val vilkårsvurderingTidslinjer = VilkårsvurderingTidslinjer(
            vilkårsvurdering = vilkårsvurderingBygger.byggVilkårsvurdering(),
            søkerOgBarn = lagTestPersonopplysningGrunnlag(behandling.id, søker, barn1, barn2)
                .tilPersonEnkelSøkerOgBarn(),
        )

        assertEquals(søkerResult, vilkårsvurderingTidslinjer.søkersTidslinjer().regelverkResultatTidslinje)
        assertEquals(barn1Result, vilkårsvurderingTidslinjer.forBarn(barn1).regelverkResultatTidslinje.kombinertResultat)
        assertEquals(barn2Result, vilkårsvurderingTidslinjer.forBarn(barn2).regelverkResultatTidslinje.kombinertResultat)
    }

    @Test
    fun `lag en søker med ett barn og søker går fra EØS-regelverk til nasjonalt`() {
        val barnsFødselsdato = 13.jan(2020)
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = barnsFødselsdato.tilLocalDate())

        val behandling = lagBehandling()
        val startMåned = barnsFødselsdato.tilInneværendeMåned()

        val vilkårsvurderingBygger = VilkårsvurderingBuilder<Måned>(behandling)
            .forPerson(søker, startMåned)
            .medVilkår("EEEEEEEEEEEEENNNNNNNN", BOSATT_I_RIKET)
            .medVilkår("EEEEEEEEEEEEENNNNNNNN", LOVLIG_OPPHOLD)
            .byggPerson()
        val søkerResult = " EEEEEEEEEEEENNNNNNNN".tilRegelverkResultatTidslinje(startMåned).filtrerIkkeNull()

        vilkårsvurderingBygger.forPerson(barn1, startMåned)
            .medVilkår("++++++++++++++++     ", UNDER_18_ÅR)
            .medVilkår("   EEEENNNNEEEEEEEE ", BOSATT_I_RIKET)
            .medVilkår("     EEENNEEEEEEEEE  ", LOVLIG_OPPHOLD)
            .medVilkår("NNNNNNNNNNEEEEEEEEEEE", BOR_MED_SØKER)
            .medVilkår("+++++++++++++++++++++", GIFT_PARTNERSKAP)
            .byggPerson()
        val barn1Result = " ?????!!!!!EE!!!?????".tilRegelverkResultatTidslinje(startMåned).filtrerIkkeNull()

        val vilkårsvurderingTidslinjer = VilkårsvurderingTidslinjer(
            vilkårsvurdering = vilkårsvurderingBygger.byggVilkårsvurdering(),
            søkerOgBarn = lagTestPersonopplysningGrunnlag(behandling.id, søker, barn1)
                .tilPersonEnkelSøkerOgBarn(),
        )

        assertEquals(søkerResult, vilkårsvurderingTidslinjer.søkersTidslinjer().regelverkResultatTidslinje)
        assertEquals(barn1Result, vilkårsvurderingTidslinjer.forBarn(barn1).regelverkResultatTidslinje.kombinertResultat)
    }

    @Test
    fun `Virkningstidspunkt for vilkårsvurdering varer frem til måneden før barnet fyller 18 år`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = 14.des(2019).tilLocalDate())

        val behandling = lagBehandling()

        val vilkårsvurderingBygger = VilkårsvurderingBuilder<Måned>(behandling)
            .forPerson(søker, jan(2020))
            .medVilkår("+>", BOSATT_I_RIKET)
            .medVilkår("+>", LOVLIG_OPPHOLD)
            .forPerson(barn1, jan(2020))
            .medVilkår("+>", UNDER_18_ÅR)
            .medVilkår("+>", BOSATT_I_RIKET)
            .medVilkår("+>", LOVLIG_OPPHOLD)
            .medVilkår("+>", BOR_MED_SØKER)
            .medVilkår("+>", GIFT_PARTNERSKAP)
            .byggPerson()

        val vilkårsvurderingTidslinjer = VilkårsvurderingTidslinjer(
            vilkårsvurdering = vilkårsvurderingBygger.byggVilkårsvurdering(),
            søkerOgBarn = lagTestPersonopplysningGrunnlag(behandling.id, søker, barn1)
                .tilPersonEnkelSøkerOgBarn(),
        )

        assertEquals(
            barn1.fødselsdato.til18ÅrsVilkårsdato().minusMonths(1).toYearMonth(),
            vilkårsvurderingTidslinjer.forBarn(barn1).egetRegelverkResultatTidslinje.filtrerIkkeNull()
                .perioder().maxOf { it.tilOgMed.tilYearMonth() },
        )
    }

    @Test
    fun `Sjekk overgang fra oppfylt nasjonalt til oppfylt EØS i månedsskiftet`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = 14.des(2019).tilLocalDate())

        val vilkårsvurderingTidslinjer = VilkårsvurderingBuilder<Dag>()
            .forPerson(søker, 30.apr(2020))
            .medVilkår("NE", BOSATT_I_RIKET, LOVLIG_OPPHOLD)
            .forPerson(barn1, 30.apr(2020))
            .medVilkår("++", UNDER_18_ÅR, GIFT_PARTNERSKAP)
            .medVilkår("NE", BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER)
            .byggVilkårsvurderingTidslinjer()

        val barn1Result = "E".tilRegelverkResultatTidslinje(mai(2020))

        assertEquals(barn1Result, vilkårsvurderingTidslinjer.forBarn(barn1).regelverkResultatTidslinje.kombinertResultat)
    }

    @Test
    fun `Sjekk overgang fra oppfylt EØS til oppfylt nasjonalt i månedsskiftet`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = 14.des(2019).tilLocalDate())

        val vilkårsvurderingTidslinjer = VilkårsvurderingBuilder<Dag>()
            .forPerson(søker, 30.apr(2020))
            .medVilkår("EN", BOSATT_I_RIKET, LOVLIG_OPPHOLD)
            .forPerson(barn1, 30.apr(2020))
            .medVilkår("++", UNDER_18_ÅR, GIFT_PARTNERSKAP)
            .medVilkår("EN", BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER)
            .byggVilkårsvurderingTidslinjer()

        val barn1Result = "N".tilRegelverkResultatTidslinje(mai(2020))

        assertEquals(barn1Result, vilkårsvurderingTidslinjer.forBarn(barn1).regelverkResultatTidslinje.kombinertResultat)
    }

    @Test
    fun `Sjekk overgang fra oppfylt EØS til oppfylt blandet i månedsskiftet`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = 14.des(2019).tilLocalDate())

        val vilkårsvurderingTidslinjer = VilkårsvurderingBuilder<Dag>()
            .forPerson(søker, 30.apr(2020))
            .medVilkår("EE", BOSATT_I_RIKET, LOVLIG_OPPHOLD)
            .forPerson(barn1, 30.apr(2020))
            .medVilkår("++", UNDER_18_ÅR, GIFT_PARTNERSKAP)
            .medVilkår("EE", BOSATT_I_RIKET)
            .medVilkår("EE", LOVLIG_OPPHOLD)
            .medVilkår("EN", BOR_MED_SØKER)
            .byggVilkårsvurderingTidslinjer()

        val barn1Result = "!".tilRegelverkResultatTidslinje(mai(2020))

        assertEquals(barn1Result, vilkårsvurderingTidslinjer.forBarn(barn1).regelverkResultatTidslinje.kombinertResultat)
    }

    @Test
    fun `Sjekk overgang fra oppfylt nasjonalt til oppfylt EØS dagen før siste dag i måneden`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = 14.des(2019).tilLocalDate())

        val giftPartnerskap =
            (26.jan(2020)..30.nov(2021)).tilTidslinje { oppfyltVilkår(GIFT_PARTNERSKAP) }
        val under18 =
            (26.jan(2020)..30.nov(2021)).tilTidslinje { oppfyltVilkår(UNDER_18_ÅR) }
        val bosattBarnOgSøker = konkatenerTidslinjer(
            (26.jan(2020)..29.apr(2021)).tilTidslinje { oppfyltVilkår(BOSATT_I_RIKET, NASJONALE_REGLER) },
            (30.apr(2021)..30.nov(2021)).tilTidslinje { oppfyltVilkår(BOSATT_I_RIKET, EØS_FORORDNINGEN) },
        )
        val lovligOppholdBarnOgSøker = konkatenerTidslinjer(
            (26.jan(2020)..29.apr(2021)).tilTidslinje { oppfyltVilkår(LOVLIG_OPPHOLD, NASJONALE_REGLER) },
            (30.apr(2021)..30.nov(2021)).tilTidslinje { oppfyltVilkår(LOVLIG_OPPHOLD, EØS_FORORDNINGEN) },
        )
        val borMedSøker = konkatenerTidslinjer(
            (26.jan(2020)..29.apr(2021)).tilTidslinje { oppfyltVilkår(BOR_MED_SØKER, NASJONALE_REGLER) },
            (30.apr(2021)..30.nov(2021)).tilTidslinje { oppfyltVilkår(BOR_MED_SØKER, EØS_FORORDNINGEN) },
        )

        val barnaRegelverkTidslinjer = VilkårsvurderingBuilder<Dag>()
            .forPerson(søker, 26.jan(2020))
            .medVilkår(bosattBarnOgSøker)
            .medVilkår(lovligOppholdBarnOgSøker)
            .forPerson(barn, 26.jan(2020))
            .medVilkår(bosattBarnOgSøker)
            .medVilkår(lovligOppholdBarnOgSøker)
            .medVilkår(giftPartnerskap)
            .medVilkår(under18)
            .medVilkår(borMedSøker)
            .byggVilkårsvurderingTidslinjer()
            .barnasRegelverkResultatTidslinjer()

        val kompetanser = tilpassKompetanserTilRegelverk(
            emptyList(),
            barnaRegelverkTidslinjer,
            emptyMap(),
        )

        assertEquals(1, kompetanser.size)
        assertEquals(YearMonth.of(2021, 5), kompetanser.first().fom)
        assertEquals(YearMonth.of(2021, 11), kompetanser.first().tom)
    }

    @Test
    fun `Sjekk overgang fra oppfylt nasjonalt til oppfylt EØS dagen andre dag i måneden`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = 14.des(2019).tilLocalDate())

        val giftPartnerskap =
            (26.jan(2020)..30.nov(2021)).tilTidslinje { oppfyltVilkår(GIFT_PARTNERSKAP) }
        val under18 =
            (26.jan(2020)..30.nov(2021)).tilTidslinje { oppfyltVilkår(UNDER_18_ÅR) }
        val bosattBarnOgSøker = konkatenerTidslinjer(
            (26.jan(2020)..1.mai(2021)).tilTidslinje { oppfyltVilkår(BOSATT_I_RIKET, NASJONALE_REGLER) },
            (2.mai(2021)..30.nov(2021)).tilTidslinje { oppfyltVilkår(BOSATT_I_RIKET, EØS_FORORDNINGEN) },
        )
        val lovligOppholdBarnOgSøker = konkatenerTidslinjer(
            (26.jan(2020)..1.mai(2021)).tilTidslinje { oppfyltVilkår(LOVLIG_OPPHOLD, NASJONALE_REGLER) },
            (2.mai(2021)..30.nov(2021)).tilTidslinje { oppfyltVilkår(LOVLIG_OPPHOLD, EØS_FORORDNINGEN) },
        )
        val borMedSøker = konkatenerTidslinjer(
            (26.jan(2020)..1.mai(2021)).tilTidslinje { oppfyltVilkår(BOR_MED_SØKER, NASJONALE_REGLER) },
            (2.mai(2021)..30.nov(2021)).tilTidslinje { oppfyltVilkår(BOR_MED_SØKER, EØS_FORORDNINGEN) },
        )

        val barnaRegelverkTidslinjer = VilkårsvurderingBuilder<Dag>()
            .forPerson(søker, 26.jan(2020))
            .medVilkår(bosattBarnOgSøker)
            .medVilkår(lovligOppholdBarnOgSøker)
            .forPerson(barn, 26.jan(2020))
            .medVilkår(bosattBarnOgSøker)
            .medVilkår(lovligOppholdBarnOgSøker)
            .medVilkår(giftPartnerskap)
            .medVilkår(under18)
            .medVilkår(borMedSøker)
            .byggVilkårsvurderingTidslinjer()
            .barnasRegelverkResultatTidslinjer()

        val kompetanser = tilpassKompetanserTilRegelverk(
            emptyList(),
            barnaRegelverkTidslinjer,
            emptyMap(),
        )

        val forventetRegelverkResultat =
            "NNNNNNNNNNNNNNNNEEEEEE".tilRegelverkResultatTidslinje(feb(2020))

        assertEquals(forventetRegelverkResultat, barnaRegelverkTidslinjer[barn.aktør]?.kombinertResultat)
        assertEquals(1, kompetanser.size)
        assertEquals(YearMonth.of(2021, 6), kompetanser.first().fom)
        assertEquals(YearMonth.of(2021, 11), kompetanser.first().tom)
    }

    @Test
    fun `Sjekk overgang fra oppfylt nasjonalt til oppfylt EØS dagen før siste dag i måneden, der siste periode er uendelig`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = 14.des(2019).tilLocalDate())

        val giftPartnerskap =
            26.jan(2020).ogSenere().tilTidslinje { oppfyltVilkår(GIFT_PARTNERSKAP) }
        val under18 =
            26.jan(2020).ogSenere().tilTidslinje { oppfyltVilkår(UNDER_18_ÅR) }
        val bosattBarnOgSøker = konkatenerTidslinjer(
            (26.jan(2020)..29.apr(2021)).tilTidslinje { oppfyltVilkår(BOSATT_I_RIKET, NASJONALE_REGLER) },
            30.apr(2021).ogSenere().tilTidslinje { oppfyltVilkår(BOSATT_I_RIKET, EØS_FORORDNINGEN) },
        )
        val lovligOppholdBarnOgSøker = konkatenerTidslinjer(
            (26.jan(2020)..29.apr(2021)).tilTidslinje { oppfyltVilkår(LOVLIG_OPPHOLD, NASJONALE_REGLER) },
            30.apr(2021).ogSenere().tilTidslinje { oppfyltVilkår(LOVLIG_OPPHOLD, EØS_FORORDNINGEN) },
        )
        val borMedSøker = konkatenerTidslinjer(
            (26.jan(2020)..29.apr(2021)).tilTidslinje { oppfyltVilkår(BOR_MED_SØKER, NASJONALE_REGLER) },
            30.apr(2021).ogSenere().tilTidslinje { oppfyltVilkår(BOR_MED_SØKER, EØS_FORORDNINGEN) },
        )

        val barnaRegelverkTidslinjer = VilkårsvurderingBuilder<Dag>()
            .forPerson(søker, 26.jan(2020))
            .medVilkår(bosattBarnOgSøker)
            .medVilkår(lovligOppholdBarnOgSøker)
            .forPerson(barn, 26.jan(2020))
            .medVilkår(bosattBarnOgSøker)
            .medVilkår(lovligOppholdBarnOgSøker)
            .medVilkår(giftPartnerskap)
            .medVilkår(under18)
            .medVilkår(borMedSøker)
            .byggVilkårsvurderingTidslinjer()
            .barnasRegelverkResultatTidslinjer()

        val kompetanser = tilpassKompetanserTilRegelverk(
            emptyList(),
            barnaRegelverkTidslinjer,
            emptyMap(),
        )

        assertEquals(1, kompetanser.size)
        assertEquals(YearMonth.of(2021, 5), kompetanser.first().fom)
        assertNull(kompetanser.first().tom)
    }
}

fun VilkårsvurderingTidslinjer.barnasRegelverkResultatTidslinjer() = this.barnasTidslinjer()
    .mapValues { (_, barnetsTidslinjer) -> barnetsTidslinjer.regelverkResultatTidslinje }

private val Tidslinje<KombinertRegelverkResultat, Måned>.kombinertResultat: Tidslinje<RegelverkResultat, Måned>
    get() = map { it?.kombinertResultat }
