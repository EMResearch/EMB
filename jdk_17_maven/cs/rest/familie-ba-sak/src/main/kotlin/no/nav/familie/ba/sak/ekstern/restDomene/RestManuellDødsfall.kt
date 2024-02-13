package no.nav.familie.ba.sak.ekstern.restDomene

import java.time.LocalDate

data class RestManuellDødsfall(
    val dødsfallDato: LocalDate,
    val personIdent: String,
    val begrunnelse: String,
)
