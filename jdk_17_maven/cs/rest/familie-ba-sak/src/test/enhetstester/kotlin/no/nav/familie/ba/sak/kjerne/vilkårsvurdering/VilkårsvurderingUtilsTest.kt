package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.ekstern.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityVilkår
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.lagDødsfallFraPdl
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

class VilkårsvurderingUtilsTest {

    private val uvesentligVilkårsvurdering =
        lagVilkårsvurdering(randomAktør(), lagBehandling(), Resultat.IKKE_VURDERT)

    @Test
    fun `feil kastes når det finnes løpende oppfylt ved forsøk på å legge til avslag uten periode`() {
        val personResultat = PersonResultat(
            vilkårsvurdering = uvesentligVilkårsvurdering,
            aktør = randomAktør(),
        )
        val løpendeOppfylt = VilkårResultat(
            personResultat = personResultat,
            periodeFom = LocalDate.of(2020, 1, 1),
            periodeTom = null,
            vilkårType = Vilkår.LOVLIG_OPPHOLD,
            resultat = Resultat.OPPFYLT,
            begrunnelse = "",
            sistEndretIBehandlingId = 0,
        )
        personResultat.vilkårResultater.add(løpendeOppfylt)

        val avslagUtenPeriode = RestVilkårResultat(
            id = 123,
            vilkårType = Vilkår.LOVLIG_OPPHOLD,
            resultat = Resultat.IKKE_OPPFYLT,
            periodeFom = null,
            periodeTom = null,
            begrunnelse = "",
            endretAv = "",
            endretTidspunkt = LocalDateTime.now(),
            behandlingId = 0,
            erEksplisittAvslagPåSøknad = true,
        )

        assertThrows<FunksjonellFeil> {
            VilkårsvurderingUtils.validerAvslagUtenPeriodeMedLøpende(
                personSomEndres = personResultat,
                vilkårSomEndres = avslagUtenPeriode,
            )
        }
    }

    @Test
    fun `feil kastes når det finnes avslag uten periode ved forsøk på å legge til løpende oppfylt`() {
        val personResultat = PersonResultat(
            vilkårsvurdering = uvesentligVilkårsvurdering,
            aktør = randomAktør(),
        )
        val avslagUtenPeriode = VilkårResultat(
            personResultat = personResultat,
            periodeFom = null,
            periodeTom = null,
            vilkårType = Vilkår.LOVLIG_OPPHOLD,
            resultat = Resultat.IKKE_OPPFYLT,
            begrunnelse = "",
            sistEndretIBehandlingId = 0,
            erEksplisittAvslagPåSøknad = true,
        )
        personResultat.vilkårResultater.add(avslagUtenPeriode)

        val løpendeOppfylt = RestVilkårResultat(
            id = 123,
            vilkårType = Vilkår.LOVLIG_OPPHOLD,
            resultat = Resultat.OPPFYLT,
            periodeFom = LocalDate.of(2020, 1, 1),
            periodeTom = null,
            begrunnelse = "",
            endretAv = "",
            endretTidspunkt = LocalDateTime.now(),
            behandlingId = 0,
        )

        assertThrows<FunksjonellFeil> {
            VilkårsvurderingUtils.validerAvslagUtenPeriodeMedLøpende(
                personSomEndres = personResultat,
                vilkårSomEndres = løpendeOppfylt,
            )
        }
    }

    @Test
    fun `skal ikke kaste feil hvis vilkåret er bor med søker`() {
        val personResultat = PersonResultat(
            vilkårsvurdering = uvesentligVilkårsvurdering,
            aktør = randomAktør(),
        )
        val løpendeOppfylt = VilkårResultat(
            personResultat = personResultat,
            periodeFom = LocalDate.of(2020, 1, 1),
            periodeTom = null,
            vilkårType = Vilkår.BOR_MED_SØKER,
            resultat = Resultat.OPPFYLT,
            begrunnelse = "",
            sistEndretIBehandlingId = 0,
        )
        personResultat.vilkårResultater.add(løpendeOppfylt)

        val avslagUtenPeriode = RestVilkårResultat(
            id = 123,
            vilkårType = Vilkår.BOR_MED_SØKER,
            resultat = Resultat.IKKE_OPPFYLT,
            periodeFom = null,
            periodeTom = null,
            begrunnelse = "",
            endretAv = "",
            endretTidspunkt = LocalDateTime.now(),
            behandlingId = 0,
            erEksplisittAvslagPåSøknad = true,
        )

        assertDoesNotThrow {
            VilkårsvurderingUtils.validerAvslagUtenPeriodeMedLøpende(
                personSomEndres = personResultat,
                vilkårSomEndres = avslagUtenPeriode,
            )
        }
    }

    @Test
    fun `feil kastes ikke når når ingen periode er løpende`() {
        val personResultat = PersonResultat(
            vilkårsvurdering = uvesentligVilkårsvurdering,
            aktør = randomAktør(),
        )
        val avslagUtenPeriode = VilkårResultat(
            personResultat = personResultat,
            periodeFom = null,
            periodeTom = null,
            vilkårType = Vilkår.BOR_MED_SØKER,
            resultat = Resultat.IKKE_OPPFYLT,
            begrunnelse = "",
            sistEndretIBehandlingId = 0,
            erEksplisittAvslagPåSøknad = true,
        )
        personResultat.vilkårResultater.add(avslagUtenPeriode)

        val løpendeOppfylt = RestVilkårResultat(
            id = 123,
            vilkårType = Vilkår.BOR_MED_SØKER,
            resultat = Resultat.OPPFYLT,
            periodeFom = LocalDate.of(2020, 1, 1),
            periodeTom = LocalDate.of(2020, 6, 1),
            begrunnelse = "",
            endretAv = "",
            endretTidspunkt = LocalDateTime.now(),
            behandlingId = 0,
        )

        assertDoesNotThrow {
            VilkårsvurderingUtils.validerAvslagUtenPeriodeMedLøpende(
                personSomEndres = personResultat,
                vilkårSomEndres = løpendeOppfylt,
            )
        }
    }

    @Test
    fun `skal liste opp begrunnelser uten vilkår`() {
        val sanityBegrunnelser = mapOf(
            Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET to SanityBegrunnelse(
                vilkaar = emptyList(),
                apiNavn = "innvilgetBosattIRiket",
                navnISystem = "",
            ),
        )
        val vedtakBegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET

        val restVedtakBegrunnelserTilknyttetVilkår =
            vedtakBegrunnelseTilRestVedtakBegrunnelseTilknyttetVilkår(sanityBegrunnelser, vedtakBegrunnelse)

        Assertions.assertEquals(1, restVedtakBegrunnelserTilknyttetVilkår.size)
    }

    @Test
    fun `skal liste opp begrunnelsene en gang per vilkår`() {
        val sanityBegrunnelser = mapOf(
            Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET to SanityBegrunnelse(
                vilkaar = listOf(SanityVilkår.BOSATT_I_RIKET, SanityVilkår.LOVLIG_OPPHOLD),
                apiNavn = "innvilgetBosattIRiket",
                navnISystem = "",
            ),
        )
        val vedtakBegrunnelse = Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET

        val restVedtakBegrunnelserTilknyttetVilkår =
            vedtakBegrunnelseTilRestVedtakBegrunnelseTilknyttetVilkår(sanityBegrunnelser, vedtakBegrunnelse)

        Assertions.assertEquals(2, restVedtakBegrunnelserTilknyttetVilkår.size)
    }

    @Test
    fun `genererPersonResultatForPerson skal sette til-og-med dato på alle vilkår til dødsfallsdato og begrunnelse til dødsfall hvis barn er død`() {
        val nyBehandling = lagBehandling()

        val vilkårsvurdering = Vilkårsvurdering(behandling = nyBehandling)
        val dødtBarn = lagPerson(type = PersonType.BARN).apply { dødsfall = lagDødsfallFraPdl(this, "2012-12-12", null) }

        val personResultatForDødtBarn = genererPersonResultatForPerson(
            vilkårsvurdering = vilkårsvurdering,
            person = dødtBarn,
        )

        Assertions.assertTrue(personResultatForDødtBarn.vilkårResultater.all { it.begrunnelse == "Dødsfall" })
        Assertions.assertTrue(
            personResultatForDødtBarn.vilkårResultater.all {
                it.periodeTom == LocalDate.of(2012, 12, 12)
            },
        )
    }

    @Test
    fun `genererPersonResultatForPerson skal sette til-og-med dato på under-18-årsvilkår til 18 års datoen hvis barn ikke er død`() {
        val nyBehandling = lagBehandling()

        val vilkårsvurdering = Vilkårsvurdering(behandling = nyBehandling)
        val levendeBarn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2020, 10, 10))

        val personResultatForLevendeBarn = genererPersonResultatForPerson(
            vilkårsvurdering = vilkårsvurdering,
            person = levendeBarn,
        )

        val under18ÅrVilkår =
            personResultatForLevendeBarn.vilkårResultater.find { it.vilkårType == Vilkår.UNDER_18_ÅR }!!

        Assertions.assertEquals(under18ÅrVilkår.begrunnelse, "Vurdert og satt automatisk")
        Assertions.assertEquals(under18ÅrVilkår.periodeTom, levendeBarn.fødselsdato.til18ÅrsVilkårsdato())
    }
}
