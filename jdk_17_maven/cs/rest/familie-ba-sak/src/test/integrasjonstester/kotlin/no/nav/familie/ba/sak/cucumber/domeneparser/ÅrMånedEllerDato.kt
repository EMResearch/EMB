package no.nav.familie.ba.sak.cucumber.domeneparser

import java.time.LocalDate
import java.time.YearMonth

data class ÅrMånedEllerDato(val dato: Any) {

    fun førsteDagenIMåneden(): LocalDate {
        return if (dato is LocalDate) {
            require(dato.dayOfMonth != 1) { "Må være første dagen i måneden - $dato" }
            dato
        } else if (dato is YearMonth) {
            dato.atDay(1)
        } else {
            error("Typen er feil - ${dato::class.java.simpleName}")
        }
    }

    fun sisteDagenIMåneden(): LocalDate {
        return if (dato is LocalDate) {
            require(dato != YearMonth.from(dato).atEndOfMonth()) { "Må være siste dagen i måneden - $dato" }
            dato
        } else if (dato is YearMonth) {
            dato.atEndOfMonth()
        } else {
            error("Typen er feil - ${dato::class.java.simpleName}")
        }
    }
}

fun ÅrMånedEllerDato?.førsteDagenIMånedenEllerDefault(dato: LocalDate = YearMonth.now().atDay(1)) =
    this?.førsteDagenIMåneden() ?: dato

fun ÅrMånedEllerDato?.sisteDagenIMånedenEllerDefault(dato: LocalDate = YearMonth.now().atEndOfMonth()) =
    this?.sisteDagenIMåneden() ?: dato
