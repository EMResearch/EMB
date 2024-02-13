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
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbVedtaksbrevsperiode
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbVurderinger
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.AnnenVurdering
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vurdering
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/**
 * Brukes for å generere vilkårtekster for perioder. Resultatet er tekster med markup, som med "Insert markup"-macroen
 * kan limes inn i Confluence, og dermed bli formattert tekst.
 *
 * Confluence:
 * https://confluence.adeo.no/display/TFA/Generert+dokumentasjon
 */
// @Disabled("Kjøres ved behov for å regenerere dokumentasjon")
class DokumentasjonsgeneratorPeriodeVilkår {

    @Test
    fun `generer vilkår for BA bokmål`() {
        lagVilkårstekster(Ytelsestype.BARNETRYGD, Språkkode.NB)
    }

    @Test
    fun `generer vilkår for BA nynorsk`() {
        lagVilkårstekster(Ytelsestype.BARNETRYGD, Språkkode.NN)
    }

    @Test
    fun `generer vilkår for EFOG bokmål`() {
        lagVilkårstekster(Ytelsestype.OVERGANGSSTØNAD, Språkkode.NB)
    }

    @Test
    fun `generer vilkår for EFOG nynorsk`() {
        lagVilkårstekster(Ytelsestype.OVERGANGSSTØNAD, Språkkode.NN)
    }

    @Test
    fun `generer vilkår for EFBT bokmål`() {
        lagVilkårstekster(Ytelsestype.BARNETILSYN, Språkkode.NB)
    }

    @Test
    fun `generer vilkår for EFBT nynorsk`() {
        lagVilkårstekster(Ytelsestype.BARNETILSYN, Språkkode.NN)
    }

    @Test
    fun `generer vilkår for EFSP bokmål`() {
        lagVilkårstekster(Ytelsestype.SKOLEPENGER, Språkkode.NB)
    }

    @Test
    fun `generer vilkår for EFSP nynorsk`() {
        lagVilkårstekster(Ytelsestype.SKOLEPENGER, Språkkode.NN)
    }

    private fun lagVilkårstekster(ytelsetype: Ytelsestype, språkkode: Språkkode) {
        vilkårResultat.forEach { resultat ->
            aktsomheter.forEach { vurdering ->
                foreldelseVurderinger.forEach { foreldelseVurdering ->
                    lagResultatOgVurderingTekster(
                        ytelsetype,
                        språkkode,
                        resultat,
                        vurdering,
                        foreldelseVurdering,
                        fritekst = false,
                        pengerIBehold = false,
                        lavtBeløp = false,
                    )
                    lagResultatOgVurderingTekster(
                        ytelsetype,
                        språkkode,
                        resultat,
                        vurdering,
                        foreldelseVurdering,
                        fritekst = true,
                        pengerIBehold = false,
                        lavtBeløp = false,
                    )
                    if (vurdering === Aktsomhet.SIMPEL_UAKTSOMHET) {
                        lagResultatOgVurderingTekster(
                            ytelsetype,
                            språkkode,
                            resultat,
                            vurdering,
                            foreldelseVurdering,
                            fritekst = false,
                            pengerIBehold = false,
                            lavtBeløp = true,
                        )
                    }
                }
            }
        }
        foreldelseVurderinger.forEach { foreldelseVurdering ->
            trueFalse.forEach { fritekst: Boolean ->
                trueFalse.forEach { pengerIBehold ->
                    lagResultatOgVurderingTekster(
                        ytelsetype,
                        språkkode,
                        Vilkårsvurderingsresultat.GOD_TRO,
                        AnnenVurdering.GOD_TRO,
                        foreldelseVurdering,
                        fritekst,
                        pengerIBehold,
                        lavtBeløp = false,
                    )
                }
            }
        }
        lagResultatOgVurderingTekster(
            ytelsetype,
            språkkode,
            Vilkårsvurderingsresultat.UDEFINERT,
            AnnenVurdering.FORELDET,
            Foreldelsesvurderingstype.FORELDET,
            fritekst = false,
            pengerIBehold = false,
            lavtBeløp = false,
        )
        lagResultatOgVurderingTekster(
            ytelsetype,
            språkkode,
            Vilkårsvurderingsresultat.UDEFINERT,
            AnnenVurdering.FORELDET,
            Foreldelsesvurderingstype.FORELDET,
            fritekst = true,
            pengerIBehold = false,
            lavtBeløp = false,
        )
    }

    private fun lagResultatOgVurderingTekster(
        ytelsetype: Ytelsestype,
        språkkode: Språkkode,
        resultat: Vilkårsvurderingsresultat,
        vurdering: Vurdering,
        foreldelsevurdering: Foreldelsesvurderingstype,
        fritekst: Boolean,
        pengerIBehold: Boolean,
        lavtBeløp: Boolean,
    ) {
        val periodeOgFelles = lagPeriodeOgFelles(
            ytelsetype,
            språkkode,
            resultat,
            vurdering,
            lavtBeløp,
            foreldelsevurdering,
            fritekst,
            pengerIBehold,
        )
        val vilkårTekst = lagVilkårTekst(periodeOgFelles)
        val overskrift = overskrift(resultat, vurdering, lavtBeløp, fritekst, pengerIBehold, foreldelsevurdering)
        val prettyprint = prettyprint(vilkårTekst, overskrift)
        println()
        println(prettyprint)
    }

    private fun lagVilkårTekst(periodeOgFelles: HbVedtaksbrevPeriodeOgFelles): String {
        if (periodeOgFelles.periode.vurderinger.harForeldelsesavsnitt) {
            return FellesTekstformaterer.lagDeltekst(periodeOgFelles, AvsnittUtil.PARTIAL_PERIODE_FORELDELSE) +
                System.lineSeparator() + System.lineSeparator() +
                FellesTekstformaterer.lagDeltekst(periodeOgFelles, AvsnittUtil.PARTIAL_PERIODE_VILKÅR)
        }
        return FellesTekstformaterer.lagDeltekst(periodeOgFelles, AvsnittUtil.PARTIAL_PERIODE_VILKÅR)
    }

    private fun lagPeriodeOgFelles(
        ytelsetype: Ytelsestype,
        språkkode: Språkkode,
        vilkårResultat: Vilkårsvurderingsresultat?,
        vurdering: Vurdering,
        lavtBeløp: Boolean,
        foreldelsevurdering: Foreldelsesvurderingstype,
        fritekst: Boolean,
        pengerIBehold: Boolean,
    ): HbVedtaksbrevPeriodeOgFelles {
        val fellesBuilder = lagFelles(ytelsetype, språkkode)

        val vurderinger =
            HbVurderinger(
                foreldelsevurdering = foreldelsevurdering,
                aktsomhetsresultat = vurdering,
                unntasInnkrevingPgaLavtBeløp = lavtBeløp,
                fritekst = if (fritekst) "[ fritekst her ]" else null,
                vilkårsvurderingsresultat = vilkårResultat,
                beløpIBehold = if (AnnenVurdering.GOD_TRO === vurdering) {
                    if (pengerIBehold) BigDecimal.valueOf(3999) else BigDecimal.ZERO
                } else {
                    null
                },
                foreldelsesfrist = if (foreldelsevurdering in setOf(
                        Foreldelsesvurderingstype.FORELDET,
                        Foreldelsesvurderingstype.TILLEGGSFRIST,
                    )
                ) {
                    FORELDELSESFRIST
                } else {
                    null
                },
                fritekstForeldelse = if (foreldelsevurdering in setOf(
                        Foreldelsesvurderingstype.FORELDET,
                        Foreldelsesvurderingstype.TILLEGGSFRIST,
                    ) &&
                    fritekst
                ) {
                    "[ fritekst her ]"
                } else {
                    null
                },
                oppdagelsesdato = if (Foreldelsesvurderingstype.TILLEGGSFRIST == foreldelsevurdering) {
                    OPPDAGELSES_DATO
                } else {
                    null
                },
            )

        val periodeBuilder =
            HbVedtaksbrevsperiode(
                periode = JANUAR,
                kravgrunnlag = HbKravgrunnlag(feilutbetaltBeløp = BigDecimal.ZERO),
                fakta = HbFakta(Hendelsestype.ANNET, Hendelsesundertype.ANNET_FRITEKST),
                vurderinger = vurderinger,
                resultat = HbResultat(
                    tilbakekrevesBeløp = BigDecimal.valueOf(9999),
                    rentebeløp = BigDecimal.ZERO,
                    tilbakekrevesBeløpUtenSkattMedRenter = BigDecimal.valueOf(9999),
                    foreldetBeløp = BigDecimal.valueOf(2999),
                ),
                førstePeriode = true,
            )
        return HbVedtaksbrevPeriodeOgFelles(fellesBuilder, periodeBuilder)
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
            totaltFeilutbetaltBeløp = BigDecimal.valueOf(6855),
            varsel = HbVarsel(
                varsletBeløp = BigDecimal.valueOf(10000),
                varsletDato = LocalDate.now().minusDays(100),
            ),
            konfigurasjon = HbKonfigurasjon(
                fireRettsgebyr = BigDecimal.valueOf(4321),
                klagefristIUker = 4,
            ),
            totalresultat = HbTotalresultat(
                hovedresultat = Vedtaksresultat.FULL_TILBAKEBETALING,
                totaltTilbakekrevesBeløp = BigDecimal.ZERO,
                totaltTilbakekrevesBeløpMedRenterUtenSkatt = BigDecimal.ZERO,
                totaltTilbakekrevesBeløpMedRenter = BigDecimal.ZERO,
                totaltRentebeløp = BigDecimal.ZERO,
            ),
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

    private fun overskrift(
        resultat: Vilkårsvurderingsresultat,
        vurdering: Vurdering?,
        lavtBeløp: Boolean,
        fritekst: Boolean,
        pengerIBehold: Boolean,
        foreldelsevurdering: Foreldelsesvurderingstype,
    ): String {
        return (
            "*[ ${hentVilkårresultatOverskriftDel(resultat)}" +
                (if (vurdering != null) " - " + vurdering.navn else "") +
                (if (fritekst) " - med fritekst" else " - uten fritekst") +
                hentVIlkårsvurderingOverskriftDel(foreldelsevurdering) +
                (if (pengerIBehold) " - penger i behold" else "") +
                (if (lavtBeløp) " - lavt beløp" else "") +
                " ]*"
            )
    }

    private fun prettyprint(vilkårTekst: String, overskrift: String): String {
        return vilkårTekst.replace("__.+".toRegex(), overskrift)
            .replace(" 4\u00A0321\u00A0kroner", " <4 rettsgebyr> kroner")
            .replace(" 2\u00A0999\u00A0kroner", " <foreldet beløp> kroner")
            .replace(" 3\u00A0999\u00A0kroner", " <beløp i behold> kroner")
            .replace("1. januar 2019", "<periode start>")
            .replace("31. januar 2019", "<periode slutt>")
            .replace("1. mars 2019", "<oppdagelsesdato>")
            .replace("1. desember 2019", "<foreldelsesfrist>")
    }

    companion object {

        private val vilkårResultat = arrayOf(
            Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
            Vilkårsvurderingsresultat.MANGELFULLE_OPPLYSNINGER_FRA_BRUKER,
            Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
        )
        private val foreldelseVurderinger = arrayOf(
            Foreldelsesvurderingstype.IKKE_VURDERT,
            Foreldelsesvurderingstype.IKKE_FORELDET,
            Foreldelsesvurderingstype.TILLEGGSFRIST,
        )
        private val aktsomheter = arrayOf(
            Aktsomhet.SIMPEL_UAKTSOMHET,
            Aktsomhet.GROV_UAKTSOMHET,
            Aktsomhet.FORSETT,
        )
        private val trueFalse = booleanArrayOf(true, false)
        private val JANUAR = Datoperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 1))
        private val FORELDELSESFRIST = LocalDate.of(2019, 12, 1)
        private val OPPDAGELSES_DATO = LocalDate.of(2019, 3, 1)
    }

    private fun hentVilkårresultatOverskriftDel(resultat: Vilkårsvurderingsresultat): String {
        return when (resultat) {
            Vilkårsvurderingsresultat.UDEFINERT -> "Foreldelse"
            Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT -> "Forsto/Burde forstått"
            Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER -> "Feilaktive opplysninger"
            Vilkårsvurderingsresultat.MANGELFULLE_OPPLYSNINGER_FRA_BRUKER -> "Mangelfull opplysninger"
            Vilkårsvurderingsresultat.GOD_TRO -> "God tro"
            else -> throw IllegalArgumentException("Vilkårsvurderingsresultat ikke støttet. Resultat: $resultat")
        }
    }

    private fun hentVIlkårsvurderingOverskriftDel(foreldelsevurdering: Foreldelsesvurderingstype): String {
        return when (foreldelsevurdering) {
            Foreldelsesvurderingstype.IKKE_VURDERT -> " - automatisk vurdert"
            Foreldelsesvurderingstype.IKKE_FORELDET -> " - ikke foreldet"
            Foreldelsesvurderingstype.FORELDET -> " - foreldet"
            Foreldelsesvurderingstype.TILLEGGSFRIST -> " - med tilleggsfrist"
            else -> throw IllegalArgumentException("Foreldelsesvurderingstype ikke støttet. Type: $foreldelsevurdering")
        }
    }
}
