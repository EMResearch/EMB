package no.nav.familie.ba.sak.ekstern.restDomene

abstract class AbstractUtfyltStatus<T> {
    abstract val status: UtfyltStatus
    abstract fun medUtfyltStatus(): T

    fun finnAntallUtfylt(felter: Collection<Any?>): Int {
        return felter.fold(0) { antallUtfylte, felt -> antallUtfylte + (felt?.let { 1 } ?: 0) }
    }

    fun utfyltStatus(antallUtfylt: Int, antallFelter: Int): UtfyltStatus {
        return when (antallUtfylt) {
            antallFelter -> UtfyltStatus.OK
            in 1 until antallFelter -> UtfyltStatus.UFULLSTENDIG
            else -> UtfyltStatus.IKKE_UTFYLT
        }
    }
}

enum class UtfyltStatus {
    IKKE_UTFYLT,
    UFULLSTENDIG,
    OK,
}
