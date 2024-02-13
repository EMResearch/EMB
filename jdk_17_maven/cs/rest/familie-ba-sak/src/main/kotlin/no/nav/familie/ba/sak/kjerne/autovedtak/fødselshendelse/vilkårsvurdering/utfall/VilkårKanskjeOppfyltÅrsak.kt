package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.utfall

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.EvalueringÅrsak
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

enum class VilkårKanskjeOppfyltÅrsak(val beskrivelse: String, val vilkår: Vilkår) : EvalueringÅrsak {

    // Lovlig opphold
    LOVLIG_OPPHOLD_MÅ_VURDERE_LENGDEN_PÅ_OPPHOLDSTILLATELSEN(
        "Må vurdere lengden på oppholdstillatelsen.",
        Vilkår.LOVLIG_OPPHOLD,
    ),
    LOVLIG_OPPHOLD_IKKE_MULIG_Å_FASTSETTE("Kan ikke avgjøre om personen har lovlig opphold.", Vilkår.LOVLIG_OPPHOLD),
    LOVLIG_OPPHOLD_ANNEN_FORELDER_IKKE_MULIG_Å_FASTSETTE(
        "Kan ikke avgjøre om annen har lovlig opphold.",
        Vilkår.LOVLIG_OPPHOLD,
    ),
    ;

    override fun hentBeskrivelse(): String {
        return beskrivelse
    }

    override fun hentMetrikkBeskrivelse(): String {
        return beskrivelse
    }

    override fun hentIdentifikator(): String {
        return vilkår.name
    }
}
