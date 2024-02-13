package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

data class RestNyttVilkår(
    val personIdent: String,
    val vilkårType: Vilkår,
)
