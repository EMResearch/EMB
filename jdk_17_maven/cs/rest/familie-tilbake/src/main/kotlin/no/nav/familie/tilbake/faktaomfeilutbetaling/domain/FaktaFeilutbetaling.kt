package no.nav.familie.tilbake.faktaomfeilutbetaling.domain

import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import java.util.UUID

data class FaktaFeilutbetaling(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val aktiv: Boolean = true,
    val begrunnelse: String?,
    @MappedCollection(idColumn = "fakta_feilutbetaling_id")
    val perioder: Set<FaktaFeilutbetalingsperiode> = setOf(),
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)
