package no.nav.familie.tilbake.dokumentbestilling.vedtak

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeEmpty
import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.beregning.modell.Vedtaksresultat
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.handlebars.FellesTekstformaterer
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbBehandling
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbHjemmel
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbKonfigurasjon
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbPerson
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbTotalresultat
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVarsel
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevDatoer
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevFelles
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevPeriodeOgFelles
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevsdata
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.Vedtaksbrevstype
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbFakta
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbKravgrunnlag
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbResultat
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbResultatTestBuilder
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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.Scanner

class TekstformatererVedtaksbrevTest {

    private val januar = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))
    private val februar = Datoperiode(LocalDate.of(2019, 2, 1), LocalDate.of(2019, 2, 28))
    private val mars = Datoperiode(LocalDate.of(2019, 3, 1), LocalDate.of(2019, 3, 31))
    private val april = Datoperiode(LocalDate.of(2019, 4, 1), LocalDate.of(2019, 4, 30))
    private val førsteNyttårsdag = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 1))

    private val brevmetadata = Brevmetadata(
        sakspartId = "123456",
        sakspartsnavn = "Test",
        mottageradresse = Adresseinfo("ident", "bob"),
        behandlendeEnhetsNavn = "NAV Familie- og pensjonsytelser Skien",
        ansvarligSaksbehandler = "Ansvarlig Saksbehandler",
        saksnummer = "1232456",
        språkkode = Språkkode.NB,
        ytelsestype = Ytelsestype.OVERGANGSSTØNAD,
        gjelderDødsfall = false,
    )

    private val felles =
        HbVedtaksbrevFelles(
            brevmetadata = brevmetadata,
            hjemmel = HbHjemmel("Folketrygdloven"),
            totalresultat = HbTotalresultat(
                Vedtaksresultat.FULL_TILBAKEBETALING,
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(11000),
                BigDecimal.valueOf(11000),
                BigDecimal.valueOf(1000),
            ),
            varsel = HbVarsel(
                varsletBeløp = BigDecimal.valueOf(10000),
                varsletDato = LocalDate.now().minusDays(100),
            ),
            konfigurasjon = HbKonfigurasjon(klagefristIUker = 6),
            søker = HbPerson(
                navn = "Søker Søkersen",
            ),
            fagsaksvedtaksdato = LocalDate.now(),
            behandling = HbBehandling(),
            totaltFeilutbetaltBeløp = BigDecimal.valueOf(10000),
            vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
            ansvarligBeslutter = "Ansvarlig Beslutter",
        )

    @Nested
    inner class LagVedtaksbrevFritekst {

        @Test
        fun `skal generere vedtaksbrev for OS og god tro uten tilbakekreving uten varsel`() {
            val perioder: List<HbVedtaksbrevsperiode> =
                listOf(
                    HbVedtaksbrevsperiode(
                        periode = januar,
                        kravgrunnlag = HbKravgrunnlag(BigDecimal.ZERO, BigDecimal(1000), BigDecimal(1000)),
                        fakta = HbFakta(
                            Hendelsestype.DØDSFALL,
                            Hendelsesundertype.BRUKER_DØD,
                        ),
                        vurderinger =
                        HbVurderinger(
                            foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
                            aktsomhetsresultat = AnnenVurdering.GOD_TRO,
                            beløpIBehold = BigDecimal.ZERO,
                        ),
                        resultat = HbResultatTestBuilder.forTilbakekrevesBeløp(0),
                        førstePeriode = true,
                    ),
                )
            val vedtaksbrevData = felles.copy(
                fagsaksvedtaksdato = LocalDate.of(2019, 3, 21),
                varsel = null,
                totalresultat = HbTotalresultat(
                    Vedtaksresultat.INGEN_TILBAKEBETALING,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                ),
                hjemmel = HbHjemmel("Folketrygdloven § 22-15"),
                datoer = HbVedtaksbrevDatoer(perioder = perioder),
                vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
            )
            val data = HbVedtaksbrevsdata(vedtaksbrevData, perioder)

            val generertBrev = TekstformatererVedtaksbrev.lagVedtaksbrevsfritekst(data)

            val fasit = les("/vedtaksbrev/OS_ingen_tilbakekreving.txt")
            generertBrev shouldBe fasit
        }

        @Test
        fun `skal generere vedtaksbrev for OS uten tilbakekreving uten varsel i 3dje person`() {
            val perioder: List<HbVedtaksbrevsperiode> =
                listOf(
                    HbVedtaksbrevsperiode(
                        periode = januar,
                        kravgrunnlag = HbKravgrunnlag(BigDecimal.ZERO, BigDecimal(1000), BigDecimal(1000)),
                        fakta = HbFakta(
                            Hendelsestype.DØDSFALL,
                            Hendelsesundertype.BRUKER_DØD,
                        ),
                        vurderinger =
                        HbVurderinger(
                            foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
                            aktsomhetsresultat = AnnenVurdering.GOD_TRO,
                            beløpIBehold = BigDecimal.ZERO,
                        ),
                        resultat = HbResultatTestBuilder.forTilbakekrevesBeløp(0),
                        førstePeriode = true,
                    ),
                )
            val vedtaksbrevData = felles.copy(
                brevmetadata = felles.brevmetadata.copy(gjelderDødsfall = true),
                fagsaksvedtaksdato = LocalDate.of(2019, 3, 21),
                varsel = null,
                totalresultat = HbTotalresultat(
                    Vedtaksresultat.INGEN_TILBAKEBETALING,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                ),
                hjemmel = HbHjemmel("Folketrygdloven § 22-15"),
                datoer = HbVedtaksbrevDatoer(perioder = perioder),
                vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
            )
            val data = HbVedtaksbrevsdata(vedtaksbrevData, perioder)

            val generertBrev = TekstformatererVedtaksbrev.lagVedtaksbrevsfritekst(data)

            val fasit = les("/vedtaksbrev/OS_ingen_tilbakekreving_bruker_død.txt")
            generertBrev shouldBe fasit
        }

        @Test
        fun `skal generere vedtaksbrev for OS med tilbakekreving med varsel i 3dje person`() {
            val perioder: List<HbVedtaksbrevsperiode> =
                listOf(
                    HbVedtaksbrevsperiode(
                        periode = januar,
                        kravgrunnlag = HbKravgrunnlag(BigDecimal.ZERO, BigDecimal(1000), BigDecimal(1000)),
                        fakta = HbFakta(
                            Hendelsestype.DØDSFALL,
                            Hendelsesundertype.BRUKER_DØD,
                        ),
                        vurderinger =
                        HbVurderinger(
                            foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                            aktsomhetsresultat = Aktsomhet.GROV_UAKTSOMHET,
                        ),
                        resultat = HbResultatTestBuilder.forTilbakekrevesBeløp(1000),
                        førstePeriode = true,
                    ),
                )
            val vedtaksbrevData = felles.copy(
                brevmetadata = felles.brevmetadata.copy(gjelderDødsfall = true),
                fagsaksvedtaksdato = LocalDate.of(2019, 3, 21),
                varsel = HbVarsel(
                    varsletBeløp = BigDecimal(1234567893),
                    varsletDato = LocalDate.of(2019, 1, 3),
                ),
                totalresultat = HbTotalresultat(
                    hovedresultat = Vedtaksresultat.FULL_TILBAKEBETALING,
                    totaltTilbakekrevesBeløp = BigDecimal(10000),
                    totaltTilbakekrevesBeløpMedRenter = BigDecimal(11000),
                    totaltTilbakekrevesBeløpMedRenterUtenSkatt = BigDecimal(7011),
                    totaltRentebeløp = BigDecimal(1000),
                ),
                hjemmel = HbHjemmel("Folketrygdloven § 22-15"),
                datoer = HbVedtaksbrevDatoer(perioder = perioder),
                vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
            )
            val data = HbVedtaksbrevsdata(vedtaksbrevData, perioder)

            val generertBrev = TekstformatererVedtaksbrev.lagVedtaksbrevsfritekst(data)

            val fasit = les("/vedtaksbrev/OS_tilbakekreving_bruker_død.txt")
            generertBrev shouldBe fasit
        }

        @Test
        fun `skal generere vedtaksbrev for OS uten tilbakekreving uten varsel i 3dje person nynorsk`() {
            val perioder: List<HbVedtaksbrevsperiode> =
                listOf(
                    HbVedtaksbrevsperiode(
                        periode = januar,
                        kravgrunnlag = HbKravgrunnlag(BigDecimal.ZERO, BigDecimal(1000), BigDecimal(1000)),
                        fakta = HbFakta(
                            Hendelsestype.DØDSFALL,
                            Hendelsesundertype.BRUKER_DØD,
                        ),
                        vurderinger =
                        HbVurderinger(
                            foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
                            aktsomhetsresultat = AnnenVurdering.GOD_TRO,
                            beløpIBehold = BigDecimal.ZERO,
                        ),
                        resultat = HbResultatTestBuilder.forTilbakekrevesBeløp(0),
                        førstePeriode = true,
                    ),
                )
            val vedtaksbrevData = felles.copy(
                brevmetadata = felles.brevmetadata.copy(gjelderDødsfall = true, språkkode = Språkkode.NN),
                fagsaksvedtaksdato = LocalDate.of(2019, 3, 21),
                varsel = null,
                totalresultat = HbTotalresultat(
                    Vedtaksresultat.INGEN_TILBAKEBETALING,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                ),
                søker = HbPerson(
                    navn = "Søker Søkersen",
                    dødsdato = LocalDate.of(2018, 3, 1),
                ),
                hjemmel = HbHjemmel("Folketrygdloven § 22-15"),
                datoer = HbVedtaksbrevDatoer(perioder = perioder),
                vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
            )
            val data = HbVedtaksbrevsdata(vedtaksbrevData, perioder)

            val generertBrev = TekstformatererVedtaksbrev.lagVedtaksbrevsfritekst(data)

            val fasit = les("/vedtaksbrev/OS_ingen_tilbakekreving_bruker_død_nn.txt")
            generertBrev shouldBe fasit
        }

        @Test
        fun `skal generere vedtaksbrev for OS med tilbakekreving med varsel i 3dje person nynorsk`() {
            val perioder: List<HbVedtaksbrevsperiode> =
                listOf(
                    HbVedtaksbrevsperiode(
                        periode = januar,
                        kravgrunnlag = HbKravgrunnlag(BigDecimal.ZERO, BigDecimal(1000), BigDecimal(1000)),
                        fakta = HbFakta(
                            Hendelsestype.DØDSFALL,
                            Hendelsesundertype.BRUKER_DØD,
                        ),
                        vurderinger =
                        HbVurderinger(
                            foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                            aktsomhetsresultat = Aktsomhet.GROV_UAKTSOMHET,
                        ),
                        resultat = HbResultatTestBuilder.forTilbakekrevesBeløp(1000),
                        førstePeriode = true,
                    ),
                )
            val vedtaksbrevData = felles.copy(
                brevmetadata = felles.brevmetadata.copy(gjelderDødsfall = true, språkkode = Språkkode.NN),
                fagsaksvedtaksdato = LocalDate.of(2019, 3, 21),
                varsel = HbVarsel(
                    varsletBeløp = BigDecimal(1234567893),
                    varsletDato = LocalDate.of(2019, 1, 3),
                ),
                totalresultat = HbTotalresultat(
                    hovedresultat = Vedtaksresultat.FULL_TILBAKEBETALING,
                    totaltTilbakekrevesBeløp = BigDecimal(10000),
                    totaltTilbakekrevesBeløpMedRenter = BigDecimal(11000),
                    totaltTilbakekrevesBeløpMedRenterUtenSkatt = BigDecimal(7011),
                    totaltRentebeløp = BigDecimal(1000),
                ),
                hjemmel = HbHjemmel("Folketrygdloven § 22-15"),
                datoer = HbVedtaksbrevDatoer(perioder = perioder),
                vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
            )
            val data = HbVedtaksbrevsdata(vedtaksbrevData, perioder)

            val generertBrev = TekstformatererVedtaksbrev.lagVedtaksbrevsfritekst(data)

            val fasit = les("/vedtaksbrev/OS_tilbakekreving_bruker_død_nn.txt")
            generertBrev shouldBe fasit
        }

        @Test
        fun `skal generere vedtaksbrev for OS og god tro uten tilbakekreving uten varsel med verge`() {
            val perioder: List<HbVedtaksbrevsperiode> =
                listOf(
                    HbVedtaksbrevsperiode(
                        periode = januar,
                        kravgrunnlag = HbKravgrunnlag(BigDecimal.ZERO, BigDecimal(1000), BigDecimal(1000)),
                        fakta = HbFakta(
                            Hendelsestype.DØDSFALL,
                            Hendelsesundertype.BRUKER_DØD,
                        ),
                        vurderinger =
                        HbVurderinger(
                            foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
                            aktsomhetsresultat = AnnenVurdering.GOD_TRO,
                            beløpIBehold = BigDecimal.ZERO,
                        ),
                        resultat = HbResultatTestBuilder.forTilbakekrevesBeløp(0),
                        førstePeriode = true,
                    ),
                )
            val vedtaksbrevData = felles.copy(
                fagsaksvedtaksdato = LocalDate.of(2019, 3, 21),
                brevmetadata = brevmetadata.copy(
                    mottageradresse = Adresseinfo(
                        "12345678901",
                        "Semba AS c/o John Doe",
                    ),
                    sakspartsnavn = "Test",
                    vergenavn = "John Doe",
                    finnesVerge = true,
                    finnesAnnenMottaker = true,
                ),
                varsel = null,
                totalresultat = HbTotalresultat(
                    Vedtaksresultat.INGEN_TILBAKEBETALING,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                ),
                hjemmel = HbHjemmel("Folketrygdloven § 22-15"),
                datoer = HbVedtaksbrevDatoer(perioder = perioder),
                vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
            )
            val data = HbVedtaksbrevsdata(vedtaksbrevData, perioder)

            val generertBrev = TekstformatererVedtaksbrev.lagVedtaksbrevsfritekst(data)

            val fasit = les("/vedtaksbrev/OS_ingen_tilbakekreving_med_verge.txt")
            generertBrev shouldBe "$fasit"
        }

        @Test
        fun `skal generere vedtaksbrev for revurdering med OS og mye fritekst`() {
            val vedtaksbrevData = felles.copy(
                fagsaksvedtaksdato = LocalDate.now(),
                behandling = HbBehandling(
                    erRevurdering = true,
                    originalBehandlingsdatoFagsakvedtak = LocalDate.of(
                        2019,
                        1,
                        1,
                    ),
                ),
                totalresultat = HbTotalresultat(
                    Vedtaksresultat.DELVIS_TILBAKEBETALING,
                    BigDecimal(1234567892),
                    BigDecimal(1234567892),
                    BigDecimal(1234567000),
                    BigDecimal.ZERO,
                ),
                søker = HbPerson(
                    navn = "Søker Søkersen",
                    dødsdato = LocalDate.of(2018, 3, 1),
                ),
                hjemmel = HbHjemmel("Folketrygdloven § 22-15"),
                varsel = HbVarsel(
                    varsletBeløp = BigDecimal(1234567893),
                    varsletDato = LocalDate.of(2019, 1, 3),
                ),
                fritekstoppsummering = "Skynd deg å betale, vi trenger pengene med en gang!",
                vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
            )
            val perioder =
                listOf(
                    HbVedtaksbrevsperiode(
                        periode = januar,
                        kravgrunnlag = HbKravgrunnlag(feilutbetaltBeløp = BigDecimal(1234567890)),
                        fakta = HbFakta(
                            Hendelsestype.ANNET,
                            Hendelsesundertype.ANNET_FRITEKST,
                            "Ingen vet riktig hva som har skjedd, " +
                                "men du har fått utbetalt alt for mye penger.",
                        ),
                        vurderinger =
                        HbVurderinger(
                            foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                            vilkårsvurderingsresultat =
                            Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                            aktsomhetsresultat = Aktsomhet.GROV_UAKTSOMHET,
                            fritekst = "Det er helt utrolig om du ikke har oppdaget dette!",
                            særligeGrunner =
                            HbSærligeGrunner(
                                listOf(
                                    SærligGrunn.HELT_ELLER_DELVIS_NAVS_FEIL,
                                    SærligGrunn.STØRRELSE_BELØP,
                                    SærligGrunn.TID_FRA_UTBETALING,
                                    SærligGrunn.ANNET,
                                ),
                                "Gratulerer, du fikk norgesrekord i feilutbetalt" +
                                    " beløp! Du skal slippe å betale renter!",
                                "at du jobber med OVERGANGSSTØNAD " +
                                    "og dermed vet hvordan dette fungerer!",
                            ),
                        ),
                        resultat = HbResultatTestBuilder.forTilbakekrevesBeløp(1234567890),
                        førstePeriode = true,
                    ),
                    HbVedtaksbrevsperiode(
                        periode = februar,
                        kravgrunnlag = HbKravgrunnlag(
                            riktigBeløp = BigDecimal(0),
                            utbetaltBeløp = BigDecimal(1),
                            feilutbetaltBeløp = BigDecimal(1),
                        ),
                        fakta = HbFakta(
                            Hendelsestype.ENSLIG_FORSØRGER,
                            Hendelsesundertype.BARN_FLYTTET,
                            "Her har økonomisystemet gjort noe helt feil.",
                        ),
                        vurderinger =
                        HbVurderinger(
                            foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
                            aktsomhetsresultat = AnnenVurdering.GOD_TRO,
                            fritekst = "Vi skjønner at du ikke har oppdaget beløpet, " +
                                "siden du hadde så mye annet på konto.",
                            beløpIBehold = BigDecimal(1),
                        ),
                        resultat = HbResultatTestBuilder.forTilbakekrevesBeløp(1),
                        førstePeriode = true,
                    ),
                    HbVedtaksbrevsperiode(
                        periode = mars,
                        kravgrunnlag = HbKravgrunnlag(
                            riktigBeløp = BigDecimal(0),
                            utbetaltBeløp = BigDecimal(1),
                            feilutbetaltBeløp = BigDecimal(1),
                        ),
                        fakta = HbFakta(
                            Hendelsestype.ENSLIG_FORSØRGER,
                            Hendelsesundertype.BARN_FLYTTET,
                        ),
                        vurderinger =
                        HbVurderinger(
                            foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                            vilkårsvurderingsresultat =
                            Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
                            aktsomhetsresultat = Aktsomhet.SIMPEL_UAKTSOMHET,
                            fritekst = "Her burde du passet mer på!",
                        ),
                        resultat = HbResultatTestBuilder.forTilbakekrevesBeløp(1),
                        førstePeriode = true,
                    ),
                    HbVedtaksbrevsperiode(
                        periode = april,
                        kravgrunnlag = HbKravgrunnlag(
                            riktigBeløp = BigDecimal(0),
                            utbetaltBeløp = BigDecimal(1),
                            feilutbetaltBeløp = BigDecimal(1),
                        ),
                        fakta = HbFakta(
                            Hendelsestype.ENSLIG_FORSØRGER,
                            Hendelsesundertype.BARN_FLYTTET,
                        ),
                        vurderinger =
                        HbVurderinger(
                            foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                            vilkårsvurderingsresultat =
                            Vilkårsvurderingsresultat.MANGELFULLE_OPPLYSNINGER_FRA_BRUKER,
                            aktsomhetsresultat = Aktsomhet.FORSETT,
                            fritekst = "Dette gjorde du med vilje!",
                        ),
                        resultat = HbResultatTestBuilder.forTilbakekrevesBeløp(1),
                        førstePeriode = true,
                    ),
                )
            val data = HbVedtaksbrevsdata(vedtaksbrevData, perioder)

            val generertBrev = TekstformatererVedtaksbrev.lagVedtaksbrevsfritekst(data)

            val fasit = les("/vedtaksbrev/OS_fritekst_overalt.txt")
            generertBrev shouldBe fasit
        }

        @Test
        fun `skal generere vedtaksbrev for OS og ett barn og forsett`() {
            val vedtaksbrevData = felles
                .copy(
                    fagsaksvedtaksdato = LocalDate.now(),
                    totalresultat = HbTotalresultat(
                        hovedresultat = Vedtaksresultat.FULL_TILBAKEBETALING,
                        totaltTilbakekrevesBeløp = BigDecimal(10000),
                        totaltTilbakekrevesBeløpMedRenter = BigDecimal(11000),
                        totaltTilbakekrevesBeløpMedRenterUtenSkatt = BigDecimal(7011),
                        totaltRentebeløp = BigDecimal(1000),
                    ),
                    hjemmel = HbHjemmel("Folketrygdloven § 22-15"),
                    varsel = HbVarsel(
                        varsletBeløp = BigDecimal(10000),
                        varsletDato = LocalDate.of(2020, 4, 4),
                    ),
                    konfigurasjon = HbKonfigurasjon(klagefristIUker = 6),
                    vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
                )
            val perioder = listOf(
                HbVedtaksbrevsperiode(
                    januar,
                    HbKravgrunnlag.forFeilutbetaltBeløp(BigDecimal(10000)),
                    HbFakta(
                        Hendelsestype.ENSLIG_FORSØRGER,
                        Hendelsesundertype.BARN_FLYTTET,
                    ),
                    HbVurderinger(
                        foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                        vilkårsvurderingsresultat =
                        Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                        aktsomhetsresultat = Aktsomhet.FORSETT,
                    ),
                    HbResultatTestBuilder.forTilbakekrevesBeløpOgRenter(10000, 1000),
                    true,
                ),
            )
            val data = HbVedtaksbrevsdata(vedtaksbrevData, perioder)

            val generertBrev = TekstformatererVedtaksbrev.lagVedtaksbrevsfritekst(data)

            val fasit = les("/vedtaksbrev/OS_forsett.txt")
            generertBrev shouldBe fasit
        }

        @Test
        fun `skal generere vedtaksbrev for revurdering med OS og ett barn og forsett og bruker død`() {
            val perioder = listOf(
                HbVedtaksbrevsperiode(
                    januar,
                    HbKravgrunnlag.forFeilutbetaltBeløp(BigDecimal(10000)),
                    HbFakta(
                        Hendelsestype.DØDSFALL,
                        Hendelsesundertype.BRUKER_DØD,
                    ),
                    HbVurderinger(
                        foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                        vilkårsvurderingsresultat =
                        Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                        aktsomhetsresultat = Aktsomhet.FORSETT,
                    ),
                    HbResultatTestBuilder.forTilbakekrevesBeløpOgRenter(10000, 1000),
                    true,
                ),
            )
            val vedtaksbrevData = felles
                .copy(
                    fagsaksvedtaksdato = LocalDate.now(),
                    totalresultat = HbTotalresultat(
                        hovedresultat = Vedtaksresultat.FULL_TILBAKEBETALING,
                        totaltTilbakekrevesBeløp = BigDecimal(10000),
                        totaltTilbakekrevesBeløpMedRenter = BigDecimal(11000),
                        totaltTilbakekrevesBeløpMedRenterUtenSkatt = BigDecimal(7011),
                        totaltRentebeløp = BigDecimal(1000),
                    ),
                    behandling = HbBehandling(
                        erRevurdering = true,
                        originalBehandlingsdatoFagsakvedtak = LocalDate.of(
                            2019,
                            1,
                            1,
                        ),
                    ),
                    hjemmel = HbHjemmel("Folketrygdloven § 22-15"),
                    varsel = HbVarsel(
                        varsletBeløp = BigDecimal(10000),
                        varsletDato = LocalDate.of(2020, 4, 4),
                    ),
                    konfigurasjon = HbKonfigurasjon(klagefristIUker = 6),
                    vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
                    brevmetadata = brevmetadata.copy(gjelderDødsfall = true),
                    søker = HbPerson(
                        navn = "Søker Søkersen",
                        dødsdato = LocalDate.of(2018, 3, 1),
                    ),
                    datoer = HbVedtaksbrevDatoer(perioder = perioder),
                )
            val data = HbVedtaksbrevsdata(vedtaksbrevData, perioder)

            val generertBrev = TekstformatererVedtaksbrev.lagVedtaksbrevsfritekst(data)

            val fasit = les("/vedtaksbrev/OS_revurdering_bruker_død.txt")
            generertBrev shouldBe fasit
        }

        @Test
        fun `skal generere vedtaksbrev for revurdering med OS og ett barn og forsett og bruker død annet annet fritekst er valgt`() {
            val perioder = listOf(
                HbVedtaksbrevsperiode(
                    januar,
                    HbKravgrunnlag.forFeilutbetaltBeløp(BigDecimal(10000)),
                    HbFakta(
                        Hendelsestype.ANNET,
                        Hendelsesundertype.ANNET_FRITEKST,
                        "Død bruker annet fritekst er valgt",
                    ),
                    HbVurderinger(
                        foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                        vilkårsvurderingsresultat =
                        Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                        aktsomhetsresultat = Aktsomhet.FORSETT,
                        fritekst = "Død bruker annet fritekst er valgt",
                    ),
                    HbResultatTestBuilder.forTilbakekrevesBeløpOgRenter(10000, 1000),
                    true,
                ),
            )
            val vedtaksbrevData = felles
                .copy(
                    fagsaksvedtaksdato = LocalDate.now(),
                    totalresultat = HbTotalresultat(
                        hovedresultat = Vedtaksresultat.FULL_TILBAKEBETALING,
                        totaltTilbakekrevesBeløp = BigDecimal(10000),
                        totaltTilbakekrevesBeløpMedRenter = BigDecimal(11000),
                        totaltTilbakekrevesBeløpMedRenterUtenSkatt = BigDecimal(7011),
                        totaltRentebeløp = BigDecimal(1000),
                    ),
                    behandling = HbBehandling(
                        erRevurdering = true,
                        originalBehandlingsdatoFagsakvedtak = LocalDate.of(
                            2019,
                            1,
                            1,
                        ),
                    ),
                    hjemmel = HbHjemmel("Folketrygdloven § 22-15"),
                    varsel = HbVarsel(
                        varsletBeløp = BigDecimal(10000),
                        varsletDato = LocalDate.of(2020, 4, 4),
                    ),
                    konfigurasjon = HbKonfigurasjon(klagefristIUker = 6),
                    vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
                    brevmetadata = brevmetadata.copy(gjelderDødsfall = true),
                    søker = HbPerson(
                        navn = "Søker Søkersen",
                        dødsdato = LocalDate.of(2018, 3, 1),
                    ),
                    datoer = HbVedtaksbrevDatoer(perioder = perioder),
                )
            val data = HbVedtaksbrevsdata(vedtaksbrevData, perioder)

            val generertBrev = TekstformatererVedtaksbrev.lagVedtaksbrevsfritekst(data)

            val fasit = les("/vedtaksbrev/OS_revurdering_bruker_død_annet_fritekst.txt")
            generertBrev shouldBe fasit
        }

        @Test
        fun `skal generere vedtaksbrev for revurdering med OS og ett barn og forsett og bruker død nynorsk`() {
            val perioder = listOf(
                HbVedtaksbrevsperiode(
                    januar,
                    HbKravgrunnlag.forFeilutbetaltBeløp(BigDecimal(10000)),
                    HbFakta(
                        Hendelsestype.DØDSFALL,
                        Hendelsesundertype.BRUKER_DØD,
                    ),
                    HbVurderinger(
                        foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                        vilkårsvurderingsresultat =
                        Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                        aktsomhetsresultat = Aktsomhet.FORSETT,
                    ),
                    HbResultatTestBuilder.forTilbakekrevesBeløpOgRenter(10000, 1000),
                    true,
                ),
            )
            val vedtaksbrevData = felles
                .copy(
                    fagsaksvedtaksdato = LocalDate.now(),
                    totalresultat = HbTotalresultat(
                        hovedresultat = Vedtaksresultat.FULL_TILBAKEBETALING,
                        totaltTilbakekrevesBeløp = BigDecimal(10000),
                        totaltTilbakekrevesBeløpMedRenter = BigDecimal(11000),
                        totaltTilbakekrevesBeløpMedRenterUtenSkatt = BigDecimal(7011),
                        totaltRentebeløp = BigDecimal(1000),
                    ),
                    behandling = HbBehandling(
                        erRevurdering = true,
                        originalBehandlingsdatoFagsakvedtak = LocalDate.of(
                            2019,
                            1,
                            1,
                        ),
                    ),
                    hjemmel = HbHjemmel("Folketrygdloven § 22-15"),
                    varsel = HbVarsel(
                        varsletBeløp = BigDecimal(10000),
                        varsletDato = LocalDate.of(2020, 4, 4),
                    ),
                    konfigurasjon = HbKonfigurasjon(klagefristIUker = 6),
                    vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
                    brevmetadata = brevmetadata.copy(språkkode = Språkkode.NN, gjelderDødsfall = true),
                    søker = HbPerson(
                        navn = "Søker Søkersen",
                        dødsdato = LocalDate.of(2018, 3, 1),
                    ),
                    datoer = HbVedtaksbrevDatoer(perioder = perioder),
                )
            val data = HbVedtaksbrevsdata(vedtaksbrevData, perioder)

            val generertBrev = TekstformatererVedtaksbrev.lagVedtaksbrevsfritekst(data)

            val fasit = les("/vedtaksbrev/OS_revurdering_bruker_død_nynorsk.txt")
            generertBrev shouldBe fasit
        }

        @Test
        fun `skal generere vedtaksbrev for revurdering med OS og ett barn og forsett og bruker død nynorsk annet annet fritekst er valgt`() {
            val perioder = listOf(
                HbVedtaksbrevsperiode(
                    januar,
                    HbKravgrunnlag.forFeilutbetaltBeløp(BigDecimal(10000)),
                    HbFakta(
                        Hendelsestype.ANNET,
                        Hendelsesundertype.ANNET_FRITEKST,
                        "Død bruker annet fritekst er valgt",
                    ),
                    HbVurderinger(
                        foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                        vilkårsvurderingsresultat =
                        Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                        aktsomhetsresultat = Aktsomhet.FORSETT,
                        fritekst = "Død bruker annet fritekst er valgt",
                    ),
                    HbResultatTestBuilder.forTilbakekrevesBeløpOgRenter(10000, 1000),
                    true,
                ),
            )
            val vedtaksbrevData = felles
                .copy(
                    fagsaksvedtaksdato = LocalDate.now(),
                    totalresultat = HbTotalresultat(
                        hovedresultat = Vedtaksresultat.FULL_TILBAKEBETALING,
                        totaltTilbakekrevesBeløp = BigDecimal(10000),
                        totaltTilbakekrevesBeløpMedRenter = BigDecimal(11000),
                        totaltTilbakekrevesBeløpMedRenterUtenSkatt = BigDecimal(7011),
                        totaltRentebeløp = BigDecimal(1000),
                    ),
                    behandling = HbBehandling(
                        erRevurdering = true,
                        originalBehandlingsdatoFagsakvedtak = LocalDate.of(
                            2019,
                            1,
                            1,
                        ),
                    ),
                    hjemmel = HbHjemmel("Folketrygdloven § 22-15"),
                    varsel = HbVarsel(
                        varsletBeløp = BigDecimal(10000),
                        varsletDato = LocalDate.of(2020, 4, 4),
                    ),
                    konfigurasjon = HbKonfigurasjon(klagefristIUker = 6),
                    vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
                    brevmetadata = brevmetadata.copy(språkkode = Språkkode.NN, gjelderDødsfall = true),
                    søker = HbPerson(
                        navn = "Søker Søkersen",
                        dødsdato = LocalDate.of(2018, 3, 1),
                    ),
                    datoer = HbVedtaksbrevDatoer(perioder = perioder),
                )
            val data = HbVedtaksbrevsdata(vedtaksbrevData, perioder)

            val generertBrev = TekstformatererVedtaksbrev.lagVedtaksbrevsfritekst(data)

            val fasit = les("/vedtaksbrev/OS_revurdering_bruker_død_nynorsk_annet_fritekst.txt")
            generertBrev shouldBe fasit
        }

        @Test
        fun `skal generere vedtaksbrev for_KS_og forsett`() {
            val vedtaksbrevData = felles
                .copy(
                    brevmetadata = brevmetadata.copy(ytelsestype = Ytelsestype.KONTANTSTØTTE),
                    fagsaksvedtaksdato = LocalDate.now(),
                    totalresultat = HbTotalresultat(
                        hovedresultat = Vedtaksresultat.FULL_TILBAKEBETALING,
                        totaltTilbakekrevesBeløp = BigDecimal(10000),
                        totaltTilbakekrevesBeløpMedRenter = BigDecimal(10000),
                        totaltTilbakekrevesBeløpMedRenterUtenSkatt = BigDecimal(10000),
                        totaltRentebeløp = BigDecimal(0),
                    ),
                    hjemmel = HbHjemmel("Folketrygdloven § 22-15"),
                    varsel = HbVarsel(
                        varsletBeløp = BigDecimal(10000),
                        varsletDato = LocalDate.of(2020, 4, 4),
                    ),
                    konfigurasjon = HbKonfigurasjon(klagefristIUker = 6),
                    vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
                )
            val perioder: List<HbVedtaksbrevsperiode> =
                listOf(
                    HbVedtaksbrevsperiode(
                        periode = januar,
                        kravgrunnlag = HbKravgrunnlag.forFeilutbetaltBeløp(BigDecimal(10000)),
                        fakta = HbFakta(
                            hendelsestype = Hendelsestype.ANNET_KS,
                            hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
                            fritekstFakta = "Dette er svindel!",
                        ),
                        vurderinger =
                        HbVurderinger(
                            foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                            vilkårsvurderingsresultat =
                            Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                            aktsomhetsresultat = Aktsomhet.FORSETT,
                        ),
                        resultat = HbResultatTestBuilder.forTilbakekrevesBeløpOgRenter(10000, 0),
                        førstePeriode = true,
                    ),
                )
            val data = HbVedtaksbrevsdata(vedtaksbrevData, perioder)

            val generertBrev = TekstformatererVedtaksbrev.lagVedtaksbrevsfritekst(data)

            val fasit = les("/vedtaksbrev/KS_forsett.txt")
            generertBrev shouldBe fasit
        }

        @Test
        fun `skal generere vedtaksbrev for OS med og uten foreldelse og uten skatt`() {
            val vedtaksbrevData = felles
                .copy(
                    fagsaksvedtaksdato = LocalDate.of(2019, 11, 12),
                    totalresultat = HbTotalresultat(
                        hovedresultat = Vedtaksresultat.DELVIS_TILBAKEBETALING,
                        totaltTilbakekrevesBeløp = BigDecimal(1000),
                        totaltTilbakekrevesBeløpMedRenter = BigDecimal(1000),
                        totaltRentebeløp = BigDecimal.ZERO,
                        totaltTilbakekrevesBeløpMedRenterUtenSkatt = BigDecimal(1000),
                    ),
                    varsel = null,
                    hjemmel = HbHjemmel("Folketrygdloven § 22-15"),
                    vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
                )
            val perioder: List<HbVedtaksbrevsperiode> =
                listOf(
                    HbVedtaksbrevsperiode(
                        periode = januar,
                        kravgrunnlag = HbKravgrunnlag(
                            BigDecimal.ZERO,
                            BigDecimal(1000),
                            BigDecimal(1000),
                        ),
                        fakta = HbFakta(
                            Hendelsestype.ENSLIG_FORSØRGER,
                            Hendelsesundertype.BARN_FLYTTET,
                        ),
                        vurderinger = HbVurderinger(
                            foreldelsevurdering = Foreldelsesvurderingstype.FORELDET,
                            aktsomhetsresultat = AnnenVurdering.FORELDET,
                            foreldelsesfrist = januar.fom.plusMonths(11),
                        ),
                        resultat = HbResultat(
                            tilbakekrevesBeløp = BigDecimal.ZERO,
                            tilbakekrevesBeløpUtenSkattMedRenter = BigDecimal.ZERO,
                            rentebeløp = BigDecimal.ZERO,
                            foreldetBeløp = BigDecimal(1000),
                        ),
                        førstePeriode = true,
                    ),
                    HbVedtaksbrevsperiode(
                        periode = februar,
                        kravgrunnlag = HbKravgrunnlag(
                            BigDecimal.ZERO,
                            BigDecimal(1000),
                            BigDecimal(1000),
                        ),
                        fakta = HbFakta(
                            Hendelsestype.MEDLEMSKAP,
                            Hendelsesundertype.LOVLIG_OPPHOLD,
                        ),
                        vurderinger =
                        HbVurderinger(
                            foreldelsevurdering = Foreldelsesvurderingstype.TILLEGGSFRIST,
                            foreldelsesfrist = januar.fom.plusMonths(11),
                            oppdagelsesdato = januar.fom.plusMonths(8),
                            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
                            aktsomhetsresultat = AnnenVurdering.GOD_TRO,
                            beløpIBehold = BigDecimal(1000),
                        ),
                        resultat = HbResultatTestBuilder.forTilbakekrevesBeløp(1000),
                        førstePeriode = true,
                    ),
                )
            val data = HbVedtaksbrevsdata(vedtaksbrevData, perioder)
            val generertBrev = TekstformatererVedtaksbrev.lagVedtaksbrevsfritekst(data)
            val fasit = les("/vedtaksbrev/OS_delvis_foreldelse_uten_varsel.txt")
            generertBrev shouldBe fasit
        }

        @Test
        fun `skal generere vedtaksbrev for OS ingen tilbakekreving pga lavt beløp`() {
            val vedtaksbrevData = felles
                .copy(
                    fagsaksvedtaksdato = LocalDate.now(),
                    totalresultat = HbTotalresultat(
                        hovedresultat = Vedtaksresultat.INGEN_TILBAKEBETALING,
                        totaltTilbakekrevesBeløp = BigDecimal.ZERO,
                        totaltTilbakekrevesBeløpMedRenter = BigDecimal.ZERO,
                        totaltRentebeløp = BigDecimal.ZERO,
                        totaltTilbakekrevesBeløpMedRenterUtenSkatt = BigDecimal.ZERO,
                    ),
                    hjemmel = HbHjemmel("Folketrygdloven § 22-15 6.ledd"),
                    varsel = HbVarsel(
                        varsletBeløp = BigDecimal(500),
                        varsletDato = LocalDate.of(2020, 4, 4),
                    ),
                    vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
                )
            val perioder: List<HbVedtaksbrevsperiode> =
                listOf(
                    HbVedtaksbrevsperiode(
                        periode = førsteNyttårsdag,
                        kravgrunnlag = HbKravgrunnlag.forFeilutbetaltBeløp(BigDecimal(500)),
                        fakta = HbFakta(
                            Hendelsestype.ANNET,
                            Hendelsesundertype.ANNET_FRITEKST,
                            "foo bar baz",
                        ),
                        vurderinger =
                        HbVurderinger(
                            foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                            vilkårsvurderingsresultat =
                            Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
                            aktsomhetsresultat = Aktsomhet.SIMPEL_UAKTSOMHET,
                            unntasInnkrevingPgaLavtBeløp = true,
                        ),
                        resultat = HbResultatTestBuilder.forTilbakekrevesBeløp(0),
                        førstePeriode = true,
                    ),
                )
            val data = HbVedtaksbrevsdata(vedtaksbrevData, perioder)

            val generertBrev = TekstformatererVedtaksbrev.lagVedtaksbrevsfritekst(data)

            val fasit = les("/vedtaksbrev/OS_ikke_tilbakekreves_pga_lavt_beløp.txt")
            generertBrev shouldBe fasit
        }

        @Test
        fun `skal generere vedtaksbrev for BA ingen tilbakekreving pga lavt beløp død bruker`() {
            val perioder: List<HbVedtaksbrevsperiode> =
                listOf(
                    HbVedtaksbrevsperiode(
                        periode = januar,
                        kravgrunnlag = HbKravgrunnlag.forFeilutbetaltBeløp(BigDecimal(500)),
                        fakta = HbFakta(
                            Hendelsestype.DØDSFALL,
                            Hendelsesundertype.BRUKER_DØD,
                            "foo bar baz",
                        ),
                        vurderinger =
                        HbVurderinger(
                            foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                            vilkårsvurderingsresultat =
                            Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
                            aktsomhetsresultat = Aktsomhet.SIMPEL_UAKTSOMHET,
                            unntasInnkrevingPgaLavtBeløp = true,
                        ),
                        resultat = HbResultatTestBuilder.forTilbakekrevesBeløp(0),
                        førstePeriode = true,
                    ),
                )
            val vedtaksbrevData = felles
                .copy(
                    brevmetadata = brevmetadata.copy(ytelsestype = Ytelsestype.BARNETRYGD, gjelderDødsfall = true),
                    fagsaksvedtaksdato = LocalDate.now(),
                    totalresultat = HbTotalresultat(
                        hovedresultat = Vedtaksresultat.INGEN_TILBAKEBETALING,
                        totaltTilbakekrevesBeløp = BigDecimal.ZERO,
                        totaltTilbakekrevesBeløpMedRenter = BigDecimal.ZERO,
                        totaltRentebeløp = BigDecimal.ZERO,
                        totaltTilbakekrevesBeløpMedRenterUtenSkatt = BigDecimal.ZERO,
                    ),
                    hjemmel = HbHjemmel("Folketrygdloven § 22-15 6.ledd"),
                    varsel = HbVarsel(
                        varsletBeløp = BigDecimal(500),
                        varsletDato = LocalDate.of(2020, 4, 4),
                    ),
                    søker = HbPerson(
                        navn = "Søker Søkersen",
                        dødsdato = LocalDate.of(2018, 3, 1),
                    ),
                    vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
                    datoer = HbVedtaksbrevDatoer(perioder = perioder),
                )
            val data = HbVedtaksbrevsdata(vedtaksbrevData, perioder)

            val generertBrev = TekstformatererVedtaksbrev.lagVedtaksbrevsfritekst(data)

            val fasit = les("/vedtaksbrev/BA_ikke_tilbakekreves_pga_lavt_beløp_død_bruker.txt")
            generertBrev shouldBe fasit
        }

        @Test
        fun `skal generere vedtaksbrev for BA ingen tilbakekreving pga lavt beløp fritekst død bruker nynorsk`() {
            val perioder: List<HbVedtaksbrevsperiode> =
                listOf(
                    HbVedtaksbrevsperiode(
                        periode = januar,
                        kravgrunnlag = HbKravgrunnlag.forFeilutbetaltBeløp(BigDecimal(500)),
                        fakta = HbFakta(
                            Hendelsestype.DØDSFALL,
                            Hendelsesundertype.BRUKER_DØD,
                        ),
                        vurderinger =
                        HbVurderinger(
                            foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                            vilkårsvurderingsresultat =
                            Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
                            aktsomhetsresultat = Aktsomhet.SIMPEL_UAKTSOMHET,
                            unntasInnkrevingPgaLavtBeløp = true,
                            fritekst = "foo bar baz",
                        ),
                        resultat = HbResultatTestBuilder.forTilbakekrevesBeløp(0),
                        førstePeriode = true,
                    ),
                )
            val vedtaksbrevData = felles
                .copy(
                    brevmetadata = brevmetadata.copy(
                        ytelsestype = Ytelsestype.BARNETRYGD,
                        gjelderDødsfall = true,
                        språkkode = Språkkode.NN,
                    ),
                    fagsaksvedtaksdato = LocalDate.now(),
                    totalresultat = HbTotalresultat(
                        hovedresultat = Vedtaksresultat.INGEN_TILBAKEBETALING,
                        totaltTilbakekrevesBeløp = BigDecimal.ZERO,
                        totaltTilbakekrevesBeløpMedRenter = BigDecimal.ZERO,
                        totaltRentebeløp = BigDecimal.ZERO,
                        totaltTilbakekrevesBeløpMedRenterUtenSkatt = BigDecimal.ZERO,
                    ),
                    hjemmel = HbHjemmel("Folketrygdloven § 22-15 6.ledd"),
                    varsel = HbVarsel(
                        varsletBeløp = BigDecimal(500),
                        varsletDato = LocalDate.of(2020, 4, 4),
                    ),
                    søker = HbPerson(
                        navn = "Søker Søkersen",
                        dødsdato = LocalDate.of(2018, 3, 1),
                    ),
                    vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
                    datoer = HbVedtaksbrevDatoer(perioder = perioder),
                )
            val data = HbVedtaksbrevsdata(vedtaksbrevData, perioder)

            val generertBrev = TekstformatererVedtaksbrev.lagVedtaksbrevsfritekst(data)

            val fasit = les("/vedtaksbrev/BA_ikke_tilbakekreves_pga_lavt_beløp_død_bruker_nynorsk.txt")
            generertBrev shouldBe fasit
        }

        @Test
        fun `skal generere vedtaksbrev for OS ingen tilbakekreving pga lavt beløp med korrigert beløp`() {
            val vedtaksbrevData = felles
                .copy(
                    fagsaksvedtaksdato = LocalDate.now(),
                    totalresultat = HbTotalresultat(
                        hovedresultat = Vedtaksresultat.INGEN_TILBAKEBETALING,
                        totaltTilbakekrevesBeløp = BigDecimal.ZERO,
                        totaltTilbakekrevesBeløpMedRenter = BigDecimal.ZERO,
                        totaltRentebeløp = BigDecimal.ZERO,
                        totaltTilbakekrevesBeløpMedRenterUtenSkatt = BigDecimal.ZERO,
                    ),
                    hjemmel = HbHjemmel("Folketrygdloven § 22-15 6.ledd"),
                    varsel = HbVarsel(
                        varsletBeløp = BigDecimal(15000),
                        varsletDato = LocalDate.of(2020, 4, 4),
                    ),
                    erFeilutbetaltBeløpKorrigertNed = true,
                    totaltFeilutbetaltBeløp = BigDecimal(1000),
                    vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
                )
            val perioder: List<HbVedtaksbrevsperiode> =
                listOf(
                    HbVedtaksbrevsperiode(
                        periode = førsteNyttårsdag,
                        kravgrunnlag = HbKravgrunnlag.forFeilutbetaltBeløp(BigDecimal(500)),
                        fakta = HbFakta(
                            Hendelsestype.ANNET,
                            Hendelsesundertype.ANNET_FRITEKST,
                            "foo bar baz",
                        ),
                        vurderinger =
                        HbVurderinger(
                            foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                            vilkårsvurderingsresultat =
                            Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
                            aktsomhetsresultat = Aktsomhet.SIMPEL_UAKTSOMHET,
                            unntasInnkrevingPgaLavtBeløp = true,
                        ),
                        resultat = HbResultatTestBuilder.forTilbakekrevesBeløp(0),
                        førstePeriode = true,
                    ),
                )
            val data = HbVedtaksbrevsdata(vedtaksbrevData, perioder)

            val generertBrev = TekstformatererVedtaksbrev.lagVedtaksbrevsfritekst(data)

            val fasit = les("/vedtaksbrev/OS_ikke_tilbakekreves_med_korrigert_beløp.txt")
            generertBrev shouldBe fasit
        }

        @Test
        fun `lagVedtaksbrevFritekst skal generere fritekst og uten perioder vedtaksbrev revurdering for OS med full tilbakebetaling`() {
            val fritekstVedtaksbrevsdata: HbVedtaksbrevsdata =
                lagFritekstVedtaksbrevData(Ytelsestype.OVERGANGSSTØNAD, Vedtaksresultat.FULL_TILBAKEBETALING)

            val generertBrev = TekstformatererVedtaksbrev.lagVedtaksbrevsfritekst(fritekstVedtaksbrevsdata)

            generertBrev.shouldNotBeEmpty()
            val fasit = les("/vedtaksbrev/Fritekst_Vedtaksbrev_OS_full_tilbakebetaling.txt")
            generertBrev shouldBe fasit
        }

        @Test
        fun `lagVedtaksbrevFritekst skal generere fritekst og uten perioder vedtaksbrev revurdering for KS med ingen tilbakebetaling`() {
            val fritekstVedtaksbrevsdata: HbVedtaksbrevsdata =
                lagFritekstVedtaksbrevData(Ytelsestype.KONTANTSTØTTE, Vedtaksresultat.INGEN_TILBAKEBETALING)

            val generertBrev = TekstformatererVedtaksbrev.lagVedtaksbrevsfritekst(fritekstVedtaksbrevsdata)

            generertBrev.shouldNotBeEmpty()
            val fasit = les("/vedtaksbrev/Fritekst_Vedtaksbrev_KS_ingen_tilbakebetaling.txt")
            generertBrev shouldBe fasit
        }

        private fun lagFritekstVedtaksbrevData(
            ytelsestype: Ytelsestype,
            hovedresultat: Vedtaksresultat,
        ): HbVedtaksbrevsdata {
            return HbVedtaksbrevsdata(
                felles.copy(
                    brevmetadata = brevmetadata.copy(
                        språkkode = Språkkode.NB,
                        ytelsestype = ytelsestype,
                    ),
                    totalresultat = felles.totalresultat.copy(hovedresultat = hovedresultat),
                    behandling = HbBehandling(
                        erRevurdering = true,
                        originalBehandlingsdatoFagsakvedtak = LocalDate.now(),
                    ),
                    hjemmel = HbHjemmel("Folketrygdloven § 22-15"),
                    fritekstoppsummering = "sender fritekst vedtaksbrev",
                    vedtaksbrevstype = Vedtaksbrevstype.FRITEKST_FEILUTBETALING_BORTFALT,
                ),
                emptyList(),
            )
        }
    }

    @Nested
    inner class LagVedtaksbrevOverskrift {

        @Test
        fun `skal generere vedtaksbrev overskrift_OVERGANGSSTØNAD_full tilbakebetaling`() {
            val data: HbVedtaksbrevsdata =
                lagBrevOverskriftTestoppsett(Ytelsestype.OVERGANGSSTØNAD, Vedtaksresultat.FULL_TILBAKEBETALING, Språkkode.NB)

            val overskrift = TekstformatererVedtaksbrev.lagVedtaksbrevsoverskrift(data)

            val fasit = "Du må betale tilbake overgangsstønaden"
            overskrift shouldBe fasit
        }

        @Test
        fun `skal generere vedtaksbrev overskrift_kontantstøtte_full tilbakebetaling nynorsk`() {
            val data = lagBrevOverskriftTestoppsett(
                Ytelsestype.KONTANTSTØTTE,
                Vedtaksresultat.FULL_TILBAKEBETALING,
                Språkkode.NN,
            )

            val overskrift = TekstformatererVedtaksbrev.lagVedtaksbrevsoverskrift(data)

            val fasit = "Du må betale tilbake kontantstøtta"
            overskrift shouldBe fasit
        }

        @Test
        fun `skal generere vedtaksbrev overskrift_KONTANTSTØTTE_ingen tilbakebetaling`() {
            val data: HbVedtaksbrevsdata =
                lagBrevOverskriftTestoppsett(Ytelsestype.OVERGANGSSTØNAD, Vedtaksresultat.INGEN_TILBAKEBETALING, Språkkode.NB)

            val overskrift = TekstformatererVedtaksbrev.lagVedtaksbrevsoverskrift(data)

            val fasit = "Du må ikke betale tilbake overgangsstønaden"
            overskrift shouldBe fasit
        }

        private fun lagBrevOverskriftTestoppsett(
            ytelsestype: Ytelsestype,
            hovedresultat: Vedtaksresultat,
            språkkode: Språkkode,
        ): HbVedtaksbrevsdata {
            return HbVedtaksbrevsdata(
                felles.copy(
                    brevmetadata = brevmetadata.copy(språkkode = språkkode, ytelsestype = ytelsestype),
                    totalresultat = felles.totalresultat.copy(hovedresultat = hovedresultat),
                ),
                emptyList(),
            )
        }
    }

    @Nested
    inner class LagDeltekst {

        @Test
        fun `skal ha riktig tekst for særlige grunner når det er reduksjon av beløp`() {
            val felles = felles.copy(
                brevmetadata = brevmetadata.copy(språkkode = Språkkode.NN),
                fagsaksvedtaksdato = LocalDate.now(),
                totalresultat = HbTotalresultat(
                    Vedtaksresultat.FULL_TILBAKEBETALING,
                    BigDecimal(1000),
                    BigDecimal(1100),
                    BigDecimal(1100),
                    BigDecimal(100),
                ),
                hjemmel = HbHjemmel("foo"),
                varsel = HbVarsel(
                    varsletBeløp = BigDecimal(1000),
                    varsletDato = LocalDate.of(2020, 4, 4),
                ),
            )
            val periode =
                HbVedtaksbrevsperiode(
                    periode = januar,
                    kravgrunnlag = HbKravgrunnlag.forFeilutbetaltBeløp(BigDecimal(1000)),
                    fakta = HbFakta(Hendelsestype.ANNET, Hendelsesundertype.ANNET_FRITEKST),
                    vurderinger = HbVurderinger(
                        foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                        vilkårsvurderingsresultat = Vilkårsvurderingsresultat
                            .FEIL_OPPLYSNINGER_FRA_BRUKER,
                        aktsomhetsresultat = Aktsomhet.SIMPEL_UAKTSOMHET,
                        særligeGrunner =
                        HbSærligeGrunner(
                            listOf(SærligGrunn.GRAD_AV_UAKTSOMHET),
                            null,
                            null,
                        ),
                    ),
                    resultat = HbResultat(
                        tilbakekrevesBeløp = BigDecimal(500),
                        rentebeløp = BigDecimal(0),
                        tilbakekrevesBeløpUtenSkattMedRenter = BigDecimal(500),
                    ),
                    førstePeriode = true,
                )

            val generertTekst: String = FellesTekstformaterer.lagDeltekst(
                HbVedtaksbrevPeriodeOgFelles(felles, periode),
                AvsnittUtil.PARTIAL_PERIODE_SÆRLIGE_GRUNNER,
            )

            generertTekst shouldContain "Vi har lagt vekt på at du ikkje har gitt oss alle nødvendige opplysningar tidsnok " +
                "til at vi kunne unngå feilutbetalinga. Vi vurderer likevel at aktløysa di har vore så lita at vi har " +
                "redusert beløpet du må betale tilbake."
            generertTekst shouldContain "Du må betale 500 kroner"
        }

        @Test
        fun `skal generere tekst for faktaperiode`() {
            val felles = felles.copy(
                fagsaksvedtaksdato = LocalDate.now(),
                totalresultat = HbTotalresultat(
                    Vedtaksresultat.DELVIS_TILBAKEBETALING,
                    BigDecimal(23002),
                    BigDecimal(23002),
                    BigDecimal(23002),
                    BigDecimal.ZERO,
                ),
                hjemmel = HbHjemmel("foo"),
                datoer = HbVedtaksbrevDatoer(
                    opphørsdatoIkkeOmsorg = LocalDate.of(
                        2020,
                        4,
                        4,
                    ),
                ),
                varsel = HbVarsel(
                    varsletBeløp = BigDecimal(33001),
                    varsletDato = LocalDate.of(2020, 4, 4),
                ),
            )
            val periode =
                HbVedtaksbrevsperiode(
                    periode = januar,
                    kravgrunnlag = HbKravgrunnlag.forFeilutbetaltBeløp(BigDecimal(30001)),
                    fakta = HbFakta(Hendelsestype.ENSLIG_FORSØRGER, Hendelsesundertype.BARN_FLYTTET),
                    vurderinger = HbVurderinger(
                        foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                        vilkårsvurderingsresultat = Vilkårsvurderingsresultat
                            .MANGELFULLE_OPPLYSNINGER_FRA_BRUKER,
                        aktsomhetsresultat = Aktsomhet.SIMPEL_UAKTSOMHET,
                        særligeGrunner =
                        HbSærligeGrunner(
                            listOf(
                                SærligGrunn.TID_FRA_UTBETALING,
                                SærligGrunn.STØRRELSE_BELØP,
                            ),
                        ),
                    ),
                    resultat = HbResultatTestBuilder.forTilbakekrevesBeløp(20002),
                    førstePeriode = true,
                )
            val data = HbVedtaksbrevPeriodeOgFelles(felles, periode)

            val generertTekst = FellesTekstformaterer.lagDeltekst(data, AvsnittUtil.PARTIAL_PERIODE_FAKTA)

            val fasit = "Du har fått overgangsstønad for barn som ikke bor fast hos deg. Du har derfor fått 30 001 kroner " +
                "for mye utbetalt i denne perioden."
            generertTekst shouldBe fasit
        }

        @Test
        fun `skal si at du ikke trenger betale tilbake når det er god tro og beløp ikke er i behold`() {
            val felles = felles.copy(
                fagsaksvedtaksdato = LocalDate.now(),
                totalresultat = HbTotalresultat(
                    Vedtaksresultat.DELVIS_TILBAKEBETALING,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                ),
                hjemmel = HbHjemmel("foo"),
                varsel = HbVarsel(
                    varsletBeløp = BigDecimal(1000),
                    varsletDato = LocalDate.of(2020, 4, 4),
                ),
            )
            val periode =
                HbVedtaksbrevsperiode(
                    periode = januar,
                    kravgrunnlag = HbKravgrunnlag.forFeilutbetaltBeløp(BigDecimal(1000)),
                    fakta = HbFakta(Hendelsestype.ANNET, Hendelsesundertype.ANNET_FRITEKST),
                    vurderinger = HbVurderinger(
                        foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                        vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
                        aktsomhetsresultat = AnnenVurdering.GOD_TRO,
                        beløpIBehold = BigDecimal.ZERO,
                    ),
                    resultat = HbResultatTestBuilder.forTilbakekrevesBeløp(0),
                    førstePeriode = true,
                )
            val data = HbVedtaksbrevPeriodeOgFelles(felles, periode)

            val generertTekst = FellesTekstformaterer.lagDeltekst(data, AvsnittUtil.PARTIAL_PERIODE_VILKÅR)

            generertTekst shouldContain "_Hvordan har vi kommet fram til at du ikke må betale tilbake?"
        }

        @Test
        fun `skal ha riktig tekst for særlige grunner når det ikke er reduksjon av beløp`() {
            val felles = felles.copy(
                fagsaksvedtaksdato = LocalDate.now(),
                totalresultat = HbTotalresultat(
                    Vedtaksresultat.FULL_TILBAKEBETALING,
                    BigDecimal(1000),
                    BigDecimal(1100),
                    BigDecimal(1100),
                    BigDecimal(100),
                ),
                hjemmel = HbHjemmel("foo"),
                varsel = HbVarsel(
                    varsletBeløp = BigDecimal(1000),
                    varsletDato = LocalDate.of(2020, 4, 4),
                ),
            )
            val periode =
                HbVedtaksbrevsperiode(
                    periode = januar,
                    kravgrunnlag = HbKravgrunnlag.forFeilutbetaltBeløp(BigDecimal(1000)),
                    fakta = HbFakta(Hendelsestype.ANNET, Hendelsesundertype.ANNET_FRITEKST),
                    vurderinger = HbVurderinger(
                        foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                        vilkårsvurderingsresultat = Vilkårsvurderingsresultat
                            .FEIL_OPPLYSNINGER_FRA_BRUKER,
                        aktsomhetsresultat = Aktsomhet.GROV_UAKTSOMHET,
                        særligeGrunner =
                        HbSærligeGrunner(listOf(SærligGrunn.GRAD_AV_UAKTSOMHET)),
                    ),
                    resultat = HbResultat(
                        tilbakekrevesBeløp = BigDecimal(1000),
                        rentebeløp = BigDecimal(100),
                        tilbakekrevesBeløpUtenSkattMedRenter = BigDecimal(1000),
                    ),
                    førstePeriode = true,
                )

            val generertTekst: String = FellesTekstformaterer.lagDeltekst(
                HbVedtaksbrevPeriodeOgFelles(felles, periode),
                AvsnittUtil.PARTIAL_PERIODE_SÆRLIGE_GRUNNER,
            )
            generertTekst shouldContain "Vi har vurdert om det er grunner til å redusere beløpet. " +
                "Vi har lagt vekt på at du ikke har gitt oss alle nødvendige opplysninger tidsnok " +
                "til at vi kunne unngå feilutbetalingen. Derfor må du betale tilbake hele beløpet."
        }
    }

    private fun les(filnavn: String): String? {
        javaClass.getResourceAsStream(filnavn).use { resource ->
            Scanner(resource, StandardCharsets.UTF_8).use { scanner ->
                scanner.useDelimiter("\\A")
                return if (scanner.hasNext()) scanner.next() else null
            }
        }
    }
}
