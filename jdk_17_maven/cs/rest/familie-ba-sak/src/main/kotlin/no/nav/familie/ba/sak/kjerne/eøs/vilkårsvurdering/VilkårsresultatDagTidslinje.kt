package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.DagTidspunkt.Companion.tilTidspunktEllerUendeligSent
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.DagTidspunkt.Companion.tilTidspunktEllerUendeligTidlig
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat

/**
 * Lager tidslinje av VilkårRegelverkResultat for ett vilkår og én aktør
 * For beregning er vi strengt tatt bare interessert i oppfylte vilkår, og her fjernes alle andre vilkårsresultater
 * Antakelsen er at IKKE_OPPFYLT i ALLE tilfeller kan ignoreres for beregning, og evt bare brukes for info i brev
 * Løser problemet med BOR_MED_SØKER-vilkår som kan være oppfylt mens undervilkåret DELT_BOSTED ikke er oppfylt.
 * Ikke oppfylt DELT_BOSTED er løst funksjonelt ved at BOR_MED_SØKER settes til IKKE_OPPFYLT med fom og tom lik null.
 * fom og tom lik null tolkes som fra uendelig lenge siden til uendelig lenge til, som ville skapt overlapp med oppfylt vilkår
 * Overlapp er ikke støttet av tidsliner, og ville gitt exception
 */
fun Iterable<VilkårResultat>.tilVilkårRegelverkResultatTidslinje() = tidslinje {
    this.filter { it.erOppfylt() }
        .map { it.tilPeriode() }
}

fun VilkårResultat.tilPeriode(): Periode<VilkårRegelverkResultat, Dag> {
    val fom = periodeFom.tilTidspunktEllerUendeligTidlig(periodeTom)
    val tom = periodeTom.tilTidspunktEllerUendeligSent(periodeFom)
    return Periode(
        fom,
        tom,
        VilkårRegelverkResultat(
            vilkår = vilkårType,
            regelverkResultat = this.tilRegelverkResultat(),
            utdypendeVilkårsvurderinger = this.utdypendeVilkårsvurderinger,
        ),
    )
}
