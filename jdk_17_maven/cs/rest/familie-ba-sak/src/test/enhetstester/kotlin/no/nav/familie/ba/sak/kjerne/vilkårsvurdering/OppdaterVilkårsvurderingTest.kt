package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingUtils.flyttResultaterTilInitielt
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingUtils.lagFjernAdvarsel
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppdaterVilkårsvurderingTest {

    @Test
    fun `Skal legge til nytt vilkår`() {
        val fnr1 = randomFnr()
        val aktørId1 = randomAktør()
        val behandling = lagBehandling()
        val resA = lagVilkårsvurdering(behandling = behandling, fnrAktør = listOf(Pair(fnr1, aktørId1)))
        val resB = lagVilkårsvurderingResultatB(behandling = behandling, fnrAktør = listOf(Pair(fnr1, aktørId1)))

        val (oppdatert, gammelt) = flyttResultaterTilInitielt(resB, resA)
        Assertions.assertEquals(3, oppdatert.personResultater.first().vilkårResultater.size)
        Assertions.assertEquals(
            Resultat.OPPFYLT,
            oppdatert.personResultater.first()
                .vilkårResultater.find { it.vilkårType == Vilkår.BOSATT_I_RIKET }?.resultat,
        )
        Assertions.assertTrue(gammelt.personResultater.isEmpty())
    }

    @Test
    fun `Skal fjerne vilkår`() {
        val fnr1 = randomFnr()
        val aktørId1 = randomAktør()
        val behandling = lagBehandling()
        val resA = lagVilkårsvurderingResultatB(behandling = behandling, fnrAktør = listOf(Pair(fnr1, aktørId1)))
        val resB = lagVilkårsvurdering(behandling = behandling, fnrAktør = listOf(Pair(fnr1, aktørId1)))

        val (oppdatert, gammelt) = flyttResultaterTilInitielt(resB, resA)
        Assertions.assertEquals(2, oppdatert.personResultater.first().vilkårResultater.size)
        Assertions.assertEquals(
            Resultat.OPPFYLT,
            oppdatert.personResultater.first()
                .vilkårResultater.find { it.vilkårType == Vilkår.BOSATT_I_RIKET }?.resultat,
        )
        Assertions.assertEquals(1, gammelt.personResultater.size)
        Assertions.assertEquals(1, gammelt.personResultater.first().vilkårResultater.size)
    }

    @Test
    fun `Skal legge til person på vilkårsvurdering`() {
        val fnr1 = randomFnr()
        val fnr2 = randomFnr()
        val aktørId1 = randomAktør()
        val aktørId2 = randomAktør()
        val behandling = lagBehandling()
        val resA = lagVilkårsvurdering(behandling = behandling, fnrAktør = listOf(Pair(fnr1, aktørId1)))
        val resB = lagVilkårsvurdering(
            behandling = behandling,
            fnrAktør = listOf(Pair(fnr1, aktørId1), Pair(fnr2, aktørId2)),
        )

        val (oppdatert, gammelt) = flyttResultaterTilInitielt(resB, resA)
        Assertions.assertEquals(2, oppdatert.personResultater.size)
        Assertions.assertEquals(0, gammelt.personResultater.size)
    }

    @Test
    fun `Skal fjerne person på vilkårsvurdering`() {
        val fnr1 = randomFnr()
        val fnr2 = randomFnr()
        val aktørId1 = randomAktør()
        val aktørId2 = randomAktør()
        val behandling = lagBehandling()
        val resA = lagVilkårsvurdering(
            behandling = behandling,
            fnrAktør = listOf(Pair(fnr1, aktørId1), Pair(fnr2, aktørId2)),
        )
        val resB = lagVilkårsvurdering(behandling = behandling, fnrAktør = listOf(Pair(fnr1, aktørId1)))

        val (oppdatert, gammelt) = flyttResultaterTilInitielt(resB, resA)
        Assertions.assertEquals(1, oppdatert.personResultater.size)
        Assertions.assertEquals(1, gammelt.personResultater.size)
    }

    @Test
    fun `Skal lage advarsel tekst`() {
        val fnr1 = randomFnr()
        val fnr2 = randomFnr()
        val aktørId1 = tilAktør(fnr1)
        val aktørId2 = tilAktør(fnr2)
        val behandling = lagBehandling()
        val resultat1 = lagVilkårsvurdering(
            behandling = behandling,
            fnrAktør = listOf(Pair(fnr1, aktørId1), Pair(fnr2, aktørId2)),
        )
        val resultat2 = lagVilkårsvurdering(behandling = behandling, fnrAktør = listOf(Pair(fnr2, aktørId2)))

        val resterende = flyttResultaterTilInitielt(resultat2, resultat1).second
        val fjernedeVilkår = resultat1.personResultater.first().vilkårResultater.toList()
        val generertAdvarsel = lagFjernAdvarsel(resterende.personResultater)

        Assertions.assertEquals(
            "Du har gjort endringer i behandlingsgrunnlaget. Dersom du går videre vil vilkår for følgende personer fjernes:\n" +
                fnr1 + ":\n" +
                "   - " + fjernedeVilkår[0].vilkårType.beskrivelse + "\n" +
                "   - " + fjernedeVilkår[1].vilkårType.beskrivelse + "\n",
            generertAdvarsel,
        )
    }

    @Test
    fun `Skal ha med tomt vilkår på person hvis vilkåret ble avslått forrige behandling`() {
        val søkerAktørId = randomAktør()
        val nyBehandling = lagBehandling()
        val forrigeBehandling = lagBehandling()

        val init =
            lagBasicVilkårsvurdering(
                behandling = nyBehandling,
                personer = listOf(
                    lagPerson(type = PersonType.SØKER, aktør = søkerAktørId),
                    lagPerson(type = PersonType.BARN),
                ),
            )
        val aktivMedBosattIRiketIkkeOppfylt = Vilkårsvurdering(behandling = forrigeBehandling)
        val personResultat =
            PersonResultat(
                vilkårsvurdering = aktivMedBosattIRiketIkkeOppfylt,
                aktør = søkerAktørId,
            )
        val bosattIRiketVilkårResultater =
            setOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    personResultat = personResultat,
                    resultat = Resultat.IKKE_OPPFYLT,
                    periodeFom = LocalDate.now().minusYears(2),
                    periodeTom = LocalDate.now().minusYears(1),
                ),
            )
        personResultat.setSortedVilkårResultater(bosattIRiketVilkårResultater)
        aktivMedBosattIRiketIkkeOppfylt.personResultater = setOf(personResultat)

        val (nyInit, nyAktiv) = flyttResultaterTilInitielt(
            initiellVilkårsvurdering = init,
            aktivVilkårsvurdering = aktivMedBosattIRiketIkkeOppfylt,
        )

        val nyInitBosattIRiketVilkår =
            nyInit.personResultater.find { it.aktør == søkerAktørId }?.vilkårResultater?.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }
                ?: emptyList()

        Assertions.assertTrue(nyInitBosattIRiketVilkår.isNotEmpty())
        Assertions.assertTrue(nyInitBosattIRiketVilkår.single().resultat == Resultat.IKKE_VURDERT)
        Assertions.assertTrue(nyAktiv.personResultater.isEmpty())
    }

    @Test
    fun `Skal ha med oppfylte perioder fra vilkår på person hvis vilkåret ble både avslått og innvilget forrige behandling`() {
        val søkerAktørId = randomAktør()
        val nyBehandling = lagBehandling()
        val forrigeBehandling = lagBehandling()

        val init =
            lagBasicVilkårsvurdering(
                behandling = nyBehandling,
                personer = listOf(
                    lagPerson(type = PersonType.SØKER, aktør = søkerAktørId),
                    lagPerson(type = PersonType.BARN),
                ),
            )
        val aktivMedBosattIRiketDelvisIkkeOppfylt = Vilkårsvurdering(behandling = forrigeBehandling)
        val personResultat =
            PersonResultat(
                vilkårsvurdering = aktivMedBosattIRiketDelvisIkkeOppfylt,
                aktør = søkerAktørId,
            )
        val bosattIRiketVilkårResultater =
            setOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    personResultat = personResultat,
                    resultat = Resultat.IKKE_OPPFYLT,
                    periodeFom = LocalDate.now().minusYears(2),
                    periodeTom = LocalDate.now().minusYears(1),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    personResultat = personResultat,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.now(),
                    periodeTom = LocalDate.now().plusYears(1),
                ),
            )
        personResultat.setSortedVilkårResultater(bosattIRiketVilkårResultater)
        aktivMedBosattIRiketDelvisIkkeOppfylt.personResultater = setOf(personResultat)

        val (nyInit, nyAktiv) = flyttResultaterTilInitielt(
            initiellVilkårsvurdering = init,
            aktivVilkårsvurdering = aktivMedBosattIRiketDelvisIkkeOppfylt,
        )

        val nyInitBosattIRiketVilkår =
            nyInit.personResultater.find { it.aktør == søkerAktørId }?.vilkårResultater?.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }
                ?: emptyList()

        Assertions.assertTrue(nyInitBosattIRiketVilkår.isNotEmpty())
        Assertions.assertTrue(nyInitBosattIRiketVilkår.single().resultat == Resultat.OPPFYLT)
        Assertions.assertTrue(nyAktiv.personResultater.isEmpty())
    }

    @Test
    fun `Skal beholde vilkår om utvidet barnetrygd når forrige behandling inneholdt utvidet-vilkåret, men inneværende behandling er ordinær`() {
        val søkerAktørId = randomAktør()
        val behandling = lagBehandling()

        val initUtenUtvidetVilkår =
            lagVilkårsvurderingMedForskjelligeTyperVilkår(søkerAktørId, behandling, listOf())
        val aktivMedUtvidetVilkår =
            lagVilkårsvurderingMedForskjelligeTyperVilkår(
                søkerAktørId,
                behandling,
                listOf(Vilkår.UTVIDET_BARNETRYGD),
            )

        val (nyInit, nyAktiv) = flyttResultaterTilInitielt(
            initiellVilkårsvurdering = initUtenUtvidetVilkår,
            aktivVilkårsvurdering = aktivMedUtvidetVilkår,
            aktørerMedUtvidetAndelerIForrigeBehandling = listOf(søkerAktørId),
            løpendeUnderkategori = BehandlingUnderkategori.UTVIDET,
        )

        val nyInitInnholderUtvidetVilkår =
            nyInit.personResultater.first().vilkårResultater.any { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.UTVIDET_BARNETRYGD }

        Assertions.assertTrue(nyInitInnholderUtvidetVilkår)
        Assertions.assertTrue(nyAktiv.personResultater.isEmpty())
    }

    @Test
    fun `Skal beholde vilkår om utvidet barnetrygd når det eksisterer løpende sak med utvidet, men inneværende behandling er ordinær`() {
        val søkerAktørId = randomAktør()
        val behandling = lagBehandling()

        val initUtenUtvidetVilkår =
            lagVilkårsvurderingMedForskjelligeTyperVilkår(søkerAktørId, behandling, listOf())
        val aktivMedUtvidetVilkår =
            lagVilkårsvurderingMedForskjelligeTyperVilkår(
                søkerAktørId,
                behandling,
                listOf(Vilkår.UTVIDET_BARNETRYGD),
            )

        val (nyInit, nyAktiv) = flyttResultaterTilInitielt(
            initiellVilkårsvurdering = initUtenUtvidetVilkår,
            aktivVilkårsvurdering = aktivMedUtvidetVilkår,
            løpendeUnderkategori = BehandlingUnderkategori.UTVIDET,
        )

        val nyInitInnholderUtvidetVilkår =
            nyInit.personResultater.first().vilkårResultater.any { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.UTVIDET_BARNETRYGD }

        Assertions.assertTrue(nyInitInnholderUtvidetVilkår)
        Assertions.assertTrue(nyAktiv.personResultater.isEmpty())
    }

    @Test
    fun `Skal fjerne vilkår om utvidet barnetrygd når den inneværende behandlingen gjelder ordinær, og det ikke eksisterer løpende sak med utvidet, eller utvidet-vilkåret var på forrige behandling`() {
        val søkerAktørId = randomAktør()
        val behandling = lagBehandling()

        val initUtenUtvidetVilkår =
            lagVilkårsvurderingMedForskjelligeTyperVilkår(søkerAktørId, behandling, listOf())
        val aktivMedUtvidetVilkår =
            lagVilkårsvurderingMedForskjelligeTyperVilkår(
                søkerAktørId,
                behandling,
                listOf(Vilkår.UTVIDET_BARNETRYGD),
            )

        val (nyInit, nyAktiv) = flyttResultaterTilInitielt(
            initiellVilkårsvurdering = initUtenUtvidetVilkår,
            aktivVilkårsvurdering = aktivMedUtvidetVilkår,
            løpendeUnderkategori = BehandlingUnderkategori.ORDINÆR,
            aktørerMedUtvidetAndelerIForrigeBehandling = emptyList(),
        )

        val nyInitInnholderIkkeUtvidetVilkår =
            nyInit.personResultater.first().vilkårResultater.none { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.UTVIDET_BARNETRYGD }
        val nyAktivInneholderUtvidetVilkår =
            nyAktiv.personResultater.first().vilkårResultater.any { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.UTVIDET_BARNETRYGD }

        Assertions.assertTrue(nyInitInnholderIkkeUtvidetVilkår)
        Assertions.assertTrue(nyAktivInneholderUtvidetVilkår)
    }

    @Test
    fun `Skal kun kopiere over oppfylte utvidet-vilkår ved opprettelse av ny behandling, men slette alle fra aktiv`() {
        val søkerAktørId = randomAktør()
        val nyBehandling = lagBehandling()
        val forrigeBehandling = lagBehandling()

        val initUtenUtvidetVilkår =
            lagVilkårsvurderingMedForskjelligeTyperVilkår(søkerAktørId, nyBehandling, listOf())

        val aktivVilkårsvurderingMedUtvidet = Vilkårsvurdering(behandling = forrigeBehandling)
        val personResultat =
            PersonResultat(
                vilkårsvurdering = aktivVilkårsvurderingMedUtvidet,
                aktør = søkerAktørId,
            )
        val utvidetVilkårResultater =
            setOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                    personResultat = personResultat,
                    resultat = Resultat.IKKE_OPPFYLT,
                    periodeFom = LocalDate.now().minusYears(2),
                    periodeTom = LocalDate.now().minusYears(1),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                    personResultat = personResultat,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.now().minusYears(1),
                    periodeTom = LocalDate.now(),
                ),
            )
        personResultat.setSortedVilkårResultater(utvidetVilkårResultater)
        aktivVilkårsvurderingMedUtvidet.personResultater = setOf(personResultat)

        val (nyInit, nyAktiv) = flyttResultaterTilInitielt(
            initiellVilkårsvurdering = initUtenUtvidetVilkår,
            aktivVilkårsvurdering = aktivVilkårsvurderingMedUtvidet,
            løpendeUnderkategori = BehandlingUnderkategori.UTVIDET,
            aktørerMedUtvidetAndelerIForrigeBehandling = listOf(søkerAktørId),
        )

        val nyInitUtvidetVilkår =
            nyInit.personResultater.first().vilkårResultater.single { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.UTVIDET_BARNETRYGD }

        Assertions.assertTrue(nyInitUtvidetVilkår.resultat == Resultat.OPPFYLT)
        Assertions.assertTrue(nyAktiv.personResultater.isEmpty())
    }

    @Test
    fun `Skal kopiere over alle utvidet-vilkår fra aktiv vilkårsvurdering hvis den aktive vilkårsvurderingen er fra den inneværende behandlingen`() {
        val søkerAktørId = randomAktør()
        val behandling = lagBehandling()

        val initUtenUtvidetVilkår =
            lagVilkårsvurderingMedForskjelligeTyperVilkår(søkerAktørId, behandling, listOf())

        val aktivVilkårsvurderingMedUtvidet = Vilkårsvurdering(behandling = behandling)
        val personResultat =
            PersonResultat(
                vilkårsvurdering = aktivVilkårsvurderingMedUtvidet,
                aktør = søkerAktørId,
            )
        val utvidetVilkårResultater =
            setOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                    personResultat = personResultat,
                    resultat = Resultat.IKKE_OPPFYLT,
                    periodeFom = LocalDate.now().minusYears(2),
                    periodeTom = LocalDate.now().minusYears(1),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                    personResultat = personResultat,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.now().minusYears(1),
                    periodeTom = LocalDate.now(),
                ),
            )
        personResultat.setSortedVilkårResultater(utvidetVilkårResultater)
        aktivVilkårsvurderingMedUtvidet.personResultater = setOf(personResultat)

        val (nyInit, nyAktiv) = flyttResultaterTilInitielt(
            initiellVilkårsvurdering = initUtenUtvidetVilkår,
            aktivVilkårsvurdering = aktivVilkårsvurderingMedUtvidet,
            løpendeUnderkategori = BehandlingUnderkategori.UTVIDET,
            aktørerMedUtvidetAndelerIForrigeBehandling = listOf(søkerAktørId),
        )

        val nyInitUtvidetVilkår =
            nyInit.personResultater.first().vilkårResultater.filter { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.UTVIDET_BARNETRYGD }

        Assertions.assertTrue(nyInitUtvidetVilkår.size == 2)
        Assertions.assertTrue(nyAktiv.personResultater.isEmpty())
    }

    @Test
    fun `Skal ikke legge til utvidet vilkåret hvis det kun eksisterer ikke-oppfylte perioder, men fortsatt slette fra aktiv`() {
        val søkerAktørId = randomAktør()
        val nyBehandling = lagBehandling()
        val forrigeBehandling = lagBehandling()

        val initUtenUtvidetVilkår =
            lagVilkårsvurderingMedForskjelligeTyperVilkår(søkerAktørId, nyBehandling, listOf())

        val aktivVilkårsvurderingMedUtvidetIkkeOppfylt = Vilkårsvurdering(behandling = forrigeBehandling)
        val personResultat =
            PersonResultat(
                vilkårsvurdering = aktivVilkårsvurderingMedUtvidetIkkeOppfylt,
                aktør = søkerAktørId,
            )
        val utvidetVilkårResultater =
            setOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                    personResultat = personResultat,
                    resultat = Resultat.IKKE_OPPFYLT,
                    periodeFom = LocalDate.now().minusYears(2),
                    periodeTom = LocalDate.now().minusYears(1),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                    personResultat = personResultat,
                    resultat = Resultat.IKKE_OPPFYLT,
                    periodeFom = LocalDate.now().minusYears(1),
                    periodeTom = LocalDate.now(),
                ),
            )
        personResultat.setSortedVilkårResultater(utvidetVilkårResultater)
        aktivVilkårsvurderingMedUtvidetIkkeOppfylt.personResultater = setOf(personResultat)

        val (nyInit, nyAktiv) = flyttResultaterTilInitielt(
            initiellVilkårsvurdering = initUtenUtvidetVilkår,
            aktivVilkårsvurdering = aktivVilkårsvurderingMedUtvidetIkkeOppfylt,
            løpendeUnderkategori = BehandlingUnderkategori.UTVIDET,
            aktørerMedUtvidetAndelerIForrigeBehandling = emptyList(),
        )

        val nyInitInneholderIkkeUtvidetVilkår =
            nyInit.personResultater.first().vilkårResultater.none { vilkårResultat -> vilkårResultat.vilkårType == Vilkår.UTVIDET_BARNETRYGD }

        Assertions.assertTrue(nyInitInneholderIkkeUtvidetVilkår)
        Assertions.assertTrue(nyAktiv.personResultater.isEmpty())
    }

    @Test
    fun `Skal beholde andreVurderinger lagt til på inneværende behandling`() {
        val søkerAktørId = randomAktør()
        val nyBehandling = lagBehandling()

        val initiellVilkårsvurderingUtenAndreVurderinger =
            lagBasicVilkårsvurdering(
                behandling = nyBehandling,
                personer = listOf(
                    lagPerson(type = PersonType.SØKER, aktør = søkerAktørId),
                    lagPerson(type = PersonType.BARN),
                ),
            )
        val aktivVilkårsvurdering = initiellVilkårsvurderingUtenAndreVurderinger.copy()
        aktivVilkårsvurdering.personResultater.find { it.erSøkersResultater() }!!
            .leggTilBlankAnnenVurdering(AnnenVurderingType.OPPLYSNINGSPLIKT)

        val (nyInit, nyAktiv) = flyttResultaterTilInitielt(
            initiellVilkårsvurdering = initiellVilkårsvurderingUtenAndreVurderinger,
            aktivVilkårsvurdering = aktivVilkårsvurdering,
        )

        val nyInitInnholderOpplysningspliktVilkår = nyInit.personResultater.find { it.erSøkersResultater() }!!.andreVurderinger
            .any { it.type == AnnenVurderingType.OPPLYSNINGSPLIKT }

        Assertions.assertTrue(nyInitInnholderOpplysningspliktVilkår)
        Assertions.assertTrue(nyAktiv.personResultater.isEmpty())
    }

    fun lagVilkårsvurderingMedForskjelligeTyperVilkår(
        søkerAktør: Aktør,
        behandling: Behandling,
        vilkår: List<Vilkår>,
    ): Vilkårsvurdering {
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
        val personResultat =
            PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = søkerAktør)
        val vilkårResultater =
            vilkår.map { lagVilkårResultat(vilkårType = it, personResultat = personResultat) }.toSet()
        personResultat.setSortedVilkårResultater(vilkårResultater)
        vilkårsvurdering.personResultater = setOf(personResultat)
        return vilkårsvurdering
    }

    fun lagVilkårsvurdering(fnrAktør: List<Pair<String, Aktør>>, behandling: Behandling): Vilkårsvurdering {
        val vilkårsvurdering = Vilkårsvurdering(
            behandling = behandling,
        )

        vilkårsvurdering.personResultater = fnrAktør.map {
            val personResultat = PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = it.second,
            )

            personResultat.setSortedVilkårResultater(
                setOf(
                    VilkårResultat(
                        personResultat = personResultat,
                        vilkårType = Vilkår.BOSATT_I_RIKET,
                        resultat = Resultat.OPPFYLT,
                        periodeFom = LocalDate.now(),
                        periodeTom = LocalDate.now(),
                        begrunnelse = "",
                        sistEndretIBehandlingId = behandling.id,
                    ),
                    VilkårResultat(
                        personResultat = personResultat,
                        vilkårType = Vilkår.GIFT_PARTNERSKAP,
                        resultat = Resultat.OPPFYLT,
                        periodeFom = LocalDate.now(),
                        periodeTom = LocalDate.now(),
                        begrunnelse = "",
                        sistEndretIBehandlingId = behandling.id,
                    ),
                ),
            )

            personResultat
        }.toSet()

        return vilkårsvurdering
    }

    fun lagVilkårsvurderingResultatB(fnrAktør: List<Pair<String, Aktør>>, behandling: Behandling): Vilkårsvurdering {
        val vilkårsvurdering = Vilkårsvurdering(
            behandling = behandling,
        )

        vilkårsvurdering.personResultater = fnrAktør.map {
            val personResultat = PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = it.second,
            )

            personResultat.setSortedVilkårResultater(
                setOf(
                    VilkårResultat(
                        personResultat = personResultat,
                        vilkårType = Vilkår.BOSATT_I_RIKET,
                        resultat = Resultat.OPPFYLT,
                        periodeFom = LocalDate.now(),
                        periodeTom = LocalDate.now(),
                        begrunnelse = "",
                        sistEndretIBehandlingId = behandling.id,
                    ),
                    VilkårResultat(
                        personResultat = personResultat,
                        vilkårType = Vilkår.GIFT_PARTNERSKAP,
                        resultat = Resultat.OPPFYLT,
                        periodeFom = LocalDate.now(),
                        periodeTom = LocalDate.now(),
                        begrunnelse = "",
                        sistEndretIBehandlingId = behandling.id,
                    ),
                    VilkårResultat(
                        personResultat = personResultat,
                        vilkårType = Vilkår.LOVLIG_OPPHOLD,
                        resultat = Resultat.OPPFYLT,
                        periodeFom = LocalDate.now(),
                        periodeTom = LocalDate.now(),
                        begrunnelse = "",
                        sistEndretIBehandlingId = behandling.id,
                    ),
                ),
            )
            personResultat
        }.toSet()

        return vilkårsvurdering
    }

    fun lagBasicVilkårsvurdering(behandling: Behandling, personer: List<Person>): Vilkårsvurdering {
        val vilkårsvurdering = Vilkårsvurdering(
            behandling = behandling,
        )

        val personResultater = personer.map { person ->
            genererPersonResultatForPerson(
                vilkårsvurdering = vilkårsvurdering,
                person = person,
            )
        }.toSet()

        vilkårsvurdering.personResultater = personResultater

        return vilkårsvurdering
    }
}
