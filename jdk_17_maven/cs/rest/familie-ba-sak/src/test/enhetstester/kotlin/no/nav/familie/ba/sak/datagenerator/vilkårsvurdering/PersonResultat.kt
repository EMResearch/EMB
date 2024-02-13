package no.nav.familie.ba.sak.datagenerator.vilkårsvurdering

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import java.time.LocalDate

fun lagPersonResultatAvOverstyrteResultater(
    person: Person,
    overstyrendeVilkårResultater: List<VilkårResultat>,
    vilkårsvurdering: Vilkårsvurdering,
    id: Long = 0,

): PersonResultat {
    val personResultat = PersonResultat(
        id = id,
        vilkårsvurdering = vilkårsvurdering,
        aktør = person.aktør,
    )

    val erUtvidet = overstyrendeVilkårResultater.any { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD }

    val vilkårResultater = Vilkår.hentVilkårFor(
        personType = person.type,
        fagsakType = FagsakType.NORMAL,
        behandlingUnderkategori = if (erUtvidet) BehandlingUnderkategori.UTVIDET else BehandlingUnderkategori.ORDINÆR,
    ).foldIndexed(mutableListOf<VilkårResultat>()) { index, acc, vilkårType ->
        val overstyrteVilkårResultaterForVilkår: List<VilkårResultat> = overstyrendeVilkårResultater
            .filter { it.vilkårType == vilkårType }
        if (overstyrteVilkårResultaterForVilkår.isNotEmpty()) {
            acc.addAll(overstyrteVilkårResultaterForVilkår)
        } else {
            acc.add(
                VilkårResultat(
                    id = if (id != 0L) index + 1L else 0L,
                    personResultat = personResultat,
                    periodeFom = if (vilkårType == Vilkår.UNDER_18_ÅR) {
                        person.fødselsdato
                    } else {
                        maxOf(
                            person.fødselsdato,
                            LocalDate.now().minusYears(3),
                        )
                    },
                    periodeTom = if (vilkårType == Vilkår.UNDER_18_ÅR) person.fødselsdato.plusYears(18) else null,
                    vilkårType = vilkårType,
                    resultat = Resultat.OPPFYLT,
                    begrunnelse = "",
                    sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                    utdypendeVilkårsvurderinger = emptyList(),
                ),
            )
        }
        acc
    }.toSet()

    personResultat.setSortedVilkårResultater(vilkårResultater)

    return personResultat
}
