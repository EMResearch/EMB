package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

internal class BehandlingsresultatValideringUtilsTest {

    @Test
    fun `Valider eksplisitt avlag - Skal kaste feil hvis eksplisitt avslått for barn det ikke er fremstilt krav for`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)
        val vikårsvurdering = Vilkårsvurdering(behandling = behandling)
        val barn1 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(5))
        val barn2 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(7))

        val barn1PersonResultat = lagPersonResultat(
            vilkårsvurdering = vikårsvurdering,
            person = barn1,
            resultat = Resultat.IKKE_OPPFYLT,
            periodeFom = LocalDate.now().minusMonths(5),
            periodeTom = LocalDate.now(),
            lagFullstendigVilkårResultat = true,
            personType = PersonType.BARN,
            erEksplisittAvslagPåSøknad = true,
        )
        val barn2PersonResultat = lagPersonResultat(
            vilkårsvurdering = vikårsvurdering,
            person = barn2,
            resultat = Resultat.IKKE_OPPFYLT,
            periodeFom = LocalDate.now().minusMonths(5),
            periodeTom = LocalDate.now(),
            lagFullstendigVilkårResultat = true,
            personType = PersonType.BARN,
            erEksplisittAvslagPåSøknad = true,
        )

        assertThrows<FunksjonellFeil> {
            BehandlingsresultatValideringUtils.validerAtBarePersonerFremstiltKravForEllerSøkerHarFåttEksplisittAvslag(
                personResultater = setOf(barn1PersonResultat, barn2PersonResultat),
                personerFremstiltKravFor = listOf(barn2.aktør),
            )
        }
    }

    @Test
    fun `Valider eksplisitt avslag - Skal ikke kaste feil hvis søker er eksplisitt avslått`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)
        val vikårsvurdering = Vilkårsvurdering(behandling = behandling)
        val søker = lagPerson(type = PersonType.SØKER)

        val søkerPersonResultat = lagPersonResultat(
            vilkårsvurdering = vikårsvurdering,
            person = søker,
            resultat = Resultat.IKKE_OPPFYLT,
            periodeFom = LocalDate.now().minusMonths(5),
            periodeTom = LocalDate.now(),
            lagFullstendigVilkårResultat = true,
            personType = PersonType.SØKER,
            erEksplisittAvslagPåSøknad = true,
        )

        assertDoesNotThrow {
            BehandlingsresultatValideringUtils.validerAtBarePersonerFremstiltKravForEllerSøkerHarFåttEksplisittAvslag(
                personResultater = setOf(søkerPersonResultat),
                personerFremstiltKravFor = emptyList(),
            )
        }
    }

    @Test
    fun `Valider eksplisitt avslag - Skal ikke kaste feil hvis person med eksplsitt avslag er fremstilt krav for`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)
        val vikårsvurdering = Vilkårsvurdering(behandling = behandling)
        val barn1 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(5))
        val barn2 = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(7))

        val barn1PersonResultat = lagPersonResultat(
            vilkårsvurdering = vikårsvurdering,
            person = barn1,
            resultat = Resultat.IKKE_OPPFYLT,
            periodeFom = LocalDate.now().minusMonths(5),
            periodeTom = LocalDate.now(),
            lagFullstendigVilkårResultat = true,
            personType = PersonType.BARN,
            erEksplisittAvslagPåSøknad = true,
        )
        val barn2PersonResultat = lagPersonResultat(
            vilkårsvurdering = vikårsvurdering,
            person = barn2,
            resultat = Resultat.OPPFYLT,
            periodeFom = LocalDate.now().minusMonths(5),
            periodeTom = LocalDate.now(),
            lagFullstendigVilkårResultat = true,
            personType = PersonType.BARN,
            erEksplisittAvslagPåSøknad = false,
        )

        assertDoesNotThrow {
            BehandlingsresultatValideringUtils.validerAtBarePersonerFremstiltKravForEllerSøkerHarFåttEksplisittAvslag(
                personResultater = setOf(barn1PersonResultat, barn2PersonResultat),
                personerFremstiltKravFor = listOf(barn1.aktør, barn2.aktør),
            )
        }
    }
}
