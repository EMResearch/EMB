package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.utbetalingsperiodemedbegrunnelser

import hentPerioderMedUtbetaling
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class UtbetalingsperiodeUtilTest {

    @Test
    fun `Skal beholde split i andel tilkjent ytelse`() {
        val mars2020 = YearMonth.of(2020, 3)
        val april2020 = YearMonth.of(2020, 4)
        val mai2020 = YearMonth.of(2020, 5)
        val juli2020 = YearMonth.of(2020, 7)

        val person1 = lagPerson()
        val person2 = lagPerson()

        val vedtak = lagVedtak()

        val andelPerson1MarsTilApril = lagAndelTilkjentYtelse(
            fom = mars2020,
            tom = april2020,
            beløp = 1000,
            person = person1,
        )

        val andelPerson1MaiTilJuli = lagAndelTilkjentYtelse(
            fom = mai2020,
            tom = juli2020,
            beløp = 1000,
            person = person1,
        )

        val andelPerson2MarsTilJuli = lagAndelTilkjentYtelse(
            fom = mars2020,
            tom = juli2020,
            beløp = 1000,
            person = person2,
        )

        val forventetResultat = listOf(
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = mars2020.førsteDagIInneværendeMåned(),
                tom = april2020.sisteDagIInneværendeMåned(),
                type = Vedtaksperiodetype.UTBETALING,
                begrunnelser = mutableSetOf(),
            ),
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = mai2020.førsteDagIInneværendeMåned(),
                tom = juli2020.sisteDagIInneværendeMåned(),
                type = Vedtaksperiodetype.UTBETALING,
                begrunnelser = mutableSetOf(),
            ),
        )

        val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
        val personResultater = setOf(
            vilkårsvurdering.lagGodkjentPersonResultatForBarn(person1),
            vilkårsvurdering.lagGodkjentPersonResultatForBarn(person2),
        )

        val faktiskResultat = hentPerioderMedUtbetaling(
            andelerTilkjentYtelse = listOf(andelPerson1MarsTilApril, andelPerson1MaiTilJuli, andelPerson2MarsTilJuli),
            vedtak = vedtak,
            personResultater = personResultater,
            personerIPersongrunnlag = listOf(person1, person2),
            fagsakType = FagsakType.NORMAL,
        )

        Assertions.assertEquals(
            forventetResultat.map { Periode(it.fom ?: TIDENES_MORGEN, it.tom ?: TIDENES_ENDE) },
            faktiskResultat.map { Periode(it.fom ?: TIDENES_MORGEN, it.tom ?: TIDENES_ENDE) },
        )

        Assertions.assertEquals(
            forventetResultat.map { it.type }.toSet(),
            faktiskResultat.map { it.type }.toSet(),
        )
    }

    @Test
    fun `Skal splitte på forskjellige personer`() {
        val mars2020 = YearMonth.of(2020, 3)
        val april2020 = YearMonth.of(2020, 4)
        val mai2020 = YearMonth.of(2020, 5)
        val juni2020 = YearMonth.of(2020, 6)
        val juli2020 = YearMonth.of(2020, 7)

        val person1 = lagPerson(type = PersonType.BARN)
        val person2 = lagPerson(type = PersonType.BARN)

        val vedtak = lagVedtak()

        val andelPerson1MarsTilMai = lagAndelTilkjentYtelse(
            fom = mars2020,
            tom = mai2020,
            beløp = 1000,
            person = person1,
        )

        val andelPerson2MaiTilJuli = lagAndelTilkjentYtelse(
            fom = mai2020,
            tom = juli2020,
            beløp = 1000,
            person = person2,
        )

        val forventetResultat = listOf(
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = mars2020.førsteDagIInneværendeMåned(),
                tom = april2020.sisteDagIInneværendeMåned(),
                type = Vedtaksperiodetype.UTBETALING,
                begrunnelser = mutableSetOf(),
            ),
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = mai2020.førsteDagIInneværendeMåned(),
                tom = mai2020.sisteDagIInneværendeMåned(),
                type = Vedtaksperiodetype.UTBETALING,
                begrunnelser = mutableSetOf(),
            ),
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = juni2020.førsteDagIInneværendeMåned(),
                tom = juli2020.sisteDagIInneværendeMåned(),
                type = Vedtaksperiodetype.UTBETALING,
                begrunnelser = mutableSetOf(),
            ),
        )

        val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
        val personResultater = setOf(
            vilkårsvurdering.lagGodkjentPersonResultatForBarn(person1),
            vilkårsvurdering.lagGodkjentPersonResultatForBarn(person2),
        )

        val faktiskResultat = hentPerioderMedUtbetaling(
            andelerTilkjentYtelse = listOf(andelPerson1MarsTilMai, andelPerson2MaiTilJuli),
            vedtak = vedtak,
            personResultater = personResultater,
            personerIPersongrunnlag = listOf(person1, person2),
            fagsakType = FagsakType.NORMAL,
        )

        Assertions.assertEquals(
            forventetResultat.map { Periode(it.fom ?: TIDENES_MORGEN, it.tom ?: TIDENES_ENDE) },
            faktiskResultat.map { Periode(it.fom ?: TIDENES_MORGEN, it.tom ?: TIDENES_ENDE) },
        )

        Assertions.assertEquals(
            forventetResultat.map { it.type }.toSet(),
            faktiskResultat.map { it.type }.toSet(),
        )
    }

    @Test
    fun `Skal splitte på utdypende vilkårsvurdering når det flytter seg fra ett barn til et annet`() {
        val mars2020 = YearMonth.of(2020, 3)
        val april2020 = YearMonth.of(2020, 4)
        val mai2020 = YearMonth.of(2020, 5)
        val juli2020 = YearMonth.of(2020, 7)

        val søker = lagPerson(type = PersonType.SØKER)
        val barn1 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(16))
        val barn2 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(7))

        val vedtak = lagVedtak()

        val andelBarn1MarsTilJuli = lagAndelTilkjentYtelse(
            fom = mars2020,
            tom = juli2020,
            beløp = 1000,
            person = barn1,
        )

        val andelBarn2MarsTilJuli = lagAndelTilkjentYtelse(
            fom = mars2020,
            tom = juli2020,
            beløp = 2000,
            person = barn2,
        )

        val vilkårsvurdering = lagVilkårsvurdering(
            søkerAktør = søker.aktør,
            behandling = lagBehandling(),
            resultat = Resultat.OPPFYLT,
        )

        val personResultatBarn1 = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = barn1.aktør,
        )

        val vilkårResultatBorMedSøkerMedUtdypendeVilkårsvurderingBarn1 = VilkårResultat(
            personResultat = personResultatBarn1,
            periodeFom = mars2020.minusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = april2020.sisteDagIInneværendeMåned(),
            vilkårType = Vilkår.BOR_MED_SØKER,
            resultat = Resultat.OPPFYLT,
            begrunnelse = "",
            sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BARN_BOR_I_STORBRITANNIA_MED_SØKER),
        )
        val vilkårResultatBorMedSøkerUtenUtdypendeVilkårsvurderingBarn1 = VilkårResultat(
            personResultat = personResultatBarn1,
            periodeFom = mai2020.førsteDagIInneværendeMåned(),
            periodeTom = juli2020.sisteDagIInneværendeMåned(),
            vilkårType = Vilkår.BOR_MED_SØKER,
            resultat = Resultat.OPPFYLT,
            begrunnelse = "",
            sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
            utdypendeVilkårsvurderinger = emptyList(),
        )

        val resterendeVilkårForBarn = Vilkår.hentVilkårFor(PersonType.BARN, fagsakType = FagsakType.NORMAL, behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR).mapNotNull {
            if (it == Vilkår.BOR_MED_SØKER) null else lagVilkårResultat(vilkår = it, fom = mars2020.minusMonths(1), tom = juli2020)
        }

        val vilkårResultaterBarn1 = listOf(
            vilkårResultatBorMedSøkerMedUtdypendeVilkårsvurderingBarn1,
            vilkårResultatBorMedSøkerUtenUtdypendeVilkårsvurderingBarn1,
        ) + resterendeVilkårForBarn

        personResultatBarn1.setSortedVilkårResultater(
            vilkårResultaterBarn1.toSet(),
        )

        val personResultatBarn2 = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = barn2.aktør,
        )

        val vilkårResultatBorMedSøkerMedUtdypendeVilkårsvurderingBarn2 = VilkårResultat(
            personResultat = personResultatBarn2,
            periodeFom = mars2020.minusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = april2020.sisteDagIInneværendeMåned(),
            vilkårType = Vilkår.BOR_MED_SØKER,
            resultat = Resultat.OPPFYLT,
            begrunnelse = "",
            sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
            utdypendeVilkårsvurderinger = emptyList(),
        )
        val vilkårResultatBorMedSøkerUtenUtdypendeVilkårsvurderingBarn2 = VilkårResultat(
            personResultat = personResultatBarn2,
            periodeFom = mai2020.førsteDagIInneværendeMåned(),
            periodeTom = juli2020.sisteDagIInneværendeMåned(),
            vilkårType = Vilkår.BOR_MED_SØKER,
            resultat = Resultat.OPPFYLT,
            begrunnelse = "",
            sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BARN_BOR_I_STORBRITANNIA_MED_SØKER),
        )

        val vilkårResultaterBarn2 = listOf(
            vilkårResultatBorMedSøkerMedUtdypendeVilkårsvurderingBarn2,
            vilkårResultatBorMedSøkerUtenUtdypendeVilkårsvurderingBarn2,
        ) + resterendeVilkårForBarn

        personResultatBarn2.setSortedVilkårResultater(
            vilkårResultaterBarn2.toSet(),
        )

        val forventetResultat = listOf(
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = mars2020.førsteDagIInneværendeMåned(),
                tom = april2020.sisteDagIInneværendeMåned(),
                type = Vedtaksperiodetype.UTBETALING,
                begrunnelser = mutableSetOf(),
            ),
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = mai2020.førsteDagIInneværendeMåned(),
                tom = juli2020.sisteDagIInneværendeMåned(),
                type = Vedtaksperiodetype.UTBETALING,
                begrunnelser = mutableSetOf(),
            ),
        )

        val faktiskResultat = hentPerioderMedUtbetaling(
            andelerTilkjentYtelse = listOf(andelBarn1MarsTilJuli, andelBarn2MarsTilJuli),
            vedtak = vedtak,
            personResultater = setOf(personResultatBarn1, personResultatBarn2),
            personerIPersongrunnlag = listOf(søker, barn1, barn2),
            fagsakType = FagsakType.NORMAL,
        )

        Assertions.assertEquals(
            forventetResultat.map { Periode(it.fom ?: TIDENES_MORGEN, it.tom ?: TIDENES_ENDE) },
            faktiskResultat.map { Periode(it.fom ?: TIDENES_MORGEN, it.tom ?: TIDENES_ENDE) },
        )

        Assertions.assertEquals(
            forventetResultat.map { it.type }.toSet(),
            faktiskResultat.map { it.type }.toSet(),
        )
    }

    @Test
    fun `Skal få med opphør i andel tilkjent ytelse`() {
        val mars2020 = YearMonth.of(2020, 3)
        val april2020 = YearMonth.of(2020, 4)
        val juli2020 = YearMonth.of(2020, 7)
        val barn1 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(9))
        val vedtak = lagVedtak()

        val andelBarn1MarsTilApril = lagAndelTilkjentYtelse(
            fom = mars2020,
            tom = april2020,
            beløp = 1000,
            person = barn1,
        )
        val andelBarn1JuliTilJuli = lagAndelTilkjentYtelse(
            fom = juli2020,
            tom = juli2020,
            beløp = 1000,
            person = barn1,
        )

        val faktiskResultat = hentPerioderMedUtbetaling(
            andelerTilkjentYtelse = listOf(andelBarn1MarsTilApril, andelBarn1JuliTilJuli),
            vedtak = vedtak,
            personResultater = emptySet(),
            personerIPersongrunnlag = listOf(barn1),
            fagsakType = FagsakType.NORMAL,
        )

        val forventetResultat = listOf(
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = mars2020.førsteDagIInneværendeMåned(),
                tom = april2020.sisteDagIInneværendeMåned(),
                type = Vedtaksperiodetype.UTBETALING,
                begrunnelser = mutableSetOf(),
            ),
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = juli2020.førsteDagIInneværendeMåned(),
                tom = juli2020.sisteDagIInneværendeMåned(),
                type = Vedtaksperiodetype.UTBETALING,
                begrunnelser = mutableSetOf(),
            ),
        )

        Assertions.assertEquals(
            forventetResultat.map { Periode(it.fom ?: TIDENES_MORGEN, it.tom ?: TIDENES_ENDE) },
            faktiskResultat.map { Periode(it.fom ?: TIDENES_MORGEN, it.tom ?: TIDENES_ENDE) },
        )

        Assertions.assertEquals(
            forventetResultat.map { it.type }.toSet(),
            faktiskResultat.map { it.type }.toSet(),
        )
    }

    @Test
    fun `Skal lage splitt i vedtaksperioder med der ulikt regelverk er brukt`() {
        val mars2020 = YearMonth.of(2020, 3)
        val april2020 = YearMonth.of(2020, 4)
        val mai2020 = YearMonth.of(2020, 5)
        val juli2020 = YearMonth.of(2020, 7)

        val søker = lagPerson(type = PersonType.SØKER)
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(8))

        val vedtak = lagVedtak()

        val andelBarnMarsTilJuli = lagAndelTilkjentYtelse(
            fom = mars2020,
            tom = juli2020,
            beløp = 1000,
            person = barn,
        )

        val vilkårsvurdering = lagVilkårsvurdering(
            søkerAktør = søker.aktør,
            behandling = lagBehandling(),
            resultat = Resultat.OPPFYLT,
        )

        val personResultatBarn = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = barn.aktør,
        )

        fun nasjonaltVilkår(vilkårType: Vilkår): VilkårResultat = VilkårResultat(
            personResultat = personResultatBarn,
            periodeFom = mars2020.minusMonths(1).førsteDagIInneværendeMåned(),
            periodeTom = april2020.sisteDagIInneværendeMåned(),
            vilkårType = vilkårType,
            resultat = Resultat.OPPFYLT,
            begrunnelse = "",
            sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
            utdypendeVilkårsvurderinger = emptyList(),
            vurderesEtter = Regelverk.NASJONALE_REGLER,
        )

        fun eøsVilkår(vilkårType: Vilkår): VilkårResultat =
            VilkårResultat(
                personResultat = personResultatBarn,
                periodeFom = mai2020.førsteDagIInneværendeMåned(),
                periodeTom = juli2020.sisteDagIInneværendeMåned(),
                vilkårType = vilkårType,
                resultat = Resultat.OPPFYLT,
                begrunnelse = "",
                sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                utdypendeVilkårsvurderinger = emptyList(),
                vurderesEtter = Regelverk.EØS_FORORDNINGEN,
            )

        val vilkårForBarn = Vilkår.hentVilkårFor(personType = PersonType.BARN, fagsakType = FagsakType.NORMAL, behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR)

        val vilkårResultaterBarn1 = vilkårForBarn.map { nasjonaltVilkår(it) } + vilkårForBarn.map { eøsVilkår(it) }

        personResultatBarn.setSortedVilkårResultater(
            vilkårResultaterBarn1.toSet(),
        )

        val forventetResultat = listOf(
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = mars2020.førsteDagIInneværendeMåned(),
                tom = april2020.sisteDagIInneværendeMåned(),
                type = Vedtaksperiodetype.UTBETALING,
                begrunnelser = mutableSetOf(),
            ),
            lagVedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = mai2020.førsteDagIInneværendeMåned(),
                tom = juli2020.sisteDagIInneværendeMåned(),
                type = Vedtaksperiodetype.UTBETALING,
                begrunnelser = mutableSetOf(),
            ),
        )

        val faktiskResultat = hentPerioderMedUtbetaling(
            andelerTilkjentYtelse = listOf(andelBarnMarsTilJuli),
            vedtak = vedtak,
            personResultater = setOf(personResultatBarn),
            personerIPersongrunnlag = listOf(søker, barn),
            fagsakType = FagsakType.NORMAL,
        )

        Assertions.assertEquals(
            forventetResultat.map { Periode(it.fom ?: TIDENES_MORGEN, it.tom ?: TIDENES_ENDE) },
            faktiskResultat.map { Periode(it.fom ?: TIDENES_MORGEN, it.tom ?: TIDENES_ENDE) },
        )

        Assertions.assertEquals(
            forventetResultat.map { it.type }.toSet(),
            faktiskResultat.map { it.type }.toSet(),
        )
    }

    private fun Vilkårsvurdering.lagGodkjentPersonResultatForBarn(person: Person) = lagPersonResultat(
        vilkårsvurdering = this,
        person = person,
        resultat = Resultat.OPPFYLT,
        periodeFom = person.fødselsdato,
        periodeTom = person.fødselsdato.til18ÅrsVilkårsdato(),
        lagFullstendigVilkårResultat = true,
        personType = person.type,
    )
}
