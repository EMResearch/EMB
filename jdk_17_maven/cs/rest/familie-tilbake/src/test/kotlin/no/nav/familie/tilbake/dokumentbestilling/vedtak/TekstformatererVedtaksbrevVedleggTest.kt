package no.nav.familie.tilbake.dokumentbestilling.vedtak

import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.beregning.modell.Vedtaksresultat
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbBehandling
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbHjemmel
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbKonfigurasjon
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbPerson
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbTotalresultat
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVarsel
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevFelles
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevsdata
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
import no.nav.familie.tilbake.vilkårsvurdering.domain.AnnenVurdering
import no.nav.familie.tilbake.vilkårsvurdering.domain.SærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import org.junit.jupiter.api.Test
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Scanner

class TekstformatererVedtaksbrevVedleggTest {

    private val januar = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
    private val februar = Datoperiode(LocalDate.of(2019, 2, 1), LocalDate.of(2019, 2, 28))
    private val mars = Datoperiode(LocalDate.of(2019, 3, 1), LocalDate.of(2019, 3, 31))

    private val brevmetadata = Brevmetadata(
        sakspartId = "123456",
        sakspartsnavn = "Test",
        mottageradresse = Adresseinfo("ident", "bob"),
        behandlendeEnhetsNavn = "NAV Familie- og pensjonsytelser Skien",
        ansvarligSaksbehandler = "Bob",
        saksnummer = "1232456",
        språkkode = Språkkode.NB,
        ytelsestype = Ytelsestype.OVERGANGSSTØNAD,
        gjelderDødsfall = false,
    )

    @Test
    fun `lagVedtaksbrevVedleggHtml skal generere vedlegg med en periode uten renter`() {
        val data = getVedtaksbrevData(Språkkode.NB)

        val generertBrev = TekstformatererVedtaksbrev.lagVedtaksbrevsvedleggHtml(data)

        val fasit = les("/vedtaksbrev/vedlegg/vedlegg_uten_renter.txt")
        generertBrev shouldBe fasit
    }

    @Test
    fun `lagVedtaksbrevVedleggHtml skal generere vedlegg med en periode uten renter nynorsk`() {
        val data = getVedtaksbrevData(Språkkode.NN)

        val generertBrev = TekstformatererVedtaksbrev.lagVedtaksbrevsvedleggHtml(data)

        val fasit = les("/vedtaksbrev/vedlegg/vedlegg_uten_renter_nn.txt")
        generertBrev shouldBe fasit
    }

    @Test
    fun `lagVedtaksbrevVedleggHtml skal generere vedlegg med en periode uten skatt`() {
        val data = getVedtaksbrevData(Språkkode.NB, 10000, 30001, 30001, 0, 0, Ytelsestype.BARNETRYGD)

        val generertBrev = TekstformatererVedtaksbrev.lagVedtaksbrevsvedleggHtml(data)

        val fasit = les("/vedtaksbrev/vedlegg/vedlegg_uten_skatt.txt")
        generertBrev shouldBe fasit
    }

    private fun getVedtaksbrevData(språkkode: Språkkode): HbVedtaksbrevsdata {
        return getVedtaksbrevData(språkkode, 33001, 30001, 20002, 0, 20002 - 16015)
    }

    private fun getVedtaksbrevData(
        språkkode: Språkkode = Språkkode.NB,
        varslet: Int,
        feilutbetalt: Int,
        tilbakekreves: Int,
        renter: Int,
        skatt: Int,
        ytelsestype: Ytelsestype = Ytelsestype.OVERGANGSSTØNAD,
    ): HbVedtaksbrevsdata {
        val vedtaksbrevData =
            lagTestBuilder()
                .copy(
                    brevmetadata = brevmetadata.copy(språkkode = språkkode, ytelsestype = ytelsestype),
                    fagsaksvedtaksdato = LocalDate.now(),
                    totalresultat =
                    HbTotalresultat(
                        hovedresultat = Vedtaksresultat.DELVIS_TILBAKEBETALING,
                        totaltTilbakekrevesBeløp = BigDecimal.valueOf(tilbakekreves.toLong()),
                        totaltTilbakekrevesBeløpMedRenter = BigDecimal.valueOf(
                            (tilbakekreves + renter)
                                .toLong(),
                        ),
                        totaltRentebeløp = BigDecimal.valueOf(renter.toLong()),
                        totaltTilbakekrevesBeløpMedRenterUtenSkatt = BigDecimal
                            .valueOf((tilbakekreves + renter - skatt).toLong()),
                    ),
                    hjemmel = HbHjemmel("Folketrygdloven § 22-15"),
                    varsel = HbVarsel(
                        varsletBeløp = BigDecimal.valueOf(varslet.toLong()),
                        varsletDato = LocalDate.of(2020, 4, 4),
                    ),
                )

        val perioder =
            listOf(
                HbVedtaksbrevsperiode(
                    periode = januar,
                    kravgrunnlag = HbKravgrunnlag
                        .forFeilutbetaltBeløp(BigDecimal.valueOf(feilutbetalt.toLong())),
                    fakta = HbFakta(
                        Hendelsestype.BOSATT_I_RIKET,
                        Hendelsesundertype.BARN_BOR_IKKE_I_NORGE,
                    ),
                    vurderinger =
                    HbVurderinger(
                        foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                        vilkårsvurderingsresultat = Vilkårsvurderingsresultat
                            .MANGELFULLE_OPPLYSNINGER_FRA_BRUKER,
                        aktsomhetsresultat = Aktsomhet.SIMPEL_UAKTSOMHET,
                        fritekst = "Du er heldig som slapp å betale alt!",
                        særligeGrunner =
                        HbSærligeGrunner(
                            listOf(
                                SærligGrunn.TID_FRA_UTBETALING,
                                SærligGrunn.STØRRELSE_BELØP,
                            ),
                        ),
                    ),
                    resultat =
                    HbResultat(
                        tilbakekrevesBeløpUtenSkattMedRenter =
                        BigDecimal.valueOf((tilbakekreves - skatt).toLong()),
                        tilbakekrevesBeløp = BigDecimal.valueOf(tilbakekreves.toLong()),
                        rentebeløp = BigDecimal.valueOf(renter.toLong()),
                    ),
                    førstePeriode = true,
                ),
            )
        return HbVedtaksbrevsdata(vedtaksbrevData, perioder)
    }

    private fun lagTestBuilder(språkkode: Språkkode = Språkkode.NB, ytelsestype: Ytelsestype = Ytelsestype.OVERGANGSSTØNAD) =
        HbVedtaksbrevFelles(
            brevmetadata = brevmetadata.copy(språkkode = språkkode, ytelsestype = ytelsestype),
            hjemmel = HbHjemmel("Folketrygdloven"),
            totalresultat = HbTotalresultat(
                hovedresultat = Vedtaksresultat.FULL_TILBAKEBETALING,
                totaltRentebeløp = BigDecimal.valueOf(1000),
                totaltTilbakekrevesBeløp = BigDecimal.valueOf(10000),
                totaltTilbakekrevesBeløpMedRenter = BigDecimal.valueOf(11000),
                totaltTilbakekrevesBeløpMedRenterUtenSkatt =
                BigDecimal.valueOf(11000),
            ),
            varsel = HbVarsel(
                varsletBeløp = BigDecimal.valueOf(10000),
                varsletDato = LocalDate.now().minusDays(100),
            ),
            konfigurasjon = HbKonfigurasjon(klagefristIUker = 6),
            søker = HbPerson(
                navn = "Søker Søkersen",
                dødsdato = LocalDate.of(2018, 3, 1),
            ),
            fagsaksvedtaksdato = LocalDate.now(),
            behandling = HbBehandling(),
            totaltFeilutbetaltBeløp = BigDecimal.valueOf(10000),
            vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
            ansvarligBeslutter = "ansvarlig person sin signatur",
        )

    @Test
    fun `lagVedtaksbrevVedleggHtml skal generere vedlegg med flere perioder og med renter`() {
        val vedtaksbrevData = lagTestBuilder()
            .copy(
                fagsaksvedtaksdato = LocalDate.now(),
                totalresultat = HbTotalresultat(
                    hovedresultat = Vedtaksresultat.DELVIS_TILBAKEBETALING,
                    totaltTilbakekrevesBeløp = BigDecimal(23002),
                    totaltTilbakekrevesBeløpMedRenter = BigDecimal(23302),
                    totaltRentebeløp = BigDecimal(300),
                    totaltTilbakekrevesBeløpMedRenterUtenSkatt = BigDecimal(18537),
                ),
                hjemmel = HbHjemmel("Folketrygdloven § 22-15"),
                varsel = HbVarsel(
                    varsletBeløp = BigDecimal(33001),
                    varsletDato = LocalDate.of(2020, 4, 4),
                ),
            )
        val perioder =
            listOf(
                HbVedtaksbrevsperiode(
                    periode = januar,
                    kravgrunnlag = HbKravgrunnlag.forFeilutbetaltBeløp(BigDecimal(30001)),
                    fakta = HbFakta(
                        Hendelsestype.ANNET,
                        Hendelsesundertype.ANNET_FRITEKST,
                    ),
                    vurderinger =
                    HbVurderinger(
                        foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                        vilkårsvurderingsresultat = Vilkårsvurderingsresultat
                            .MANGELFULLE_OPPLYSNINGER_FRA_BRUKER,
                        aktsomhetsresultat = Aktsomhet.SIMPEL_UAKTSOMHET,
                        fritekst = "Du er heldig som slapp å betale alt!",
                        særligeGrunner = HbSærligeGrunner(
                            listOf(
                                SærligGrunn
                                    .TID_FRA_UTBETALING,
                                SærligGrunn.STØRRELSE_BELØP,
                            ),
                            null,
                            null,
                        ),
                    ),
                    resultat = HbResultat(
                        tilbakekrevesBeløpUtenSkattMedRenter = BigDecimal(16015),
                        tilbakekrevesBeløp = BigDecimal(20002),
                        rentebeløp = BigDecimal.ZERO,
                    ),
                    førstePeriode = true,
                ),
                HbVedtaksbrevsperiode(
                    periode = februar,
                    kravgrunnlag = HbKravgrunnlag(
                        feilutbetaltBeløp = BigDecimal(3000),
                        riktigBeløp = BigDecimal(3000),
                        utbetaltBeløp = BigDecimal(6000),
                    ),
                    fakta = HbFakta(
                        Hendelsestype.BOR_MED_SØKER,
                        Hendelsesundertype.BOR_IKKE_MED_BARN,
                    ),
                    vurderinger =
                    HbVurderinger(
                        foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                        vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
                        aktsomhetsresultat = AnnenVurdering.GOD_TRO,
                        beløpIBehold = BigDecimal.ZERO,
                    ),
                    resultat = HbResultat(
                        tilbakekrevesBeløpUtenSkattMedRenter = BigDecimal.ZERO,
                        tilbakekrevesBeløp = BigDecimal.ZERO,
                        rentebeløp = BigDecimal.ZERO,
                    ),
                    førstePeriode = true,
                ),
                HbVedtaksbrevsperiode(
                    periode = mars,
                    kravgrunnlag = HbKravgrunnlag(
                        feilutbetaltBeløp = BigDecimal(3000),
                        riktigBeløp = BigDecimal(3000),
                        utbetaltBeløp = BigDecimal(6000),
                    ),
                    fakta = HbFakta(
                        Hendelsestype.BOR_MED_SØKER,
                        Hendelsesundertype.BOR_IKKE_MED_BARN,
                    ),
                    vurderinger =
                    HbVurderinger(
                        foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                        vilkårsvurderingsresultat = Vilkårsvurderingsresultat
                            .FORSTO_BURDE_FORSTÅTT,
                        aktsomhetsresultat = Aktsomhet.FORSETT,
                    ),
                    resultat = HbResultat(
                        tilbakekrevesBeløpUtenSkattMedRenter = BigDecimal(2222),
                        tilbakekrevesBeløp = BigDecimal(3000),
                        rentebeløp = BigDecimal(300),
                    ),
                    førstePeriode = true,
                ),
            )
        val data = HbVedtaksbrevsdata(vedtaksbrevData, perioder)

        val generertBrev = TekstformatererVedtaksbrev.lagVedtaksbrevsvedleggHtml(data)

        val fasit = les("/vedtaksbrev/vedlegg/vedlegg_med_og_uten_renter.txt")
        generertBrev shouldBe fasit
    }

    @Throws(IOException::class)
    private fun les(filnavn: String): String? {
        javaClass.getResourceAsStream(filnavn).use { resource ->
            Scanner(resource, "UTF-8").use { scanner ->
                scanner.useDelimiter("\\A")
                return if (scanner.hasNext()) scanner.next() else null
            }
        }
    }
}
