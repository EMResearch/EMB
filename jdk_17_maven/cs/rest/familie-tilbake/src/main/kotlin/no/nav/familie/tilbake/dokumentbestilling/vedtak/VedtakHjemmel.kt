package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.beregning.modell.Vedtaksresultat
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbHjemmel
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsperiode
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat

object VedtakHjemmel {

    private val Vilkårsvurderingsresultat_MED_FORSETT_ALLTID_RENTER: List<Vilkårsvurderingsresultat> =
        listOf(
            Vilkårsvurderingsresultat.MANGELFULLE_OPPLYSNINGER_FRA_BRUKER,
            Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
        )

    fun lagHjemmel(
        vedtaksresultatstype: Vedtaksresultat,
        vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag,
        effektForBruker: EffektForBruker,
        språkkode: Språkkode,
        visHjemmelForRenter: Boolean,
        klagebehandling: Boolean,
    ): HbHjemmel {
        val foreldetVanlig = erNoeSattTilVanligForeldet(vedtaksbrevgrunnlag.vurdertForeldelse)
        val foreldetMedTilleggsfrist = erTilleggsfristBenyttet(vedtaksbrevgrunnlag.vurdertForeldelse)
        val ignorerteSmåbeløp = heleVurderingPgaSmåbeløp(
            vedtaksresultatstype,
            vedtaksbrevgrunnlag.vilkårsvurderingsperioder,
        )
        val renter = visHjemmelForRenter && erRenterBenyttet(vedtaksbrevgrunnlag.vilkårsvurderingsperioder)
        val barnetrygd = Ytelsestype.BARNETRYGD == vedtaksbrevgrunnlag.ytelsestype
        val kontantstøtte = Ytelsestype.KONTANTSTØTTE == vedtaksbrevgrunnlag.ytelsestype
        val hjemler: MutableList<Hjemler> = ArrayList()
        if (vedtaksbrevgrunnlag.vilkårsvurderingsperioder.isNotEmpty()) {
            when {
                barnetrygd && ignorerteSmåbeløp -> hjemler.addAll(setOf(Hjemler.BARNETRYGD_13, Hjemler.FOLKETRYGD_22_15_SJETTE))
                ignorerteSmåbeløp -> hjemler.add(Hjemler.FOLKETRYGD_22_15_SJETTE)
                barnetrygd -> hjemler.addAll(setOf(Hjemler.BARNETRYGD_13, Hjemler.FOLKETRYGD_22_15))
                kontantstøtte -> hjemler.addAll(setOf(Hjemler.KONTANTSTØTTE_11, Hjemler.FOLKETRYGD_22_15))
                renter -> hjemler.add(Hjemler.FOLKETRYGD_22_15_OG_22_17_A)
                else -> hjemler.add(Hjemler.FOLKETRYGD_22_15)
            }
        }
        if (foreldetMedTilleggsfrist) {
            hjemler.add(Hjemler.FORELDELSE_2_3_OG_10)
        } else if (foreldetVanlig) {
            hjemler.add(Hjemler.FORELDELSE_2_3)
        }

        if (!klagebehandling) {
            if (EffektForBruker.ENDRET_TIL_GUNST_FOR_BRUKER == effektForBruker) {
                hjemler.add(Hjemler.FORVALTNING_35_A)
            }
            if (EffektForBruker.ENDRET_TIL_UGUNST_FOR_BRUKER == effektForBruker) {
                hjemler.add(Hjemler.FORVALTNING_35_C)
            }
        }
        val hjemmelstekst = join(hjemler, " og ", språkkode)
        return HbHjemmel(hjemmelstekst, hjemmelstekst.contains("og"))
    }

    private fun erRenterBenyttet(vilkårPerioder: Set<Vilkårsvurderingsperiode>): Boolean {
        return vilkårPerioder.any {
            it.aktsomhet?.ileggRenter == true || erForsettOgAlltidRenter(it)
        }
    }

    private fun erForsettOgAlltidRenter(v: Vilkårsvurderingsperiode): Boolean {
        return Vilkårsvurderingsresultat_MED_FORSETT_ALLTID_RENTER.contains(v.vilkårsvurderingsresultat) &&
            Aktsomhet.FORSETT == v.aktsomhet?.aktsomhet
    }

    private fun heleVurderingPgaSmåbeløp(
        vedtakResultatType: Vedtaksresultat,
        vilkårPerioder: Set<Vilkårsvurderingsperiode>,
    ): Boolean {
        return Vedtaksresultat.INGEN_TILBAKEBETALING == vedtakResultatType &&
            vilkårPerioder.any { false == it.aktsomhet?.tilbakekrevSmåbeløp }
    }

    private fun erTilleggsfristBenyttet(foreldelse: VurdertForeldelse?): Boolean {
        return foreldelse?.foreldelsesperioder?.any { it.foreldelsesvurderingstype == Foreldelsesvurderingstype.TILLEGGSFRIST }
            ?: false
    }

    private fun erNoeSattTilVanligForeldet(foreldelse: VurdertForeldelse?): Boolean {
        return foreldelse?.foreldelsesperioder?.any { it.foreldelsesvurderingstype == Foreldelsesvurderingstype.FORELDET }
            ?: false
    }

    private fun join(
        elementer: List<Hjemler>,
        sisteSkille: String,
        lokale: Språkkode,
    ): String {
        val lokalListe = elementer.map { it.hjemmelTekst(lokale) }
        if (lokalListe.size == 1) {
            return lokalListe.first()!!
        }
        return lokalListe.subList(0, elementer.size - 1).joinToString(", ") + sisteSkille + lokalListe.last()
    }

    enum class EffektForBruker {
        FØRSTEGANGSVEDTAK,
        ENDRET_TIL_GUNST_FOR_BRUKER,
        ENDRET_TIL_UGUNST_FOR_BRUKER,
    }

    private enum class Hjemler(bokmål: String, nynorsk: String) {
        FOLKETRYGD_22_15("folketrygdloven § 22-15", "folketrygdlova § 22-15"),
        FOLKETRYGD_22_15_SJETTE("folketrygdloven § 22-15 sjette ledd", "folketrygdlova § 22-15 sjette ledd"),
        FOLKETRYGD_22_15_OG_22_17_A("folketrygdloven §§ 22-15 og 22-17 a", "folketrygdlova §§ 22-15 og 22-17 a"),
        FORELDELSE_2_3_OG_10("foreldelsesloven §§ 2, 3 og 10", "foreldingslova §§ 2, 3 og 10"),
        FORELDELSE_2_3("foreldelsesloven §§ 2 og 3", "foreldingslova §§ 2 og 3"),
        FORVALTNING_35_A("forvaltningsloven § 35 a)", "forvaltningslova § 35 a)"),
        FORVALTNING_35_C("forvaltningsloven § 35 c)", "forvaltningslova § 35 c)"),
        KONTANTSTØTTE_11("kontantstøtteloven § 11", "kontantstøttelova § 11"),
        BARNETRYGD_13("barnetrygdloven § 13", "barnetrygdlova § 13"),
        ;

        private val hjemmelTekster = mapOf(
            Språkkode.NB to bokmål,
            Språkkode.NN to nynorsk,
        )

        fun hjemmelTekst(språkkode: Språkkode): String? {
            return hjemmelTekster.getOrDefault(språkkode, hjemmelTekster[Språkkode.NB])
        }
    }
}
