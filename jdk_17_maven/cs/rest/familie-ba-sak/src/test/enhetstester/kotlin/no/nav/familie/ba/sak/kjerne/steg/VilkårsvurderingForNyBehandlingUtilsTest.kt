package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.datagenerator.vilkårsvurdering.lagBarnVilkårResultat
import no.nav.familie.ba.sak.datagenerator.vilkårsvurdering.lagSøkerVilkårResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Dødsfall
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingUtils
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.finnAktørerMedUtvidetFraAndeler
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class VilkårsvurderingForNyBehandlingUtilsTest {

    @Test
    fun `Skal kun ta med aktører som hadde andeler med utvidet barnetrygd`() {
        val søker = lagPerson(type = PersonType.SØKER)
        val barn = lagPerson(type = PersonType.BARN)

        val andeler = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.now().minusYears(2).plusMonths(1),
                tom = YearMonth.now(),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                person = barn,
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.now().minusYears(1),
                tom = YearMonth.now(),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                person = søker,
            ),
        )

        val aktørerMedUtvidet = finnAktørerMedUtvidetFraAndeler(
            andeler = andeler,
        )

        Assertions.assertThat(aktørerMedUtvidet).containsExactly(søker.aktør)
    }

    @Test
    fun `Skal lage vilkårsvurdering med søkers vilkår satt med tom=dødsdato`() {
        val søker = lagPerson(type = PersonType.SØKER).also { it.dødsfall = Dødsfall(person = it, dødsfallDato = LocalDate.now(), dødsfallAdresse = "Adresse 1", dødsfallPostnummer = "1234", dødsfallPoststed = "Oslo") }
        val barn = lagPerson(type = PersonType.BARN)
        val behandling = lagBehandling()
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)

        val tomPåFørsteUtvidetVilkår = LocalDate.now().minusMonths(8)

        val søkerPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = søker.aktør)
        val søkerVilkårResultater = lagSøkerVilkårResultat(søkerPersonResultat = søkerPersonResultat, periodeFom = LocalDate.now().minusYears(2), periodeTom = null, behandlingId = behandling.id) + setOf(
            VilkårResultat(
                personResultat = søkerPersonResultat,
                vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                resultat = Resultat.OPPFYLT,
                periodeFom = LocalDate.now().minusYears(2),
                periodeTom = tomPåFørsteUtvidetVilkår,
                begrunnelse = "",
                sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                utdypendeVilkårsvurderinger = emptyList(),
            ),
            VilkårResultat(
                personResultat = søkerPersonResultat,
                vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                resultat = Resultat.OPPFYLT,
                periodeFom = tomPåFørsteUtvidetVilkår.plusMonths(1),
                periodeTom = null,
                begrunnelse = "",
                sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                utdypendeVilkårsvurderinger = emptyList(),
            ),
        )

        val barnPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn.aktør)
        val barnVilkårResultater = lagBarnVilkårResultat(
            barnPersonResultat = barnPersonResultat,
            barnetsFødselsdato = barn.fødselsdato,
            periodeFom = LocalDate.now().minusYears(2),
            behandlingId = behandling.id,
        )

        søkerPersonResultat.setSortedVilkårResultater(søkerVilkårResultater)
        barnPersonResultat.setSortedVilkårResultater(barnVilkårResultater)

        vilkårsvurdering.personResultater = setOf(søkerPersonResultat, barnPersonResultat)

        val nyVilkårsvurdering = VilkårsvurderingForNyBehandlingUtils(personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id, personer = mutableSetOf(barn, søker))).hentVilkårsvurderingMedDødsdatoSomTomDato(
            vilkårsvurdering = vilkårsvurdering,
        )
        val søkersVilkårResultater = nyVilkårsvurdering.personResultater.find { it.erSøkersResultater() }?.vilkårResultater
        val søkersUtvidetVilkår = søkersVilkårResultater?.filter { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD }

        Assertions.assertThat(søkersUtvidetVilkår).hasSize(2)

        val utvidetVilkårSortert = søkersUtvidetVilkår?.sortedBy { it.periodeTom }

        Assertions.assertThat(utvidetVilkårSortert?.first()?.periodeTom).isEqualTo(tomPåFørsteUtvidetVilkår)
        Assertions.assertThat(utvidetVilkårSortert?.first()?.periodeFom).isEqualTo(LocalDate.now().minusYears(2))

        Assertions.assertThat(utvidetVilkårSortert?.last()?.periodeTom).isEqualTo(søker.dødsfall?.dødsfallDato)
        Assertions.assertThat(utvidetVilkårSortert?.last()?.periodeFom).isEqualTo(tomPåFørsteUtvidetVilkår.plusMonths(1))

        Assertions.assertThat(søkerVilkårResultater.filter { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }).hasSize(1)
        Assertions.assertThat(søkerVilkårResultater.first { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }.periodeTom).isEqualTo(søker.dødsfall?.dødsfallDato)

        Assertions.assertThat(søkerVilkårResultater.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }).hasSize(1)
        Assertions.assertThat(søkerVilkårResultater.first { it.vilkårType == Vilkår.BOSATT_I_RIKET }.periodeTom).isEqualTo(søker.dødsfall?.dødsfallDato)
    }
}
