package no.nav.familie.tilbake.faktaomfeilutbetaling.domain

object HendelsesundertypePerHendelsestype {

    val HIERARKI = mapOf(
        Hendelsestype.ANNET to setOf(Hendelsesundertype.ANNET_FRITEKST),
        Hendelsestype.SATSER to setOf(Hendelsesundertype.SATSENDRING),
        Hendelsestype.SMÅBARNSTILLEGG to setOf(
            Hendelsesundertype.SMÅBARNSTILLEGG_3_ÅR,
            Hendelsesundertype.SMÅBARNSTILLEGG_OVERGANGSSTØNAD,
        ),
        Hendelsestype.BOR_MED_SØKER to setOf(Hendelsesundertype.BOR_IKKE_MED_BARN),
        Hendelsestype.BOSATT_I_RIKET to setOf(
            Hendelsesundertype.BARN_FLYTTET_FRA_NORGE,
            Hendelsesundertype.BRUKER_FLYTTET_FRA_NORGE,
            Hendelsesundertype.BARN_BOR_IKKE_I_NORGE,
            Hendelsesundertype.BRUKER_BOR_IKKE_I_NORGE,
            Hendelsesundertype.BRUKER_OG_BARN_FLYTTET_FRA_NORGE,
            Hendelsesundertype.BRUKER_OG_BARN_BOR_IKKE_I_NORGE,
        ),
        Hendelsestype.LOVLIG_OPPHOLD to setOf(Hendelsesundertype.UTEN_OPPHOLDSTILLATELSE),
        Hendelsestype.DØDSFALL to setOf(
            Hendelsesundertype.BARN_DØD,
            Hendelsesundertype.BRUKER_DØD,
        ),
        Hendelsestype.DELT_BOSTED to setOf(
            Hendelsesundertype.ENIGHET_OM_OPPHØR_DELT_BOSTED,
            Hendelsesundertype.UENIGHET_OM_OPPHØR_DELT_BOSTED,
            Hendelsesundertype.FLYTTET_SAMMEN,
        ),
        Hendelsestype.BARNS_ALDER to setOf(
            Hendelsesundertype.BARN_OVER_6_ÅR,
            Hendelsesundertype.BARN_OVER_18_ÅR,
        ),
        Hendelsestype.MEDLEMSKAP to setOf(
            Hendelsesundertype.MEDLEM_SISTE_5_ÅR,
            Hendelsesundertype.LOVLIG_OPPHOLD,
        ),
        Hendelsestype.MEDLEMSKAP_BA to setOf(
            Hendelsesundertype.UTENLANDS_IKKE_MEDLEM,
            Hendelsesundertype.MEDLEMSKAP_OPPHØRT,
            Hendelsesundertype.ANNEN_FORELDER_IKKE_MEDLEM,
            Hendelsesundertype.ANNEN_FORELDER_OPPHØRT_MEDLEMSKAP,
            Hendelsesundertype.FLERE_UTENLANDSOPPHOLD,
            Hendelsesundertype.BOSATT_IKKE_MEDLEM,
        ),
        Hendelsestype.UTVIDET to setOf(
            Hendelsesundertype.GIFT,
            Hendelsesundertype.NYTT_BARN,
            Hendelsesundertype.SAMBOER_12_MÅNEDER,
            Hendelsesundertype.FLYTTET_SAMMEN_ANNEN_FORELDER,
            Hendelsesundertype.FLYTTET_SAMMEN_EKTEFELLE,
            Hendelsesundertype.FLYTTET_SAMMEN_SAMBOER,
            Hendelsesundertype.GIFT_IKKE_EGEN_HUSHOLDNING,
            Hendelsesundertype.SAMBOER_IKKE_EGEN_HUSHOLDNING,
            Hendelsesundertype.EKTEFELLE_AVSLUTTET_SONING,
            Hendelsesundertype.SAMBOER_AVSLUTTET_SONING,
            Hendelsesundertype.EKTEFELLE_INSTITUSJON,
            Hendelsesundertype.SAMBOER_INSTITUSJON,
        ),
        Hendelsestype.OPPHOLD_I_NORGE to setOf(
            Hendelsesundertype.BRUKER_IKKE_OPPHOLD_I_NORGE,
            Hendelsesundertype.BARN_IKKE_OPPHOLD_I_NORGE,
            Hendelsesundertype.BRUKER_FLYTTET_FRA_NORGE,
            Hendelsesundertype.BARN_FLYTTET_FRA_NORGE,
            Hendelsesundertype.OPPHOLD_UTLAND_6_UKER_ELLER_MER,
        ),
        Hendelsestype.ENSLIG_FORSØRGER to setOf(
            Hendelsesundertype.UGIFT,
            Hendelsesundertype.SEPARERT_SKILT,
            Hendelsesundertype.SAMBOER,
            Hendelsesundertype.NYTT_BARN_SAMME_PARTNER,
            Hendelsesundertype.ENDRET_SAMVÆRSORDNING,
            Hendelsesundertype.BARN_FLYTTET,
            Hendelsesundertype.NÆRE_BOFORHOLD,
            Hendelsesundertype.FORELDRE_LEVER_SAMMEN,
        ),
        Hendelsestype.OVERGANGSSTØNAD to setOf(Hendelsesundertype.BARN_8_ÅR),
        Hendelsestype.YRKESRETTET_AKTIVITET to setOf(
            Hendelsesundertype.ARBEID,
            Hendelsesundertype.REELL_ARBEIDSSØKER,
            Hendelsesundertype.UTDANNING,
            Hendelsesundertype.ETABLERER_EGEN_VIRKSOMHET,
            Hendelsesundertype.BARN_FYLT_1_ÅR,

        ),
        Hendelsestype.STØNADSPERIODE to setOf(
            Hendelsesundertype.HOVEDPERIODE_3_ÅR,
            Hendelsesundertype.UTVIDELSE_UTDANNING,
            Hendelsesundertype.UTVIDELSE_SÆRLIG_TILSYNSKREVENDE_BARN,
            Hendelsesundertype.UTVIDELSE_FORBIGÅENDE_SYKDOM,
            Hendelsesundertype.PÅVENTE_AV_SKOLESTART_STARTET_IKKE,
            Hendelsesundertype.PÅVENTE_SKOLESTART_STARTET_TIDLIGERE,
            Hendelsesundertype.PÅVENTE_ARBEIDSTILBUD_STARTET_IKKE,
            Hendelsesundertype.PÅVENTE_ARBEIDSTILBUD_STARTET_TIDLIGERE,
            Hendelsesundertype.PÅVENTE_BARNETILSYN_IKKE_HA_TILSYN,
            Hendelsesundertype.PÅVENTE_BARNETILSYN_STARTET_TIDLIGERE,
            Hendelsesundertype.ARBEIDSSØKER,
        ),
        Hendelsestype.INNTEKT to setOf(
            Hendelsesundertype.ARBEIDSINNTEKT_FÅTT_INNTEKT,
            Hendelsesundertype.ARBEIDSINNTEKT_ENDRET_INNTEKT,
            Hendelsesundertype.ANDRE_FOLKETRYGDYTELSER,
            Hendelsesundertype.SELVSTENDIG_NÆRINGSDRIVENDE_FÅTT_INNTEKT,
            Hendelsesundertype.SELVSTENDIG_NÆRINGSDRIVENDE_ENDRET_INNTEKT,
        ),
        Hendelsestype.PENSJONSYTELSER to setOf(
            Hendelsesundertype.UFØRETRYGD,
            Hendelsesundertype.GJENLEVENDE_EKTEFELLE,
        ),
        Hendelsestype.STØNAD_TIL_BARNETILSYN to setOf(
            Hendelsesundertype.IKKE_ARBEID,
            Hendelsesundertype.EGEN_VIRKSOMHET,
            Hendelsesundertype.TILSYNSUTGIFTER_OPPHØRT,
            Hendelsesundertype.TILSYNSUTGIFTER_ENDRET,
            Hendelsesundertype.FORBIGÅENDE_SYKDOM,
            Hendelsesundertype.ETTER_4_SKOLEÅR_UTGIFTENE_OPPHØRT,
            Hendelsesundertype.ETTER_4_SKOLEÅR_ENDRET_ARBEIDSTID,
            Hendelsesundertype.INNTEKT_OVER_6G,
            Hendelsesundertype.KONTANTSTØTTE,
            Hendelsesundertype.ØKT_KONTANTSTØTTE,
        ),
        Hendelsestype.SKOLEPENGER to setOf(
            Hendelsesundertype.IKKE_RETT_TIL_OVERGANGSSTØNAD,
            Hendelsesundertype.SLUTTET_I_UTDANNING,
        ),

        Hendelsestype.VILKÅR_BARN to setOf(
            Hendelsesundertype.FULLTIDSPLASS_BARNEHAGE,
            Hendelsesundertype.DELTIDSPLASS_BARNEHAGEPLASS,
            Hendelsesundertype.BARN_IKKE_BOSATT,
            Hendelsesundertype.BARN_IKKE_OPPHOLDSTILLATELSE,
            Hendelsesundertype.BARN_FLYTTET_FRA_NORGE,
            Hendelsesundertype.BARN_OVER_2_ÅR,
        ),
        Hendelsestype.VILKÅR_SØKER to setOf(
            Hendelsesundertype.DEN_ANDRE_FORELDEREN_IKKE_MEDLEM_FOLKETRYGDEN,
            Hendelsesundertype.DEN_ANDRE_FORELDEREN_IKKE_MEDLEM_FOLKETRYGDEN_ELLER_EØS,
            Hendelsesundertype.SØKER_IKKE_MEDLEM_FOLKETRYGDEN,
            Hendelsesundertype.SØKER_IKKE_MEDLEM_FOLKETRYGDEN_ELLER_EØS,
            Hendelsesundertype.BEGGE_FORELDRENE_IKKE_MEDLEM_FOLKETRYGDEN,
            Hendelsesundertype.BEGGE_FORELDRENE_IKKE_MEDLEM_FOLKETRYGDEN_ELLER_EØS,
            Hendelsesundertype.BARN_BOR_IKKE_HOS_SØKER,
            Hendelsesundertype.UTENLANDSOPPHOLD_OVER_3_MÅNEDER,
            Hendelsesundertype.SØKER_FLYTTET_FRA_NORGE,
            Hendelsesundertype.SØKER_IKKE_BOSATT,
            Hendelsesundertype.SØKER_IKKE_OPPHOLDSTILLATELSE,
            Hendelsesundertype.SØKER_IKKE_OPPHOLDSTILLATELSE_I_MER_ENN_12_MÅNEDER,
        ),
        Hendelsestype.BARN_I_FOSTERHJEM_ELLER_INSTITUSJON to setOf(
            Hendelsesundertype.BARN_I_FOSTERHJEM,
            Hendelsesundertype.BARN_I_INSTITUSJON,

        ),
        Hendelsestype.KONTANTSTØTTENS_STØRRELSE to setOf(
            Hendelsesundertype.FULLTIDSPLASS_BARNEHAGE,
            Hendelsesundertype.DELTIDSPLASS_BARNEHAGEPLASS,
            Hendelsesundertype.ØKT_TIMEANTALL_I_BARNEHAGE,
            Hendelsesundertype.SATSENDRING,
        ),
        Hendelsestype.STØTTEPERIODE to setOf(
            Hendelsesundertype.BARN_2_ÅR,
        ),
        Hendelsestype.UTBETALING to setOf(
            Hendelsesundertype.DELT_BOSTED_AVTALE_OPPHØRT,
            Hendelsesundertype.DOBBELUTBETALING,
        ),
        Hendelsestype.KONTANTSTØTTE_FOR_ADOPTERTE_BARN to setOf(
            Hendelsesundertype.MER_ENN_11_MÅNEDER,
            Hendelsesundertype.BARN_STARTET_PÅ_SKOLEN,
        ),
        Hendelsestype.ANNET_KS to setOf(
            Hendelsesundertype.ANNET_FRITEKST,
            Hendelsesundertype.BARN_DØD,
            Hendelsesundertype.BRUKER_DØD,
        ),

    )

    fun getHendelsesundertyper(hendelsestype: Hendelsestype): Set<Hendelsesundertype> {
        return HIERARKI[hendelsestype] ?: error("Ikke-støttet hendelseType: $hendelsestype")
    }
}
