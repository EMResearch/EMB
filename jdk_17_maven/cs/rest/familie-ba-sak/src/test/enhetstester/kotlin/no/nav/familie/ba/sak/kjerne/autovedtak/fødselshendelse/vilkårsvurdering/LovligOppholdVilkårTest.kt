package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.datagenerator.grunnlag.opprettAdresse
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.utfall.VilkårIkkeOppfyltÅrsak
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.utfall.VilkårOppfyltÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.arbeidsforhold.GrArbeidsforhold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.opphold.GrOpphold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LovligOppholdVilkårTest {

    @Test
    fun `Ikke lovlig opphold dersom søker ikke har noen gjeldende opphold registrert`() {
        val evaluering = vilkår.vurderVilkår(tredjelandsborger).evaluering
        assertThat(evaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
    }

    @Test
    fun `Ikke lovlig opphold dersom søker er statsløs og ikke har noen gjeldende opphold registrert`() {
        val statsløsEvaluering = vilkår.vurderVilkår(statsløsPerson).evaluering
        assertThat(statsløsEvaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)

        val ukjentStatsborgerskapEvaluering =
            vilkår.vurderVilkår(ukjentStatsborger).evaluering
        assertThat(ukjentStatsborgerskapEvaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
    }

    @Test
    fun `Lovlig opphold vurdert på bakgrunn av status`() {
        var evaluering = vilkår.vurderVilkår(faktaPerson(OPPHOLDSTILLATELSE.MIDLERTIDIG, null)).evaluering
        assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)
        evaluering = vilkår.vurderVilkår(faktaPerson(OPPHOLDSTILLATELSE.PERMANENT, null)).evaluering
        assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)
        evaluering = vilkår.vurderVilkår(faktaPerson(OPPHOLDSTILLATELSE.OPPLYSNING_MANGLER, null)).evaluering
        assertThat(evaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
    }

    @Test
    fun `Lovlig opphold vurdert på bakgrunn av status for statsløs søker`() {
        var evaluering = vilkår.vurderVilkår(
            statsløsPerson.copy().apply {
                opphold =
                    mutableListOf(GrOpphold(gyldigPeriode = null, type = OPPHOLDSTILLATELSE.MIDLERTIDIG, person = this))
            },
        ).evaluering
        assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)

        evaluering = vilkår.vurderVilkår(
            ukjentStatsborger.copy().apply {
                opphold =
                    mutableListOf(GrOpphold(gyldigPeriode = null, type = OPPHOLDSTILLATELSE.MIDLERTIDIG, person = this))
            },
        ).evaluering
        assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)

        evaluering = vilkår.vurderVilkår(
            statsløsPerson.copy().apply {
                opphold =
                    mutableListOf(
                        GrOpphold(
                            gyldigPeriode = null,
                            type = OPPHOLDSTILLATELSE.OPPLYSNING_MANGLER,
                            person = this,
                        ),
                    )
            },
        ).evaluering
        assertThat(evaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
    }

    @Test
    fun `Ikke lovlig opphold dersom utenfor gyldig periode`() {
        var evaluering = vilkår.vurderVilkår(
            tredjelandsborger.copy(
                statsborgerskap = mutableListOf(
                    GrStatsborgerskap(
                        landkode = "ANG",
                        medlemskap = Medlemskap.TREDJELANDSBORGER,
                        person = tredjelandsborger,
                    ),
                ),
                opphold = mutableListOf(
                    GrOpphold(
                        gyldigPeriode = DatoIntervallEntitet(
                            fom = LocalDate.now().minusYears(10),
                            tom = LocalDate.now().minusYears(5),
                        ),
                        type = OPPHOLDSTILLATELSE.MIDLERTIDIG,
                        person = tredjelandsborger,
                    ),
                ),
            ),
        ).evaluering
        assertThat(evaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)

        evaluering = vilkår.vurderVilkår(
            statsløsPerson.copy().apply {
                opphold = mutableListOf(
                    GrOpphold(
                        gyldigPeriode = DatoIntervallEntitet(
                            fom = LocalDate.now().minusYears(10),
                            tom = LocalDate.now().minusYears(5),
                        ),
                        type = OPPHOLDSTILLATELSE.MIDLERTIDIG,
                        person = this,
                    ),
                )
            },
        ).evaluering
        assertThat(evaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
    }

    @Test
    fun `Lovlig opphold dersom status med gjeldende periode`() {
        var evaluering = vilkår.vurderVilkår(
            tredjelandsborger.copy(
                statsborgerskap = mutableListOf(
                    GrStatsborgerskap(
                        landkode = "ANG",
                        medlemskap = Medlemskap.TREDJELANDSBORGER,
                        person = tredjelandsborger,
                    ),
                ),
                opphold = mutableListOf(
                    GrOpphold(
                        gyldigPeriode = DatoIntervallEntitet(
                            fom = LocalDate.now().minusYears(10),
                            tom = LocalDate.now().minusYears(5),
                        ),
                        type = OPPHOLDSTILLATELSE.OPPLYSNING_MANGLER,
                        person = tredjelandsborger,
                    ),
                    GrOpphold(
                        gyldigPeriode = DatoIntervallEntitet(
                            fom = LocalDate.now().minusYears(5),
                            tom = null,
                        ),
                        type = OPPHOLDSTILLATELSE.MIDLERTIDIG,
                        person = tredjelandsborger,
                    ),
                ),
            ),
        ).evaluering
        assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)

        evaluering = vilkår.vurderVilkår(
            statsløsPerson.copy().apply {
                opphold = mutableListOf(
                    GrOpphold(
                        gyldigPeriode = DatoIntervallEntitet(
                            fom = LocalDate.now().minusYears(10),
                            tom = LocalDate.now().minusYears(5),
                        ),
                        type = OPPHOLDSTILLATELSE.OPPLYSNING_MANGLER,
                        person = this,
                    ),
                    GrOpphold(
                        gyldigPeriode = DatoIntervallEntitet(
                            fom = LocalDate.now().minusYears(5),
                            tom = null,
                        ),
                        type = OPPHOLDSTILLATELSE.MIDLERTIDIG,
                        person = this,
                    ),
                )
            },
        ).evaluering
        assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)
    }

    @Test
    fun `Lovlig opphold blir oppfylt for mor med EØS medlemskap og har løpende arbeidsforhold`() {
        val evaluering = vilkår.vurderVilkår(
            eøsBorger.copy().apply {
                arbeidsforhold = mutableListOf(
                    GrArbeidsforhold(
                        periode = DatoIntervallEntitet(fom = null, tom = LocalDate.now().plusMonths(6)),
                        arbeidsgiverId = null,
                        arbeidsgiverType = null,
                        person = this,
                    ),
                )
            },
        ).evaluering
        assertEquals(Resultat.OPPFYLT, evaluering.resultat)
        assertEquals(listOf(VilkårOppfyltÅrsak.EØS_MED_LØPENDE_ARBEIDSFORHOLD), evaluering.evalueringÅrsaker)
    }

    @Test
    fun `Lovlig opphold blir ikke oppfylt for mor med EØS medlemskap, uten løpende arbeidsforhold og annen forelder`() {
        val evaluering = vilkår.vurderVilkår(
            person = eøsBorger.copy().apply {
                arbeidsforhold = mutableListOf()
            },
            annenForelder = null,
        ).evaluering
        assertEquals(Resultat.IKKE_OPPFYLT, evaluering.resultat)
        assertEquals(listOf(VilkårIkkeOppfyltÅrsak.EØS_STATSBORGERSKAP_ANNEN_FORELDER_UKLART), evaluering.evalueringÅrsaker)
    }

    @Test
    fun `Lovlig opphold blir ikke oppfylt for mor med EØS medlemskap, uten løpende arbeidsforhold og annen forelder bor ikke med mor`() {
        val evaluering = vilkår.vurderVilkår(
            person = eøsBorger.copy().apply {
                arbeidsforhold = mutableListOf()
                bostedsadresser =
                    mutableListOf(opprettAdresse(adressenavn = "Osloveien", husnummer = "123", postnummer = "0245"))
            },
            annenForelder = annenForelderNordiskBorger.copy(
                bostedsadresser = mutableListOf(
                    opprettAdresse(
                        adressenavn = "Fågelveien",
                        husnummer = "123",
                        postnummer = "0245",
                    ),
                ),
            ),
        ).evaluering
        assertEquals(Resultat.IKKE_OPPFYLT, evaluering.resultat)
        assertEquals(
            listOf(VilkårIkkeOppfyltÅrsak.EØS_BOR_IKKE_SAMMEN_MED_ANNEN_FORELDER),
            evaluering.evalueringÅrsaker,
        )
    }

    @Test
    fun `Lovlig opphold blir ikke oppfylt for mor med EØS medlemskap, uten løpende arbeidsforhold og annen forelder har bodd med mor`() {
        val tidligereAdresse = opprettAdresse(adressenavn = "Uteveien", husnummer = "123", postnummer = "0245").also {
            it.periode = DatoIntervallEntitet(
                fom = LocalDate.now().minusYears(2),
                tom = LocalDate.now().minusYears(1),
            )
        }

        val evaluering = vilkår.vurderVilkår(
            person = eøsBorger.copy().apply {
                arbeidsforhold = mutableListOf()
                bostedsadresser =
                    mutableListOf(
                        tidligereAdresse,
                        opprettAdresse(adressenavn = "Osloveien", husnummer = "123", postnummer = "0245"),
                    )
            },
            annenForelder = annenForelderNordiskBorger.copy(
                bostedsadresser = mutableListOf(
                    tidligereAdresse,
                    opprettAdresse(
                        adressenavn = "Fågelveien",
                        husnummer = "123",
                        postnummer = "0245",
                    ),
                ),
            ),
        ).evaluering
        assertEquals(Resultat.IKKE_OPPFYLT, evaluering.resultat)
        assertEquals(
            listOf(VilkårIkkeOppfyltÅrsak.EØS_BOR_IKKE_SAMMEN_MED_ANNEN_FORELDER),
            evaluering.evalueringÅrsaker,
        )
    }

    @Test
    fun `Lovlig opphold blir oppfylt for mor med EØS medlemskap, uten løpende arbeidsforhold og annen forelder bor med mor og nordisk`() {
        val adresse = opprettAdresse(adressenavn = "Osloveien", husnummer = "123", postnummer = "0245")
        val evaluering = vilkår.vurderVilkår(
            person = eøsBorger.copy().apply {
                arbeidsforhold = mutableListOf()
                bostedsadresser =
                    mutableListOf(adresse)
            },
            annenForelder = annenForelderNordiskBorger.copy(
                bostedsadresser = mutableListOf(
                    adresse,
                ),
            ),
        ).evaluering
        assertEquals(Resultat.OPPFYLT, evaluering.resultat)
        assertEquals(
            listOf(VilkårOppfyltÅrsak.ANNEN_FORELDER_NORDISK),
            evaluering.evalueringÅrsaker,
        )
    }

    @Test
    fun `Lovlig opphold blir ikke oppfylt for mor med EØS medlemskap, annen forelder(EØS) ikke løpende arbeidsforhold`() {
        val adresse = opprettAdresse(adressenavn = "Osloveien", husnummer = "123", postnummer = "0245")
        val evaluering = vilkår.vurderVilkår(
            person = eøsBorger.copy().apply {
                arbeidsforhold = mutableListOf()
                bostedsadresser =
                    mutableListOf(adresse)
            },
            annenForelder = annenForelderEØS.copy(
                bostedsadresser = mutableListOf(
                    adresse,
                ),
                arbeidsforhold = mutableListOf(),
            ),
        ).evaluering
        assertEquals(Resultat.IKKE_OPPFYLT, evaluering.resultat)
        assertEquals(
            listOf(VilkårIkkeOppfyltÅrsak.EØS_ANNEN_FORELDER_EØS_MEN_IKKE_MED_LØPENDE_ARBEIDSFORHOLD),
            evaluering.evalueringÅrsaker,
        )
    }

    @Test
    fun `Lovlig opphold blir oppfylt for mor med EØS medlemskap, annen forelder(EØS) har løpende arbeidsforhold`() {
        val adresse = opprettAdresse(adressenavn = "Osloveien", husnummer = "123", postnummer = "0245")
        val evaluering = vilkår.vurderVilkår(
            person = eøsBorger.copy().apply {
                arbeidsforhold = mutableListOf()
                bostedsadresser =
                    mutableListOf(adresse)
            },
            annenForelder = annenForelderEØS.copy().apply {
                bostedsadresser = mutableListOf(
                    adresse,
                )
                arbeidsforhold = mutableListOf(
                    GrArbeidsforhold(
                        periode = DatoIntervallEntitet(fom = null, tom = LocalDate.now().plusMonths(6)),
                        arbeidsgiverId = null,
                        arbeidsgiverType = null,
                        person = this,
                    ),
                )
            },
        ).evaluering
        assertEquals(Resultat.OPPFYLT, evaluering.resultat)
        assertEquals(
            listOf(VilkårOppfyltÅrsak.ANNEN_FORELDER_EØS_MEN_MED_LØPENDE_ARBEIDSFORHOLD),
            evaluering.evalueringÅrsaker,
        )
    }

    @Test
    fun `Lovlig opphold blir ikke oppfylt for mor med EØS medlemskap, annen forelder er tredjelandsborger`() {
        val adresse = opprettAdresse(adressenavn = "Osloveien", husnummer = "123", postnummer = "0245")
        val evaluering = vilkår.vurderVilkår(
            person = eøsBorger.copy().apply {
                arbeidsforhold = mutableListOf()
                bostedsadresser =
                    mutableListOf(adresse)
            },
            annenForelder = tredjelandsborger.copy().apply {
                bostedsadresser =
                    mutableListOf(adresse)
            },
        ).evaluering
        assertEquals(Resultat.IKKE_OPPFYLT, evaluering.resultat)
        assertEquals(
            listOf(VilkårIkkeOppfyltÅrsak.EØS_MEDFORELDER_TREDJELANDSBORGER),
            evaluering.evalueringÅrsaker,
        )
    }

    @Test
    fun `Lovlig opphold blir ikke oppfylt for mor med EØS medlemskap, annen forelder er statsløs`() {
        val adresse = opprettAdresse(adressenavn = "Osloveien", husnummer = "123", postnummer = "0245")
        val evaluering = vilkår.vurderVilkår(
            person = eøsBorger.copy().apply {
                arbeidsforhold = mutableListOf()
                bostedsadresser =
                    mutableListOf(adresse)
            },
            annenForelder = statsløsPerson.copy().apply {
                bostedsadresser =
                    mutableListOf(adresse)
            },
        ).evaluering
        assertEquals(Resultat.IKKE_OPPFYLT, evaluering.resultat)
        assertEquals(
            listOf(VilkårIkkeOppfyltÅrsak.EØS_MEDFORELDER_STATSLØS),
            evaluering.evalueringÅrsaker,
        )
    }

    @Test
    fun `Lovlig opphold gir resultat JA for barn ved fødselshendelse`() {
        val evaluering = vilkår.vurderVilkår(barn).evaluering
        assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)
    }

    private fun faktaPerson(oppholdstillatelse: OPPHOLDSTILLATELSE, periode: DatoIntervallEntitet?): Person {
        return tredjelandsborger.copy(
            statsborgerskap = mutableListOf(
                GrStatsborgerskap(
                    landkode = "ANG",
                    medlemskap = Medlemskap.TREDJELANDSBORGER,
                    person = tredjelandsborger,
                ),
            ),
            opphold = mutableListOf(
                GrOpphold(
                    gyldigPeriode = periode,
                    type = oppholdstillatelse,
                    person = tredjelandsborger,
                ),
            ),
        )
    }

    companion object {

        val vilkår = Vilkår.LOVLIG_OPPHOLD
        val tredjelandsborger = tilfeldigPerson(personType = PersonType.SØKER).apply {
            statsborgerskap = mutableListOf(
                GrStatsborgerskap(
                    landkode = "ANG",
                    medlemskap = Medlemskap.TREDJELANDSBORGER,
                    person = this,
                ),
            )
        }
        val eøsBorger = tilfeldigPerson(personType = PersonType.SØKER).apply {
            statsborgerskap = mutableListOf(
                GrStatsborgerskap(
                    landkode = "POL",
                    medlemskap = Medlemskap.EØS,
                    person = this,
                ),
            )
        }
        val annenForelderNordiskBorger = tilfeldigPerson(personType = PersonType.SØKER).apply {
            statsborgerskap = mutableListOf(
                GrStatsborgerskap(
                    landkode = "NOR",
                    medlemskap = Medlemskap.NORDEN,
                    person = this,
                ),
            )
        }
        val annenForelderEØS = tilfeldigPerson(personType = PersonType.SØKER).apply {
            statsborgerskap = mutableListOf(
                GrStatsborgerskap(
                    landkode = "POL",
                    medlemskap = Medlemskap.EØS,
                    person = this,
                ),
            )
        }
        val statsløsPerson = tilfeldigPerson(personType = PersonType.SØKER).apply {
            statsborgerskap = mutableListOf(
                GrStatsborgerskap(
                    landkode = "XXX",
                    medlemskap = Medlemskap.STATSLØS,
                    person = this,
                ),
            )
        }
        val ukjentStatsborger = tilfeldigPerson(personType = PersonType.SØKER).apply {
            statsborgerskap = mutableListOf(
                GrStatsborgerskap(
                    landkode = "XUK",
                    medlemskap = Medlemskap.UKJENT,
                    person = this,
                ),
            )
        }

        val barn = tilfeldigPerson(personType = PersonType.BARN)
    }
}
