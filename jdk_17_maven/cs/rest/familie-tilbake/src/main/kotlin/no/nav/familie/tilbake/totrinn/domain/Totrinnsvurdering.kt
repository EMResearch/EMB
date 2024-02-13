package no.nav.familie.tilbake.totrinn.domain

import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Totrinnsvurdering(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val behandlingssteg: Behandlingssteg,
    val godkjent: Boolean,
    val begrunnelse: String?,
    val aktiv: Boolean = true,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)
