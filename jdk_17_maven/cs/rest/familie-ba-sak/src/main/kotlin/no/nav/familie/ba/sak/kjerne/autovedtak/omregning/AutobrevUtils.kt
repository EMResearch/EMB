package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse

object AutobrevUtils {
    fun hentStandardbegrunnelserReduksjonForAlder(alder: Int): List<Standardbegrunnelse> =
        when (alder) {
            Alder.SEKS.år -> listOf(
                Standardbegrunnelse.REDUKSJON_UNDER_6_ÅR_AUTOVEDTAK,
                Standardbegrunnelse.REDUKSJON_UNDER_6_ÅR,
            )
            Alder.ATTEN.år -> listOf(
                Standardbegrunnelse.REDUKSJON_UNDER_18_ÅR_AUTOVEDTAK,
                Standardbegrunnelse.REDUKSJON_UNDER_18_ÅR,
            )
            else -> throw Feil("Alder må være oppgitt til enten 6 eller 18 år.")
        }

    fun hentGjeldendeVedtakbegrunnelseReduksjonForAlder(alder: Int): Standardbegrunnelse =
        when (alder) {
            Alder.SEKS.år -> Standardbegrunnelse.REDUKSJON_UNDER_6_ÅR_AUTOVEDTAK
            Alder.ATTEN.år -> Standardbegrunnelse.REDUKSJON_UNDER_18_ÅR_AUTOVEDTAK
            else -> throw Feil("Alder må være oppgitt til enten 6 eller 18 år.")
        }
}
