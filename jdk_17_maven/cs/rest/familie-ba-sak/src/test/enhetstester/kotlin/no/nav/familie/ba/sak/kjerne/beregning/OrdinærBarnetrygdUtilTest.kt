package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.OrdinærBarnetrygdUtil.mapTilProsentEllerNull
import no.nav.familie.ba.sak.kjerne.beregning.OrdinærBarnetrygdUtil.tilTidslinjeMedRettTilProsentForPerson
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.gjelderAlltidFraBarnetsFødselsdato
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class OrdinærBarnetrygdUtilTest {

    @Test
    fun `Skal lage riktig tidslinje med rett til prosent for person med start og stopp av delt bosted`() {
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(9))
        val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())

        val personResultat = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = barn.aktør,
        )

        val generellVilkårFom = LocalDate.now().minusYears(3)
        val borMedSøkerVilkårFom = LocalDate.now().minusYears(2)
        val borMedSøkerVilkårTom = LocalDate.now()
        val startPåYtelse = generellVilkårFom.plusMonths(1).toYearMonth()
        val rettTilDeltFom = borMedSøkerVilkårFom.plusMonths(1).toYearMonth()
        val rettTilDeltTom = borMedSøkerVilkårTom.toYearMonth()
        val månedFørFylte18År = barn.fødselsdato.plusYears(18).forrigeMåned()

        val vilkårResulater = Vilkår.hentVilkårFor(personType = PersonType.BARN, fagsakType = FagsakType.NORMAL, behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR).mapNotNull {
            if (it == Vilkår.BOR_MED_SØKER) {
                null
            } else {
                lagVilkårResultat(
                    personResultat = personResultat,
                    periodeFom = if (it.gjelderAlltidFraBarnetsFødselsdato()) barn.fødselsdato else generellVilkårFom,
                    periodeTom = null,
                    resultat = Resultat.OPPFYLT,
                    vilkårType = it,
                )
            }
        }.toSet()

        val borMedSøkerVilkår = listOf(
            lagVilkårResultat(
                personResultat = personResultat,
                periodeFom = generellVilkårFom,
                periodeTom = borMedSøkerVilkårFom.minusMonths(1).sisteDagIMåned(),
                resultat = Resultat.OPPFYLT,
                vilkårType = Vilkår.BOR_MED_SØKER,
            ),
            lagVilkårResultat(
                personResultat = personResultat,
                periodeFom = borMedSøkerVilkårFom.førsteDagIInneværendeMåned(),
                periodeTom = borMedSøkerVilkårTom.sisteDagIMåned(),
                resultat = Resultat.OPPFYLT,
                vilkårType = Vilkår.BOR_MED_SØKER,
                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED),
            ),
            lagVilkårResultat(
                personResultat = personResultat,
                periodeFom = borMedSøkerVilkårTom.plusMonths(1).førsteDagIInneværendeMåned(),
                periodeTom = null,
                resultat = Resultat.OPPFYLT,
                vilkårType = Vilkår.BOR_MED_SØKER,
            ),
        )

        personResultat.setSortedVilkårResultater(vilkårResulater + borMedSøkerVilkår)

        val tidslinje = personResultat.tilTidslinjeMedRettTilProsentForPerson(
            person = barn,
            fagsakType = FagsakType.NORMAL,
        )

        val perioder = tidslinje.perioder().toList()

        assertEquals(3, perioder.size)

        val periode1 = perioder[0]
        val periode2 = perioder[1]
        val periode3 = perioder[2]

        assertProsentPeriode(
            forventetFom = startPåYtelse,
            forventetTom = rettTilDeltFom.minusMonths(1),
            forventetProsent = BigDecimal(100),
            faktisk = periode1,
        )
        assertProsentPeriode(
            forventetFom = rettTilDeltFom,
            forventetTom = rettTilDeltTom,
            forventetProsent = BigDecimal(50),
            faktisk = periode2,
        )
        assertProsentPeriode(
            forventetFom = rettTilDeltTom.plusMonths(1),
            forventetTom = månedFørFylte18År,
            forventetProsent = BigDecimal(100),
            faktisk = periode3,
        )
    }

    @Test
    fun `Skal returnere 50 prosent hvis vilkårsvurderingen har delt bosted i perioden`() {
        val barn = lagPerson(type = PersonType.BARN)
        val personResultat = PersonResultat(
            vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling()),
            aktør = barn.aktør,
        )
        val vilkårResultater = Vilkår.hentVilkårFor(personType = PersonType.BARN, fagsakType = FagsakType.NORMAL, behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR).map {
            lagVilkårResultat(
                vilkårType = it,
                periodeFom = LocalDate.now().minusMonths(5),
                periodeTom = null,
                resultat = Resultat.OPPFYLT,
                utdypendeVilkårsvurderinger = if (it == Vilkår.BOR_MED_SØKER) listOf(UtdypendeVilkårsvurdering.DELT_BOSTED) else emptyList(),
                personResultat = personResultat,
            )
        }

        val prosent = vilkårResultater.mapTilProsentEllerNull(personType = PersonType.BARN, fagsakType = FagsakType.NORMAL)

        assertEquals(BigDecimal(50), prosent)
    }

    @Test
    fun `Skal returnere 100 prosent hvis vilkårsvurderingen ikke har delt bosted i perioden`() {
        val barn = lagPerson(type = PersonType.BARN)
        val personResultat = PersonResultat(
            vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling()),
            aktør = barn.aktør,
        )
        val vilkårResultater = Vilkår.hentVilkårFor(personType = PersonType.BARN, fagsakType = FagsakType.NORMAL, behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR).map {
            lagVilkårResultat(
                vilkårType = it,
                periodeFom = LocalDate.now().minusMonths(5),
                periodeTom = null,
                resultat = Resultat.OPPFYLT,
                utdypendeVilkårsvurderinger = emptyList(),
                personResultat = personResultat,
            )
        }

        val prosent = vilkårResultater.mapTilProsentEllerNull(personType = PersonType.BARN, fagsakType = FagsakType.NORMAL)

        assertEquals(BigDecimal(100), prosent)
    }

    @Test
    fun `Skal returnere null hvis ikke alle vilkår for barn er oppfylt`() {
        val barn = lagPerson(type = PersonType.BARN)
        val personResultat = PersonResultat(
            vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling()),
            aktør = barn.aktør,
        )
        val vilkårResultater = Vilkår.hentVilkårFor(personType = PersonType.BARN, fagsakType = FagsakType.NORMAL, behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR).mapNotNull {
            if (it == Vilkår.LOVLIG_OPPHOLD) {
                null
            } else {
                lagVilkårResultat(
                    vilkårType = it,
                    periodeFom = LocalDate.now().minusMonths(5),
                    periodeTom = null,
                    resultat = Resultat.OPPFYLT,
                    utdypendeVilkårsvurderinger = if (it == Vilkår.BOR_MED_SØKER) listOf(UtdypendeVilkårsvurdering.DELT_BOSTED) else emptyList(),
                    personResultat = personResultat,
                )
            }
        }

        val prosent = vilkårResultater.mapTilProsentEllerNull(personType = PersonType.BARN, fagsakType = FagsakType.NORMAL)

        assertEquals(null, prosent)
    }

    @Test
    fun `Skal returnere null hvis ikke alle vilkår for søker er oppfylt`() {
        val søker = lagPerson(type = PersonType.SØKER)
        val personResultat = PersonResultat(
            vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling()),
            aktør = søker.aktør,
        )
        val vilkårResultater = Vilkår.hentVilkårFor(personType = PersonType.SØKER, fagsakType = FagsakType.NORMAL, behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR).mapNotNull {
            if (it == Vilkår.LOVLIG_OPPHOLD) {
                null
            } else {
                lagVilkårResultat(
                    vilkårType = it,
                    periodeFom = LocalDate.now().minusMonths(5),
                    periodeTom = null,
                    resultat = Resultat.OPPFYLT,
                    utdypendeVilkårsvurderinger = emptyList(),
                    personResultat = personResultat,
                )
            }
        }

        val prosent = vilkårResultater.mapTilProsentEllerNull(personType = PersonType.SØKER, fagsakType = FagsakType.NORMAL)

        assertEquals(null, prosent)
    }

    private fun assertProsentPeriode(
        forventetFom: YearMonth,
        forventetTom: YearMonth,
        forventetProsent: BigDecimal,
        faktisk: Periode<BigDecimal, Måned>,
    ) {
        assertEquals(forventetFom, faktisk.fraOgMed.tilYearMonth())
        assertEquals(forventetTom, faktisk.tilOgMed.tilYearMonth())
        assertEquals(forventetProsent, faktisk.innhold)
    }
}
