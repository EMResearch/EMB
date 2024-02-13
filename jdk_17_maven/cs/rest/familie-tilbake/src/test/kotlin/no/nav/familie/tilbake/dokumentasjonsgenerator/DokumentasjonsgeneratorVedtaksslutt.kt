package no.nav.familie.tilbake.dokumentasjonsgenerator

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
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/**
 * Brukes for å generere tekster for slutten av vedtaksbrevet. Resultatet er tekster med markup, som med "Insert markup"-macroen
 * kan limes inn i Confluence, og dermed bli formattert tekst.
 *
 * Confluence:
 * https://confluence.adeo.no/display/TFA/Generert+dokumentasjon
 */
// @Disabled("Kjøres ved behov for å regenerere dokumentasjon")
class DokumentasjonsgeneratorVedtaksslutt {

    @Test
    fun `list ut vedtak slutt EFOG bokmål`() {
        lagVedtakSluttTekster(Ytelsestype.OVERGANGSSTØNAD, Språkkode.NB, Vedtaksresultat.FULL_TILBAKEBETALING)
        lagVedtakSluttTekster(Ytelsestype.OVERGANGSSTØNAD, Språkkode.NB, Vedtaksresultat.INGEN_TILBAKEBETALING)
    }

    @Test
    fun `list ut vedtak slutt EFOG nynorsk`() {
        lagVedtakSluttTekster(Ytelsestype.OVERGANGSSTØNAD, Språkkode.NN, Vedtaksresultat.FULL_TILBAKEBETALING)
        lagVedtakSluttTekster(Ytelsestype.OVERGANGSSTØNAD, Språkkode.NN, Vedtaksresultat.INGEN_TILBAKEBETALING)
    }

    @Test
    fun `list ut vedtak slutt EFBT bokmål`() {
        lagVedtakSluttTekster(Ytelsestype.BARNETILSYN, Språkkode.NB, Vedtaksresultat.FULL_TILBAKEBETALING, false)
        lagVedtakSluttTekster(Ytelsestype.BARNETILSYN, Språkkode.NB, Vedtaksresultat.INGEN_TILBAKEBETALING, false)
    }

    @Test
    fun `list ut vedtak slutt EFBT nynorsk`() {
        lagVedtakSluttTekster(Ytelsestype.BARNETILSYN, Språkkode.NN, Vedtaksresultat.FULL_TILBAKEBETALING, false)
        lagVedtakSluttTekster(Ytelsestype.BARNETILSYN, Språkkode.NN, Vedtaksresultat.INGEN_TILBAKEBETALING, false)
    }

    @Test
    fun `list ut vedtak slutt BA bokmål`() {
        lagVedtakSluttTekster(Ytelsestype.BARNETRYGD, Språkkode.NB, Vedtaksresultat.FULL_TILBAKEBETALING, false)
        lagVedtakSluttTekster(Ytelsestype.BARNETRYGD, Språkkode.NB, Vedtaksresultat.INGEN_TILBAKEBETALING, false)
    }

    @Test
    fun `list ut vedtak slutt BA nynorsk`() {
        lagVedtakSluttTekster(Ytelsestype.BARNETRYGD, Språkkode.NN, Vedtaksresultat.FULL_TILBAKEBETALING, false)
        lagVedtakSluttTekster(Ytelsestype.BARNETRYGD, Språkkode.NN, Vedtaksresultat.INGEN_TILBAKEBETALING, false)
    }

    @Test
    fun `list ut vedtak slutt EFSP bokmål`() {
        lagVedtakSluttTekster(Ytelsestype.SKOLEPENGER, Språkkode.NB, Vedtaksresultat.FULL_TILBAKEBETALING, false)
        lagVedtakSluttTekster(Ytelsestype.SKOLEPENGER, Språkkode.NB, Vedtaksresultat.INGEN_TILBAKEBETALING, false)
    }

    @Test
    fun `list ut vedtak slutt EFSP nynorsk`() {
        lagVedtakSluttTekster(Ytelsestype.SKOLEPENGER, Språkkode.NN, Vedtaksresultat.FULL_TILBAKEBETALING, false)
        lagVedtakSluttTekster(Ytelsestype.SKOLEPENGER, Språkkode.NN, Vedtaksresultat.INGEN_TILBAKEBETALING, false)
    }

    private fun lagVedtakSluttTekster(ytelsetype: Ytelsestype, språkkode: Språkkode, resultatType: Vedtaksresultat) {
        for (medSkattetrekk in trueFalse) {
            lagVedtakSluttTekster(ytelsetype, språkkode, resultatType, medSkattetrekk)
        }
    }

    private fun lagVedtakSluttTekster(
        ytelsetype: Ytelsestype,
        språkkode: Språkkode,
        resultatType: Vedtaksresultat,
        medSkattetrekk: Boolean,
    ) {
        trueFalse.forEach { flerePerioder ->
            trueFalse.forEach { flereLovhjemler ->
                trueFalse.forEach { medVerge ->
                    trueFalse.forEach { feilutbetaltBeløpBortfalt ->
                        lagVedtakSluttTekster(
                            ytelsetype,
                            språkkode,
                            resultatType,
                            flerePerioder,
                            medSkattetrekk,
                            flereLovhjemler,
                            medVerge,
                            feilutbetaltBeløpBortfalt,
                            false,
                        )
                        if (Vedtaksresultat.INGEN_TILBAKEBETALING != resultatType) {
                            lagVedtakSluttTekster(
                                ytelsetype,
                                språkkode,
                                resultatType,
                                flerePerioder,
                                medSkattetrekk,
                                flereLovhjemler,
                                medVerge,
                                feilutbetaltBeløpBortfalt,
                                true,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun lagVedtakSluttTekster(
        ytelsetype: Ytelsestype,
        språkkode: Språkkode,
        resultatType: Vedtaksresultat,
        flerePerioder: Boolean,
        medSkattetrekk: Boolean,
        flereLovhjemler: Boolean,
        medVerge: Boolean,
        feilutbetaltBeløpBortfalt: Boolean,
        erRevurdering: Boolean,
    ) {
        val felles: HbVedtaksbrevFelles = lagFellesdel(
            ytelsetype,
            språkkode,
            resultatType,
            medSkattetrekk,
            flereLovhjemler,
            medVerge,
            feilutbetaltBeløpBortfalt,
            erRevurdering,
        )
        val perioder: List<HbVedtaksbrevsperiode> = lagPerioder(flerePerioder)
        val sluttTekst: String = FellesTekstformaterer.lagDeltekst(HbVedtaksbrevsdata(felles, perioder), VEDTAK_SLUTT)
        println()
        println(overskrift(flerePerioder, medSkattetrekk, flereLovhjemler, medVerge, feilutbetaltBeløpBortfalt, erRevurdering))
        println(prettyprint(sluttTekst))
    }

    private fun lagFellesdel(
        ytelsetype: Ytelsestype,
        språkkode: Språkkode,
        vedtakResultatType: Vedtaksresultat,
        medSkattetrekk: Boolean,
        flereLovhjemler: Boolean,
        medVerge: Boolean,
        feilutbetaltBeløpBortfalt: Boolean,
        erRevurdering: Boolean,
    ): HbVedtaksbrevFelles {
        return HbVedtaksbrevFelles(
            brevmetadata = lagMetadata(ytelsetype, språkkode, medVerge),
            fagsaksvedtaksdato = LocalDate.now(),
            totalresultat =
            HbTotalresultat(
                hovedresultat = vedtakResultatType,
                totaltTilbakekrevesBeløp = BigDecimal.valueOf(1000),
                totaltTilbakekrevesBeløpMedRenter = BigDecimal.valueOf(1100),
                totaltTilbakekrevesBeløpMedRenterUtenSkatt =
                BigDecimal.valueOf(if (medSkattetrekk) 900 else 1100.toLong()),
                totaltRentebeløp = BigDecimal.valueOf(100),
            ),
            totaltFeilutbetaltBeløp = BigDecimal.valueOf(1000),
            hjemmel = if (flereLovhjemler) {
                HbHjemmel("<lovhjemler her>", true)
            } else {
                HbHjemmel("<lovhjemmel her>")
            },
            varsel = HbVarsel(
                varsletBeløp = BigDecimal.valueOf(1000),
                varsletDato = LocalDate.of(2020, 4, 4),
            ),
            konfigurasjon = HbKonfigurasjon(klagefristIUker = 4),
            ansvarligBeslutter = "<Beslutters navn>",
            søker = HbPerson(navn = "<Søkers navn>"),
            vedtaksbrevstype = if (feilutbetaltBeløpBortfalt) {
                Vedtaksbrevstype.FRITEKST_FEILUTBETALING_BORTFALT
            } else {
                Vedtaksbrevstype.ORDINÆR
            },
            behandling = HbBehandling(
                erRevurdering = erRevurdering,
                originalBehandlingsdatoFagsakvedtak = if (erRevurdering) {
                    PERIODE1.fom
                } else {
                    null
                },
            ),
        )
    }

    private fun lagMetadata(
        ytelsestype: Ytelsestype,
        språkkode: Språkkode,
        medVerge: Boolean,
    ): Brevmetadata {
        val annenMottagersNavn = if (medVerge) "<annen mottaker>" else null
        return Brevmetadata(
            sakspartId = "",
            sakspartsnavn = "<Søkers navn>",
            finnesVerge = medVerge,
            vergenavn = "<annen mottaker>",
            mottageradresse = Adresseinfo("01020312345", "<Søkers navn>", annenMottagersNavn),
            behandlendeEnhetsNavn = "<Behandlende enhets navn>",
            ansvarligSaksbehandler = "<Saksbehandlers navn>",
            saksnummer = "1232456",
            språkkode = språkkode,
            ytelsestype = ytelsestype,
            gjelderDødsfall = false,
        )
    }

    private fun lagPerioder(flerePerioder: Boolean): List<HbVedtaksbrevsperiode> {
        if (flerePerioder) {
            return listOf(lagPeriode(PERIODE1), lagPeriode(PERIODE2))
        }
        return listOf(lagPeriode(PERIODE1))
    }

    private fun lagPeriode(periode: Datoperiode): HbVedtaksbrevsperiode {
        return HbVedtaksbrevsperiode(
            periode = periode,
            kravgrunnlag = HbKravgrunnlag(feilutbetaltBeløp = BigDecimal.valueOf(1000)),
            fakta = HbFakta(Hendelsestype.ANNET, Hendelsesundertype.ANNET_FRITEKST),
            vurderinger = HbVurderinger(
                foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                vilkårsvurderingsresultat =
                Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                aktsomhetsresultat = Aktsomhet.SIMPEL_UAKTSOMHET,
                særligeGrunner = HbSærligeGrunner(emptyList(), null, null),
            ),
            resultat = HbResultat(
                tilbakekrevesBeløp = BigDecimal.valueOf(1000),
                tilbakekrevesBeløpUtenSkattMedRenter = BigDecimal.valueOf(800),
                rentebeløp = BigDecimal.ZERO,
            ),
            førstePeriode = true,
        )
    }

    private fun overskrift(
        flerePerioder: Boolean,
        medSkattetrekk: Boolean,
        flereLovhjemler: Boolean,
        medVerge: Boolean,
        feilutbetaltBeløpBortfalt: Boolean,
        erRevurdering: Boolean,
    ): String {
        return (
            "*[ " + (if (flerePerioder) "flere perioder" else "en periode") +
                " - " + (if (medSkattetrekk) "med skattetrekk" else "uten skattetrekk") +
                " - " + (if (flereLovhjemler) "flere lovhjemmel" else "en lovhjemmel") +
                " - " + (if (medVerge) "med verge" else "uten verge") +
                " - " + (if (feilutbetaltBeløpBortfalt) "feilutbetalt beløp bortfalt" else "ordinær") +
                (if (erRevurdering) " - revurdering" else "") +
                " ]*"
            )
    }

    private fun prettyprint(s: String): String {
        return s.replace("{venstrejustert}", "")
            .replace("{høyrejustert}", "\t\t")
            .replace("4 uker", "<klagefrist> uker")
            .replace("4 veker", "<klagefrist> veker")
            .replace("(_.+)".toRegex(), "\n*$1*")
    }

    companion object {

        private val PERIODE1 = Datoperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 1))
        private val PERIODE2 = Datoperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 1))
        private val trueFalse = booleanArrayOf(true, false)
        const val VEDTAK_SLUTT = "vedtak/vedtak_slutt"
    }
}
