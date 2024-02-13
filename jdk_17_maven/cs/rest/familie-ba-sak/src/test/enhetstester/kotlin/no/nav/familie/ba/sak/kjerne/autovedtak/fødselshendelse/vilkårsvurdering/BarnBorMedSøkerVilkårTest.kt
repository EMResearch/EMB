package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering

import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.datagenerator.grunnlag.opprettAdresse
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class BarnBorMedSøkerVilkårTest {

    @Test
    fun `Samme matrikkelId men ellers forskjellige adresser`() {
        val faktaPerson = opprettFaktaPerson(adresseMatrikkelId1barn, adresseMatrikkelId1SøkerBruksenhetsnummer)

        val evaluering = vilkår.vurderVilkår(faktaPerson)
        Assertions.assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)
    }

    @Test
    fun `Forskjellige matrikkelId`() {
        val faktaPerson = opprettFaktaPerson(adresseMatrikkelId1barn, adresseMatrikkelId2Søker)

        val evaluering = vilkår.vurderVilkår(faktaPerson)
        Assertions.assertThat(evaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
    }

    @Test
    fun `Address som mangler postnummer`() {
        val faktaPerson = opprettFaktaPerson(adresseIkkePostnummerBarn, adresseIkkePostnummerSøker)

        val evaluering = vilkår.vurderVilkår(faktaPerson)
        Assertions.assertThat(evaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
    }

    @Test
    fun `Address som mangler matrikkelid`() {
        val faktaPerson = opprettFaktaPerson(adresseAttrBarn, adresseAttrSøker)

        val evaluering = vilkår.vurderVilkår(faktaPerson)
        Assertions.assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)
    }

    @Test
    fun `To forskjellige address som begge mangler matrikkelid`() {
        val faktaPerson = opprettFaktaPerson(adresseAttrBarn, adresseAttr2Søker)

        val evaluering = vilkår.vurderVilkår(faktaPerson)
        Assertions.assertThat(evaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
    }

    private fun opprettFaktaPerson(
        bostedsadresseSøker: GrBostedsadresse,
        bostedsadresseBarn: GrBostedsadresse,
    ): Person {
        val barnMedAdresse = barn.copy(bostedsadresser = mutableListOf(bostedsadresseBarn))
        barnMedAdresse.personopplysningGrunnlag.personer.clear()
        barnMedAdresse.personopplysningGrunnlag.personer.add(
            søker.copy(
                bostedsadresser = mutableListOf(
                    bostedsadresseSøker,
                ),
            ),
        )

        return barnMedAdresse
    }

    companion object {

        val vilkår = Vilkår.BOR_MED_SØKER
        val barn = tilfeldigPerson(personType = PersonType.BARN)

        val søker = tilfeldigPerson(personType = PersonType.SØKER, kjønn = Kjønn.KVINNE)

        val adresseMatrikkelId1barn = opprettAdresse(1234L)
        val adresseMatrikkelId2Søker = opprettAdresse(4321L)
        val adresseMatrikkelId1SøkerBruksenhetsnummer = opprettAdresse(matrikkelId = 1234L, bruksenhetsnummer = "123")
        val adresseIkkePostnummerBarn = opprettAdresse(adressenavn = "Fågelveien", husnummer = "123")
        val adresseIkkePostnummerSøker = opprettAdresse(adressenavn = "Fågelveien", husnummer = "123")
        val adresseAttrBarn = opprettAdresse(adressenavn = "Fågelveien", husnummer = "123", postnummer = "0245")
        val adresseAttrSøker = opprettAdresse(adressenavn = "Fågelveien", husnummer = "123", postnummer = "0245")
        val adresseAttr2Søker = opprettAdresse(adressenavn = "Fågelveien", husnummer = "11", postnummer = "0245")
    }
}
