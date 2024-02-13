package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat.IKKE_FULLT_VURDERT
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat.OPPFYLT_BLANDET_REGELVERK
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat.OPPFYLT_EØS_FORORDNINGEN
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat.OPPFYLT_NASJONALE_REGLER
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat.OPPFYLT_REGELVERK_IKKE_SATT
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

fun kombinerVilkårResultaterTilRegelverkResultat(
    personType: PersonType,
    alleVilkårResultater: Iterable<VilkårRegelverkResultat>,
    fagsakType: FagsakType,
    behandlingUnderkategori: BehandlingUnderkategori,
): RegelverkResultat? {
    val nødvendigeVilkår = Vilkår.hentVilkårFor(
        personType = personType,
        fagsakType = fagsakType,
        behandlingUnderkategori = behandlingUnderkategori,
    )
        .filter { it != Vilkår.UTVIDET_BARNETRYGD }

    val regelverkVilkår = nødvendigeVilkår
        .filter { it.harRegelverk }

    val alleVilkårResultaterMedEøs = alleVilkårResultater
        .filter { it.regelverk == Regelverk.EØS_FORORDNINGEN }.map { it.vilkår }

    val alleVilkårResultaterMedNasjonalt = alleVilkårResultater
        .filter { it.regelverk == Regelverk.NASJONALE_REGLER }.map { it.vilkår }

    val erAlleVilkårUtenResultat = alleVilkårResultater.all { it.resultat == null }

    val erAlleNødvendigeVilkårOppfylt = alleVilkårResultater.all { it.resultat == Resultat.OPPFYLT } &&
        alleVilkårResultater.map { it.vilkår }.distinct().containsAll(nødvendigeVilkår)

    val erEttEllerFlereVilkårIkkeOppfylt = alleVilkårResultater.any { it.resultat == Resultat.IKKE_OPPFYLT }

    return when {
        erAlleVilkårUtenResultat -> null
        erEttEllerFlereVilkårIkkeOppfylt -> RegelverkResultat.IKKE_OPPFYLT
        erAlleNødvendigeVilkårOppfylt -> when {
            alleVilkårResultaterMedEøs.containsAll(regelverkVilkår) ->
                OPPFYLT_EØS_FORORDNINGEN
            alleVilkårResultaterMedNasjonalt.containsAll(regelverkVilkår) ->
                OPPFYLT_NASJONALE_REGLER
            (alleVilkårResultaterMedEøs + alleVilkårResultaterMedNasjonalt).isNotEmpty() ->
                OPPFYLT_BLANDET_REGELVERK
            else -> OPPFYLT_REGELVERK_IKKE_SATT
        }
        else -> IKKE_FULLT_VURDERT
    }
}
