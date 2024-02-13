package no.nav.familie.tilbake.dokumentbestilling.vedtak.domain

import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Vedtaksbrevsperiode(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val periode: Månedsperiode,
    val fritekst: String,
    val fritekststype: Friteksttype,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

enum class Friteksttype {
    FAKTA,
    FORELDELSE,
    VILKÅR,
    SÆRLIGE_GRUNNER,
    SÆRLIGE_GRUNNER_ANNET,
}
