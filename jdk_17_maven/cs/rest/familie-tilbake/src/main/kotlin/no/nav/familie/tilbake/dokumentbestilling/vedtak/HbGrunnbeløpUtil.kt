package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.common.DatoUtil
import no.nav.familie.tilbake.common.Grunnbeløp
import no.nav.familie.tilbake.common.Grunnbeløpsperioder
import no.nav.familie.tilbake.dokumentbestilling.handlebars.KroneFormattererMedTusenskille
import no.nav.familie.tilbake.dokumentbestilling.handlebars.KroneFormattererMedTusenskille.Companion.utf8nonBreakingSpace
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbGrunnbeløp
import java.math.BigDecimal

object HbGrunnbeløpUtil {

    fun lagHbGrunnbeløp(periode: Månedsperiode): HbGrunnbeløp {
        val grunnbeløpsperioder = Grunnbeløpsperioder.finnGrunnbeløpsperioder(periode)

        val formattertPerioder = grunnbeløpsperioder.map {
            formatterGrunnbeløp(it, periode)
        }

        return if (grunnbeløpsperioder.size > 1) {
            val kommaSeparertePerioder = formattertPerioder.dropLast(1).joinToString(", ")
            HbGrunnbeløp(
                null,
                "$kommaSeparertePerioder og ${formattertPerioder.last()}",
            )
        } else {
            HbGrunnbeløp(grunnbeløpX6(grunnbeløpsperioder.single()), null)
        }
    }

    private fun formatterGrunnbeløp(grunnbeløp: Grunnbeløp, periode: Månedsperiode): String {
        val format = DatoUtil.DATO_FORMAT_DATO_MÅNEDSNAVN_ÅR
        val snitt = grunnbeløp.periode.snitt(periode) ?: error("Finner ikke snitt for ${grunnbeløp.periode} og $periode")

        return "${formatterBeløpX6(grunnbeløp)} for perioden ${format.format(snitt.fomDato)} " +
            "til ${format.format(snitt.tomDato)}"
    }

    private fun formatterBeløpX6(grunnbeløp: Grunnbeløp): String {
        val grunnbeløpX6 = grunnbeløpX6(grunnbeløp)
        return KroneFormattererMedTusenskille.formatterKronerMedTusenskille(grunnbeløpX6, utf8nonBreakingSpace)
    }

    private fun grunnbeløpX6(grunnbeløp: Grunnbeløp) =
        grunnbeløp.grunnbeløp.multiply(BigDecimal.valueOf(6L))
}
