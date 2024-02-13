package no.nav.familie.tilbake.dokumentbestilling.vedtak

import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.beregning.modell.Vedtaksresultat
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesperiode
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingAktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsperiode
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class VedtakHjemmelTest {

    var periode: Månedsperiode = Månedsperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

    @Test
    fun `laghbHjemmel skal gi riktig hjemmel når det ikke er foreldelse eller renter bokmål`() {
        val vurderingPerioder: Set<Vilkårsvurderingsperiode> = aktsomhet(periode) { it }

        val hbHjemmel = VedtakHjemmel.lagHjemmel(
            Vedtaksresultat.INGEN_TILBAKEBETALING,
            lagVedtaksbrevgrunnlag(null, vurderingPerioder),
            VedtakHjemmel.EffektForBruker.FØRSTEGANGSVEDTAK,
            Språkkode.NB,
            visHjemmelForRenter = true,
            klagebehandling = false,
        )

        hbHjemmel.lovhjemmelVedtak shouldBe "folketrygdloven § 22-15"
        hbHjemmel.lovhjemmelFlertall shouldBe false
    }

    private fun lagVedtaksbrevgrunnlag(
        vurdertForeldelse: VurdertForeldelse?,
        vurderingPerioder: Set<Vilkårsvurderingsperiode>,
    ): Vedtaksbrevgrunnlag {
        val behandling = Testdata.vedtaksbrevbehandling
            .copy(
                vurderteForeldelser = vurdertForeldelse?.let { setOf(it) } ?: setOf(),
                vilkårsvurdering = setOf(Testdata.vilkårsvurdering.copy(perioder = vurderingPerioder)),
            )
        return Testdata.vedtaksbrevgrunnlag.copy(behandlinger = setOf(behandling), ytelsestype = Ytelsestype.OVERGANGSSTØNAD)
    }

    @Test
    fun `laghbHjemmel skal gi riktig hjemmel når det ikke er foreldelse eller renter nynorsk`() {
        val vurderingPerioder: Set<Vilkårsvurderingsperiode> = aktsomhet(periode) { it }

        val hbHjemmel = VedtakHjemmel.lagHjemmel(
            Vedtaksresultat.INGEN_TILBAKEBETALING,
            lagVedtaksbrevgrunnlag(null, vurderingPerioder),
            VedtakHjemmel.EffektForBruker.FØRSTEGANGSVEDTAK,
            Språkkode.NN,
            visHjemmelForRenter = true,
            klagebehandling = false,
        )

        hbHjemmel.lovhjemmelVedtak shouldBe "folketrygdlova § 22-15"
        hbHjemmel.lovhjemmelFlertall shouldBe false
    }

    @Test
    fun `laghbHjemmel skal gi riktig hjemmel når det er forsto burde forstått og forsett`() {
        val vurderingPerioder: Set<Vilkårsvurderingsperiode> = aktsomhet(periode) {
            it.copy(aktsomhet = Aktsomhet.FORSETT, ileggRenter = false)
        }

        val hbHjemmel = VedtakHjemmel.lagHjemmel(
            Vedtaksresultat.INGEN_TILBAKEBETALING,
            lagVedtaksbrevgrunnlag(null, vurderingPerioder),
            VedtakHjemmel.EffektForBruker.FØRSTEGANGSVEDTAK,
            Språkkode.NB,
            visHjemmelForRenter = true,
            klagebehandling = false,
        )

        hbHjemmel.lovhjemmelVedtak shouldBe "folketrygdloven § 22-15"
        hbHjemmel.lovhjemmelFlertall shouldBe false
    }

    @Test
    fun `laghbHjemmel skal gi riktig hjemmel når det er feilaktig opplysninger og forsett`() {
        val vurderingPerioder: Set<Vilkårsvurderingsperiode> =
            aktsomhet(Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER, periode) {
                it.copy(aktsomhet = Aktsomhet.FORSETT)
            }

        val hbHjemmel = VedtakHjemmel.lagHjemmel(
            Vedtaksresultat.INGEN_TILBAKEBETALING,
            lagVedtaksbrevgrunnlag(null, vurderingPerioder),
            VedtakHjemmel.EffektForBruker.FØRSTEGANGSVEDTAK,
            Språkkode.NB,
            visHjemmelForRenter = true,
            klagebehandling = false,
        )

        hbHjemmel.lovhjemmelVedtak shouldBe "folketrygdloven §§ 22-15 og 22-17 a"
        hbHjemmel.lovhjemmelFlertall shouldBe true
    }

    @Test
    fun `laghbHjemmel skal gi riktig hjemmel når det er feilaktig opplysninger og forsett men frisinn og dermed ikke renter`() {
        val vurderingPerioder: Set<Vilkårsvurderingsperiode> =
            aktsomhet(Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER, periode) {
                it.copy(aktsomhet = Aktsomhet.FORSETT)
            }

        val hbHjemmel = VedtakHjemmel.lagHjemmel(
            Vedtaksresultat.INGEN_TILBAKEBETALING,
            lagVedtaksbrevgrunnlag(null, vurderingPerioder),
            VedtakHjemmel.EffektForBruker.FØRSTEGANGSVEDTAK,
            Språkkode.NB,
            visHjemmelForRenter = false,
            klagebehandling = false,
        )

        hbHjemmel.lovhjemmelVedtak shouldBe "folketrygdloven § 22-15"
        hbHjemmel.lovhjemmelFlertall shouldBe false
    }

    @Test
    fun `laghbHjemmel skal gi riktig hjemmel når det ikke kreves tilbake pga lavt beløp bokmål`() {
        val vurderingPerioder: Set<Vilkårsvurderingsperiode> = aktsomhet(periode) {
            it.copy(tilbakekrevSmåbeløp = false)
        }

        val hbHjemmel = VedtakHjemmel.lagHjemmel(
            Vedtaksresultat.INGEN_TILBAKEBETALING,
            lagVedtaksbrevgrunnlag(null, vurderingPerioder),
            VedtakHjemmel.EffektForBruker.FØRSTEGANGSVEDTAK,
            Språkkode.NB,
            visHjemmelForRenter = true,
            klagebehandling = false,
        )

        hbHjemmel.lovhjemmelVedtak shouldBe "folketrygdloven § 22-15 sjette ledd"
        hbHjemmel.lovhjemmelFlertall shouldBe false
    }

    @Test
    fun `laghbHjemmel skal gi riktig hjemmel når det ikke kreves tilbake pga lavt beløp nynorsk`() {
        val vurderingPerioder: Set<Vilkårsvurderingsperiode> = aktsomhet(periode) {
            it.copy(tilbakekrevSmåbeløp = false)
        }

        val hbHjemmel = VedtakHjemmel.lagHjemmel(
            Vedtaksresultat.INGEN_TILBAKEBETALING,
            lagVedtaksbrevgrunnlag(null, vurderingPerioder),
            VedtakHjemmel.EffektForBruker.FØRSTEGANGSVEDTAK,
            Språkkode.NN,
            visHjemmelForRenter = true,
            klagebehandling = false,
        )

        hbHjemmel.lovhjemmelVedtak shouldBe "folketrygdlova § 22-15 sjette ledd"
        hbHjemmel.lovhjemmelFlertall shouldBe false
    }

    @Test
    fun `laghbHjemmel skal gi riktig hjemmel når alt er foreldet`() {
        val vurdertForeldelse = lagForeldelseperiode(periode) {
            it.copy(
                foreldelsesvurderingstype = Foreldelsesvurderingstype.FORELDET,
                foreldelsesfrist = periode.fom.plusMonths(11).atDay(1),
            )
        }

        val hbHjemmel = VedtakHjemmel.lagHjemmel(
            Vedtaksresultat.INGEN_TILBAKEBETALING,
            lagVedtaksbrevgrunnlag(vurdertForeldelse, emptySet()),
            VedtakHjemmel.EffektForBruker.FØRSTEGANGSVEDTAK,
            Språkkode.NB,
            visHjemmelForRenter = true,
            klagebehandling = false,
        )

        hbHjemmel.lovhjemmelVedtak shouldBe "foreldelsesloven §§ 2 og 3"
        hbHjemmel.lovhjemmelFlertall shouldBe true
    }

    @Test
    fun `laghbHjemmel skal gi riktig hjemmel når noe er foreldet uten tilleggsfrist og ikke renter`() {
        val vurdertForeldelse: VurdertForeldelse = lagForeldelseperiode(periode) {
            it.copy(
                foreldelsesvurderingstype = Foreldelsesvurderingstype.FORELDET,
                foreldelsesfrist = periode.fom.plusMonths(11).atDay(1),
            )
        }
        val vurderingPerioder: Set<Vilkårsvurderingsperiode> = aktsomhet(periode) {
            it.copy(aktsomhet = Aktsomhet.GROV_UAKTSOMHET, ileggRenter = false)
        }

        val hbHjemmel = VedtakHjemmel.lagHjemmel(
            Vedtaksresultat.INGEN_TILBAKEBETALING,
            lagVedtaksbrevgrunnlag(vurdertForeldelse, vurderingPerioder),
            VedtakHjemmel.EffektForBruker.FØRSTEGANGSVEDTAK,
            Språkkode.NB,
            visHjemmelForRenter = true,
            klagebehandling = false,
        )

        hbHjemmel.lovhjemmelVedtak shouldBe "folketrygdloven § 22-15 og foreldelsesloven §§ 2 og 3"
        hbHjemmel.lovhjemmelFlertall shouldBe true
    }

    @Test
    fun `laghbHjemmel skal gi riktig hjemmel når foreldelse er vurdert men ikke ilagt uten tilleggsfrist og renter`() {
        val vurdertForeldelse: VurdertForeldelse = lagForeldelseperiode(periode) {
            it.copy(foreldelsesvurderingstype = Foreldelsesvurderingstype.IKKE_FORELDET)
        }
        val vurderingPerioder: Set<Vilkårsvurderingsperiode> = aktsomhet(periode) {
            it.copy(aktsomhet = Aktsomhet.GROV_UAKTSOMHET, ileggRenter = true)
        }

        val hbHjemmel = VedtakHjemmel.lagHjemmel(
            Vedtaksresultat.INGEN_TILBAKEBETALING,
            lagVedtaksbrevgrunnlag(vurdertForeldelse, vurderingPerioder),
            VedtakHjemmel.EffektForBruker.FØRSTEGANGSVEDTAK,
            Språkkode.NB,
            visHjemmelForRenter = true,
            klagebehandling = false,
        )

        hbHjemmel.lovhjemmelVedtak shouldBe "folketrygdloven §§ 22-15 og 22-17 a"
        hbHjemmel.lovhjemmelFlertall shouldBe true
    }

    @Test
    fun `laghbHjemmel skal gi riktig hjemmel når det er både foreldelse med tilleggsfrist og ikke renter`() {
        val vurdertForeldelse: VurdertForeldelse = lagForeldelseperiode(periode) {
            it.copy(
                foreldelsesvurderingstype = Foreldelsesvurderingstype.TILLEGGSFRIST,
                foreldelsesfrist = periode.fom.plusMonths(11).atDay(1),
                oppdagelsesdato = periode.fom.plusMonths(5).atDay(1),
            )
        }
        val vurderingPerioder: Set<Vilkårsvurderingsperiode> = aktsomhet(periode) {
            it.copy(aktsomhet = Aktsomhet.GROV_UAKTSOMHET, ileggRenter = false)
        }

        val hbHjemmel = VedtakHjemmel.lagHjemmel(
            Vedtaksresultat.INGEN_TILBAKEBETALING,
            lagVedtaksbrevgrunnlag(vurdertForeldelse, vurderingPerioder),
            VedtakHjemmel.EffektForBruker.FØRSTEGANGSVEDTAK,
            Språkkode.NB,
            visHjemmelForRenter = true,
            klagebehandling = false,
        )

        hbHjemmel.lovhjemmelVedtak shouldBe "folketrygdloven § 22-15 og foreldelsesloven §§ 2, 3 og 10"
        hbHjemmel.lovhjemmelFlertall shouldBe true
    }

    @Test
    fun `laghbHjemmel skal gi riktig hjemmel når det er både foreldelse med tilleggsfrist og renter`() {
        val vurdertForeldelse: VurdertForeldelse = lagForeldelseperiode(periode) {
            it.copy(
                foreldelsesvurderingstype = Foreldelsesvurderingstype.TILLEGGSFRIST,
                foreldelsesfrist = periode.fom.plusMonths(11).atDay(1),
                oppdagelsesdato = periode.fom.plusMonths(5).atDay(1),
            )
        }
        val vurderingPerioder: Set<Vilkårsvurderingsperiode> = aktsomhet(periode) {
            it.copy(aktsomhet = Aktsomhet.GROV_UAKTSOMHET, ileggRenter = true)
        }

        val hbHjemmel = VedtakHjemmel.lagHjemmel(
            Vedtaksresultat.INGEN_TILBAKEBETALING,
            lagVedtaksbrevgrunnlag(
                vurdertForeldelse,
                vurderingPerioder,
            ),
            VedtakHjemmel.EffektForBruker.FØRSTEGANGSVEDTAK,
            Språkkode.NB,
            visHjemmelForRenter = true,
            klagebehandling = false,
        )

        hbHjemmel.lovhjemmelVedtak shouldBe "folketrygdloven §§ 22-15 og 22-17 a og foreldelsesloven §§ 2, 3 og 10"
        hbHjemmel.lovhjemmelFlertall shouldBe true
    }

    @Test
    fun `laghbHjemmel skal gi riktig hjemmel når det ikke er foreldelse eller renter og er klage fra KA`() {
        val vurderingPerioder: Set<Vilkårsvurderingsperiode> = aktsomhet(periode) { it }

        val hbHjemmel = VedtakHjemmel.lagHjemmel(
            Vedtaksresultat.INGEN_TILBAKEBETALING,
            lagVedtaksbrevgrunnlag(null, vurderingPerioder),
            VedtakHjemmel.EffektForBruker.ENDRET_TIL_UGUNST_FOR_BRUKER,
            Språkkode.NB,
            visHjemmelForRenter = true,
            klagebehandling = false,
        )

        hbHjemmel.lovhjemmelVedtak shouldBe "folketrygdloven § 22-15 og forvaltningsloven § 35 c)"
        hbHjemmel.lovhjemmelFlertall shouldBe true
    }

    @Test
    fun `laghbHjemmel skal gi riktig hjemmel når det er både foreldelse med tilleggsfrist og renter og er klage fra KA`() {
        val vurdertForeldelse: VurdertForeldelse = lagForeldelseperiode(periode) {
            it.copy(
                foreldelsesvurderingstype = Foreldelsesvurderingstype.TILLEGGSFRIST,
                foreldelsesfrist = periode.fom.plusMonths(11).atDay(1),
                oppdagelsesdato = periode.fom.plusMonths(5).atDay(1),
            )
        }
        val vurderingPerioder: Set<Vilkårsvurderingsperiode> = aktsomhet(periode) {
            it.copy(aktsomhet = Aktsomhet.GROV_UAKTSOMHET, ileggRenter = true)
        }

        val hbHjemmel = VedtakHjemmel.lagHjemmel(
            Vedtaksresultat.INGEN_TILBAKEBETALING,
            lagVedtaksbrevgrunnlag(vurdertForeldelse, vurderingPerioder),
            VedtakHjemmel.EffektForBruker.ENDRET_TIL_GUNST_FOR_BRUKER,
            Språkkode.NB,
            visHjemmelForRenter = true,
            klagebehandling = false,
        )

        hbHjemmel.lovhjemmelVedtak shouldBe "folketrygdloven §§ 22-15 og 22-17 a, " +
            "foreldelsesloven §§ 2, 3 og 10 og forvaltningsloven § 35 a)"
        hbHjemmel.lovhjemmelFlertall shouldBe true
    }

    @Test
    fun `laghbHjemmel skal gi riktig hjemmel når det ikke er foreldelse eller renter og er klage fra NFP`() {
        val vurderingPerioder: Set<Vilkårsvurderingsperiode> = aktsomhet(periode) { it }

        val hbHjemmel = VedtakHjemmel.lagHjemmel(
            Vedtaksresultat.INGEN_TILBAKEBETALING,
            lagVedtaksbrevgrunnlag(null, vurderingPerioder),
            VedtakHjemmel.EffektForBruker.ENDRET_TIL_UGUNST_FOR_BRUKER,
            Språkkode.NB,
            visHjemmelForRenter = true,
            klagebehandling = true,
        )

        hbHjemmel.lovhjemmelVedtak shouldBe "folketrygdloven § 22-15"
        hbHjemmel.lovhjemmelFlertall shouldBe false
    }

    @Test
    fun `laghbHjemmel skal gi riktig hjemmel når det er både foreldelse med tilleggsfrist og renter og er klage fra NFP`() {
        val vurdertForeldelse: VurdertForeldelse = lagForeldelseperiode(periode) {
            it.copy(
                foreldelsesvurderingstype = Foreldelsesvurderingstype.TILLEGGSFRIST,
                foreldelsesfrist = periode.fom.plusMonths(11).atDay(1),
                oppdagelsesdato = periode.fom.plusMonths(5).atDay(1),
            )
        }
        val vurderingPerioder: Set<Vilkårsvurderingsperiode> = aktsomhet(periode) {
            it.copy(aktsomhet = Aktsomhet.GROV_UAKTSOMHET, ileggRenter = true)
        }

        val hbHjemmel = VedtakHjemmel.lagHjemmel(
            Vedtaksresultat.INGEN_TILBAKEBETALING,
            lagVedtaksbrevgrunnlag(vurdertForeldelse, vurderingPerioder),
            VedtakHjemmel.EffektForBruker.ENDRET_TIL_GUNST_FOR_BRUKER,
            Språkkode.NB,
            visHjemmelForRenter = true,
            klagebehandling = true,
        )

        hbHjemmel.lovhjemmelVedtak shouldBe "folketrygdloven §§ 22-15 og 22-17 a og foreldelsesloven §§ 2, 3 og 10"
        hbHjemmel.lovhjemmelFlertall shouldBe true
    }

    private fun lagForeldelseperiode(
        periode: Månedsperiode,
        oppsett: (Foreldelsesperiode) -> Foreldelsesperiode,
    ): VurdertForeldelse {
        val periodeBuilder = Foreldelsesperiode(
            periode = periode,
            foreldelsesvurderingstype = Foreldelsesvurderingstype.IKKE_VURDERT,
            begrunnelse = "bob",
        )
        return VurdertForeldelse(
            behandlingId = UUID.randomUUID(),
            foreldelsesperioder = setOf(oppsett(periodeBuilder)),
        )
    }

    private fun aktsomhet(
        periode: Månedsperiode,
        oppsett: (VilkårsvurderingAktsomhet) -> VilkårsvurderingAktsomhet,
    ): Set<Vilkårsvurderingsperiode> {
        return aktsomhet(Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT, periode, oppsett)
    }

    private fun aktsomhet(
        resultat: Vilkårsvurderingsresultat,
        periode: Månedsperiode,
        oppsett: (VilkårsvurderingAktsomhet) -> VilkårsvurderingAktsomhet,
    ): Set<Vilkårsvurderingsperiode> {
        val aktsomhet: VilkårsvurderingAktsomhet =
            oppsett(VilkårsvurderingAktsomhet(aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET, begrunnelse = "foo"))
        val vurderingPeriode = Vilkårsvurderingsperiode(
            periode = periode,
            vilkårsvurderingsresultat = resultat,
            begrunnelse = "foo",
            aktsomhet = aktsomhet,
        )

        return setOf(vurderingPeriode.copy(aktsomhet = aktsomhet))
    }
}
