package no.nav.familie.ba.sak.cucumber.domeneparser

import io.cucumber.datatable.DataTable
import no.nav.familie.ba.sak.cucumber.SammenlignbarBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk

object BrevBegrunnelseParser {

    fun mapBegrunnelser(dataTable: DataTable): List<SammenlignbarBegrunnelse> {
        return dataTable.asMaps().map { rad ->
            val regelverkForInkluderteBegrunnelser =
                parseValgfriEnum<Regelverk>(DomenebegrepUtvidetVedtaksperiodeMedBegrunnelser.REGELVERK_INKLUDERTE_BEGRUNNELSER, rad)
                    ?: Regelverk.NASJONALE_REGLER

            val regelverkForEkskluderteBegrunnelser =
                parseValgfriEnum<Regelverk>(DomenebegrepUtvidetVedtaksperiodeMedBegrunnelser.REGELVERK_EKSKLUDERTE_BEGRUNNELSER, rad)
                    ?: regelverkForInkluderteBegrunnelser

            val inkluderteStandardBegrunnelser = hentForventedeBegrunnelser(
                regelverkForInkluderteBegrunnelser,
                DomenebegrepUtvidetVedtaksperiodeMedBegrunnelser.INKLUDERTE_BEGRUNNELSER,
                rad,
            )
            val ekskluderteStandardBegrunnelser = hentForventedeBegrunnelser(
                regelverkForEkskluderteBegrunnelser,
                DomenebegrepUtvidetVedtaksperiodeMedBegrunnelser.EKSKLUDERTE_BEGRUNNELSER,
                rad,
            )

            SammenlignbarBegrunnelse(
                fom = parseValgfriDato(Domenebegrep.FRA_DATO, rad),
                tom = parseValgfriDato(Domenebegrep.TIL_DATO, rad),
                type = parseEnum(DomenebegrepUtvidetVedtaksperiodeMedBegrunnelser.VEDTAKSPERIODE_TYPE, rad),
                inkluderteStandardBegrunnelser = inkluderteStandardBegrunnelser,
                ekskluderteStandardBegrunnelser = ekskluderteStandardBegrunnelser,
            )
        }
    }

    private fun hentForventedeBegrunnelser(
        vurderesEtter: Regelverk,
        inkludertEllerEkskludert: DomenebegrepUtvidetVedtaksperiodeMedBegrunnelser,
        rad: Map<String, String>,
    ): Set<IVedtakBegrunnelse> {
        return when (vurderesEtter) {
            Regelverk.NASJONALE_REGLER -> {
                parseEnumListe<Standardbegrunnelse>(
                    inkludertEllerEkskludert,
                    rad,
                ).toSet()
            }

            Regelverk.EØS_FORORDNINGEN -> {
                parseEnumListe<EØSStandardbegrunnelse>(
                    inkludertEllerEkskludert,
                    rad,
                ).toSet()
            }
        }
    }

    enum class DomenebegrepUtvidetVedtaksperiodeMedBegrunnelser(override val nøkkel: String) : Domenenøkkel {
        VEDTAKSPERIODE_TYPE("VedtaksperiodeType"),
        INKLUDERTE_BEGRUNNELSER("Inkluderte Begrunnelser"),
        EKSKLUDERTE_BEGRUNNELSER("Ekskluderte Begrunnelser"),
        REGELVERK_INKLUDERTE_BEGRUNNELSER("Regelverk Inkluderte Begrunnelser"),
        REGELVERK_EKSKLUDERTE_BEGRUNNELSER("Regelverk Ekskluderte Begrunnelser"),
    }
}
