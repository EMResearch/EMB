package no.nav.familie.ba.sak.ekstern.restDomene

import java.time.LocalDate

data class RestRefusjonEøs(
    val id: Long?,
    val fom: LocalDate,
    val tom: LocalDate,
    val refusjonsbeløp: Int,
    val land: String,
    val refusjonAvklart: Boolean,
)
