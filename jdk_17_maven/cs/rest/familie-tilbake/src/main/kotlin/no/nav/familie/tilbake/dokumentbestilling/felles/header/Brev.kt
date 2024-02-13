package no.nav.familie.tilbake.dokumentbestilling.felles.header

import java.time.LocalDate

class Brev(
    val overskrift: String?,
    val dato: LocalDate = LocalDate.now(),
)
