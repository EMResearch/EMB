package no.nav.familie.ba.sak.ekstern.restDomene

import java.time.LocalDate

data class RestFeilutbetaltValuta(
    val id: Long?,
    val fom: LocalDate,
    val tom: LocalDate,
    val feilutbetaltBeløp: Int,
    val erPerMåned: Boolean? = null,
)
