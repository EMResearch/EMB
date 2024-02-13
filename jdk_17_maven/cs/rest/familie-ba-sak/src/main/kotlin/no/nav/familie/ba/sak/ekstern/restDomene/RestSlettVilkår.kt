package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

data class RestSlettVilkår(val personIdent: String, val vilkårType: Vilkår)
