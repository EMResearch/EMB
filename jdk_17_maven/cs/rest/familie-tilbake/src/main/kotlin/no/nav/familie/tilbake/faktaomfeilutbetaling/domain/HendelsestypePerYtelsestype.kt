package no.nav.familie.tilbake.faktaomfeilutbetaling.domain

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype

object HendelsestypePerYtelsestype {

    private val HIERARKI = mapOf(
        Ytelsestype.BARNETRYGD to setOf(
            Hendelsestype.ANNET,
            Hendelsestype.SATSER,
            Hendelsestype.SMÅBARNSTILLEGG,
            Hendelsestype.MEDLEMSKAP_BA,
            Hendelsestype.BOR_MED_SØKER,
            Hendelsestype.DØDSFALL,
            Hendelsestype.BOSATT_I_RIKET,
            Hendelsestype.LOVLIG_OPPHOLD,
            Hendelsestype.DELT_BOSTED,
            Hendelsestype.BARNS_ALDER,
            Hendelsestype.UTVIDET,
        ),
        Ytelsestype.OVERGANGSSTØNAD to setOf(
            Hendelsestype.ANNET,
            Hendelsestype.MEDLEMSKAP,
            Hendelsestype.OPPHOLD_I_NORGE,
            Hendelsestype.ENSLIG_FORSØRGER,
            Hendelsestype.OVERGANGSSTØNAD,
            Hendelsestype.YRKESRETTET_AKTIVITET,
            Hendelsestype.STØNADSPERIODE,
            Hendelsestype.INNTEKT,
            Hendelsestype.PENSJONSYTELSER,
            Hendelsestype.DØDSFALL,
        ),
        Ytelsestype.BARNETILSYN to setOf(
            Hendelsestype.ANNET,
            Hendelsestype.MEDLEMSKAP,
            Hendelsestype.OPPHOLD_I_NORGE,
            Hendelsestype.ENSLIG_FORSØRGER,
            Hendelsestype.STØNAD_TIL_BARNETILSYN,
            Hendelsestype.DØDSFALL,
        ),
        Ytelsestype.SKOLEPENGER to setOf(
            Hendelsestype.ANNET,
            Hendelsestype.MEDLEMSKAP,
            Hendelsestype.OPPHOLD_I_NORGE,
            Hendelsestype.ENSLIG_FORSØRGER,
            Hendelsestype.DØDSFALL,
            Hendelsestype.SKOLEPENGER,
        ),
        Ytelsestype.KONTANTSTØTTE to setOf(
            Hendelsestype.VILKÅR_BARN,
            Hendelsestype.VILKÅR_SØKER,
            Hendelsestype.BARN_I_FOSTERHJEM_ELLER_INSTITUSJON,
            Hendelsestype.KONTANTSTØTTENS_STØRRELSE,
            Hendelsestype.STØTTEPERIODE,
            Hendelsestype.UTBETALING,
            Hendelsestype.KONTANTSTØTTE_FOR_ADOPTERTE_BARN,
            Hendelsestype.ANNET_KS,
        ),
    )

    fun getHendelsestyper(ytelsestype: Ytelsestype): Set<Hendelsestype> {
        return HIERARKI[ytelsestype] ?: error("Ikke-støttet ytelsestype: $ytelsestype")
    }
}
