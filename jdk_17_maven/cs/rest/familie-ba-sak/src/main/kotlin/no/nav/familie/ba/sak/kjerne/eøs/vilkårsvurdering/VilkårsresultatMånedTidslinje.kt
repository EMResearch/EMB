package no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering

import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Dag
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.tilMånedFraMånedsskifteIkkeNull

/**
 * Extension-funksjon som konverterer en dag-basert tidslinje til en måned-basert tidslinje med VilkårRegelverkResultat
 * Funksjonen itererer fra måneden FØR fra-og-med-måned til måneden ETTER til-og-med-måneden for å ta hensyn til uendelighet
 * Reglene er at vilkårret for siste dag i forrige måned og første dag i inneværende måned må være oppfylt
 * Da brukes regelverket for inneværende måned. Dvs slik:
 * 2020-04-30   | 2020-05-01    -> Resultat
 * Oppfylt EØS  | Oppfylt Nasj. -> 2020-05 Oppfylt Nasj
 * Oppfylt Nasj | Opppfylt EØS  -> 2020-05 Oppfylt EØS
 * Oppfylt Nasj | Opppfylt Nasj -> 2020-05 Oppfylt Nasj
 * Oppfylt EØS  | Opppfylt EØS  -> 2020-05 Oppfylt EØS
 * Oppfylt EØS  | Ikke oppfylt  -> <Tomt>
 * Oppfylt Nasj | Ikke oppfylt  -> <Tomt>
 * Ikke oppfylt | Oppfylt EØS   -> <Tomt>
 * Ikke oppfylt | Oppfylt Nasj  -> <Tomt>
 */
fun Tidslinje<VilkårRegelverkResultat, Dag>.tilMånedsbasertTidslinjeForVilkårRegelverkResultat() = this
    .tilMånedFraMånedsskifteIkkeNull { sisteDagForrigeMåned, førsteDagDenneMåned ->
        if (sisteDagForrigeMåned.erOppfylt() && førsteDagDenneMåned.erOppfylt()) førsteDagDenneMåned else null
    }
