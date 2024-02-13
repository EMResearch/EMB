package no.nav.familie.ba.sak.datagenerator.vilkårsvurdering

import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.Personident
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.LocalDate

typealias AktørId = String

/**
 * Setter vilkår som ikke er overstyrte til oppfylt fra det seneste av
 *      fødselsdato eller tre år tilbake i tid for personen som vurderes.
 * Dersom personen er et barn settes også tom-datoen til barnets attenårsdag.
 * Om du vil ha med utvidet vilkår og delt bosted må det sendes med uansett.
 **/
fun lagVilkårsvurderingMedOverstyrendeResultater(
    søker: Person,
    barna: List<Person>,
    behandling: Behandling? = null,
    id: Long = 0,
    overstyrendeVilkårResultater: Map<AktørId, List<VilkårResultat>>,
): Vilkårsvurdering {
    val vilkårsvurdering = Vilkårsvurdering(behandling = behandling ?: mockk(relaxed = true), id = id)

    val søkerPersonResultater = lagPersonResultatAvOverstyrteResultater(
        person = søker,
        overstyrendeVilkårResultater = overstyrendeVilkårResultater[søker.aktør.aktørId] ?: emptyList(),
        vilkårsvurdering = vilkårsvurdering,
        id = id,
    )

    val barnaPersonResultater = barna.map {
        lagPersonResultatAvOverstyrteResultater(
            person = it,
            overstyrendeVilkårResultater = overstyrendeVilkårResultater[it.aktør.aktørId] ?: emptyList(),
            vilkårsvurdering = vilkårsvurdering,
        )
    }

    vilkårsvurdering.personResultater = barnaPersonResultater.toSet() + søkerPersonResultater
    return vilkårsvurdering
}

fun lagVilkårsvurderingFraRestScenario(
    scenario: RestScenario,
    overstyrendeVilkårResultater: Map<AktørId, List<VilkårResultat>>,
): Vilkårsvurdering {
    fun RestScenarioPerson.TilAktør() = Aktør(
        this.aktørId!!,
        mutableSetOf(Personident(this.ident!!, mockk(relaxed = true))),
    )

    val søker =
        lagPerson(
            aktør = scenario.søker.TilAktør(),
            fødselsdato = LocalDate.parse(scenario.søker.fødselsdato),
            type = PersonType.SØKER,
        )
    val barna = scenario.barna.map {
        lagPerson(
            aktør = it.TilAktør(),
            fødselsdato = LocalDate.parse(it.fødselsdato),
            type = PersonType.BARN,
        )
    }
    return lagVilkårsvurderingMedOverstyrendeResultater(
        søker = søker,
        barna = barna,
        overstyrendeVilkårResultater = overstyrendeVilkårResultater,
    )
}

fun lagSøkerVilkårResultat(
    søkerPersonResultat: PersonResultat,
    periodeFom: LocalDate,
    periodeTom: LocalDate? = null,
    behandlingId: Long,
): Set<VilkårResultat> {
    return setOf(
        lagVilkårResultat(
            personResultat = søkerPersonResultat,
            vilkårType = Vilkår.BOSATT_I_RIKET,
            resultat = Resultat.OPPFYLT,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            behandlingId = behandlingId,
        ),
        lagVilkårResultat(
            personResultat = søkerPersonResultat,
            vilkårType = Vilkår.LOVLIG_OPPHOLD,
            resultat = Resultat.OPPFYLT,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            behandlingId = behandlingId,
        ),
    )
}

fun lagBarnVilkårResultat(
    barnPersonResultat: PersonResultat,
    barnetsFødselsdato: LocalDate,
    behandlingId: Long,
    periodeFom: LocalDate,
    flytteSak: Boolean = false,
): Set<VilkårResultat> {
    return setOf(
        lagVilkårResultat(
            personResultat = barnPersonResultat,
            vilkårType = Vilkår.UNDER_18_ÅR,
            resultat = Resultat.OPPFYLT,
            periodeFom = barnetsFødselsdato,
            periodeTom = barnetsFødselsdato.plusYears(18).minusMonths(1),
            behandlingId = behandlingId,
        ),
        lagVilkårResultat(
            personResultat = barnPersonResultat,
            vilkårType = Vilkår.GIFT_PARTNERSKAP,
            resultat = Resultat.OPPFYLT,
            periodeFom = barnetsFødselsdato,
            periodeTom = null,
            behandlingId = behandlingId,
        ),
        lagVilkårResultat(
            personResultat = barnPersonResultat,
            vilkårType = Vilkår.BOR_MED_SØKER,
            resultat = Resultat.OPPFYLT,
            periodeFom = periodeFom,
            periodeTom = null,
            behandlingId = behandlingId,
        ),
        lagVilkårResultat(
            personResultat = barnPersonResultat,
            vilkårType = Vilkår.BOSATT_I_RIKET,
            resultat = Resultat.OPPFYLT,
            periodeFom = if (flytteSak) barnetsFødselsdato else periodeFom,
            periodeTom = null,
            behandlingId = behandlingId,
        ),
        lagVilkårResultat(
            personResultat = barnPersonResultat,
            vilkårType = Vilkår.LOVLIG_OPPHOLD,
            resultat = Resultat.OPPFYLT,
            periodeFom = if (flytteSak) barnetsFødselsdato else periodeFom,
            periodeTom = null,
            behandlingId = behandlingId,
        ),
    )
}
