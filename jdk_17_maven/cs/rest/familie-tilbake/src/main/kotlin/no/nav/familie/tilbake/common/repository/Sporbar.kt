package no.nav.familie.tilbake.common.repository

import no.nav.familie.tilbake.common.ContextService
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class Sporbar(
    val opprettetAv: String = ContextService.hentSaksbehandler(),
    val opprettetTid: LocalDateTime = SporbarUtils.now(),
    @LastModifiedBy
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val endret: Endret = Endret(),
)

data class Endret(
    val endretAv: String = ContextService.hentSaksbehandler(),
    val endretTid: LocalDateTime = SporbarUtils.now(),
)

object SporbarUtils {

    fun now(): LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)
}
