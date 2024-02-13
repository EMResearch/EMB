package no.nav.familie.tilbake.dokumentasjonsgenerator

import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.beregning.modell.Vedtaksresultat
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.handlebars.FellesTekstformaterer
import no.nav.familie.tilbake.dokumentbestilling.vedtak.AvsnittUtil
import no.nav.familie.tilbake.dokumentbestilling.vedtak.HendelseMedUndertype
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
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbGrunnbeløp
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbKravgrunnlag
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbResultat
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbVedtaksbrevsperiode
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbVurderinger
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.HendelsestypePerYtelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.HendelsesundertypePerHendelsestype
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.vilkårsvurdering.domain.AnnenVurdering
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/**
 * Brukes for å generere faktatekster for perioder. Resultatet er tekster med markup, som med "Insert markup"-macroen
 * kan limes inn i Confluence, og dermed bli formattert tekst.
 *
 * Confluence:
 * https://confluence.adeo.no/display/TFA/Generert+dokumentasjon
 */
@Disabled("Kjøres ved behov for å regenerere dokumentasjon")
class DokumentasjonsgeneratorPeriodeFakta {

    private val januar = Datoperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 1))

    @Test
    fun `list ut permutasjoner for BA bokmål`() {
        val felles: HbVedtaksbrevFelles = lagFelles(Ytelsestype.BARNETRYGD, Språkkode.NB)
        val resultat: Map<HendelseMedUndertype, String> = lagFaktatekster(felles)
        prettyPrint(resultat)
    }

    @Test
    fun `list ut permutasjoner for BA nynorsk`() {
        val felles: HbVedtaksbrevFelles = lagFelles(Ytelsestype.BARNETRYGD, Språkkode.NN)
        val resultat: Map<HendelseMedUndertype, String> = lagFaktatekster(felles)
        prettyPrint(resultat)
    }

    @Test
    fun `list ut permutasjoner for EFOG bokmål`() {
        val felles: HbVedtaksbrevFelles = lagFelles(Ytelsestype.OVERGANGSSTØNAD, Språkkode.NB)
        val resultat: Map<HendelseMedUndertype, String> = lagFaktatekster(felles)
        prettyPrint(resultat)
    }

    @Test
    fun `list ut permutasjoner for EFOG nynorsk`() {
        val felles: HbVedtaksbrevFelles = lagFelles(Ytelsestype.OVERGANGSSTØNAD, Språkkode.NN)
        val resultat: Map<HendelseMedUndertype, String> = lagFaktatekster(felles)
        prettyPrint(resultat)
    }

    @Test
    fun `list ut permutasjoner for EFBT bokmål`() {
        val felles: HbVedtaksbrevFelles = lagFelles(Ytelsestype.BARNETILSYN, Språkkode.NB)
        val resultat: Map<HendelseMedUndertype, String> = lagFaktatekster(felles)
        prettyPrint(resultat)
    }

    @Test
    fun `list ut permutasjoner for EFBT nynorsk`() {
        val felles: HbVedtaksbrevFelles = lagFelles(Ytelsestype.BARNETILSYN, Språkkode.NN)
        val resultat: Map<HendelseMedUndertype, String> = lagFaktatekster(felles)
        prettyPrint(resultat)
    }

    @Test
    fun `list ut permutasjoner for EFSP bokmål`() {
        val felles: HbVedtaksbrevFelles = lagFelles(Ytelsestype.SKOLEPENGER, Språkkode.NB)
        val resultat: Map<HendelseMedUndertype, String> = lagFaktatekster(felles)
        prettyPrint(resultat)
    }

    @Test
    fun `list ut permutasjoner for EFSP nynorsk`() {
        val felles: HbVedtaksbrevFelles = lagFelles(Ytelsestype.SKOLEPENGER, Språkkode.NN)
        val resultat: Map<HendelseMedUndertype, String> = lagFaktatekster(felles)
        prettyPrint(resultat)
    }

    private fun prettyPrint(resultat: Map<HendelseMedUndertype, String>) {
        resultat.forEach { (typer, generertTekst) ->
            println("*[ ${typer.hendelsestype.name} - ${typer.hendelsesundertype.name} ]*")
            val parametrisertTekst = generertTekst
                .replace(" 10\u00A0000\u00A0kroner".toRegex(), " <feilutbetalt beløp> kroner")
                .replace(" 33\u00A0333\u00A0kroner".toRegex(), " <utbetalt beløp> kroner")
                .replace(" 23\u00A0333\u00A0kroner".toRegex(), " <riktig beløp> kroner")
                .replace("Søker Søkersen".toRegex(), "<søkers navn>")
                .replace("2. mars 2018".toRegex(), "<opphørsdato søker døde>")
                .replace("3. mars 2018".toRegex(), "<opphørsdato barn døde>")
                .replace("4. mars 2018".toRegex(), "<opphørsdato ikke omsorg>")
                .replace("ektefellen".toRegex(), "<ektefellen/partneren/samboeren>")
            println(parametrisertTekst)
            println()
        }
    }

    private fun lagFaktatekster(felles: HbVedtaksbrevFelles): Map<HendelseMedUndertype, String> {
        return getFeilutbetalingsårsaker(felles.brevmetadata.ytelsestype).associateWith {
            val periode: HbVedtaksbrevsperiode = lagPeriodeBuilder(it)
            val data = HbVedtaksbrevPeriodeOgFelles(felles, periode)
            FellesTekstformaterer.lagDeltekst(data, AvsnittUtil.PARTIAL_PERIODE_FAKTA)
        }
    }

    private fun lagPeriodeBuilder(undertype: HendelseMedUndertype): HbVedtaksbrevsperiode {
        return HbVedtaksbrevsperiode(
            periode = januar,
            vurderinger = HbVurderinger(
                foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                aktsomhetsresultat = AnnenVurdering.GOD_TRO,
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
                beløpIBehold = BigDecimal.valueOf(5000),
            ),
            kravgrunnlag = HbKravgrunnlag(
                feilutbetaltBeløp = BigDecimal.valueOf(10000),
                riktigBeløp = BigDecimal.valueOf(23333),
                utbetaltBeløp = BigDecimal.valueOf(33333),
            ),
            resultat = HbResultat(
                tilbakekrevesBeløp = BigDecimal.valueOf(5000),
                tilbakekrevesBeløpUtenSkattMedRenter = BigDecimal.valueOf(4002),
                rentebeløp = BigDecimal.ZERO,
            ),
            fakta = HbFakta(undertype.hendelsestype, undertype.hendelsesundertype),
            grunnbeløp = HbGrunnbeløp(BigDecimal.TEN, "120"),
            førstePeriode = true,
        )
    }

    private fun lagFelles(
        ytelsestype: Ytelsestype,
        språkkode: Språkkode,
    ): HbVedtaksbrevFelles {
        val datoer = HbVedtaksbrevDatoer(
            LocalDate.of(2018, 3, 2),
            LocalDate.of(2018, 3, 3),
            LocalDate.of(2018, 3, 4),
        )

        return HbVedtaksbrevFelles(
            brevmetadata = lagMetadata(ytelsestype, språkkode),
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

    private fun lagMetadata(
        ytelsestype: Ytelsestype,
        språkkode: Språkkode,
    ): Brevmetadata {
        return Brevmetadata(
            sakspartId = "",
            sakspartsnavn = "",
            mottageradresse = Adresseinfo("01020312345", "Bob"),
            behandlendeEnhetsNavn = "Oslo",
            ansvarligSaksbehandler = "Bob",
            saksnummer = "1232456",
            språkkode = språkkode,
            ytelsestype = ytelsestype,
            gjelderDødsfall = false,
        )
    }

    private fun getFeilutbetalingsårsaker(ytelseType: Ytelsestype): List<HendelseMedUndertype> {
        val hendelseTyper: Set<Hendelsestype> = HendelsestypePerYtelsestype.getHendelsestyper(ytelseType)
        val hendelseUndertypePrHendelsestype = HendelsesundertypePerHendelsestype.HIERARKI
        val resultat: List<HendelseMedUndertype> =
            hendelseTyper.map {
                hendelseUndertypePrHendelsestype[it]?.map { undertype -> HendelseMedUndertype(it, undertype) } ?: listOf()
            }.flatten()
        return resultat
    }
}
