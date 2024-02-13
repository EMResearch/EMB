package no.nav.familie.tilbake.dokumentbestilling.vedtak

import java.time.LocalDate

enum class Avsnittstype {
    OPPSUMMERING,
    PERIODE,
    TILLEGGSINFORMASJON,
}

data class Avsnitt(
    val overskrift: String? = null,
    val underavsnittsliste: List<Underavsnitt> = listOf(),
    val avsnittstype: Avsnittstype? = null,
    val fom: LocalDate? = null,
    val tom: LocalDate? = null,
)

class Underavsnitt(
    val overskrift: String? = null,
    val brødtekst: String? = null,
    val fritekst: String? = null,
    val fritekstTillatt: Boolean = false,
    val fritekstPåkrevet: Boolean = false,
    val underavsnittstype: Underavsnittstype? = null,
) {

    init {
        require(!(!fritekstTillatt && fritekstPåkrevet)) { "Det gir ikke mening at fritekst er påkrevet når fritekst ikke er tillatt" }
    }
}

enum class Underavsnittstype {
    FAKTA,
    FORELDELSE,
    VILKÅR,
    SÆRLIGEGRUNNER,
    SÆRLIGEGRUNNER_ANNET,
}
