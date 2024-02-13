package no.nav.familie.tilbake.avstemming

import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsvedtakDto
import java.math.BigDecimal

class TilbakekrevingsvedtakOppsummering(
    val økonomivedtakId: String,
    val tilbakekrevesBruttoUtenRenter: BigDecimal,
    val tilbakekrevesNettoUtenRenter: BigDecimal,
    val renter: BigDecimal,
    val skatt: BigDecimal,
) {

    fun harIngenTilbakekreving(): Boolean {
        return tilbakekrevesBruttoUtenRenter.signum() == 0
    }

    companion object {

        fun oppsummer(tilbakekrevingsvedtak: TilbakekrevingsvedtakDto): TilbakekrevingsvedtakOppsummering {
            var bruttoUtenRenter = BigDecimal.ZERO
            var renter = BigDecimal.ZERO
            var skatt = BigDecimal.ZERO
            for (periode in tilbakekrevingsvedtak.tilbakekrevingsperiode) {
                renter = renter.add(periode.belopRenter)
                for (beløp in periode.tilbakekrevingsbelop) {
                    bruttoUtenRenter = bruttoUtenRenter.add(beløp.belopTilbakekreves)
                    skatt = skatt.add(beløp.belopSkatt)
                }
            }
            return TilbakekrevingsvedtakOppsummering(
                renter = renter,
                skatt = skatt,
                tilbakekrevesBruttoUtenRenter = bruttoUtenRenter,
                tilbakekrevesNettoUtenRenter = bruttoUtenRenter.subtract(skatt),
                økonomivedtakId = tilbakekrevingsvedtak.vedtakId.toString(),
            )
        }
    }
}
