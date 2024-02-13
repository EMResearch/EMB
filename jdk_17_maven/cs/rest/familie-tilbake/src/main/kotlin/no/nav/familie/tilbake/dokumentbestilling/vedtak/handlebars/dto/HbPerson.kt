package no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto

import java.time.LocalDate

@Suppress("unused") // Handlebars
class HbPerson(
    private val navn: String,
    private val dødsdato: LocalDate? = null,
)
