package no.nav.familie.tilbake.micrometer.domain

import no.nav.familie.kontrakter.felles.Fagsystem
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.UUID

@Table
class Meldingstelling(
    @Id
    val id: UUID = UUID.randomUUID(),
    val fagsystem: Fagsystem,
    val type: Meldingstype,
    val status: Mottaksstatus,
    val antall: Int = 1,
    val dato: LocalDate = LocalDate.now(),
)

enum class Mottaksstatus {
    KOBLET,
    UKOBLET,
}

enum class Meldingstype {
    KRAVGRUNNLAG,
    STATUSMELDING,
}
