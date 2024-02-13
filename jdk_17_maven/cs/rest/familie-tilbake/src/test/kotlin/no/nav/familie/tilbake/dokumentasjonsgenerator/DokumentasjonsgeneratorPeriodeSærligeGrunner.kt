package no.nav.familie.tilbake.dokumentasjonsgenerator

import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.beregning.modell.Vedtaksresultat
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.handlebars.FellesTekstformaterer
import no.nav.familie.tilbake.dokumentbestilling.vedtak.AvsnittUtil
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbBehandling
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbHjemmel
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbKonfigurasjon
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbPerson
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbTotalresultat
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVarsel
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevDatoer
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevFelles
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevPeriodeOgFelles
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.Vedtaksbrevstype
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbFakta
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbKravgrunnlag
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbResultat
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbSærligeGrunner
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbVedtaksbrevsperiode
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbVurderinger
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.SærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/**
 * Brukes for å generere tekster for særlige grunner for perioder. Resultatet er tekster med markup, som med
 * "Insert markup"-macroen kan limes inn i Confluence, og dermed bli formattert tekst.
 *
 * Confluence:
 * https://confluence.adeo.no/display/TFA/Generert+dokumentasjon
 */
@Disabled("Kjøres ved behov for å regenerere dokumentasjon")
class DokumentasjonsgeneratorPeriodeSærligeGrunner {

    private val januar = Datoperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 1))

    @Test
    fun `list ut særlige grunner forstod burde forstått simpel uaktsomhet bokmål`() {
        val felles: HbVedtaksbrevFelles = lagFellesdel(Språkkode.NB)
        lagSærligeGrunnerTekster(felles, Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT, Aktsomhet.SIMPEL_UAKTSOMHET)
    }

    @Test
    fun `list ut særlige grunner forstod burde forstått simpel uaktsomhet nynorsk`() {
        val felles: HbVedtaksbrevFelles = lagFellesdel(Språkkode.NN)
        lagSærligeGrunnerTekster(felles, Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT, Aktsomhet.SIMPEL_UAKTSOMHET)
    }

    @Test
    fun `list ut særlige grunner forstod burde forstått grov uaktsomhet bokmål`() {
        val felles: HbVedtaksbrevFelles = lagFellesdel(Språkkode.NB)
        lagSærligeGrunnerTekster(felles, Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT, Aktsomhet.GROV_UAKTSOMHET)
    }

    @Test
    fun `list ut særlige grunner forstod burde forstått grov uaktsomhet nynorsk`() {
        val felles: HbVedtaksbrevFelles = lagFellesdel(Språkkode.NN)
        lagSærligeGrunnerTekster(felles, Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT, Aktsomhet.GROV_UAKTSOMHET)
    }

    @Test
    fun `list ut særlige grunner feilaktig mangelfulle opplysninger simpel uaktsomhet bokmål`() {
        val felles: HbVedtaksbrevFelles = lagFellesdel(Språkkode.NB)
        lagSærligeGrunnerTekster(felles, Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER, Aktsomhet.SIMPEL_UAKTSOMHET)
    }

    @Test
    fun `list ut særlige grunner feilaktig mangelfulle opplysninger simpel uaktsomhet nynorsk`() {
        val felles: HbVedtaksbrevFelles = lagFellesdel(Språkkode.NN)
        lagSærligeGrunnerTekster(felles, Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER, Aktsomhet.SIMPEL_UAKTSOMHET)
    }

    @Test
    fun `list ut særlige grunner feilaktig mangelfulle opplysninger grov uaktsomhet bokmål`() {
        val felles: HbVedtaksbrevFelles = lagFellesdel(Språkkode.NB)
        lagSærligeGrunnerTekster(felles, Vilkårsvurderingsresultat.MANGELFULLE_OPPLYSNINGER_FRA_BRUKER, Aktsomhet.GROV_UAKTSOMHET)
    }

    @Test
    fun `list ut særlige grunner feilaktig mangelfulle opplysninger grov uaktsomhet nynorsk`() {
        val felles: HbVedtaksbrevFelles = lagFellesdel(Språkkode.NN)
        lagSærligeGrunnerTekster(felles, Vilkårsvurderingsresultat.MANGELFULLE_OPPLYSNINGER_FRA_BRUKER, Aktsomhet.GROV_UAKTSOMHET)
    }

    private fun lagSærligeGrunnerTekster(
        felles: HbVedtaksbrevFelles,
        forstoBurdeForstått: Vilkårsvurderingsresultat,
        simpelUaktsom: Aktsomhet,
    ) {
        val boolske = booleanArrayOf(false, true)
        for (sgNav in boolske) {
            for (sgBeløp in boolske) {
                for (sgTid in boolske) {
                    for (reduksjon in boolske) {
                        for (sgAnnet in boolske) {
                            lagSærligeGrunnerTekster(
                                felles,
                                forstoBurdeForstått,
                                simpelUaktsom,
                                sgNav,
                                sgBeløp,
                                sgTid,
                                reduksjon,
                                sgAnnet,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun lagSærligeGrunnerTekster(
        felles: HbVedtaksbrevFelles,
        vilkårResultat: Vilkårsvurderingsresultat,
        aktsomhet: Aktsomhet,
        sgNav: Boolean,
        sgBeløp: Boolean,
        sgTid: Boolean,
        reduksjon: Boolean,
        sgAnnet: Boolean,
    ) {
        val periode: HbVedtaksbrevsperiode = lagPeriodeDel(vilkårResultat, aktsomhet, sgNav, sgBeløp, sgTid, sgAnnet, reduksjon)
        val s: String = FellesTekstformaterer.lagDeltekst(
            HbVedtaksbrevPeriodeOgFelles(felles, periode),
            AvsnittUtil.PARTIAL_PERIODE_SÆRLIGE_GRUNNER,
        )
        val overskrift = overskrift(sgNav, sgBeløp, sgTid, sgAnnet, reduksjon)
        val prettyPrint = prettyPrint(s, overskrift)
        println()
        println(prettyPrint)
    }

    private fun overskrift(sgNav: Boolean, sgBeløp: Boolean, sgTid: Boolean, sgAnnet: Boolean, reduksjon: Boolean): String {
        val deler: MutableList<String> = ArrayList()
        deler.add("grad av uaktsomhet")
        if (sgNav) {
            deler.add("NAV helt/delvis skyld")
        }
        if (sgBeløp) {
            deler.add("størrelsen på beløpet")
        }
        if (sgTid) {
            deler.add("hvor lang tid har det gått")
        }
        if (reduksjon) {
            deler.add("reduksjon")
        }
        if (sgAnnet) {
            deler.add("annet")
        }
        return deler.joinToString(" - ", "*[ ", " ]*")
    }

    private fun lagPeriodeDel(
        vilkårResultat: Vilkårsvurderingsresultat,
        aktsomhet: Aktsomhet,
        sgNav: Boolean,
        sgBeløp: Boolean,
        sgTid: Boolean,
        sgAnnet: Boolean,
        reduksjon: Boolean,
    ): HbVedtaksbrevsperiode {
        val sg: MutableList<SærligGrunn> = ArrayList()
        if (sgNav) {
            sg.add(SærligGrunn.HELT_ELLER_DELVIS_NAVS_FEIL)
        }
        if (sgBeløp) {
            sg.add(SærligGrunn.STØRRELSE_BELØP)
        }
        if (sgTid) {
            sg.add(SærligGrunn.TID_FRA_UTBETALING)
        }
        if (sgAnnet) {
            sg.add(SærligGrunn.ANNET)
        }
        val fritekstSærligeGrunnerAnnet = "[ fritekst her ]"
        return HbVedtaksbrevsperiode(
            periode = januar,
            kravgrunnlag = HbKravgrunnlag(feilutbetaltBeløp = BigDecimal.valueOf(1000)),
            fakta = HbFakta(Hendelsestype.BARNS_ALDER, Hendelsesundertype.BARN_OVER_6_ÅR),
            vurderinger = HbVurderinger(
                foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                vilkårsvurderingsresultat = vilkårResultat,
                aktsomhetsresultat = aktsomhet,
                særligeGrunner = HbSærligeGrunner(sg, null, fritekstSærligeGrunnerAnnet),
            ),
            resultat = HbResultat(
                tilbakekrevesBeløp =
                BigDecimal.valueOf(if (reduksjon) 500L else 1000L),
                tilbakekrevesBeløpUtenSkattMedRenter =
                BigDecimal.valueOf(if (reduksjon) 400L else 800L),
                rentebeløp = BigDecimal.ZERO,
            ),
            førstePeriode = true,
        )
    }

    private fun lagFellesdel(språkkode: Språkkode): HbVedtaksbrevFelles {
        val datoer = HbVedtaksbrevDatoer(
            LocalDate.of(2018, 3, 2),
            LocalDate.of(2018, 3, 3),
            LocalDate.of(2018, 3, 4),
        )

        return HbVedtaksbrevFelles(
            brevmetadata = lagMetadata(språkkode),
            fagsaksvedtaksdato = LocalDate.now(),
            behandling = HbBehandling(),
            hjemmel = HbHjemmel("Folketrygdloven"),
            totalresultat = HbTotalresultat(
                hovedresultat = Vedtaksresultat.FULL_TILBAKEBETALING,
                totaltRentebeløp = BigDecimal.valueOf(1000),
                totaltTilbakekrevesBeløp = BigDecimal.valueOf(10000),
                totaltTilbakekrevesBeløpMedRenter = BigDecimal.valueOf(11000),
                totaltTilbakekrevesBeløpMedRenterUtenSkatt =
                BigDecimal.valueOf(6855),
            ),
            totaltFeilutbetaltBeløp = BigDecimal.valueOf(6855),
            varsel = HbVarsel(
                varsletBeløp = BigDecimal.valueOf(10000),
                varsletDato = LocalDate.now().minusDays(100),
            ),
            konfigurasjon = HbKonfigurasjon(klagefristIUker = 6),
            søker = HbPerson(navn = "Søker Søkersen"),
            datoer = datoer,
            vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
        )
    }

    private fun lagMetadata(språkkode: Språkkode): Brevmetadata {
        return Brevmetadata(
            sakspartId = "",
            sakspartsnavn = "",
            mottageradresse = Adresseinfo("01020312345", "Bob"),
            behandlendeEnhetsNavn = "Oslo",
            ansvarligSaksbehandler = "Bob",
            saksnummer = "1232456",
            språkkode = språkkode,
            ytelsestype = Ytelsestype.BARNETRYGD,
            gjelderDødsfall = false,
        )
    }

    private fun prettyPrint(s: String, overskrift: String): String {
        return s.replace("__Er det særlige grunner til å redusere beløpet?", overskrift)
            .replace("__Er det særlege grunnar til å redusere beløpet?", overskrift)
            .replace(" 500\u00A0kroner", " <kravbeløp> kroner")
    }
}
