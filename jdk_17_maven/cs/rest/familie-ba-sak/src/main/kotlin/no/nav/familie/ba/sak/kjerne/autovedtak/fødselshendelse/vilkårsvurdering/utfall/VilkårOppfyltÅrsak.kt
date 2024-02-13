package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.utfall

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.EvalueringÅrsak
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

enum class VilkårOppfyltÅrsak(val beskrivelse: String, val vilkår: Vilkår) : EvalueringÅrsak {

    // Under 18 år
    ER_UNDER_18_ÅR("Barn er under 18 år", Vilkår.UNDER_18_ÅR),

    // Bor med søker
    SØKER_ER_MOR("Søker er mor", Vilkår.BOR_MED_SØKER),
    BARNET_BOR_MED_MOR("Barnet bor med mor", Vilkår.BOR_MED_SØKER),

    // Gift eller partnerskap hos barn
    BARN_MANGLER_SIVILSTAND("Barn mangler informasjon om sivilstand.", Vilkår.GIFT_PARTNERSKAP),
    BARN_ER_IKKE_GIFT_ELLER_HAR_PARTNERSKAP(
        "Person er ikke gift eller har registrert partner",
        Vilkår.GIFT_PARTNERSKAP,
    ),

    // Bosatt i riket
    BOR_I_RIKET("Er bosatt i riket", Vilkår.BOSATT_I_RIKET),
    BOR_I_RIKET_KUN_ADRESSER_UTEN_FOM(
        "Er bosatt i riket - har kun adresser uten fra- og med dato",
        Vilkår.BOSATT_I_RIKET,
    ),

    // Lovlig opphold
    AUTOMATISK_VURDERING_BARN_LOVLIG_OPPHOLD(
        "Ikke separat oppholdsvurdering for barnet ved automatisk vedtak.",
        Vilkår.LOVLIG_OPPHOLD,
    ),
    NORDISK_STATSBORGER("Er nordisk statsborger.", Vilkår.LOVLIG_OPPHOLD),
    TREDJELANDSBORGER_MED_LOVLIG_OPPHOLD("Er tredjelandsborger med lovlig opphold", Vilkår.LOVLIG_OPPHOLD),
    UKJENT_STATSBORGERSKAP_MED_LOVLIG_OPPHOLD(
        "Er statsløs eller mangler statsborgerskap med lovlig opphold",
        Vilkår.LOVLIG_OPPHOLD,
    ),
    EØS_MED_LØPENDE_ARBEIDSFORHOLD(
        "Mor er EØS-borger, men har et løpende arbeidsforhold i Norge.",
        Vilkår.LOVLIG_OPPHOLD,
    ),
    ANNEN_FORELDER_NORDISK("Annen forelder er norsk eller nordisk statsborger.", Vilkår.LOVLIG_OPPHOLD),
    ANNEN_FORELDER_EØS_MEN_MED_LØPENDE_ARBEIDSFORHOLD(
        "Annen forelder er fra EØS, men har et løpende arbeidsforhold i Norge.",
        Vilkår.LOVLIG_OPPHOLD,
    ),
    MOR_BODD_OG_JOBBET_I_NORGE_SISTE_5_ÅR("Mor har bodd og jobbet i Norge siste 5 år.", Vilkår.LOVLIG_OPPHOLD),
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
