package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevsdata
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbVurderinger
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype

object Vedtaksbrevsfritekst {

    private const val FRITEKST_MARKERING_START = "\\\\FRITEKST_START"
    private const val FRITEKST_PÅKREVET_MARKERING_START = "\\\\PÅKREVET_FRITEKST_START"
    private const val FRITEKST_MARKERING_SLUTT = "\\\\FRITEKST_SLUTT"

    fun settInnMarkeringForFritekst(vedtaksbrevsdata: HbVedtaksbrevsdata): HbVedtaksbrevsdata {
        val perioder = vedtaksbrevsdata.perioder.map { periode ->
            val fritekstTypeForFakta = utledFritekstTypeFakta(periode.fakta.hendelsesundertype)
            val fakta = periode.fakta.copy(
                fritekstFakta = markerFritekst(
                    fritekstTypeForFakta,
                    periode.fakta.fritekstFakta,
                    Underavsnittstype.FAKTA,
                ),
            )
            val vurderinger: HbVurderinger =
                periode.vurderinger
                    .copy(
                        fritekstForeldelse = markerValgfriFritekst(
                            periode.vurderinger.fritekstForeldelse,
                            Underavsnittstype.FORELDELSE,
                        ),
                        fritekst = markerValgfriFritekst(
                            periode.vurderinger.fritekst,
                            Underavsnittstype.VILKÅR,
                        ),
                        særligeGrunner = periode.vurderinger.særligeGrunner
                            ?.copy(
                                fritekst = markerValgfriFritekst(
                                    periode.vurderinger.særligeGrunner.fritekst,
                                    Underavsnittstype.SÆRLIGEGRUNNER,
                                ),
                                fritekstAnnet = markerPåkrevetFritekst(
                                    periode.vurderinger
                                        .særligeGrunner
                                        .fritekstAnnet,
                                    Underavsnittstype.SÆRLIGEGRUNNER_ANNET,
                                ),
                            ),
                    )
            periode.copy(
                fakta = fakta,
                vurderinger = vurderinger,
            )
        }

        val fritekstType = utledFritekstTypeForOppsummering(vedtaksbrevsdata)
        val felles = vedtaksbrevsdata.felles
            .copy(fritekstoppsummering = markerFritekst(fritekstType, vedtaksbrevsdata.felles.fritekstoppsummering, null))
        return vedtaksbrevsdata.copy(
            felles = felles,
            perioder = perioder,
        )
    }

    private fun utledFritekstTypeForOppsummering(vedtaksbrevsdata: HbVedtaksbrevsdata): FritekstType {
        val hbBehandling = vedtaksbrevsdata.felles.behandling
        return if (hbBehandling.erRevurdering && !hbBehandling.erRevurderingEtterKlage) {
            FritekstType.PÅKREVET
        } else {
            FritekstType.VALGFRI
        }
    }

    private fun utledFritekstTypeFakta(underType: Hendelsesundertype): FritekstType {
        return if (underType in VedtaksbrevFritekstKonfigurasjon.UNDERTYPER_MED_PÅKREVD_FRITEKST) {
            FritekstType.PÅKREVET
        } else {
            FritekstType.VALGFRI
        }
    }

    @JvmOverloads fun markerValgfriFritekst(
        fritekst: String?,
        underavsnittstype: Underavsnittstype? = null,
    ): String {
        return markerFritekst(FritekstType.VALGFRI, fritekst, underavsnittstype)
    }

    fun markerPåkrevetFritekst(fritekst: String?, underavsnittstype: Underavsnittstype?): String {
        return markerFritekst(FritekstType.PÅKREVET, fritekst, underavsnittstype)
    }

    private fun markerFritekst(
        fritekstType: FritekstType,
        fritekst: String?,
        underavsnittstype: Underavsnittstype?,
    ): String {
        val fritekstTypeMarkør =
            if (fritekstType == FritekstType.PÅKREVET) FRITEKST_PÅKREVET_MARKERING_START else FRITEKST_MARKERING_START
        val startmarkør = if (underavsnittstype == null) fritekstTypeMarkør else fritekstTypeMarkør + underavsnittstype
        return if (fritekst == null) {
            "\n$startmarkør\n$FRITEKST_MARKERING_SLUTT"
        } else {
            "\n$startmarkør\n$fritekst\n$FRITEKST_MARKERING_SLUTT"
        }
    }

    fun erFritekstStart(tekst: String): Boolean {
        return tekst.startsWith(FRITEKST_MARKERING_START) || tekst.startsWith(FRITEKST_PÅKREVET_MARKERING_START)
    }

    fun erFritekstPåkrevetStart(tekst: String): Boolean {
        return tekst.startsWith(FRITEKST_PÅKREVET_MARKERING_START)
    }

    fun fjernFritekstmarkering(tekst: String): String {
        return when {
            tekst.startsWith(FRITEKST_MARKERING_START) -> tekst.substring(FRITEKST_MARKERING_START.length)
            tekst.startsWith(FRITEKST_PÅKREVET_MARKERING_START) -> tekst.substring(FRITEKST_PÅKREVET_MARKERING_START.length)
            else -> throw IllegalArgumentException("Utvikler-feil: denne metoden skal bare brukes på fritekstmarkering-start")
        }
    }

    fun erFritekstSlutt(tekst: String): Boolean {
        return FRITEKST_MARKERING_SLUTT == tekst
    }

    enum class FritekstType {
        VALGFRI,
        PÅKREVET,
    }
}
