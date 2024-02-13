package no.nav.familie.ba.sak.ekstern.skatteetaten

import java.time.LocalDateTime

interface AndelTilkjentYtelsePeriode {

    fun getId(): Long

    fun getIdent(): String

    fun getFom(): LocalDateTime

    fun getTom(): LocalDateTime

    fun getProsent(): String

    fun getEndretDato(): LocalDateTime

    fun getBehandlingId(): Long
}
