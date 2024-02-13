package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.arbeidsforhold.GrArbeidsforhold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrUkjentBosted
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrVegadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.GyldigVilkårsperiode
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class VilkårVurderingTest(
    @Autowired
    private val behandlingService: BehandlingService,

    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,

    @Autowired
    private val databaseCleanupService: DatabaseCleanupService,

    @Autowired
    private val personidentService: PersonidentService,

    @Autowired
    private val vilkårsvurderingForNyBehandlingService: VilkårsvurderingForNyBehandlingService,

) : AbstractSpringIntegrationTest() {

    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `Henting og evaluering av oppfylte vilkår gir rett antall samlede resultater`() {
        val fnr = randomFnr()
        val barnFnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(fagsak, årsak = BehandlingÅrsak.FØDSELSHENDELSE, skalBehandlesAutomatisk = true),
        )

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                fnr,
                listOf(barnFnr),
                søkerAktør = personidentService.hentOgLagreAktør(fnr, true),
                barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true),
            )
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val vilkårsvurdering =
            vilkårsvurderingForNyBehandlingService.initierVilkårsvurderingForBehandling(behandling, false, null)

        val forventetAntallVurderteVilkår =
            Vilkår.hentVilkårFor(personType = PersonType.BARN, fagsakType = FagsakType.NORMAL, behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR).size +
                Vilkår.hentVilkårFor(personType = PersonType.SØKER, fagsakType = FagsakType.NORMAL, behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR).size
        assertEquals(
            forventetAntallVurderteVilkår,
            vilkårsvurdering.personResultater.flatMap { personResultat -> personResultat.vilkårResultater }.size,
        )
    }

    @Test
    fun `Sjekk gyldig vilkårsperiode`() {
        val ubegrensetGyldigVilkårsperiode = GyldigVilkårsperiode()
        assertTrue(ubegrensetGyldigVilkårsperiode.gyldigFor(LocalDate.now()))

        val begrensetGyldigVilkårsperiode = GyldigVilkårsperiode(
            gyldigFom = LocalDate.now().minusDays(5),
            gyldigTom = LocalDate.now().plusDays(5),
        )
        assertTrue(begrensetGyldigVilkårsperiode.gyldigFor(LocalDate.now()))
        assertTrue(begrensetGyldigVilkårsperiode.gyldigFor(LocalDate.now().minusDays(5)))
        assertFalse(begrensetGyldigVilkårsperiode.gyldigFor(LocalDate.now().minusDays(6)))
        assertTrue(begrensetGyldigVilkårsperiode.gyldigFor(LocalDate.now().plusDays(5)))
        assertFalse(begrensetGyldigVilkårsperiode.gyldigFor(LocalDate.now().plusDays(6)))
    }

    private fun genererPerson(
        type: PersonType,
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        grBostedsadresse: GrBostedsadresse? = null,
        kjønn: Kjønn = Kjønn.KVINNE,
        sivilstand: SIVILSTAND = SIVILSTAND.UGIFT,
    ): Person {
        val fnr = randomFnr()
        return Person(
            aktør = randomAktør(fnr),
            type = type,
            personopplysningGrunnlag = personopplysningGrunnlag,
            fødselsdato = LocalDate.of(1991, 1, 1),
            navn = "navn",
            kjønn = kjønn,
            bostedsadresser = grBostedsadresse?.let { mutableListOf(grBostedsadresse) } ?: mutableListOf(),
        )
            .apply {
                this.sivilstander =
                    mutableListOf(GrSivilstand(type = sivilstand, person = this, fom = LocalDate.of(1991, 1, 1)))
            }
    }

    @Test
    fun `Sjekk barn bor med søker`() {
        val søkerAddress = GrVegadresse(
            1234,
            "11",
            "B",
            "H022",
            "St. Olavsvegen",
            "1232",
            "whatever",
            "4322",
        )
        val barnAddress = GrVegadresse(
            1235,
            "11",
            "B",
            "H024",
            "St. Olavsvegen",
            "1232",
            "whatever",
            "4322",
        )
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 1)

        val søker = genererPerson(PersonType.SØKER, personopplysningGrunnlag, søkerAddress)
        personopplysningGrunnlag.personer.add(søker)

        val barn1 = genererPerson(PersonType.BARN, personopplysningGrunnlag, søkerAddress, Kjønn.MANN)
        personopplysningGrunnlag.personer.add(barn1)

        val barn2 = genererPerson(PersonType.BARN, personopplysningGrunnlag, barnAddress, Kjønn.MANN)
        personopplysningGrunnlag.personer.add(barn2)

        val barn3 = genererPerson(PersonType.BARN, personopplysningGrunnlag, null, Kjønn.MANN)
        personopplysningGrunnlag.personer.add(barn3)

        assertEquals(Resultat.OPPFYLT, Vilkår.BOR_MED_SØKER.vurderVilkår(barn1, LocalDate.now()).resultat)
        assertEquals(Resultat.IKKE_OPPFYLT, Vilkår.BOR_MED_SØKER.vurderVilkår(barn2, LocalDate.now()).resultat)
        assertEquals(Resultat.IKKE_OPPFYLT, Vilkår.BOR_MED_SØKER.vurderVilkår(barn3, LocalDate.now()).resultat)
    }

    @Test
    fun `Sjekk barn bor med mor når mor har bodd på adressen lengre enn barn`() {
        val søkerAddress = GrVegadresse(
            1234,
            "11",
            "B",
            "H022",
            "St. Olavsvegen",
            "1232",
            "whatever",
            "4322",
        )
            .apply {
                periode = DatoIntervallEntitet(LocalDate.now().minusYears(10))
            }

        val barnAddress = GrVegadresse(
            1234,
            "11",
            "B",
            "H024",
            "St. Olavsvegen",
            "1232",
            "whatever",
            "4322",
        )
            .apply {
                periode = DatoIntervallEntitet(LocalDate.now().minusMonths(1))
            }

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 1)

        val søker = genererPerson(PersonType.SØKER, personopplysningGrunnlag, søkerAddress)
        personopplysningGrunnlag.personer.add(søker)

        val barn1 = genererPerson(PersonType.BARN, personopplysningGrunnlag, barnAddress, Kjønn.MANN)
        personopplysningGrunnlag.personer.add(barn1)

        assertEquals(Resultat.OPPFYLT, Vilkår.BOR_MED_SØKER.vurderVilkår(barn1, LocalDate.now()).resultat)
    }

    @Test
    fun `Negativ vurdering - Barn og søker har ikke adresse angitt`() {
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 2)
        val søker = genererPerson(PersonType.SØKER, personopplysningGrunnlag, null)
        personopplysningGrunnlag.personer.add(søker)

        val barn = genererPerson(PersonType.BARN, personopplysningGrunnlag, null)
        personopplysningGrunnlag.personer.add(barn)

        assertEquals(Resultat.IKKE_OPPFYLT, Vilkår.BOR_MED_SØKER.vurderVilkår(barn, barn.fødselsdato).resultat)
    }

    @Test
    fun `Skal kaste exception - ingen søker`() {
        val søkerAddress = GrVegadresse(
            1234,
            "11",
            "B",
            "H022",
            "St. Olavsvegen",
            "1232",
            "whatever",
            "4322",
        )

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 4)
        val barn = genererPerson(PersonType.BARN, personopplysningGrunnlag, søkerAddress, Kjønn.MANN)
        val feilregistrertSøker = genererPerson(PersonType.BARN, personopplysningGrunnlag, søkerAddress, Kjønn.KVINNE)
        personopplysningGrunnlag.personer.add(barn)
        personopplysningGrunnlag.personer.add(feilregistrertSøker)

        assertThrows(IllegalStateException::class.java) {
            Vilkår.BOR_MED_SØKER.vurderVilkår(barn, LocalDate.now()).resultat
        }
    }

    @Test
    fun `Negativ vurdering - søker har ukjentadresse`() {
        val ukjentbosted = GrUkjentBosted("Oslo")
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 6)
        val søker = genererPerson(PersonType.SØKER, personopplysningGrunnlag, ukjentbosted)
        personopplysningGrunnlag.personer.add(søker)
        val barn = genererPerson(PersonType.BARN, personopplysningGrunnlag, ukjentbosted)
        personopplysningGrunnlag.personer.add(barn)

        assertEquals(Resultat.IKKE_OPPFYLT, Vilkår.BOR_MED_SØKER.vurderVilkår(barn, LocalDate.now()).resultat)
    }

    @Test
    fun `Sjekk at barn er ugift`() {
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 6)
        val barn = genererPerson(PersonType.BARN, personopplysningGrunnlag)
        personopplysningGrunnlag.personer.add(barn)

        assertEquals(Resultat.OPPFYLT, Vilkår.GIFT_PARTNERSKAP.vurderVilkår(barn, LocalDate.now()).resultat)
    }

    @Test
    fun `Negativ vurdering - barn er gift`() {
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 6)
        val barn = genererPerson(PersonType.BARN, personopplysningGrunnlag, sivilstand = SIVILSTAND.GIFT)
        personopplysningGrunnlag.personer.add(barn)

        assertEquals(
            Resultat.IKKE_OPPFYLT,
            Vilkår.GIFT_PARTNERSKAP.vurderVilkår(barn, LocalDate.now()).resultat,
        )
    }

    @Test
    fun `Negativ vurdering - barn har vært gift`() {
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 6)
        val barn = genererPerson(PersonType.BARN, personopplysningGrunnlag, sivilstand = SIVILSTAND.GIFT).apply {
            sivilstander = mutableListOf(
                GrSivilstand(fom = LocalDate.now().minusMonths(2), type = SIVILSTAND.GIFT, person = this),
                GrSivilstand(fom = LocalDate.now().minusMonths(1), type = SIVILSTAND.UGIFT, person = this),
            )
        }
        personopplysningGrunnlag.personer.add(barn)

        assertEquals(
            Resultat.IKKE_OPPFYLT,
            Vilkår.GIFT_PARTNERSKAP.vurderVilkår(barn).resultat,
        )
    }

    @Test
    fun `Negativ vurdering - søker er ikke bosatt i norge`() {
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 6)
        val søker = genererPerson(PersonType.SØKER, personopplysningGrunnlag, sivilstand = SIVILSTAND.GIFT)
        personopplysningGrunnlag.personer.add(søker)

        assertEquals(Resultat.IKKE_OPPFYLT, Vilkår.BOSATT_I_RIKET.vurderVilkår(søker, LocalDate.now()).resultat)
    }

    @Test
    fun `Negativ vurdering - søker har ikke vært bosatt i norge siden barnets fødselsdato`() {
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 6)
        val søker = genererPerson(PersonType.SØKER, personopplysningGrunnlag, sivilstand = SIVILSTAND.GIFT).apply {
            bostedsadresser = mutableListOf(
                GrVegadresse(
                    1234,
                    "11",
                    "B",
                    "H022",
                    "St. Olavsvegen",
                    "1232",
                    "whatever",
                    "4322",
                )
                    .apply {
                        periode = DatoIntervallEntitet(LocalDate.now().minusDays(10))
                    },
            )
        }
        personopplysningGrunnlag.personer.add(søker)

        assertEquals(
            Resultat.IKKE_OPPFYLT,
            Vilkår.BOSATT_I_RIKET.vurderVilkår(søker, LocalDate.now().minusMonths(1)).resultat,
        )
    }

    @Test
    fun `Sjekk at mor er bosatt i norge`() {
        val vegadresse = GrVegadresse(
            1234,
            "11",
            "B",
            "H022",
            "St. Olavsvegen",
            "1232",
            "whatever",
            "4322",
        ).apply {
            periode = DatoIntervallEntitet(TIDENES_MORGEN)
        }
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 6)
        val mor = genererPerson(PersonType.SØKER, personopplysningGrunnlag, vegadresse)
        personopplysningGrunnlag.personer.add(mor)

        assertEquals(Resultat.OPPFYLT, Vilkår.BOSATT_I_RIKET.vurderVilkår(mor, LocalDate.now()).resultat)
    }

    @Test
    fun `Sjekk at mor har vært bosatt i norge siden barnet ble født`() {
        val vegadresse = GrVegadresse(
            matrikkelId = 1234,
            husnummer = "11",
            husbokstav = "B",
            bruksenhetsnummer = "H022",
            adressenavn = "St. Olavsvegen",
            kommunenummer = "1232",
            tilleggsnavn = "whatever",
            postnummer = "4322",
        ).apply {
            periode = DatoIntervallEntitet(LocalDate.now().minusMonths(10))
        }

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 6)
        val mor = genererPerson(PersonType.SØKER, personopplysningGrunnlag, vegadresse)
        personopplysningGrunnlag.personer.add(mor)

        assertEquals(
            Resultat.OPPFYLT,
            Vilkår.BOSATT_I_RIKET.vurderVilkår(mor, LocalDate.now().minusMonths(3)).resultat,
        )
    }

    @Test
    fun `Negativ vurdering - mor har bare adresse deler av perioden siden barnet ble født`() {
        val vegadresser = listOf(
            DatoIntervallEntitet(LocalDate.now().minusMonths(7), LocalDate.now().minusMonths(4)),
            DatoIntervallEntitet(LocalDate.now().minusMonths(2)),
        ).map {
            GrVegadresse(
                matrikkelId = 1234,
                husnummer = "11",
                husbokstav = "B",
                bruksenhetsnummer = "H022",
                adressenavn = "St. Olavsvegen",
                kommunenummer = "1232",
                tilleggsnavn = "whatever",
                postnummer = "4322",
            ).apply {
                periode = it
            }
        }

        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 6)
        val mor = genererPerson(PersonType.SØKER, personopplysningGrunnlag).apply {
            bostedsadresser = vegadresser.toMutableList()
        }
        personopplysningGrunnlag.personer.add(mor)

        assertEquals(
            Resultat.IKKE_OPPFYLT,
            Vilkår.BOSATT_I_RIKET.vurderVilkår(mor, LocalDate.now().minusMonths(6)).resultat,
        )
    }

    @Test
    fun `Negativ vurdering - mor er ikke bosatt i norge`() {
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 6)
        val mor = genererPerson(PersonType.SØKER, personopplysningGrunnlag, sivilstand = SIVILSTAND.GIFT)
        personopplysningGrunnlag.personer.add(mor)

        assertEquals(Resultat.IKKE_OPPFYLT, Vilkår.BOSATT_I_RIKET.vurderVilkår(mor, LocalDate.now()).resultat)
    }

    @Test
    fun `Lovlig opphold - nordisk statsborger`() {
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 6)
        val person = genererPerson(PersonType.BARN, personopplysningGrunnlag, sivilstand = SIVILSTAND.GIFT)
            .also {
                it.statsborgerskap =
                    mutableListOf(
                        GrStatsborgerskap(
                            gyldigPeriode = DatoIntervallEntitet(
                                tom = null,
                                fom = LocalDate.now().minusYears(1),
                            ),
                            landkode = "DNK",
                            medlemskap = Medlemskap.NORDEN,
                            person = it,
                        ),
                    )
            }

        assertEquals(Resultat.OPPFYLT, Vilkår.LOVLIG_OPPHOLD.vurderVilkår(person, LocalDate.now()).resultat)
    }

    @Test
    @Disabled
    fun `Mor er fra EØS og har et løpende arbeidsforhold - lovlig opphold, skal evalueres til Ja`() {
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 6)
        val person = genererPerson(PersonType.SØKER, personopplysningGrunnlag, sivilstand = SIVILSTAND.GIFT)
            .also {
                it.statsborgerskap = mutableListOf(
                    GrStatsborgerskap(
                        gyldigPeriode = DatoIntervallEntitet(LocalDate.now().minusYears(1)),
                        landkode = "BEL",
                        medlemskap = Medlemskap.EØS,
                        person = it,
                    ),
                )
                it.arbeidsforhold = løpendeArbeidsforhold(it)
            }

        assertEquals(Resultat.OPPFYLT, Vilkår.LOVLIG_OPPHOLD.vurderVilkår(person, LocalDate.now()).resultat)
        assertEquals(
            "Mor er EØS-borger, men har et løpende arbeidsforhold i Norge.",
            Vilkår.LOVLIG_OPPHOLD.vurderVilkår(person, LocalDate.now()).evaluering.begrunnelse,
        )
    }

    @Test
    @Disabled
    fun `Mor er fra EØS og har ikke et løpende arbeidsforhold, bor sammen med annen forelder som er fra norden - lovlig opphold, skal evalueres til Ja`() {
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 6)
        val bostedsadresse = Bostedsadresse(vegadresse = Vegadresse(0, null, null, "32E", null, null, null, null))
        val person = genererPerson(
            PersonType.SØKER,
            personopplysningGrunnlag,
            sivilstand = SIVILSTAND.GIFT,
        )
            .also {
                it.statsborgerskap = mutableListOf(
                    GrStatsborgerskap(
                        gyldigPeriode = DatoIntervallEntitet(LocalDate.now().minusYears(1)),
                        landkode = "BEL",
                        medlemskap = Medlemskap.EØS,
                        person = it,
                    ),
                )
                it.bostedsadresser =
                    mutableListOf(GrBostedsadresse.fraBostedsadresse(bostedsadresse, it))
            }
        val annenForelder = opprettAnnenForelder(personopplysningGrunnlag, bostedsadresse, Medlemskap.NORDEN)
        person.personopplysningGrunnlag.personer.add(annenForelder)

        assertEquals(Resultat.OPPFYLT, Vilkår.LOVLIG_OPPHOLD.vurderVilkår(person, LocalDate.now()).resultat)
        assertEquals(
            "Annen forelder er norsk eller nordisk statsborger.",
            Vilkår.LOVLIG_OPPHOLD.vurderVilkår(person, LocalDate.now()).evaluering.begrunnelse,
        )
    }

    @Test
    @Disabled
    fun `Mor er fra EØS og har ikke et løpende arbeidsforhold, bor sammen med annen forelder som er tredjelandsborger - lovlig opphold, skal evalueres til Nei`() {
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 6)
        val bostedsadresse = Bostedsadresse(vegadresse = Vegadresse(0, null, null, "32E", null, null, null, null))
        val person = genererPerson(
            PersonType.SØKER,
            personopplysningGrunnlag,
            sivilstand = SIVILSTAND.GIFT,
        )
            .also {
                it.statsborgerskap = mutableListOf(
                    GrStatsborgerskap(
                        gyldigPeriode = DatoIntervallEntitet(LocalDate.now().minusYears(1)),
                        landkode = "BEL",
                        medlemskap = Medlemskap.EØS,
                        person = it,
                    ),
                )
                it.bostedsadresser =
                    mutableListOf(GrBostedsadresse.fraBostedsadresse(bostedsadresse, it))
            }
        val annenForelder = opprettAnnenForelder(personopplysningGrunnlag, bostedsadresse, Medlemskap.TREDJELANDSBORGER)

        person.personopplysningGrunnlag.personer.add(annenForelder)

        assertEquals(
            Resultat.IKKE_OPPFYLT,
            Vilkår.LOVLIG_OPPHOLD.vurderVilkår(person, LocalDate.now()).resultat,
        )
        assertEquals(
            "Mor har ikke lovlig opphold - EØS borger. Mor er ikke registrert med arbeidsforhold. Medforelder er tredjelandsborger.",
            Vilkår.LOVLIG_OPPHOLD.vurderVilkår(person, LocalDate.now()).evaluering.begrunnelse,
        )
    }

    @Test
    @Disabled
    fun `Mor er fra EØS og har ikke et løpende arbeidsforhold, bor sammen med annen forelder som er statsløs - lovlig opphold, skal evalueres til Nei`() {
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 6)
        val bostedsadresse = Bostedsadresse(vegadresse = Vegadresse(0, null, null, "32E", null, null, null, null))
        val person = genererPerson(
            PersonType.SØKER,
            personopplysningGrunnlag,
            sivilstand = SIVILSTAND.GIFT,
        )
            .also {
                it.statsborgerskap = mutableListOf(
                    GrStatsborgerskap(
                        gyldigPeriode = DatoIntervallEntitet(LocalDate.now().minusYears(1)),
                        landkode = "BEL",
                        medlemskap = Medlemskap.EØS,
                        person = it,
                    ),
                )
                it.bostedsadresser =
                    mutableListOf(GrBostedsadresse.fraBostedsadresse(bostedsadresse, it))
            }
        val annenForelder = opprettAnnenForelder(personopplysningGrunnlag, bostedsadresse, Medlemskap.UKJENT)
        person.personopplysningGrunnlag.personer.add(annenForelder)

        assertEquals(
            Resultat.IKKE_OPPFYLT,
            Vilkår.LOVLIG_OPPHOLD.vurderVilkår(person, LocalDate.now()).resultat,
        )
        assertEquals(
            "Mor har ikke lovlig opphold - EØS borger. Mor er ikke registrert med arbeidsforhold. Medforelder er statsløs.",
            Vilkår.LOVLIG_OPPHOLD.vurderVilkår(person, LocalDate.now()).evaluering.begrunnelse,
        )
    }

    @Test
    @Disabled
    fun `Mor er fra EØS og har ikke et løpende arbeidsforhold, bor sammen med annen forelder fra EØS som har løpende arbeidsforhold - lovlig opphold, skal evalueres til Ja`() {
        val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 6)
        val bostedsadresse = Bostedsadresse(vegadresse = Vegadresse(0, null, null, "32E", null, null, null, null))
        val person = genererPerson(
            PersonType.SØKER,
            personopplysningGrunnlag,
            sivilstand = SIVILSTAND.GIFT,
        )
            .also {
                it.statsborgerskap = mutableListOf(
                    GrStatsborgerskap(
                        gyldigPeriode = DatoIntervallEntitet(LocalDate.now().minusYears(1)),
                        landkode = "BEL",
                        medlemskap = Medlemskap.EØS,
                        person = it,
                    ),
                )
                it.bostedsadresser =
                    mutableListOf(GrBostedsadresse.fraBostedsadresse(bostedsadresse, it))
            }
        val annenForelder = opprettAnnenForelder(personopplysningGrunnlag, bostedsadresse, Medlemskap.EØS)
            .also { it.arbeidsforhold = løpendeArbeidsforhold(it) }
        person.personopplysningGrunnlag.personer.add(annenForelder)

        assertEquals(Resultat.OPPFYLT, Vilkår.LOVLIG_OPPHOLD.vurderVilkår(person, LocalDate.now()).resultat)
        assertEquals(
            "Annen forelder er fra EØS, men har et løpende arbeidsforhold i Norge.",
            Vilkår.LOVLIG_OPPHOLD.vurderVilkår(person, LocalDate.now()).evaluering.begrunnelse,
        )
    }

    private fun opprettAnnenForelder(
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        bostedsadresse: Bostedsadresse,
        medlemskap: Medlemskap,
    ): Person {
        return genererPerson(
            PersonType.ANNENPART,
            personopplysningGrunnlag,
            sivilstand = SIVILSTAND.GIFT,
        )
            .also {
                it.statsborgerskap = mutableListOf(
                    GrStatsborgerskap(
                        gyldigPeriode = DatoIntervallEntitet(LocalDate.now().minusYears(1)),
                        landkode = "LOL",
                        medlemskap = medlemskap,
                        person = it,
                    ),
                )
                it.bostedsadresser =
                    mutableListOf(GrBostedsadresse.fraBostedsadresse(bostedsadresse, it))
            }
    }

    private fun løpendeArbeidsforhold(person: Person) = mutableListOf(
        GrArbeidsforhold(
            periode = DatoIntervallEntitet(
                LocalDate.now()
                    .minusYears(
                        1,
                    ),
            ),
            arbeidsgiverId = "998877665",
            arbeidsgiverType = "Organisasjon",
            person = person,
        ),
    )
}
