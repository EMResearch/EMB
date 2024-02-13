package no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode

import java.math.BigDecimal

object HbResultatTestBuilder {

    fun forTilbakekrevesBeløp(tilbakekrevesBeløp: Int): HbResultat {
        return forTilbakekrevesBeløpOgRenter(tilbakekrevesBeløp, 0)
    }

    fun forTilbakekrevesBeløpOgRenter(tilbakekrevesBeløp: Int, renter: Int): HbResultat {
        return HbResultat(
            tilbakekrevesBeløp = BigDecimal.valueOf(tilbakekrevesBeløp.toLong()),
            rentebeløp = BigDecimal.valueOf(renter.toLong()),
            tilbakekrevesBeløpUtenSkattMedRenter = BigDecimal.valueOf(tilbakekrevesBeløp.toLong()),
        )
    }
}
