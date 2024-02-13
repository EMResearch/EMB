package no.nav.familie.tilbake.dokumentbestilling.felles

import no.nav.familie.kontrakter.felles.dokdist.ManuellAdresse

class Adresseinfo(
    val ident: String,
    val mottagernavn: String,
    val annenMottagersNavn: String? = null,
    val manuellAdresse: ManuellAdresse? = null,
)
