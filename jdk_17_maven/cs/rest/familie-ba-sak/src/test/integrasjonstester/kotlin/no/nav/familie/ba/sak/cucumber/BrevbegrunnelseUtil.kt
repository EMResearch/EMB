package no.nav.familie.ba.sak.cucumber

import io.cucumber.datatable.DataTable
import no.nav.familie.ba.sak.cucumber.domeneparser.BrevPeriodeParser
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser
import no.nav.familie.ba.sak.cucumber.domeneparser.norskDatoFormatter
import no.nav.familie.ba.sak.cucumber.domeneparser.parseBoolean
import no.nav.familie.ba.sak.cucumber.domeneparser.parseEnum
import no.nav.familie.ba.sak.cucumber.domeneparser.parseInt
import no.nav.familie.ba.sak.cucumber.domeneparser.parseString
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriBoolean
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriEnum
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriString
import no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent.SøkersRettTilUtvidet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseMedData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.EØSBegrunnelseData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.EØSBegrunnelseDataMedKompetanse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.EØSBegrunnelseDataUtenKompetanse
import java.time.LocalDate

typealias Tabellrad = Map<String, String>

enum class Begrunnelsetype {
    EØS,
    STANDARD,
}

fun parseBegrunnelser(dataTable: DataTable): List<BegrunnelseMedData> {
    return dataTable.asMaps().map { rad: Tabellrad ->

        val type = parseValgfriEnum<Begrunnelsetype>(
            BrevPeriodeParser.DomenebegrepBrevBegrunnelse.TYPE,
            rad,
        ) ?: Begrunnelsetype.STANDARD

        when (type) {
            Begrunnelsetype.STANDARD -> parseStandardBegrunnelse(rad)
            Begrunnelsetype.EØS -> parseEøsBegrunnelse(rad)
        }
    }
}

fun parseStandardBegrunnelse(rad: Tabellrad) =
    BegrunnelseData(
        vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET,
        apiNavn = parseEnum<Standardbegrunnelse>(
            BrevPeriodeParser.DomenebegrepBrevBegrunnelse.BEGRUNNELSE,
            rad,
        ).sanityApiNavn,

        gjelderSoker = parseBoolean(BrevPeriodeParser.DomenebegrepBrevBegrunnelse.GJELDER_SØKER, rad),
        barnasFodselsdatoer = parseValgfriString(
            BrevPeriodeParser.DomenebegrepBrevBegrunnelse.BARNAS_FØDSELSDATOER,
            rad,
        ) ?: "",

        fodselsdatoerBarnOppfyllerTriggereOgHarUtbetaling = "",
        fodselsdatoerBarnOppfyllerTriggereOgHarNullutbetaling = "",

        antallBarn = parseInt(BrevPeriodeParser.DomenebegrepBrevBegrunnelse.ANTALL_BARN, rad),

        antallBarnOppfyllerTriggereOgHarUtbetaling = 0,
        antallBarnOppfyllerTriggereOgHarNullutbetaling = 0,

        maanedOgAarBegrunnelsenGjelderFor = parseString(
            BrevPeriodeParser.DomenebegrepBrevBegrunnelse.MÅNED_OG_ÅR_BEGRUNNELSEN_GJELDER_FOR,
            rad,
        ),
        maalform = parseEnum<Målform>(BrevPeriodeParser.DomenebegrepBrevBegrunnelse.MÅLFORM, rad).tilSanityFormat(),
        belop = parseString(BrevPeriodeParser.DomenebegrepBrevBegrunnelse.BELØP, rad).replace(' ', ' '),
        soknadstidspunkt = parseValgfriString(
            BrevPeriodeParser.DomenebegrepBrevBegrunnelse.SØKNADSTIDSPUNKT,
            rad,
        ) ?: "",
        avtaletidspunktDeltBosted = parseValgfriString(
            BrevPeriodeParser.DomenebegrepBrevBegrunnelse.AVTALETIDSPUNKT_DELT_BOSTED,
            rad,
        ) ?: "",
        sokersRettTilUtvidet = parseValgfriEnum<SøkersRettTilUtvidet>(
            BrevPeriodeParser.DomenebegrepBrevBegrunnelse.SØKERS_RETT_TIL_UTVIDET,
            rad,
        )?.tilSanityFormat() ?: SøkersRettTilUtvidet.SØKER_HAR_IKKE_RETT.tilSanityFormat(),
    )

fun parseEøsBegrunnelse(rad: Tabellrad): EØSBegrunnelseData {
    val gjelderSoker = parseValgfriBoolean(BrevPeriodeParser.DomenebegrepBrevBegrunnelse.GJELDER_SØKER, rad)

    val annenForeldersAktivitet = parseValgfriEnum<KompetanseAktivitet>(
        VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.ANNEN_FORELDERS_AKTIVITET,
        rad,
    )
    val annenForeldersAktivitetsland = parseValgfriString(
        VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.ANNEN_FORELDERS_AKTIVITETSLAND,
        rad,
    )
    val barnetsBostedsland = parseValgfriString(
        VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.BARNETS_BOSTEDSLAND,
        rad,
    )
    val søkersAktivitet = parseValgfriEnum<KompetanseAktivitet>(
        VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.SØKERS_AKTIVITET,
        rad,
    )
    val søkersAktivitetsland = parseValgfriString(
        VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.SØKERS_AKTIVITETSLAND,
        rad,
    )

    val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET

    val apiNavn = parseEnum<EØSStandardbegrunnelse>(
        BrevPeriodeParser.DomenebegrepBrevBegrunnelse.BEGRUNNELSE,
        rad,
    ).sanityApiNavn

    val barnasFodselsdatoer = parseString(
        BrevPeriodeParser.DomenebegrepBrevBegrunnelse.BARNAS_FØDSELSDATOER,
        rad,
    )

    val antallBarn = parseInt(BrevPeriodeParser.DomenebegrepBrevBegrunnelse.ANTALL_BARN, rad)

    val målform = parseEnum<Målform>(BrevPeriodeParser.DomenebegrepBrevBegrunnelse.MÅLFORM, rad).tilSanityFormat()

    return if (gjelderSoker == null) {
        if (annenForeldersAktivitet == null ||
            annenForeldersAktivitetsland == null ||
            barnetsBostedsland == null ||
            søkersAktivitet == null ||
            søkersAktivitetsland == null
        ) {
            error("For EØS-begrunnelser må enten 'Gjelder søker' eller kompetansefeltene settes")
        }

        EØSBegrunnelseDataMedKompetanse(
            vedtakBegrunnelseType = VedtakBegrunnelseType.EØS_INNVILGET,
            apiNavn = apiNavn,
            barnasFodselsdatoer = barnasFodselsdatoer,
            antallBarn = antallBarn,
            maalform = målform,

            annenForeldersAktivitet = annenForeldersAktivitet,
            annenForeldersAktivitetsland = annenForeldersAktivitetsland,
            barnetsBostedsland = barnetsBostedsland,
            sokersAktivitet = søkersAktivitet,
            sokersAktivitetsland = søkersAktivitetsland,
        )
    } else {
        EØSBegrunnelseDataUtenKompetanse(
            vedtakBegrunnelseType = vedtakBegrunnelseType,
            apiNavn = apiNavn,
            barnasFodselsdatoer = barnasFodselsdatoer,

            antallBarn = antallBarn,
            maalform = målform,
            gjelderSoker = gjelderSoker,
        )
    }
}

fun parseNullableDato(fom: String) = if (fom.uppercase() in listOf("NULL", "-", "")) {
    null
} else {
    LocalDate.parse(
        fom,
        norskDatoFormatter,
    )
}
