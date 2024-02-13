package no.nav.familie.tilbake.avstemming

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class Rad(
    val avsender: String,
    val vedtakId: String,
    val fnr: String,
    val vedtaksdato: LocalDate,
    val fagsakYtelseType: Ytelsestype,
    val tilbakekrevesBruttoUtenRenter: BigDecimal,
    val skatt: BigDecimal,
    val tilbakekrevesNettoUtenRenter: BigDecimal,
    val renter: BigDecimal,
    val erOmgjøringTilIngenTilbakekreving: Boolean = false,
) {

    fun toCsvString(): String {
        return (
            format(avsender) +
                SKILLETEGN_KOLONNER + format(vedtakId) +
                SKILLETEGN_KOLONNER + format(fnr) +
                SKILLETEGN_KOLONNER + format(vedtaksdato) +
                SKILLETEGN_KOLONNER + format(fagsakYtelseType) +
                SKILLETEGN_KOLONNER + format(tilbakekrevesBruttoUtenRenter) +
                SKILLETEGN_KOLONNER + format(skatt) +
                SKILLETEGN_KOLONNER + format(tilbakekrevesNettoUtenRenter) +
                SKILLETEGN_KOLONNER + format(renter) +
                SKILLETEGN_KOLONNER + formatOmgjøring(erOmgjøringTilIngenTilbakekreving)
            )
    }

    private fun format(verdi: String): String {
        return verdi
    }

    private fun format(dato: LocalDate): String {
        return dato.format(DATOFORMAT)
    }

    private fun format(kode: Ytelsestype): String {
        return format(kode.kode)
    }

    private fun format(verdi: BigDecimal): String {
        return verdi.setScale(0, RoundingMode.UNNECESSARY).toPlainString()
    }

    private fun formatOmgjøring(verdi: Boolean): String {
        return if (verdi) "Omgjoring0" else ""
    }

    companion object {

        const val SKILLETEGN_KOLONNER = ";"
        private val DATOFORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
