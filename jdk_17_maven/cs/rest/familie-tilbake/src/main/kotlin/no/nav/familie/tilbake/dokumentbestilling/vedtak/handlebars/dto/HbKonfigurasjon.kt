package no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto

import no.nav.familie.tilbake.config.Constants
import java.math.BigDecimal

@Suppress("unused") // Handlebars
class HbKonfigurasjon(
    val fireRettsgebyr: BigDecimal = BigDecimal.valueOf(Constants.rettsgebyr * 4),
    val klagefristIUker: Int,
)
