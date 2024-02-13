package no.nav.familie.tilbake.dokumentbestilling.vedtak

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
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
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbResultatTestBuilder
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbSærligeGrunner
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbVedtaksbrevsperiode
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbVurderinger
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.SærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class AvsnittUtilTest {

    private val januar = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
    private val februar = Datoperiode(LocalDate.of(2019, 2, 1), LocalDate.of(2019, 2, 28))

    private val brevmetadata = Brevmetadata(
        sakspartId = "123456",
        sakspartsnavn = "Test",
        mottageradresse = Adresseinfo("ident", "bob"),
        behandlendeEnhetsNavn = "NAV Familie- og pensjonsytelser Skien",
        ansvarligSaksbehandler = "Bob",
        saksnummer = "1232456",
        språkkode = Språkkode.NB,
        ytelsestype = Ytelsestype.BARNETRYGD,
        gjelderDødsfall = false,
    )

    private val vedtaksbrevFelles = HbVedtaksbrevFelles(
        brevmetadata = brevmetadata,
        konfigurasjon = HbKonfigurasjon(klagefristIUker = 4),
        søker = HbPerson(
            navn = "Søker Søkersen",
        ),
        fagsaksvedtaksdato = LocalDate.now(),
        behandling = HbBehandling(erRevurdering = false),
        totalresultat = HbTotalresultat(
            Vedtaksresultat.DELVIS_TILBAKEBETALING,
            BigDecimal(23002),
            BigDecimal(23002),
            BigDecimal(23002),
            BigDecimal.ZERO,
        ),
        hjemmel = HbHjemmel("Folketrygdloven § 22-15"),
        totaltFeilutbetaltBeløp = BigDecimal.valueOf(20000),
        vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
        ansvarligBeslutter = "ansvarlig person sin signatur",
    )

    @Test
    fun `lagVedtaksbrevDeltIAvsnitt skal generere brev delt i avsnitt og underavsnitt`() {
        val vedtaksbrevData = vedtaksbrevFelles.copy(
            fagsaksvedtaksdato = LocalDate.now(),
            behandling = HbBehandling(erRevurdering = false),
            totalresultat = HbTotalresultat(
                Vedtaksresultat.DELVIS_TILBAKEBETALING,
                BigDecimal(23002),
                BigDecimal(23002),
                BigDecimal(23002),
                BigDecimal.ZERO,
            ),
            fritekstoppsummering = "Her finner du friteksten til oppsummeringen",
            hjemmel = HbHjemmel("Folketrygdloven § 22-15"),
            varsel = HbVarsel(
                varsletBeløp = BigDecimal(33001),
                varsletDato = LocalDate.of(2020, 4, 4),
            ),
            vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
        )

        val perioder =
            listOf(
                HbVedtaksbrevsperiode(
                    periode = januar,
                    kravgrunnlag = HbKravgrunnlag.forFeilutbetaltBeløp(BigDecimal(30001)),
                    fakta = HbFakta(Hendelsestype.ANNET, Hendelsesundertype.ANNET_FRITEKST),
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
                                SærligGrunn.ANNET,
                            ),
                            "Fritekst særlige grunner",
                            "Fritekst særlige grunner annet",
                        ),
                    ),
                    resultat = HbResultatTestBuilder.forTilbakekrevesBeløp(20002),
                    førstePeriode = true,
                ),
                HbVedtaksbrevsperiode(
                    periode = februar,
                    kravgrunnlag = HbKravgrunnlag(
                        feilutbetaltBeløp = BigDecimal(3000),
                        riktigBeløp = BigDecimal(3000),
                        utbetaltBeløp = BigDecimal(6000),
                    ),
                    fakta = HbFakta(Hendelsestype.BOR_MED_SØKER, Hendelsesundertype.BOR_IKKE_MED_BARN),
                    vurderinger =
                    HbVurderinger(
                        foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                        vilkårsvurderingsresultat = Vilkårsvurderingsresultat
                            .FORSTO_BURDE_FORSTÅTT,
                        aktsomhetsresultat = Aktsomhet.SIMPEL_UAKTSOMHET,
                        særligeGrunner =
                        HbSærligeGrunner(
                            listOf(
                                SærligGrunn.HELT_ELLER_DELVIS_NAVS_FEIL,
                                SærligGrunn.STØRRELSE_BELØP,
                            ),
                        ),
                    ),
                    resultat = HbResultatTestBuilder.forTilbakekrevesBeløp(3000),
                    førstePeriode = true,
                ),
            )
        val data = HbVedtaksbrevsdata(vedtaksbrevData, perioder)

        val resultat = AvsnittUtil.lagVedtaksbrevDeltIAvsnitt(data, "Du må betale tilbake overgangsstønaden")

        resultat.shouldHaveSize(4)
        resultat[0].avsnittstype shouldBe Avsnittstype.OPPSUMMERING
        resultat[0].underavsnittsliste.shouldHaveSize(2)
        resultat[0].underavsnittsliste[0].fritekstTillatt shouldBe false
        resultat[1].underavsnittsliste[0].fritekstTillatt shouldBe true
        resultat[1].avsnittstype shouldBe Avsnittstype.PERIODE
        resultat[1].underavsnittsliste.shouldHaveSize(7)
        resultat[1].underavsnittsliste.filter { it.fritekstTillatt }.size shouldBe 4
        resultat[2].avsnittstype shouldBe Avsnittstype.PERIODE
        resultat[2].underavsnittsliste.shouldHaveSize(7)
        resultat[2].underavsnittsliste.filter { it.fritekstTillatt }.size shouldBe 3
        resultat[3].avsnittstype shouldBe Avsnittstype.TILLEGGSINFORMASJON
        resultat[3].underavsnittsliste.shouldHaveSize(14)
        resultat[3].underavsnittsliste.forEach { it.fritekstTillatt shouldBe false }
    }

    @Test
    fun `parseTekst skal parse tekst til avsnitt`() {
        val tekst = "_Hovedoverskrift i brevet\n\n" +
            "Brødtekst første avsnitt\n\n" +
            "Brødtekst andre avsnitt\n\n" +
            "_underoverskrift\n\n" +
            "Brødtekst tredje avsnitt\n\n" +
            "_Avsluttende overskrift uten etterfølgende tekst\n" + Vedtaksbrevsfritekst.markerValgfriFritekst(null)

        val resultat = AvsnittUtil.parseTekst(tekst, Avsnitt(), null)

        resultat.overskrift shouldBe "Hovedoverskrift i brevet"
        val underavsnitt: List<Underavsnitt> = resultat.underavsnittsliste
        underavsnitt.shouldHaveSize(4)
        underavsnitt[0].overskrift shouldBe null
        underavsnitt[0].brødtekst shouldBe "Brødtekst første avsnitt"
        underavsnitt[0].fritekstTillatt.shouldBeFalse()
        underavsnitt[1].overskrift shouldBe null
        underavsnitt[1].brødtekst shouldBe "Brødtekst andre avsnitt"
        underavsnitt[1].fritekstTillatt.shouldBeFalse()
        underavsnitt[2].overskrift shouldBe "underoverskrift"
        underavsnitt[2].brødtekst shouldBe "Brødtekst tredje avsnitt"
        underavsnitt[2].fritekstTillatt.shouldBeFalse()
        underavsnitt[3].overskrift shouldBe "Avsluttende overskrift uten etterfølgende tekst"
        underavsnitt[3].brødtekst shouldBe null
        underavsnitt[3].fritekstTillatt.shouldBeTrue()
    }

    @Test
    fun `parseTekst skal plassere fritekstfelt etter første avsnitt når det er valgt`() {
        val tekst = "_Hovedoverskrift i brevet\n\n" +
            "Brødtekst første avsnitt\n" +
            "${Vedtaksbrevsfritekst.markerValgfriFritekst(null)}\n" +
            "_underoverskrift\n\n" +
            "Brødtekst andre avsnitt\n\n" +
            "_Avsluttende overskrift uten etterfølgende tekst"

        val resultat = AvsnittUtil.parseTekst(tekst, Avsnitt(), null)

        resultat.overskrift shouldBe "Hovedoverskrift i brevet"
        val underavsnitt: List<Underavsnitt> = resultat.underavsnittsliste
        underavsnitt.shouldHaveSize(4)
        underavsnitt[0].overskrift shouldBe null
        underavsnitt[0].brødtekst shouldBe "Brødtekst første avsnitt"
        underavsnitt[0].fritekstTillatt.shouldBeFalse()
        underavsnitt[1].overskrift shouldBe null
        underavsnitt[1].brødtekst shouldBe null
        underavsnitt[1].fritekstTillatt.shouldBeTrue()
        underavsnitt[2].overskrift shouldBe "underoverskrift"
        underavsnitt[2].brødtekst shouldBe "Brødtekst andre avsnitt"
        underavsnitt[2].fritekstTillatt.shouldBeFalse()
        underavsnitt[3].overskrift shouldBe "Avsluttende overskrift uten etterfølgende tekst"
        underavsnitt[3].brødtekst shouldBe null
        underavsnitt[3].fritekstTillatt.shouldBeFalse()
    }

    @Test
    fun `parseTekst skal plassere fritekstfelt etter overskriften når det er valgt`() {
        val avsnitt = Avsnitt(overskrift = "Hovedoverskrift")
        val tekst = "_underoverskrift 1\n" +
            "${Vedtaksbrevsfritekst.markerValgfriFritekst(null)}\n" +
            "Brødtekst første avsnitt\n\n" +
            "_underoverskrift 2\n\n" +
            "Brødtekst andre avsnitt"

        val resultat = AvsnittUtil.parseTekst(tekst, avsnitt, null)

        resultat.overskrift shouldBe "Hovedoverskrift"
        val underavsnitt: List<Underavsnitt> = resultat.underavsnittsliste
        underavsnitt.shouldHaveSize(3)
        underavsnitt[0].overskrift shouldBe "underoverskrift 1"
        underavsnitt[0].brødtekst shouldBe null
        underavsnitt[0].fritekstTillatt.shouldBeTrue()
        underavsnitt[1].overskrift shouldBe null
        underavsnitt[1].brødtekst shouldBe "Brødtekst første avsnitt"
        underavsnitt[1].fritekstTillatt.shouldBeFalse()
        underavsnitt[2].overskrift shouldBe "underoverskrift 2"
        underavsnitt[2].brødtekst shouldBe "Brødtekst andre avsnitt"
        underavsnitt[2].fritekstTillatt.shouldBeFalse()
    }

    @Test
    fun `parseTekst skal parse fritekstfelt med eksisterende fritekst`() {
        val avsnitt = Avsnitt(overskrift = "Hovedoverskrift")
        val tekst = "_underoverskrift 1\n${Vedtaksbrevsfritekst.markerValgfriFritekst("fritekst linje 1\nfritekst linje2")}"

        val resultat = AvsnittUtil.parseTekst(tekst, avsnitt, null)

        resultat.overskrift shouldBe "Hovedoverskrift"
        val underavsnitt: List<Underavsnitt> = resultat.underavsnittsliste
        // underavsnitt.shouldHaveSize(1);
        underavsnitt[0].overskrift shouldBe "underoverskrift 1"
        underavsnitt[0].brødtekst shouldBe null
        underavsnitt[0].fritekstTillatt.shouldBeTrue()
        underavsnitt[0].fritekst shouldBe "fritekst linje 1\nfritekst linje2"
    }

    @Test
    fun `parseTekst skal skille mellom påkrevet og valgfritt fritekstfelt`() {
        val avsnitt = Avsnitt(overskrift = "Hovedoverskrift")
        val tekst = "_underoverskrift 1\n${Vedtaksbrevsfritekst.markerPåkrevetFritekst(null, null)}\n" +
            "_underoverskrift 2\n${Vedtaksbrevsfritekst.markerValgfriFritekst(null)}"

        val resultat = AvsnittUtil.parseTekst(tekst, avsnitt, null)

        resultat.overskrift shouldBe "Hovedoverskrift"
        val underavsnitt: List<Underavsnitt> = resultat.underavsnittsliste
        underavsnitt.shouldHaveSize(2)
        underavsnitt[0].overskrift shouldBe "underoverskrift 1"
        underavsnitt[0].brødtekst shouldBe null
        underavsnitt[0].fritekstTillatt.shouldBeTrue()
        underavsnitt[0].fritekstPåkrevet.shouldBeTrue()
        underavsnitt[0].fritekst shouldBe ""
        underavsnitt[1].overskrift shouldBe "underoverskrift 2"
        underavsnitt[1].brødtekst shouldBe null
        underavsnitt[1].fritekstTillatt.shouldBeTrue()
        underavsnitt[1].fritekstPåkrevet.shouldBeFalse()
        underavsnitt[1].fritekst shouldBe ""
    }

    @Test
    fun `parseTekst skal utlede underavsnittstype fra fritekstmarkering slik at det er mulig å skille mellom særlige grunner`() {
        val avsnitt = Avsnitt(overskrift = "Hovedoverskrift")
        val tekst = "_underoverskrift 1\n" +
            Vedtaksbrevsfritekst.markerValgfriFritekst(null, Underavsnittstype.SÆRLIGEGRUNNER) +
            "\n_underoverskrift 2\n" +
            "brødtekst ${Vedtaksbrevsfritekst.markerValgfriFritekst(null, Underavsnittstype.SÆRLIGEGRUNNER_ANNET)}" +
            "\n_underoverskrift 3"

        val resultat = AvsnittUtil.parseTekst(tekst, avsnitt, null)

        resultat.overskrift shouldBe "Hovedoverskrift"
        val underavsnitt: List<Underavsnitt> = resultat.underavsnittsliste
        underavsnitt.shouldHaveSize(3)
        underavsnitt[0].underavsnittstype shouldBe Underavsnittstype.SÆRLIGEGRUNNER
        underavsnitt[1].underavsnittstype shouldBe Underavsnittstype.SÆRLIGEGRUNNER_ANNET
        underavsnitt[1].brødtekst shouldBe "brødtekst "
        underavsnitt[1].fritekstTillatt.shouldBeTrue()
        underavsnitt[2].underavsnittstype shouldBe Underavsnittstype.SÆRLIGEGRUNNER_ANNET
        underavsnitt[2].fritekstTillatt.shouldBeFalse()
    }
}
