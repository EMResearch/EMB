package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType

/**
 * Data class for å gruppere andeler per ident og [YtelseType], sånn at man kan lage kjeder per ident/type
 */
data class IdentOgYtelse(
    val ident: String,
    val type: YtelseType,
)
